package com.project.demo.controller;

import com.project.demo.entity.Post;
import com.project.demo.repository.PostRepository;
import com.project.demo.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.util.Optional;

@Controller
@RequestMapping("/files")
public class FileController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FileService fileService;

    @GetMapping("/video/{postId}")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long postId, 
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        
        try {
            System.out.println("=== 동영상 스트리밍 요청 시작 ===");
            System.out.println("Post ID: " + postId);
            System.out.println("Range Header: " + rangeHeader);
            
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                System.err.println("❌ Post를 찾을 수 없습니다: " + postId);
                return ResponseEntity.notFound().build();
            }

            Post post = postOpt.get();
            System.out.println("Post 정보: " + post.getTitle());
            System.out.println("동영상 파일명: " + post.getVideoFilename());
            System.out.println("동영상 원본명: " + post.getVideoOriginalName());
            System.out.println("동영상 타입: " + post.getVideoContentType());
            
            if (post.getVideoFilename() == null || post.getVideoFilename().isEmpty()) {
                System.err.println("❌ 동영상 파일이 없습니다.");
                return ResponseEntity.notFound().build();
            }

            File videoFile = fileService.getVideoFile(post.getVideoFilename());
            System.out.println("동영상 파일 경로: " + videoFile.getAbsolutePath());
            System.out.println("동영상 파일 존재: " + videoFile.exists());
            System.out.println("동영상 파일 크기: " + videoFile.length() + " bytes");

            if (!videoFile.exists()) {
                System.err.println("❌ 동영상 파일이 존재하지 않습니다: " + videoFile.getAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            long fileSize = videoFile.length();
            String contentType = determineContentType(post.getVideoContentType(), post.getVideoFilename());
            
            System.out.println("결정된 Content-Type: " + contentType);
            
            // Range 요청이 있는 경우
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return handleRangeRequest(videoFile, rangeHeader, fileSize, contentType);
            }
            
            // 전체 파일 스트리밍
            return handleFullFileRequest(videoFile, fileSize, contentType);

        } catch (Exception e) {
            System.err.println("❌ 동영상 스트리밍 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private ResponseEntity<Resource> handleRangeRequest(File videoFile, String rangeHeader, 
            long fileSize, String contentType) throws IOException {
        
        String range = rangeHeader.substring(6); // "bytes=" 제거
        String[] ranges = range.split("-");
        
        long start = 0;
        long end = fileSize - 1;
        
        try {
            if (ranges.length > 0 && !ranges[0].isEmpty()) {
                start = Long.parseLong(ranges[0]);
            }
            if (ranges.length > 1 && !ranges[1].isEmpty()) {
                end = Math.min(Long.parseLong(ranges[1]), fileSize - 1);
            }
        } catch (NumberFormatException e) {
            System.err.println("❌ Range 헤더 파싱 오류: " + rangeHeader);
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
        
        // Range 검증
        if (start >= fileSize || end >= fileSize || start > end) {
            System.err.println("❌ 잘못된 Range 요청: " + start + "-" + end + "/" + fileSize);
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header("Content-Range", "bytes */" + fileSize)
                    .build();
        }
        
        long contentLength = end - start + 1;
        System.out.println("✅ Range 요청 처리: " + start + "-" + end + "/" + fileSize + " (length: " + contentLength + ")");
        
        // 부분 파일 스트림 생성
        InputStream inputStream = new FileInputStream(videoFile);
        inputStream.skip(start);
        InputStreamResource resource = new InputStreamResource(new LimitedInputStream(inputStream, contentLength));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("Accept-Ranges", "bytes");
        headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        headers.setContentLength(contentLength);
        headers.set("Cache-Control", "public, max-age=3600");
        
        // CORS 헤더 추가
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Range");
        
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(resource);
    }
    
    private ResponseEntity<Resource> handleFullFileRequest(File videoFile, long fileSize, String contentType) {
        System.out.println("✅ 전체 파일 스트리밍: " + videoFile.getName() + ", 크기: " + fileSize + " bytes");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("Accept-Ranges", "bytes");
        headers.setContentLength(fileSize);
        headers.set("Cache-Control", "public, max-age=3600");
        
        // CORS 헤더 추가
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Range");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(new FileSystemResource(videoFile));
    }
    
    private String determineContentType(String originalContentType, String filename) {
        // 파일 확장자로 Content-Type 결정
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
            default:
                // 원본 Content-Type이 있으면 사용, 없으면 기본값
                return (originalContentType != null && !originalContentType.isEmpty()) 
                    ? originalContentType : "video/mp4";
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
        System.out.println("=== 동영상 다운로드 요청: " + postId + " ===");
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            System.err.println("❌ Post를 찾을 수 없습니다: " + postId);
            return ResponseEntity.notFound().build();
        }

        Post post = postOpt.get();
        if (post.getVideoFilename() == null || post.getVideoFilename().isEmpty()) {
            System.err.println("❌ 동영상 파일이 없습니다.");
            return ResponseEntity.notFound().build();
        }

        File videoFile = fileService.getVideoFile(post.getVideoFilename());
        if (!videoFile.exists()) {
            System.err.println("❌ 동영상 파일이 존재하지 않습니다: " + videoFile.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(videoFile);
        String contentType = determineContentType(post.getVideoContentType(), post.getVideoFilename());
        
        System.out.println("✅ 다운로드 시작: " + post.getVideoOriginalName());
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + post.getVideoOriginalName() + "\"")
                .body(resource);
    }

    @GetMapping("/thumbnail/{thumbnailFilename}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String thumbnailFilename) {
        System.out.println("=== 썸네일 요청: " + thumbnailFilename + " ===");
        
        File thumbnailFile = fileService.getThumbnailFile(thumbnailFilename);
        
        if (!thumbnailFile.exists()) {
            System.err.println("❌ 썸네일 파일이 존재하지 않습니다: " + thumbnailFile.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(thumbnailFile);
        
        System.out.println("✅ 썸네일 제공: " + thumbnailFilename);
        
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
    
    // InputStream을 제한하는 헬퍼 클래스
    private static class LimitedInputStream extends InputStream {
        private final InputStream inputStream;
        private long remaining;
        
        public LimitedInputStream(InputStream inputStream, long limit) {
            this.inputStream = inputStream;
            this.remaining = limit;
        }
        
        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int result = inputStream.read();
            if (result >= 0) {
                remaining--;
            }
            return result;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int toRead = (int) Math.min(len, remaining);
            int result = inputStream.read(b, off, toRead);
            if (result > 0) {
                remaining -= result;
            }
            return result;
        }
        
        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }
}