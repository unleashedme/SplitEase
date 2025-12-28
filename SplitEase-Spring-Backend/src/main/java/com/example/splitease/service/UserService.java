package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.UserRepo;
import com.example.splitease.requestAndResponse.LogInCredentials;
import com.example.splitease.requestAndResponse.LogInResponse;
import com.example.splitease.requestAndResponse.RegisterUserDetails;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
public class UserService {


    @Autowired
    private UserRepo userRepo;

    @Autowired
    AuthenticationManager authManager;

    @Autowired
    private JWTService jwtService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    public void register(RegisterUserDetails registerUser){
        Users user = new Users();

        user.setPassword(encoder.encode(registerUser.getPassword()));
        user.setName(registerUser.getName());
        user.setEmail(registerUser.getEmail());
        user.setPhone(registerUser.getPhone());
        userRepo.save(user);
    }

    public LogInResponse verify(LogInCredentials logInCredentials){

        System.out.println("VERIFY CALLED");

        Authentication authentication =
                authManager.authenticate(new UsernamePasswordAuthenticationToken(logInCredentials.getEmail(), logInCredentials.getPassword()));

        if(!authentication.isAuthenticated()){
            throw new BadCredentialsException("Invalid credentials");
        }

        Users user = userRepo.findByEmail(logInCredentials.getEmail());
        String token =  jwtService.generateToken(user.getEmail());

        user.setLastActiveAt(Instant.now());
        userRepo.save(user);

        return new LogInResponse(token, user.getName(), user.getEmail(), user.getPhone());
    }

    @Transactional
    public void updateUserFcmToken(String email, String token) {
        Users user = userRepo.findByEmail(email);
        user.setFcmToken(token);
        userRepo.save(user);
    }
}
