package com.example.orderservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @GetMapping
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Order Service is working!");
    }
}
