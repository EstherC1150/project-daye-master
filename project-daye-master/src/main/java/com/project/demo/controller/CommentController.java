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
            System.out.println("=== 댓글 목록 조회 요청: postId=" + postId + " ===");
            
            List<Comment> comments = commentService.getCommentsByPostId(postId);
            System.out.println("조회된 댓글 수: " + comments.size());
            
            List<CommentDto> commentDtos = comments.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            
            System.out.println("✅ 댓글 DTO 변환 완료: " + commentDtos.size() + "개");
            
            return ResponseEntity.ok(commentDtos);
        } catch (Exception e) {
            System.err.println("❌ 댓글 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("=== 댓글 작성 요청 ===");
            System.out.println("postId: " + request.getPostId());
            System.out.println("content: " + request.getContent());
            System.out.println("user: " + (authentication != null ? authentication.getName() : "null"));
            
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("❌ 인증되지 않은 사용자");
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.getCurrentUser(authentication);
            System.out.println("현재 사용자: " + currentUser.getUsername());
            
            Comment comment = commentService.createComment(request.getPostId(), currentUser.getId(), request.getContent());
            System.out.println("✅ 댓글 작성 완료: " + comment.getId());
            
            return ResponseEntity.ok(convertToDto(comment));
        } catch (Exception e) {
            System.err.println("❌ 댓글 작성 중 오류: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("=== 답글 작성 요청: commentId=" + commentId + " ===");
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.getCurrentUser(authentication);
            Comment reply = commentService.createReply(commentId, currentUser.getId(), request.getContent());
            System.out.println("✅ 답글 작성 완료: " + reply.getId());
            
            return ResponseEntity.ok(convertToDto(reply));
        } catch (Exception e) {
            System.err.println("❌ 답글 작성 중 오류: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("=== 댓글 수정 요청: commentId=" + commentId + " ===");
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.getCurrentUser(authentication);
            Comment comment = commentService.updateComment(commentId, currentUser.getId(), request.getContent());
            System.out.println("✅ 댓글 수정 완료: " + comment.getId());
            
            return ResponseEntity.ok(convertToDto(comment));
        } catch (Exception e) {
            System.err.println("❌ 댓글 수정 중 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 댓글 삭제 (인증 필요)
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, Authentication authentication) {
        try {
            System.out.println("=== 댓글 삭제 요청: commentId=" + commentId + " ===");
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            User currentUser = userService.getCurrentUser(authentication);
            commentService.deleteComment(commentId, currentUser.getId());
            System.out.println("✅ 댓글 삭제 완료: " + commentId);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("❌ 댓글 삭제 중 오류: " + e.getMessage());
            e.printStackTrace();
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
        dto.setContent(comment.getContent());
        dto.setAuthorName(comment.getAuthor().getFullName());
        dto.setAuthorUsername(comment.getAuthor().getUsername());
        dto.setCreatedAt(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
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
        private boolean reply;
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
        
        public boolean isReply() { return reply; }
        public void setReply(boolean reply) { this.reply = reply; }
        
        public List<CommentDto> getReplies() { return replies; }
        public void setReplies(List<CommentDto> replies) { this.replies = replies != null ? replies : new ArrayList<>(); }
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