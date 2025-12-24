package com.example.splitease.controller;

import com.example.splitease.repo.UserRepo;
import com.example.splitease.requestAndResponse.LogInCredentials;
import com.example.splitease.requestAndResponse.RegisterUserDetails;
import com.example.splitease.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    UserService service;


    @PostMapping("/register")
    public void register(@RequestBody RegisterUserDetails user){
        service.register(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserRepo.LogInResponse> login(@RequestBody LogInCredentials user){
        UserRepo.LogInResponse result = service.verify(user);
        return ResponseEntity.ok(result);
    }
}
