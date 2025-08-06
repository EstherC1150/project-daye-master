package com.project.demo.service;

import com.project.demo.entity.User;
import com.project.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRoles() != null ? user.getRoles().toArray(new String[0]) : new String[0])
                .disabled(!user.isEnabled())
                .build();
    }

    public User registerUser(String username, String password, String fullName, String email, String phoneNumber) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setEnabled(true);
        if (user.getRoles() == null) {
            user.setRoles(new ArrayList<>());
        }
        user.getRoles().add("USER");

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public List<User> getRecentUsers(int page, int size) {
        Page<User> userPage = userRepository.findAllByOrderByCreatedAtDesc(Pageable.ofSize(size).withPage(page));
        return userPage.getContent();
    }

    public Page<User> searchUsers(String searchType, String keyword, Pageable pageable) {
        switch (searchType) {
            case "username":
                return userRepository.findByUsernameContainingIgnoreCase(keyword, pageable);
            case "fullName":
                return userRepository.findByFullNameContainingIgnoreCase(keyword, pageable);
            case "email":
                return userRepository.findByEmailContainingIgnoreCase(keyword, pageable);
            case "all":
            default:
                return userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    keyword, keyword, keyword, pageable);
        }
    }

    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // roles가 null이면 초기화
        if (user.getRoles() == null) {
            user.setRoles(new ArrayList<>());
        }
        
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 관리자는 삭제할 수 없음
        if (user.getRoles() != null && user.getRoles().contains("ADMIN")) {
            throw new RuntimeException("관리자는 삭제할 수 없습니다.");
        }
        
        userRepository.delete(user);
    }

    public void bulkDeleteUsers(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        
        // 관리자 제외
        users.removeIf(user -> user.getRoles() != null && user.getRoles().contains("ADMIN"));
        
        userRepository.deleteAll(users);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = getUserById(userId);
        
        // 관리자는 수정 불가
        if (user.getRoles() != null && user.getRoles().contains("ADMIN")) {
            throw new RuntimeException("관리자는 수정할 수 없습니다.");
        }
        
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEnabled(request.isEnabled());
        
        // 역할 업데이트 (ADMIN 역할은 제거 불가)
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            if (user.getRoles() == null) {
                user.setRoles(new ArrayList<>());
            }
            user.getRoles().clear();
            user.getRoles().addAll(request.getRoles());
            // ADMIN 역할이 제거되려고 하면 다시 추가
            if (user.getRoles().contains("ADMIN")) {
                user.getRoles().add("ADMIN");
            }
        }
        
        userRepository.save(user);
    }

    // DTO 클래스
    public static class UpdateUserRequest {
        private String fullName;
        private String email;
        private String phoneNumber;
        private boolean enabled;
        private List<String> roles;

        // Getters and Setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
} 