package com.project.demo.repository;

import com.project.demo.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.author.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.videoOriginalName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> findBySearchTypeAndKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    Page<Post> findByTitleContaining(String title, Pageable pageable);
    
    Page<Post> findByContentContaining(String content, Pageable pageable);
    
    Page<Post> findByAuthorFullNameContaining(String authorFullName, Pageable pageable);
    
    Page<Post> findByVideoOriginalNameContaining(String videoOriginalName, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId ORDER BY p.createdAt DESC")
    Page<Post> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword% ORDER BY p.createdAt DESC")
    Page<Post> findByTitleContainingOrContentContaining(@Param("keyword") String keyword, Pageable pageable);
} 