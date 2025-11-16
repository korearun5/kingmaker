package com.kore.king.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorController {

    @GetMapping("/error/access-denied")
    public String accessDenied() {
        return "error/403"; // This should match your template name
    }
    
    @GetMapping("/error/403")
    public String error403() {
        return "error/403";
    }
}