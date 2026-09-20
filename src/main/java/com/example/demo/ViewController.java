package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller 
public class ViewController {

    @GetMapping("/admin")
    public String adminPage() {
        return "admin.html"; 
    }

    @GetMapping("/inspector")
    public String inspectorPage() {
        return "inspector.html";
    }
}