package com.tobacco.weight.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 许可证配置管理类
 * 负责读取和管理许可证验证相关的配置
 */
public class LicenseConfig {
    private static final Logger logger = LoggerFactory.getLogger(LicenseConfig.class);
    
    private static final String CONFIG_FILE = "/license_config.properties";
    private static LicenseConfig instance;
    
    private final Properties properties;
    
    private LicenseConfig() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    public static synchronized LicenseConfig getInstance() {
        if (instance == null) {
            instance = new LicenseConfig();
        }
        return instance;
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfiguration() {
        try (InputStream inputStream = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
                logger.info("许可证配置文件加载成功");
            } else {
                logger.warn("未找到许可证配置文件: {}", CONFIG_FILE);
                loadDefaultConfiguration();
            }
        } catch (IOException e) {
            logger.error("加载许可证配置文件失败", e);
            loadDefaultConfiguration();
        }
    }
    
    /**
     * 加载默认配置
     */
    private void loadDefaultConfiguration() {
        logger.info("使用默认许可证配置");
        
        // GitHub Gist 配置
        properties.setProperty("github.gist.id", "your_gist_id_here");
        properties.setProperty("github.token", "your_github_token_here");
        properties.setProperty("github.gist.filename", "licenses.json");
        
        // API 配置
        properties.setProperty("github.api.url", "https://api.github.com/gists/");
        properties.setProperty("connection.timeout", "10000");
        properties.setProperty("read.timeout", "15000");
        
        // 缓存配置
        properties.setProperty("cache.validity.hours", "72");
        properties.setProperty("cache.grace.period.hours", "24");
        
        // 验证策略配置
        properties.setProperty("online.verification.enabled", "true");
        properties.setProperty("offline.fallback.enabled", "true");
        properties.setProperty("legacy.mode.forced", "false");
        
        // 网络检查配置
        properties.setProperty("network.check.url", "https://api.github.com");
        properties.setProperty("network.check.timeout", "5000");
        
        // 日志配置
        properties.setProperty("logging.level", "INFO");
        properties.setProperty("logging.enable.status.print", "true");
    }
    
    // GitHub Gist 配置获取方法
    public String getGistId() {
        return properties.getProperty("github.gist.id", "your_gist_id_here");
    }
    
    public String getGithubToken() {
        return properties.getProperty("github.token", "your_github_token_here");
    }
    
    public String getGistFilename() {
        return properties.getProperty("github.gist.filename", "licenses.json");
    }
    
    public String getGithubApiUrl() {
        return properties.getProperty("github.api.url", "https://api.github.com/gists/");
    }
    
    // 连接配置获取方法
    public int getConnectionTimeout() {
        return getIntProperty("api.connection.timeout", 10000);
    }
    
    public int getReadTimeout() {
        return getIntProperty("api.read.timeout", 15000);
    }
    
    // 缓存配置获取方法
    public int getCacheValidityHours() {
        return getIntProperty("cache.validity.hours", 72);
    }
    
    public int getCacheGracePeriodHours() {
        return getIntProperty("cache.grace.period.hours", 24);
    }
    
    // 验证策略配置获取方法
    public boolean isOnlineVerificationEnabled() {
        return getBooleanProperty("online.verification.enabled", true);
    }
    
    public boolean isOfflineFallbackEnabled() {
        return getBooleanProperty("offline.fallback.enabled", true);
    }
    
    public boolean isLegacyModeForced() {
        return getBooleanProperty("legacy.mode.forced", false);
    }
    
    // 网络检查配置获取方法
    public String getNetworkCheckUrl() {
        return properties.getProperty("network.check.url", "https://api.github.com");
    }
    
    public int getNetworkCheckTimeout() {
        return getIntProperty("network.check.timeout", 5000);
    }
    
    // 日志配置获取方法
    public String getLoggingLevel() {
        return properties.getProperty("logging.level", "INFO");
    }
    
    public boolean isStatusPrintEnabled() {
        return getBooleanProperty("logging.enable.status.print", true);
    }
    
    /**
     * 验证配置是否有效
     */
    public boolean isConfigurationValid() {
        String gistId = getGistId();
        String token = getGithubToken();
        
        if ("your_gist_id_here".equals(gistId) || gistId.trim().isEmpty()) {
            logger.warn("GitHub Gist ID 未配置");
            return false;
        }
        
        if ("your_github_token_here".equals(token) || token.trim().isEmpty()) {
            logger.warn("GitHub Token 未配置");
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取配置摘要信息
     */
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== 许可证配置摘要 ===\n");
        summary.append("Gist ID: ").append(maskSensitiveInfo(getGistId())).append("\n");
        summary.append("Token: ").append(maskSensitiveInfo(getGithubToken())).append("\n");
        summary.append("文件名: ").append(getGistFilename()).append("\n");
        summary.append("在线验证: ").append(isOnlineVerificationEnabled() ? "启用" : "禁用").append("\n");
        summary.append("离线回退: ").append(isOfflineFallbackEnabled() ? "启用" : "禁用").append("\n");
        summary.append("强制传统模式: ").append(isLegacyModeForced() ? "是" : "否").append("\n");
        summary.append("缓存有效期: ").append(getCacheValidityHours()).append(" 小时\n");
        summary.append("配置有效性: ").append(isConfigurationValid() ? "有效" : "无效").append("\n");
        
        return summary.toString();
    }
    
    /**
     * 动态更新配置
     */
    public void updateConfiguration(String key, String value) {
        properties.setProperty(key, value);
        logger.info("配置已更新: {} = {}", key, maskSensitiveInfo(value));
    }
    
    /**
     * 获取整数属性
     */
    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            logger.warn("配置项 {} 的值无效，使用默认值: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔属性
     */
    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim());
    }
    
    /**
     * 掩码敏感信息
     */
    private String maskSensitiveInfo(String info) {
        if (info == null || info.length() <= 8) {
            return "****";
        }
        return info.substring(0, 4) + "****" + info.substring(info.length() - 4);
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfiguration() {
        logger.info("重新加载许可证配置");
        loadConfiguration();
    }
}