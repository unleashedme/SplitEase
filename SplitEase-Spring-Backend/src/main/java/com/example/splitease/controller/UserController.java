package com.example.splitease.controller;

import com.example.splitease.repo.UserRepo;
import com.example.splitease.requestAndResponse.LogInCredentials;
import com.example.splitease.requestAndResponse.LogInResponse;
import com.example.splitease.requestAndResponse.RegisterUserDetails;
import com.example.splitease.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
public class UserController {

    @Autowired
    UserService userService;


    @PostMapping("/register")
    public void register(@RequestBody RegisterUserDetails user){
        userService.register(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LogInResponse> login(@RequestBody LogInCredentials user){
        LogInResponse result = userService.verify(user);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/update-token")
    public ResponseEntity<?> updateFcmToken(@RequestParam String token, Principal principal) {
        userService.updateUserFcmToken(principal.getName(), token);
        return ResponseEntity.ok().build();
    }
}
