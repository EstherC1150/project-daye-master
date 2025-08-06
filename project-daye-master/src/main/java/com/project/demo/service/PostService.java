package com.project.demo.service;

import com.project.demo.entity.Post;
import com.project.demo.entity.User;
import com.project.demo.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private FileService fileService;
    
    public Page<Post> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Page<Post> getAllPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Post getPostById(Long id) {
        try {
            return postRepository.findById(id)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Post createPost(Post post, User author, MultipartFile videoFile) {
        post.setAuthor(author);
        post.setViewCount(0);
        
        // 동영상 파일 처리
        if (videoFile != null && !videoFile.isEmpty()) {
            try {
                String videoFilename = fileService.uploadVideo(videoFile);
                String thumbnailFilename = fileService.generateThumbnail(videoFilename);
                
                post.setVideoFilename(videoFilename);
                post.setVideoOriginalName(videoFile.getOriginalFilename());
                post.setVideoContentType(videoFile.getContentType());
                post.setVideoSize(videoFile.getSize());
                post.setThumbnailFilename(thumbnailFilename);
                
            } catch (Exception e) {
                // 업로드된 파일이 있다면 정리
                if (post.getVideoFilename() != null) {
                    fileService.deleteFile(post.getVideoFilename());
                }
                if (post.getThumbnailFilename() != null) {
                    fileService.deleteThumbnail(post.getThumbnailFilename());
                }
                
                throw new RuntimeException("동영상 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        }
        
        Post savedPost = postRepository.save(post);
        return savedPost;
    }
    
    public Post updatePost(Long id, Post updatedPost, User currentUser, MultipartFile videoFile) {
        Post existingPost = getPostById(id);
        
        // 작성자만 수정 가능
        if (!existingPost.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("게시글을 수정할 권한이 없습니다.");
        }
        
        // 기본 정보 업데이트
        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setContent(updatedPost.getContent());
        
        // 새로운 동영상 파일이 업로드된 경우
        if (videoFile != null && !videoFile.isEmpty()) {
            System.out.println("=== 동영상 업데이트 시작 ===");
            System.out.println("새 동영상 파일명: " + videoFile.getOriginalFilename());
            System.out.println("새 동영상 크기: " + videoFile.getSize());
            
            // 기존 파일들 백업 (실패 시 복구용)
            String oldVideoFilename = existingPost.getVideoFilename();
            String oldThumbnailFilename = existingPost.getThumbnailFilename();
            
            System.out.println("기존 동영상 파일명: " + oldVideoFilename);
            System.out.println("기존 썸네일 파일명: " + oldThumbnailFilename);
            
            try {
                // 새 파일 업로드
                String newVideoFilename = fileService.uploadVideo(videoFile);
                System.out.println("새 동영상 업로드 완료: " + newVideoFilename);
                
                String newThumbnailFilename = fileService.generateThumbnail(newVideoFilename);
                System.out.println("새 썸네일 생성 완료: " + newThumbnailFilename);
                
                // DB 정보 업데이트
                existingPost.setVideoFilename(newVideoFilename);
                existingPost.setVideoOriginalName(videoFile.getOriginalFilename());
                existingPost.setVideoContentType(videoFile.getContentType());
                existingPost.setVideoSize(videoFile.getSize());
                existingPost.setThumbnailFilename(newThumbnailFilename);
                
                System.out.println("DB 정보 업데이트 완료");
                
                // 기존 파일들 삭제 (새 파일 업로드 성공 후)
                if (oldVideoFilename != null) {
                    fileService.deleteFile(oldVideoFilename);
                    System.out.println("기존 동영상 삭제 완료: " + oldVideoFilename);
                }
                if (oldThumbnailFilename != null) {
                    fileService.deleteThumbnail(oldThumbnailFilename);
                    System.out.println("기존 썸네일 삭제 완료: " + oldThumbnailFilename);
                }
                
                System.out.println("=== 동영상 업데이트 성공 ===");
                
            } catch (Exception e) {
                System.out.println("=== 동영상 업데이트 실패 ===");
                System.out.println("오류: " + e.getMessage());
                e.printStackTrace();
                
                // 실패한 새 파일들 정리
                if (existingPost.getVideoFilename() != null && !existingPost.getVideoFilename().equals(oldVideoFilename)) {
                    fileService.deleteFile(existingPost.getVideoFilename());
                }
                if (existingPost.getThumbnailFilename() != null && !existingPost.getThumbnailFilename().equals(oldThumbnailFilename)) {
                    fileService.deleteThumbnail(existingPost.getThumbnailFilename());
                }
                
                // 기존 정보 복구
                existingPost.setVideoFilename(oldVideoFilename);
                existingPost.setThumbnailFilename(oldThumbnailFilename);
                
                throw new RuntimeException("동영상 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        } else {
            System.out.println("새 동영상 파일이 없습니다. 기본 정보만 업데이트합니다.");
        }
        
        Post savedPost = postRepository.save(existingPost);
        System.out.println("게시글 저장 완료. ID: " + savedPost.getId());
        return savedPost;
    }
    
    public void deletePost(Long id, User currentUser) {
        Post post = getPostById(id);
        
        // 작성자만 삭제 가능
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("게시글을 삭제할 권한이 없습니다.");
        }
        
        // 파일들 삭제
        if (post.getVideoFilename() != null) {
            fileService.deleteFile(post.getVideoFilename());
        }
        
        if (post.getThumbnailFilename() != null) {
            fileService.deleteThumbnail(post.getThumbnailFilename());
        }
        
        // 게시글 삭제
        postRepository.delete(post);
    }
    
    public void incrementViewCount(Long id) {
        try {
            Post post = getPostById(id);
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        } catch (Exception e) {
            // 조회수 증가 실패는 게시글 조회에 영향을 주지 않음
        }
    }
    
    public Page<Post> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByTitleContainingOrContentContaining(keyword, pageable);
    }
    
    // 고급 검색 기능
    public Page<Post> searchPostsByType(String searchType, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Post> results;
        switch (searchType) {
            case "title":
                results = postRepository.findByTitleContaining(keyword, pageable);
                break;
            case "content":
                results = postRepository.findByContentContaining(keyword, pageable);
                break;
            case "author":
                results = postRepository.findByAuthorFullNameContaining(keyword, pageable);
                break;
            case "filename":
                results = postRepository.findByVideoOriginalNameContaining(keyword, pageable);
                break;
            case "all":
            default:
                results = postRepository.findBySearchTypeAndKeyword(keyword, pageable);
                break;
        }
        
        return results;
    }
    
    // 페이징 크기 검증
    public int validatePageSize(int size) {
        if (size <= 0) return 10;
        if (size > 100) return 100;
        return size;
    }
    
    // Admin 기능들
    public long getTotalPostCount() {
        return postRepository.count();
    }

    public List<Post> getRecentPosts(int page, int size) {
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(Pageable.ofSize(size).withPage(page));
        return postPage.getContent();
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }
    
    public void deletePostByAdmin(Long postId) {
        Post post = getPostById(postId);
        if (post == null) {
            throw new RuntimeException("게시글을 찾을 수 없습니다.");
        }
        
        // 파일들 삭제
        if (post.getVideoFilename() != null) {
            fileService.deleteFile(post.getVideoFilename());
        }
        
        if (post.getThumbnailFilename() != null) {
            fileService.deleteThumbnail(post.getThumbnailFilename());
        }
        
        // 게시글 삭제
        postRepository.delete(post);
    }
    
    public Page<Post> getPostsByAuthor(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByAuthorId(authorId, pageable);
    }
    
    /**
     * 게시글의 동영상 파일 존재 여부 확인
     */
    public boolean hasVideo(Post post) {
        return post.getVideoFilename() != null && !post.getVideoFilename().trim().isEmpty();
    }
    
    /**
     * 게시글의 썸네일 파일 존재 여부 확인
     */
    public boolean hasThumbnail(Post post) {
        return post.getThumbnailFilename() != null && !post.getThumbnailFilename().trim().isEmpty();
    }
    
    /**
     * 동영상 정보 문자열 생성 (로그용)
     */
    private String getVideoInfoString(Post post) {
        if (!hasVideo(post)) {
            return "동영상 없음";
        }
        
        return String.format("동영상: %s (원본: %s, 크기: %s bytes, 타입: %s, 썸네일: %s)",
            post.getVideoFilename(),
            post.getVideoOriginalName(),
            post.getVideoSize() != null ? post.getVideoSize().toString() : "알 수 없음",
            post.getVideoContentType(),
            post.getThumbnailFilename()
        );
    }

    public Page<Post> searchPosts(String searchType, String keyword, Pageable pageable) {
        Page<Post> results;
        switch (searchType) {
            case "title":
                results = postRepository.findByTitleContaining(keyword, pageable);
                break;
            case "content":
                results = postRepository.findByContentContaining(keyword, pageable);
                break;
            case "author":
                results = postRepository.findByAuthorFullNameContaining(keyword, pageable);
                break;
            case "all":
            default:
                results = postRepository.findBySearchTypeAndKeyword(keyword, pageable);
                break;
        }
        
        return results;
    }
    
    public void bulkDeletePosts(List<Long> postIds) {
        for (Long postId : postIds) {
            try {
                deletePostByAdmin(postId);
            } catch (Exception e) {
                // 개별 실패는 전체 작업을 중단하지 않음
            }
        }
    }
}