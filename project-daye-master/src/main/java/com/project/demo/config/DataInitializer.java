package com.project.demo.config;

import com.project.demo.entity.Post;
import com.project.demo.entity.User;
import com.project.demo.repository.PostRepository;
import com.project.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 기존 사용자가 없을 때만 초기 데이터 생성
        if (userRepository.count() == 0) {
            createInitialUsers();
        }
        
        // 기존 게시글이 없을 때만 초기 데이터 생성
        if (postRepository.count() == 0) {
            createInitialPosts();
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

        System.out.println("초기 사용자 데이터가 생성되었습니다.");
    }
    
    private void createInitialPosts() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        User user = userRepository.findByUsername("user").orElse(null);
        
        if (admin != null && user != null) {
            // 관리자가 작성한 게시글
            Post post1 = new Post();
            post1.setTitle("환영합니다! 동영상 갤러리에 오신 것을 환영합니다.");
            post1.setContent("안녕하세요! 이 갤러리는 Spring Boot와 Thymeleaf를 사용하여 만든 동영상 갤러리입니다.\n\n" +
                           "다음과 같은 기능들을 사용할 수 있습니다:\n" +
                           "- 동영상 업로드 및 재생\n" +
                           "- 동영상 썸네일 자동 생성\n" +
                           "- 사용자 인증 및 권한 관리\n" +
                           "- 반응형 웹 디자인\n\n" +
                           "많은 관심 부탁드립니다!");
            post1.setAuthor(admin);
            post1.setViewCount(15);
            post1.setCreatedAt(LocalDateTime.now().minusDays(2));
            post1.setUpdatedAt(LocalDateTime.now().minusDays(2));
            postRepository.save(post1);
            
            // 일반 사용자가 작성한 게시글
            Post post2 = new Post();
            post2.setTitle("동영상 갤러리 개발 후기");
            post2.setContent("Spring Boot를 사용해서 동영상 갤러리를 개발해보니 정말 재미있었습니다.\n\n" +
                           "특히 다음과 같은 부분들이 인상적이었습니다:\n" +
                           "- Spring Security를 통한 보안 구현\n" +
                           "- Thymeleaf 템플릿 엔진의 강력함\n" +
                           "- JPA를 통한 데이터베이스 연동\n" +
                           "- Bootstrap을 활용한 반응형 UI\n\n" +
                           "앞으로도 더 많은 기능을 추가해보고 싶습니다!");
            post2.setAuthor(user);
            post2.setViewCount(8);
            post2.setCreatedAt(LocalDateTime.now().minusDays(1));
            post2.setUpdatedAt(LocalDateTime.now().minusDays(1));
            postRepository.save(post2);
            
            // 관리자가 작성한 두 번째 게시글
            Post post3 = new Post();
            post3.setTitle("동영상 갤러리 사용 가이드");
            post3.setContent("동영상 갤러리를 처음 사용하시는 분들을 위한 가이드입니다.\n\n" +
                           "1. 로그인 후 업로드 버튼을 클릭하세요\n" +
                           "2. 제목과 내용을 입력하세요\n" +
                           "3. 동영상 파일을 선택하세요\n" +
                           "4. 등록하기 버튼을 클릭하세요\n" +
                           "5. 업로드한 동영상은 갤러리에서 확인할 수 있습니다\n\n" +
                           "궁금한 점이 있으시면 언제든 문의해주세요!");
            post3.setAuthor(admin);
            post3.setViewCount(12);
            post3.setCreatedAt(LocalDateTime.now().minusHours(6));
            post3.setUpdatedAt(LocalDateTime.now().minusHours(6));
            postRepository.save(post3);
            
            System.out.println("초기 게시글 데이터가 생성되었습니다.");
        }
    }
} 