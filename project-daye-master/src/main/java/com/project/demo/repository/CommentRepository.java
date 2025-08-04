package com.project.demo.repository;

import com.project.demo.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 특정 게시글의 모든 댓글을 계층형으로 조회 (대댓글 포함)
    @Query("SELECT c FROM Comment c " +
           "LEFT JOIN FETCH c.replies r " +
           "LEFT JOIN FETCH c.author " +
           "LEFT JOIN FETCH r.author " +
           "WHERE c.post.id = :postId AND c.parent IS NULL " +
           "ORDER BY c.createdAt ASC, r.createdAt ASC")
    List<Comment> findByPostIdWithReplies(@Param("postId") Long postId);
    
    // 특정 게시글의 댓글 수 조회 (대댓글 포함)
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
    
    // 특정 댓글의 대댓글 수 조회
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parent.id = :commentId")
    long countRepliesByCommentId(@Param("commentId") Long commentId);
    
    // 사용자가 작성한 댓글 조회
    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
    
    // 특정 댓글의 대댓글들 조회
    @Query("SELECT c FROM Comment c WHERE c.parent.id = :commentId")
    List<Comment> findRepliesByCommentId(@Param("commentId") Long commentId);
} 