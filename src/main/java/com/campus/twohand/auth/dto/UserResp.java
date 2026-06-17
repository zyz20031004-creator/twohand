package com.campus.twohand.auth.dto;

public class UserResp {
    private Long id;
    private String username;
    private String role;
    private String name;
    private String avatar;
    private Boolean isSuperAdmin;

    public UserResp() {}

    public UserResp(Long id, String username, String role) {
        this(id, username, role, null, null, false);
    }

    public UserResp(Long id, String username, String role, String name, String avatar, Boolean isSuperAdmin) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.name = name;
        this.avatar = avatar;
        this.isSuperAdmin = isSuperAdmin;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Boolean getIsSuperAdmin() { return isSuperAdmin; }
    public void setIsSuperAdmin(Boolean isSuperAdmin) { this.isSuperAdmin = isSuperAdmin; }
}
