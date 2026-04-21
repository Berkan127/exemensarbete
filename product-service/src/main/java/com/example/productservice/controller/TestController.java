package com.example.productservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @GetMapping
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Product Service is working!");
    }
    
    @PostMapping
    public ResponseEntity<String> create() {
        return ResponseEntity.ok("Created!");
    }
}
