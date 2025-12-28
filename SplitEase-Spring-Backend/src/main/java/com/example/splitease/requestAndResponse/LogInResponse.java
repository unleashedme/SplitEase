package com.example.splitease.requestAndResponse;

public class LogInResponse {
    private String token;
    private String name;
    private String email;
    private String phone;

    public LogInResponse(String token, String name, String email, String phone) {
        this.token = token;
        this.name = name;
        this.email = email;
        this.phone = phone;
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
}
