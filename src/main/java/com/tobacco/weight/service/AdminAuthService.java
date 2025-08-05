package com.tobacco.weight.service;

import com.tobacco.weight.data.AdminAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员认证服务
 * 管理管理员登录、认证和账户管理
 */
public class AdminAuthService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthService.class);
    private static AdminAuthService instance;
    
    // 默认管理员账户（实际应用中应存储在数据库中）
    private Map<String, AdminAccount> adminAccounts;
    private AdminAccount currentLoggedInAdmin;

    private AdminAuthService() {
        initializeDefaultAccounts();
    }

    public static synchronized AdminAuthService getInstance() {
        if (instance == null) {
            instance = new AdminAuthService();
        }
        return instance;
    }

    /**
     * 初始化默认管理员账户
     */
    private void initializeDefaultAccounts() {
        adminAccounts = new HashMap<>();
        
        // 默认管理员账户1：普通管理员
        AdminAccount admin1 = new AdminAccount("admin", "admin123", "系统管理员", "admin");
        admin1.setId(1L);
        adminAccounts.put("admin", admin1);
        
        // 默认管理员账户2：超级管理员
        AdminAccount admin2 = new AdminAccount("superadmin", "super123", "超级管理员", "super_admin");
        admin2.setId(2L);
        adminAccounts.put("superadmin", admin2);
        
        logger.info("默认管理员账户初始化完成，共 {} 个账户", adminAccounts.size());
    }

    /**
     * 管理员登录
     */
    public LoginResult login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return new LoginResult(false, "用户名不能为空", null);
        }
        
        if (password == null || password.trim().isEmpty()) {
            return new LoginResult(false, "密码不能为空", null);
        }

        AdminAccount account = adminAccounts.get(username.trim());
        if (account == null) {
            logger.warn("登录失败：用户名不存在 - {}", username);
            return new LoginResult(false, "用户名或密码错误", null);
        }

        if (!account.isActive()) {
            logger.warn("登录失败：账户已禁用 - {}", username);
            return new LoginResult(false, "账户已被禁用", null);
        }

        if (!account.validatePassword(password)) {
            logger.warn("登录失败：密码错误 - {}", username);
            return new LoginResult(false, "用户名或密码错误", null);
        }

        // 登录成功
        account.updateLastLogin();
        currentLoggedInAdmin = account;
        
        logger.info("管理员登录成功 - 用户名: {}, 姓名: {}, 角色: {}", 
                   account.getUsername(), account.getFullName(), account.getRole());
        
        return new LoginResult(true, "登录成功", account);
    }

    /**
     * 管理员登出
     */
    public void logout() {
        if (currentLoggedInAdmin != null) {
            logger.info("管理员登出 - 用户名: {}", currentLoggedInAdmin.getUsername());
            currentLoggedInAdmin = null;
        }
    }

    /**
     * 获取当前登录的管理员
     */
    public AdminAccount getCurrentAdmin() {
        return currentLoggedInAdmin;
    }

    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        return currentLoggedInAdmin != null;
    }

    /**
     * 检查当前用户是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return currentLoggedInAdmin != null && currentLoggedInAdmin.isSuperAdmin();
    }

    /**
     * 验证管理员权限
     */
    public boolean hasPermission(String permission) {
        if (!isLoggedIn()) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        // 根据权限类型判断
        switch (permission) {
            case "VIEW_ADMIN_PANEL":
            case "VIEW_FARMER_STATS":
            case "EXPORT_DATA":
            case "MANAGE_FARMERS":
                return true; // 普通管理员可以执行这些操作
            
            case "MANAGE_ACCOUNTS":
            case "SYSTEM_SETTINGS":
                return isSuperAdmin(); // 只有超级管理员可以执行
                
            default:
                return false;
        }
    }

    /**
     * 获取所有管理员账户（仅供超级管理员使用）
     */
    public Map<String, AdminAccount> getAllAccounts() {
        if (!isSuperAdmin()) {
            throw new SecurityException("只有超级管理员才能查看所有账户");
        }
        return new HashMap<>(adminAccounts);
    }

    /**
     * 添加新的管理员账户（仅供超级管理员使用）
     */
    public boolean addAccount(AdminAccount account) {
        if (!isSuperAdmin()) {
            throw new SecurityException("只有超级管理员才能添加账户");
        }

        if (adminAccounts.containsKey(account.getUsername())) {
            return false; // 用户名已存在
        }

        account.setId((long) (adminAccounts.size() + 1));
        adminAccounts.put(account.getUsername(), account);
        logger.info("新增管理员账户 - 用户名: {}, 姓名: {}", account.getUsername(), account.getFullName());
        return true;
    }

    /**
     * 登录结果封装类
     */
    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final AdminAccount account;

        public LoginResult(boolean success, String message, AdminAccount account) {
            this.success = success;
            this.message = message;
            this.account = account;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public AdminAccount getAccount() {
            return account;
        }
    }
}