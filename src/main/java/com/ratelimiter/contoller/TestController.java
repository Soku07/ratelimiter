package com.ratelimiter.contoller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TestController {
    @GetMapping("/get-users")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("This request is under rate limits");
    }
}
