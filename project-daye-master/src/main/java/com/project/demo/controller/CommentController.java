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
            System.err.println("❌ 댓글 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    /**
     * 댓글 작성 (인증 필요)
     */
    @PostMapping
    public ResponseEntity<CommentDto> createComment(@RequestBody CreateCommentRequest request, 
                                                   Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User currentUser = userService.getCurrentUser(authentication);
        Comment comment = commentService.createComment(request.getPostId(), currentUser.getId(), request.getContent());
        
        return ResponseEntity.ok(convertToDto(comment));
    }
    
    /**
     * 대댓글 작성 (인증 필요)
     */
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<CommentDto> createReply(@PathVariable Long commentId,
                                                 @RequestBody CreateCommentRequest request,
                                                 Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User currentUser = userService.getCurrentUser(authentication);
        Comment reply = commentService.createReply(commentId, currentUser.getId(), request.getContent());
        
        return ResponseEntity.ok(convertToDto(reply));
    }
    
    /**
     * 댓글 수정 (인증 필요)
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long commentId,
                                                   @RequestBody UpdateCommentRequest request,
                                                   Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User currentUser = userService.getCurrentUser(authentication);
        Comment comment = commentService.updateComment(commentId, currentUser.getId(), request.getContent());
        
        return ResponseEntity.ok(convertToDto(comment));
    }
    
    /**
     * 댓글 삭제 (인증 필요)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User currentUser = userService.getCurrentUser(authentication);
        commentService.deleteComment(commentId, currentUser.getId());
        
        return ResponseEntity.ok().build();
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
        dto.setContent(comment.getContent());
        dto.setAuthorName(comment.getAuthor().getFullName());
        dto.setAuthorUsername(comment.getAuthor().getUsername());
        dto.setCreatedAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        dto.setReply(comment.isReply());
        
        // 대댓글들도 변환
        if (!comment.getReplies().isEmpty()) {
            List<CommentDto> replyDtos = comment.getReplies().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            dto.setReplies(replyDtos);
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
        private boolean reply;
        private List<CommentDto> replies;
        
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
        
        public boolean isReply() { return reply; }
        public void setReply(boolean reply) { this.reply = reply; }
        
        public List<CommentDto> getReplies() { return replies; }
        public void setReplies(List<CommentDto> replies) { this.replies = replies; }
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