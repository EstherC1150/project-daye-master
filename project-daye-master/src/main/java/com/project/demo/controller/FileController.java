package com.project.demo.controller;

import com.project.demo.entity.Post;
import com.project.demo.repository.PostRepository;
import com.project.demo.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/files")
public class FileController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FileService fileService;

    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB 청크 크기

    @GetMapping("/video/{postId}")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long postId,
                                               @RequestHeader(value = "Range", required = false) String rangeHeader) {
        
        try {
            System.out.println("=== 동영상 요청 시작 ===");
            System.out.println("요청된 Post ID: " + postId);
            
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                System.out.println("Post를 찾을 수 없습니다: " + postId);
                return ResponseEntity.notFound().build();
            }

            Post post = postOpt.get();
            System.out.println("Post 정보:");
            System.out.println("- ID: " + post.getId());
            System.out.println("- 제목: " + post.getTitle());
            System.out.println("- 동영상 파일명: " + post.getVideoFilename());

            if (post.getVideoFilename() == null || post.getVideoFilename().isEmpty()) {
                System.out.println("동영상 파일명이 없습니다.");
                return ResponseEntity.notFound().build();
            }

            File videoFile = fileService.getVideoFile(post.getVideoFilename());
            System.out.println("찾은 동영상 파일 경로: " + videoFile.getAbsolutePath());
            System.out.println("파일 존재 여부: " + videoFile.exists());
            
            if (!videoFile.exists()) {
                System.out.println("동영상 파일이 존재하지 않습니다.");
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(videoFile);
            String contentType = determineContentType(post.getVideoContentType(), post.getVideoFilename());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.set("Accept-Ranges", "bytes");
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Range, Content-Type");
            headers.set("Cache-Control", "public, max-age=3600");
            headers.set("Content-Disposition", "inline");
            headers.set("Content-Length", String.valueOf(videoFile.length()));
            
            System.out.println("응답 헤더:");
            System.out.println("- Content-Type: " + contentType);
            System.out.println("- Content-Length: " + videoFile.length());
            System.out.println("- Accept-Ranges: bytes");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String determineContentType(String originalContentType, String filename) {
        String extension = getFileExtension(filename).toLowerCase();

        switch (extension) {
            case ".mp4":
                return "video/mp4";
            case ".webm":
                return "video/webm";
            case ".ogv":
            case ".ogg":
                return "video/ogg";
            case ".avi":
                return "video/x-msvideo";
            case ".mov":
                return "video/quicktime";
            case ".mkv":
                return "video/x-matroska";
            case ".m4v":
                return "video/mp4";
            case ".3gp":
                return "video/3gpp";
            case ".flv":
                return "video/x-flv";
            default:
                // 원본 ContentType이 video로 시작하면 사용, 아니면 기본값
                if (originalContentType != null && originalContentType.startsWith("video/")) {
                    return originalContentType;
                }
                return "video/mp4";
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    @GetMapping("/download/{postId}")
    public ResponseEntity<Resource> downloadVideo(@PathVariable Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Post post = postOpt.get();
        if (post.getVideoFilename() == null || post.getVideoFilename().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        File videoFile = fileService.getVideoFile(post.getVideoFilename());
        if (!videoFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(videoFile);
        String contentType = determineContentType(post.getVideoContentType(), post.getVideoFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                       "attachment; filename=\"" + post.getVideoOriginalName() + "\"")
                .body(resource);
    }

    @GetMapping("/thumbnail/{thumbnailFilename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String thumbnailFilename) {
        File thumbnailFile = fileService.getThumbnailFile(thumbnailFilename);

        if (!thumbnailFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(thumbnailFile);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.set("Cache-Control", "public, max-age=86400"); // 24시간 캐시

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    // OPTIONS 요청 처리 (CORS preflight)
    @RequestMapping(value = "/video/{postId}", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleVideoOptions() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Range, Content-Type");
        headers.set("Access-Control-Max-Age", "3600");

        return ResponseEntity.ok().headers(headers).build();
    }

    // HEAD 요청 처리 (동영상 정보 확인)
    @RequestMapping(value = "/video/{postId}", method = RequestMethod.HEAD)
    public ResponseEntity<?> handleVideoHead(@PathVariable Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Post post = postOpt.get();
        if (post.getVideoFilename() == null || post.getVideoFilename().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        File videoFile = fileService.getVideoFile(post.getVideoFilename());
        if (!videoFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        long fileSize = videoFile.length();
        String contentType = determineContentType(post.getVideoContentType(), post.getVideoFilename());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("Accept-Ranges", "bytes");
        headers.setContentLength(fileSize);
        headers.set("Access-Control-Allow-Origin", "*");

        return ResponseEntity.ok().headers(headers).build();
    }
}