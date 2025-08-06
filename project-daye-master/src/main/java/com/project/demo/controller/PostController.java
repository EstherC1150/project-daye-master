package com.project.demo.controller;

import com.project.demo.entity.Post;
import com.project.demo.entity.User;
import com.project.demo.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
@RequestMapping("/posts")
public class PostController {
    
    @Autowired
    private PostService postService;
    
    @GetMapping
    public String listPosts(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false, defaultValue = "all") String searchType,
                           Model model) {
        
        try {
            // 페이징 크기 검증
            size = postService.validatePageSize(size);
            
            Page<Post> posts;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 고급 검색 사용
                posts = postService.searchPostsByType(searchType, keyword.trim(), page, size);
                model.addAttribute("keyword", keyword);
                model.addAttribute("searchType", searchType);
            } else {
                posts = postService.getAllPosts(page, size);
                model.addAttribute("searchType", searchType);
            }
            
            // 게시글이 없어도 오류가 아닌 정상적인 상태로 처리
            model.addAttribute("posts", posts);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", posts.getTotalPages());
            model.addAttribute("totalElements", posts.getTotalElements());
            model.addAttribute("currentSize", size);
            
            // 검색 타입 옵션들
            model.addAttribute("searchTypes", new String[]{"all", "title", "content", "author", "filename"});
            
            return "posts/list";
        } catch (Exception e) {
            // 오류가 발생해도 절대 error.html로 가지 않고 빈 페이지로 처리
            try {
                Page<Post> emptyPosts = postService.getAllPosts(0, 10);
                model.addAttribute("posts", emptyPosts);
            } catch (Exception ex) {
                // PostService에서도 오류가 발생하면 완전히 빈 객체 생성
                model.addAttribute("posts", new PageImpl<>(new ArrayList<>()));
            }
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0L);
            model.addAttribute("currentSize", 10);
            model.addAttribute("searchTypes", new String[]{"all", "title", "content", "author", "filename"});
            model.addAttribute("keyword", keyword);
            model.addAttribute("searchType", searchType != null ? searchType : "all");
            
            return "posts/list";
        }
    }
    
    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id, Model model) {
        try {
            Post post = postService.getPostById(id);
            if (post == null) {
                model.addAttribute("errorMessage", "게시글을 찾을 수 없습니다.");
                return "error";
            }
            postService.incrementViewCount(id);
            model.addAttribute("post", post);
            return "posts/view";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "게시글을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "error";
        }
    }
    
    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("post", new Post());
        return "posts/write";
    }
    
    @PostMapping("/write")
    public String writePost(@ModelAttribute Post post,
                           @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                           @AuthenticationPrincipal User user,
                           RedirectAttributes redirectAttributes) {
        try {
            postService.createPost(post, user, videoFile);
            redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 작성되었습니다.");
            return "redirect:/posts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/write";
        }
    }
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                          @AuthenticationPrincipal User user,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            Post post = postService.getPostById(id);
            if (!post.getAuthor().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "수정 권한이 없습니다.");
                return "redirect:/posts/" + id;
            }
            model.addAttribute("post", post);
            return "posts/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "게시글을 찾을 수 없습니다.");
            return "redirect:/posts";
        }
    }
    
    @PostMapping("/{id}/edit")
    public String editPost(@PathVariable Long id,
                          @ModelAttribute Post post,
                          @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                          @AuthenticationPrincipal User user,
                          RedirectAttributes redirectAttributes) {
        try {
            postService.updatePost(id, post, user, videoFile);
            redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 수정되었습니다.");
            return "redirect:/posts/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/" + id + "/edit";
        }
    }
    
    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
                            @AuthenticationPrincipal User user,
                            RedirectAttributes redirectAttributes) {
        try {
            postService.deletePost(id, user);
            redirectAttributes.addFlashAttribute("message", "게시글이 성공적으로 삭제되었습니다.");
            return "redirect:/posts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/posts/" + id;
        }
    }
} 