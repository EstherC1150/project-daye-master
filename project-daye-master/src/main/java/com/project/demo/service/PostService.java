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
    
    public Post getPostById(Long id) {
        try {
            return postRepository.findById(id)
                    .orElse(null);
        } catch (Exception e) {
            System.err.println("❌ 게시글 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public Post createPost(Post post, User author, MultipartFile videoFile) {
        System.out.println("=== 게시글 생성 시작 ===");
        System.out.println("제목: " + post.getTitle());
        System.out.println("작성자: " + author.getUsername());
        System.out.println("동영상 파일: " + (videoFile != null ? videoFile.getOriginalFilename() : "없음"));
        
        post.setAuthor(author);
        post.setViewCount(0);
        
        // 동영상 파일 처리
        if (videoFile != null && !videoFile.isEmpty()) {
            try {
                System.out.println("동영상 파일 업로드 시작...");
                String videoFilename = fileService.uploadVideo(videoFile);
                System.out.println("동영상 파일 업로드 완료: " + videoFilename);
                
                System.out.println("썸네일 생성 시작...");
                String thumbnailFilename = fileService.generateThumbnail(videoFilename);
                System.out.println("썸네일 생성 완료: " + thumbnailFilename);
                
                post.setVideoFilename(videoFilename);
                post.setVideoOriginalName(videoFile.getOriginalFilename());
                post.setVideoContentType(videoFile.getContentType());
                post.setVideoSize(videoFile.getSize());
                post.setThumbnailFilename(thumbnailFilename);
                
                System.out.println("동영상 정보 설정 완료:");
                System.out.println("- 파일명: " + post.getVideoFilename());
                System.out.println("- 원본명: " + post.getVideoOriginalName());
                System.out.println("- 타입: " + post.getVideoContentType());
                System.out.println("- 크기: " + post.getVideoSize() + " bytes");
                System.out.println("- 썸네일: " + post.getThumbnailFilename());
                
            } catch (Exception e) {
                System.err.println("❌ 동영상 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
                
                // 업로드된 파일이 있다면 정리
                if (post.getVideoFilename() != null) {
                    fileService.deleteFile(post.getVideoFilename());
                }
                if (post.getThumbnailFilename() != null) {
                    fileService.deleteThumbnail(post.getThumbnailFilename());
                }
                
                throw new RuntimeException("동영상 업로드 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        } else {
            System.out.println("동영상 파일이 없습니다.");
        }
        
        Post savedPost = postRepository.save(post);
        System.out.println("✅ 게시글 저장 완료. ID: " + savedPost.getId());
        return savedPost;
    }
    
    public Post updatePost(Long id, Post updatedPost, User currentUser, MultipartFile videoFile) {
        System.out.println("=== 게시글 수정 시작: ID " + id + " ===");
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
            System.out.println("새로운 동영상 파일 업로드: " + videoFile.getOriginalFilename());
            
            // 기존 파일들 백업 (실패 시 복구용)
            String oldVideoFilename = existingPost.getVideoFilename();
            String oldThumbnailFilename = existingPost.getThumbnailFilename();
            
            try {
                // 새 파일 업로드
                String newVideoFilename = fileService.uploadVideo(videoFile);
                System.out.println("새 동영상 파일 업로드 완료: " + newVideoFilename);
                
                String newThumbnailFilename = fileService.generateThumbnail(newVideoFilename);
                System.out.println("새 썸네일 생성 완료: " + newThumbnailFilename);
                
                // DB 정보 업데이트
                existingPost.setVideoFilename(newVideoFilename);
                existingPost.setVideoOriginalName(videoFile.getOriginalFilename());
                existingPost.setVideoContentType(videoFile.getContentType());
                existingPost.setVideoSize(videoFile.getSize());
                existingPost.setThumbnailFilename(newThumbnailFilename);
                
                // 기존 파일들 삭제 (새 파일 업로드 성공 후)
                if (oldVideoFilename != null) {
                    fileService.deleteFile(oldVideoFilename);
                    System.out.println("기존 동영상 파일 삭제: " + oldVideoFilename);
                }
                if (oldThumbnailFilename != null) {
                    fileService.deleteThumbnail(oldThumbnailFilename);
                    System.out.println("기존 썸네일 파일 삭제: " + oldThumbnailFilename);
                }
                
            } catch (Exception e) {
                System.err.println("❌ 새 동영상 업로드 실패: " + e.getMessage());
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
            System.out.println("동영상 파일 변경 없음");
        }
        
        Post savedPost = postRepository.save(existingPost);
        System.out.println("✅ 게시글 수정 완료. ID: " + savedPost.getId());
        return savedPost;
    }
    
    public void deletePost(Long id, User currentUser) {
        System.out.println("=== 게시글 삭제 시작: ID " + id + " ===");
        Post post = getPostById(id);
        
        // 작성자만 삭제 가능
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("게시글을 삭제할 권한이 없습니다.");
        }
        
        // 파일들 삭제
        if (post.getVideoFilename() != null) {
            System.out.println("동영상 파일 삭제: " + post.getVideoFilename());
            fileService.deleteFile(post.getVideoFilename());
        }
        
        if (post.getThumbnailFilename() != null) {
            System.out.println("썸네일 파일 삭제: " + post.getThumbnailFilename());
            fileService.deleteThumbnail(post.getThumbnailFilename());
        }
        
        // 게시글 삭제
        postRepository.delete(post);
        System.out.println("✅ 게시글 삭제 완료. ID: " + id);
    }
    
    public void incrementViewCount(Long id) {
        try {
            Post post = getPostById(id);
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
            System.out.println("조회수 증가: " + post.getTitle() + " -> " + post.getViewCount());
        } catch (Exception e) {
            System.err.println("❌ 조회수 증가 실패: " + e.getMessage());
            // 조회수 증가 실패는 게시글 조회에 영향을 주지 않음
        }
    }
    
    public Page<Post> searchPosts(String keyword, int page, int size) {
        System.out.println("검색 실행: '" + keyword + "' (페이지: " + page + ", 크기: " + size + ")");
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> results = postRepository.findByTitleContainingOrContentContaining(keyword, pageable);
        System.out.println("검색 결과: " + results.getTotalElements() + "개 게시글");
        return results;
    }
    
    public Page<Post> getPostsByAuthor(Long authorId, int page, int size) {
        System.out.println("작성자별 게시글 조회: 작성자 ID " + authorId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> results = postRepository.findByAuthorId(authorId, pageable);
        System.out.println("작성자 게시글 수: " + results.getTotalElements() + "개");
        return results;
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
}