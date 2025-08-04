package com.project.demo.service;

import com.project.demo.entity.Comment;
import com.project.demo.entity.Post;
import com.project.demo.entity.User;
import com.project.demo.repository.CommentRepository;
import com.project.demo.repository.PostRepository;
import com.project.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    /**
     * 게시글의 모든 댓글과 대댓글을 조회
     */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdWithReplies(postId);
    }
    
    /**
     * 댓글 작성
     */
    public Comment createComment(Long postId, Long authorId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(content);
        
        return commentRepository.save(comment);
    }
    
    /**
     * 대댓글 작성
     */
    public Comment createReply(Long parentCommentId, Long authorId, String content) {
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
        
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        Comment reply = new Comment();
        reply.setPost(parentComment.getPost());
        reply.setAuthor(author);
        reply.setContent(content);
        reply.setParent(parentComment);
        
        return commentRepository.save(reply);
    }
    
    /**
     * 댓글 수정
     */
    public Comment updateComment(Long commentId, Long authorId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        
        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("댓글을 수정할 권한이 없습니다.");
        }
        
        comment.setContent(content);
        return commentRepository.save(comment);
    }
    
    /**
     * 댓글 삭제 (대댓글도 함께 삭제)
     */
    public void deleteComment(Long commentId, Long authorId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        
        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new IllegalArgumentException("댓글을 삭제할 권한이 없습니다.");
        }
        
        // 대댓글들을 먼저 삭제
        List<Comment> replies = commentRepository.findRepliesByCommentId(commentId);
        commentRepository.deleteAll(replies);
        
        // 댓글 삭제
        commentRepository.delete(comment);
    }
    
    /**
     * 게시글의 댓글 수 조회
     */
    @Transactional(readOnly = true)
    public long getCommentCountByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }
    
    /**
     * 댓글의 대댓글 수 조회
     */
    @Transactional(readOnly = true)
    public long getReplyCountByCommentId(Long commentId) {
        return commentRepository.countRepliesByCommentId(commentId);
    }
    
    /**
     * 댓글 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long commentId) {
        return commentRepository.existsById(commentId);
    }
    
    /**
     * 댓글 작성자 확인
     */
    @Transactional(readOnly = true)
    public boolean isAuthor(Long commentId, Long authorId) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        return comment.isPresent() && comment.get().getAuthor().getId().equals(authorId);
    }
} 