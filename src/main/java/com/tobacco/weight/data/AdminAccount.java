package com.tobacco.weight.data;

import java.util.Date;

/**
 * 管理员账户数据模型
 */
public class AdminAccount {
    
    private Long id;
    private String username;
    private String password; // 注意：实际应用中应该存储加密后的密码
    private String fullName;
    private String role; // "admin" 或 "super_admin"
    private boolean isActive;
    private Date createdAt;
    private Date lastLoginAt;

    public AdminAccount() {
        this.isActive = true;
        this.createdAt = new Date();
    }

    public AdminAccount(String username, String password, String fullName, String role) {
        this();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /**
     * 验证密码
     */
    public boolean validatePassword(String inputPassword) {
        return this.password != null && this.password.equals(inputPassword);
    }

    /**
     * 是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return "super_admin".equals(role);
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLogin() {
        this.lastLoginAt = new Date();
    }

    @Override
    public String toString() {
        return "AdminAccount{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}