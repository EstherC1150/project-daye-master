package com.project.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/posts";
    }

    @GetMapping("/user")
    public String user() {
        return "redirect:/posts"; // user 페이지를 갤러리로 리다이렉트
    }

    @GetMapping("/profile")
    public String profile() {
        return "redirect:/posts"; // profile 페이지를 갤러리로 리다이렉트
    }
} 