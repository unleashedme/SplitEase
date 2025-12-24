package com.example.splitease.service;

import com.example.splitease.models.*;
import com.example.splitease.repo.UserRepo;
import com.example.splitease.requestAndResponse.LogInCredentials;
import com.example.splitease.requestAndResponse.RegisterUserDetails;
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
    private UserRepo repo;

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
        user.setUpiId(registerUser.getUpiId());
        repo.save(user);
    }

    public UserRepo.LogInResponse verify(LogInCredentials logInCredentials){

        System.out.println("VERIFY CALLED");

        Authentication authentication =
                authManager.authenticate(new UsernamePasswordAuthenticationToken(logInCredentials.getEmail(), logInCredentials.getPassword()));

        if(!authentication.isAuthenticated()){
            throw new BadCredentialsException("Invalid credentials");
        }

        Users user = repo.findByEmail(logInCredentials.getEmail());
        String token =  jwtService.generateToken(user.getEmail());

        user.setLastActiveAt(Instant.now());
        repo.save(user);

        return new UserRepo.LogInResponse(token, user.getName(), user.getEmail(), user.getPhone(), user.getUpiId());
    }
}
