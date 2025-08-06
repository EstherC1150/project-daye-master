package com.project.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> replies = new ArrayList<>();
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
    
    // 댓글인지 대댓글인지 확인하는 메서드
    public boolean isReply() {
        return parent != null;
    }
    
    // 댓글인지 확인하는 메서드
    public boolean isComment() {
        return parent == null;
    }
    
    // 삭제된 댓글인지 확인하는 메서드
    public boolean isDeleted() {
        return deleted;
    }
    
    // 댓글 삭제 메서드 (소프트 삭제)
    public void delete() {
        this.deleted = true;
    }
    
    // 대댓글 추가 메서드
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParent(this);
    }
    
    // 대댓글 제거 메서드
    public void removeReply(Comment reply) {
        replies.remove(reply);
        reply.setParent(null);
    }
} 