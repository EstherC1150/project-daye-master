package com.project.demo.repository;

import com.project.demo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    
    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', ?1, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', ?2, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', ?3, '%'))")
    Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String fullName, String email, Pageable pageable);
} 