package com.project.demo.config;

import com.project.demo.entity.User;
import com.project.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

// @Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 기존 사용자가 없을 때만 초기 데이터 생성
        if (userRepository.count() == 0) {
            createInitialUsers();
        }
    }

    private void createInitialUsers() {
        // 관리자 사용자
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@demo.com");
        admin.setFullName("관리자");
        admin.setRoles(Arrays.asList("ADMIN"));
        userRepository.save(admin);

        // 일반 사용자
        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setEmail("user@demo.com");
        user.setFullName("일반 사용자");
        user.setRoles(Arrays.asList("USER"));
        userRepository.save(user);
    }
    

} 