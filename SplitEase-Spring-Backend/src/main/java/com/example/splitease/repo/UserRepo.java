package com.example.splitease.repo;

import com.example.splitease.models.Users;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<Users, UUID> {

    Users findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE Users u SET u.fcmToken = null WHERE u.fcmToken = :token")
    void clearFcmTokenByToken(@Param("token") String token);
}
