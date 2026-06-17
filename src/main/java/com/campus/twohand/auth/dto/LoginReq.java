package com.campus.twohand.auth.dto;

public class LoginReq {
    private String username;
    private String password;
    private String role; // 前端下拉框传 USER / ADMIN（可选）

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
