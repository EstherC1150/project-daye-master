package com.project.demo.controller;

import com.project.demo.entity.Comment;
import com.project.demo.entity.User;
import com.project.demo.service.CommentService;
import com.project.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    private final UserService userService;
    
    /**
     * 댓글 목록 조회 (게스트 접근 가능)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable Long postId) {
        try {
            List<Comment> comments = commentService.getCommentsByPostId(postId);
            
            List<CommentDto> commentDtos = comments.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(commentDtos);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * 댓글 작성 (인증 필요)
     */
    @PostMapping
    public ResponseEntity<CommentDto> createComment(@RequestBody CreateCommentRequest request, 
                                                   Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            Comment comment = commentService.createComment(request.getPostId(), currentUser.getId(), request.getContent());
            
            return ResponseEntity.ok(convertToDto(comment));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 대댓글 작성 (인증 필요)
     */
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<CommentDto> createReply(@PathVariable Long commentId,
                                                 @RequestBody CreateCommentRequest request,
                                                 Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            Comment reply = commentService.createReply(commentId, currentUser.getId(), request.getContent());
            
            return ResponseEntity.ok(convertToDto(reply));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 댓글 수정 (인증 필요)
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long commentId,
                                                   @RequestBody UpdateCommentRequest request,
                                                   Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            Comment comment = commentService.updateComment(commentId, currentUser.getId(), request.getContent());
            
            return ResponseEntity.ok(convertToDto(comment));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 댓글 삭제 (인증 필요)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            commentService.deleteComment(commentId, currentUser.getId());
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 댓글 수 조회 (게스트 접근 가능)
     */
    @GetMapping("/{postId}/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long postId) {
        long count = commentService.getCommentCountByPostId(postId);
        return ResponseEntity.ok(count);
    }
    
    /**
     * Comment 엔티티를 DTO로 변환
     */
    private CommentDto convertToDto(Comment comment) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        
        // 삭제된 댓글인지 확인
        if (comment.isDeleted()) {
            dto.setContent("삭제된 댓글입니다.");
            dto.setAuthorName("알 수 없음");
            dto.setAuthorUsername("unknown");
            dto.setDeleted(true);
        } else {
            dto.setContent(comment.getContent());
            dto.setAuthorName(comment.getAuthor().getFullName());
            dto.setAuthorUsername(comment.getAuthor().getUsername());
            dto.setDeleted(false);
        }
        
        dto.setCreatedAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (comment.getUpdatedAt() != null) {
            dto.setUpdatedAt(comment.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            dto.setUpdatedAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        dto.setReply(comment.isReply());
        
        // 대댓글들도 변환 - null 체크 추가
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDto> replyDtos = comment.getReplies().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            dto.setReplies(replyDtos);
        } else {
            dto.setReplies(new ArrayList<>()); // 빈 리스트로 초기화
        }
        
        return dto;
    }
    
    // DTO 클래스들
    public static class CommentDto {
        private Long id;
        private String content;
        private String authorName;
        private String authorUsername;
        private String createdAt;
        private String updatedAt;
        private boolean reply;
        private boolean deleted;
        private List<CommentDto> replies = new ArrayList<>(); // 기본값으로 빈 리스트
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        
        public String getAuthorUsername() { return authorUsername; }
        public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        
        public boolean isReply() { return reply; }
        public void setReply(boolean reply) { this.reply = reply; }
        
        public List<CommentDto> getReplies() { return replies; }
        public void setReplies(List<CommentDto> replies) { this.replies = replies != null ? replies : new ArrayList<>(); }
        
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
    }
    
    public static class CreateCommentRequest {
        private Long postId;
        private String content;
        
        public Long getPostId() { return postId; }
        public void setPostId(Long postId) { this.postId = postId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
    
    public static class UpdateCommentRequest {
        private String content;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}