package com.example.splitease.repo;

import com.example.splitease.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<Users, UUID> {

    Users findByEmail(String email);

    class LogInResponse {
        private String token;
        private String name;
        private String email;
        private String phone;
        private String upiId;

        public LogInResponse(String token, String name, String email, String phone, String upiId) {
            this.token = token;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.upiId = upiId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getUpiId() {
            return upiId;
        }

        public void setUpiId(String upiId) {
            this.upiId = upiId;
        }
    }
}
