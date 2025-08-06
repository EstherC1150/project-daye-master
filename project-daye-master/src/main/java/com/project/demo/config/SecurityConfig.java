package com.project.demo.config;

import com.project.demo.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // 공개 접근 가능한 경로
                .requestMatchers("/", "/posts", "/posts/**", "/files/**", "/auth/**",
                                "/.well-known/**", "/error").permitAll()
                // 업로드된 파일들 (동영상, 썸네일) - 모든 사용자 접근 가능
                .requestMatchers("/uploads/**").permitAll()
                // 댓글 조회 API (GET 요청만 게스트 접근 가능)
                .requestMatchers("/api/comments/{postId}").permitAll()
                .requestMatchers("/api/comments/{postId}/count").permitAll()
                // 정적 리소스
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // 로그인, 회원가입 페이지
                .requestMatchers("/login", "/register").permitAll()
                // 관리자 페이지 - ADMIN 권한 필요
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 게시글 작성/수정/삭제는 인증 필요
                .requestMatchers("/posts/write", "/posts/*/edit", "/posts/create", "/posts/*/update", "/posts/*/delete").authenticated()
                // 댓글 작성/수정/삭제는 인증 필요 (POST, PUT, DELETE)
                .requestMatchers("/api/comments").authenticated()
                .requestMatchers("/api/comments/*/replies").authenticated()
                .requestMatchers("/api/comments/*").authenticated()
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/posts", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/posts")
                .permitAll()
            )
            .userDetailsService(userDetailsService)
            .csrf(csrf -> csrf.disable());
    
        return http.build();
    }
}