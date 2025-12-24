package com.example.splitease.service;

import com.example.splitease.models.UserPrincipal;
import com.example.splitease.models.Users;
import com.example.splitease.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepo repo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Users user = repo.findByEmail(email);

        if(user==null){
            System.out.println("User not found");
            throw new UsernameNotFoundException("User not found");
        }

        return new UserPrincipal(user);
    }
}
