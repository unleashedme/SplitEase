package com.example.splitease.requestAndResponse;

public class RegisterUserDetails {
    private String name;
    private String email;
    private String phone;
    private String upiId;
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
