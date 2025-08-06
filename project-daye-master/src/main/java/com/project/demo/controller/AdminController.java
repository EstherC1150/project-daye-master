package com.project.demo.controller;

import com.project.demo.entity.Post;
import com.project.demo.entity.User;
import com.project.demo.service.PostService;
import com.project.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @GetMapping("")
    public String adminDashboard(Model model) {
        long totalUsers = userService.getTotalUserCount();
        long totalPosts = postService.getTotalPostCount();
        List<User> recentUsers = userService.getRecentUsers(0, 5);
        List<Post> recentPosts = postService.getRecentPosts(0, 5);

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalPosts", totalPosts);
        model.addAttribute("recentUsers", recentUsers);
        model.addAttribute("recentPosts", recentPosts);
        return "admin";
    }

    @GetMapping("/users")
    public String userManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String searchType,
            Model model) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> usersPage;
            
            if (search.isEmpty()) {
                usersPage = userService.getAllUsers(pageable);
            } else {
                usersPage = userService.searchUsers(searchType, search, pageable);
            }

            // roles가 null인 사용자들을 처리
            for (User user : usersPage.getContent()) {
                if (user.getRoles() == null) {
                    user.setRoles(new ArrayList<>());
                }
            }

            model.addAttribute("users", usersPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usersPage.getTotalPages());
            model.addAttribute("totalItems", usersPage.getTotalElements());
            model.addAttribute("size", size);
            model.addAttribute("search", search);
            model.addAttribute("searchType", searchType);
            model.addAttribute("searchTypes", new String[]{"all", "username", "fullName", "email"});
            
            return "admin/users";
        } catch (Exception e) {
            model.addAttribute("error", "사용자 목록을 불러오는데 실패했습니다: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/users/{id}")
    @ResponseBody
    public User getUserInfo(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping("/users/{id}/update")
    @ResponseBody
    public String updateUser(@PathVariable Long id, @RequestBody UserService.UpdateUserRequest request) {
        try {
            userService.updateUser(id, request);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/users/{id}/toggle-status")
    @ResponseBody
    public String toggleUserStatus(@PathVariable Long id) {
        try {
            userService.toggleUserStatus(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/users/{id}/delete")
    @ResponseBody
    public String deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/users/bulk-delete")
    @ResponseBody
    public String bulkDeleteUsers(@RequestBody List<Long> userIds) {
        try {
            userService.bulkDeleteUsers(userIds);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/posts")
    public String postManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String searchType,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage;
        
        if (search.isEmpty()) {
            postsPage = postService.getAllPosts(pageable);
        } else {
            postsPage = postService.searchPosts(searchType, search, pageable);
        }

        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalItems", postsPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("search", search);
        model.addAttribute("searchType", searchType);
        model.addAttribute("searchTypes", new String[]{"all", "title", "content", "author"});
        
        return "admin/posts";
    }

    @GetMapping("/posts/{id}")
    @ResponseBody
    public Post getPostInfo(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PostMapping("/posts/{id}/delete")
    @ResponseBody
    public String deletePost(@PathVariable Long id) {
        try {
            postService.deletePostByAdmin(id);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @PostMapping("/posts/bulk-delete")
    @ResponseBody
    public String bulkDeletePosts(@RequestBody List<Long> postIds) {
        try {
            postService.bulkDeletePosts(postIds);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
} 