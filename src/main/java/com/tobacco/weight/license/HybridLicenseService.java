package com.tobacco.weight.license;

import javafx.stage.Stage;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 混合许可证服务
 * 集成在线验证和离线缓存功能，提供完整的许可证管理解决方案
 */
public class HybridLicenseService {
    private static final Logger logger = LoggerFactory.getLogger(HybridLicenseService.class);
    
    private static HybridLicenseService instance;
    
    private final OnlineLicenseService onlineService;
    private final OfflineLicenseCache offlineCache;
    private final LicenseService legacyService; // 保持向后兼容
    private final LicenseConfig config;
    
    // 验证策略配置
    private boolean enableOnlineVerification = true;
    private boolean fallbackToOffline = true;
    private boolean forceLegacyMode = false;
    
    private HybridLicenseService() {
        this.config = LicenseConfig.getInstance();
        this.onlineService = new OnlineLicenseService();
        this.offlineCache = new OfflineLicenseCache();
        this.legacyService = LicenseService.getInstance();
        
        // 检查配置
        if (!onlineService.validateConfiguration()) {
            logger.warn("在线验证配置无效，将使用离线模式");
            enableOnlineVerification = false;
        }
    }
    
    public static synchronized HybridLicenseService getInstance() {
        if (instance == null) {
            instance = new HybridLicenseService();
        }
        return instance;
    }
    
    /**
     * 确保应用程序已获得许可
     * 优先使用在线验证，失败时回退到离线缓存或传统模式
     * 
     * @param primaryStage 主舞台（用于显示激活对话框）
     * @return 是否已获得许可
     */
    public boolean ensureLicensed(Stage primaryStage) {
        logger.info("开始许可证验证流程");
        
        // 打印配置摘要
        if (config.isStatusPrintEnabled()) {
            logger.info(config.getConfigurationSummary());
        }
        
        // 如果强制使用传统模式
        if (config.isLegacyModeForced()) {
            logger.info("使用传统许可证验证模式");
            return legacyService.ensureLicensed(primaryStage);
        }
        
        // 获取当前许可证信息
        LicenseInfo currentLicense = legacyService.getCurrentLicense();
        if (currentLicense == null) {
            logger.info("未找到本地许可证，需要激活");
            return showActivationDialog(primaryStage);
        }
        
        String licenseId = currentLicense.getLicenseId();
        String deviceFingerprint = HardwareFingerprint.generateFingerprint();
        
        // 1. 尝试在线验证
        if (config.isOnlineVerificationEnabled()) {
            LicenseVerificationResult onlineResult = verifyOnline(licenseId, deviceFingerprint);
            
            if (onlineResult == LicenseVerificationResult.SUCCESS) {
                logger.info("在线验证成功");
                // 缓存成功的验证结果
                JSONObject licenseData = createLicenseDataFromInfo(currentLicense);
                offlineCache.cacheLicenseVerification(licenseId, deviceFingerprint, onlineResult, licenseData);
                return true;
            }
            
            // 处理在线验证失败的情况
            if (onlineResult.isFatalError()) {
                logger.error("在线验证失败（致命错误）: {}", onlineResult);
                return handleFatalError(onlineResult, primaryStage);
            }
            
            // 网络错误，尝试离线验证
            if (onlineResult.isNetworkError() && config.isOfflineFallbackEnabled()) {
                logger.warn("在线验证网络错误，尝试离线验证: {}", onlineResult);
                return verifyOffline(licenseId, deviceFingerprint, primaryStage);
            }
        }
        
        // 2. 离线验证
        if (config.isOfflineFallbackEnabled()) {
            return verifyOffline(licenseId, deviceFingerprint, primaryStage);
        }
        
        // 3. 最后回退到传统验证
        logger.info("回退到传统许可证验证");
        return legacyService.ensureLicensed(primaryStage);
    }
    
    /**
     * 激活许可证（混合模式）
     * 
     * @param licenseId 许可证ID
     * @return 是否激活成功
     */
    public boolean activateLicense(String licenseId) {
        logger.info("开始激活许可证: {}", licenseId);
        
        String deviceFingerprint = HardwareFingerprint.generateFingerprint();
        
        // 1. 尝试在线激活
        if (config.isOnlineVerificationEnabled()) {
            LicenseVerificationResult onlineResult = verifyOnline(licenseId, deviceFingerprint);
            
            if (onlineResult == LicenseVerificationResult.SUCCESS) {
                logger.info("在线激活成功");
                
                // 同步到本地传统系统
                boolean legacyActivated = legacyService.activateLicense(licenseId);
                if (legacyActivated) {
                    // 缓存激活结果
                    LicenseInfo licenseInfo = legacyService.getCurrentLicense();
                    JSONObject licenseData = createLicenseDataFromInfo(licenseInfo);
                    offlineCache.cacheLicenseVerification(licenseId, deviceFingerprint, onlineResult, licenseData);
                    
                    logger.info("许可证激活完成（在线+本地）");
                    return true;
                } else {
                    logger.error("在线激活成功但本地同步失败");
                    return false;
                }
            }
            
            // 处理在线激活失败
            if (onlineResult.isFatalError()) {
                logger.error("在线激活失败: {}", onlineResult);
                return false;
            }
        }
        
        // 2. 回退到传统激活
        logger.info("回退到传统激活模式");
        return legacyService.activateLicense(licenseId);
    }
    
    /**
     * 在线验证许可证
     */
    private LicenseVerificationResult verifyOnline(String licenseId, String deviceFingerprint) {
        try {
            return onlineService.verifyLicenseOnline(licenseId, deviceFingerprint);
        } catch (Exception e) {
            logger.error("在线验证异常", e);
            return LicenseVerificationResult.NETWORK_ERROR;
        }
    }
    
    /**
     * 离线验证许可证
     */
    private boolean verifyOffline(String licenseId, String deviceFingerprint, Stage primaryStage) {
        logger.info("尝试离线验证");
        
        // 1. 检查离线缓存
        LicenseVerificationResult cacheResult = offlineCache.verifyFromCache(licenseId, deviceFingerprint);
        if (cacheResult == LicenseVerificationResult.SUCCESS) {
            logger.info("离线缓存验证成功");
            return true;
        }
        
        if (cacheResult != null) {
            logger.warn("离线缓存验证失败: {}", cacheResult);
            if (cacheResult.isFatalError()) {
                return handleFatalError(cacheResult, primaryStage);
            }
        }
        
        // 2. 回退到传统验证
        logger.info("离线缓存无效，使用传统验证");
        return legacyService.isLicensed();
    }
    
    /**
     * 处理致命错误
     */
    private boolean handleFatalError(LicenseVerificationResult result, Stage primaryStage) {
        switch (result) {
            case LICENSE_NOT_FOUND:
            case LICENSE_INACTIVE:
            case INVALID_LICENSE_FORMAT:
                logger.error("许可证无效，需要重新激活");
                return showActivationDialog(primaryStage);
                
            case LICENSE_EXPIRED:
                logger.error("许可证已过期");
                // 可以显示续费对话框
                return false;
                
            case MAX_DEVICES_EXCEEDED:
                logger.error("设备数量超限");
                // 可以显示设备管理对话框
                return false;
                
            default:
                logger.error("未知致命错误: {}", result);
                return false;
        }
    }
    
    /**
     * 显示激活对话框
     */
    private boolean showActivationDialog(Stage primaryStage) {
        try {
            LicenseActivationDialog dialog = new LicenseActivationDialog(primaryStage);
            java.util.Optional<String> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                String licenseId = result.get();
                return activateLicense(licenseId);
            }
            
            return false;
        } catch (Exception e) {
            logger.error("显示激活对话框失败", e);
            return false;
        }
    }
    
    /**
     * 从LicenseInfo创建JSON数据
     */
    private JSONObject createLicenseDataFromInfo(LicenseInfo licenseInfo) {
        JSONObject data = new JSONObject();
        data.put("licenseId", licenseInfo.getLicenseId());
        data.put("customerName", licenseInfo.getCustomerName());
        data.put("maxDevices", licenseInfo.getMaxDevices());
        
        // 处理可能为null的expiryDate
        if (licenseInfo.getExpiryDate() != null) {
            data.put("expiryDate", licenseInfo.getExpiryDate().toString());
        } else {
            // 设置默认过期时间为一年后
            data.put("expiryDate", java.time.LocalDateTime.now().plusYears(1).toString());
            logger.warn("许可证过期时间为null，设置默认过期时间为一年后");
        }
        
        data.put("status", "active");
        
        // 添加设备绑定信息
        org.json.JSONArray devices = new org.json.JSONArray();
        licenseInfo.getDeviceBindings().forEach(binding -> {
            JSONObject device = new JSONObject();
            device.put("deviceFingerprint", binding.getDeviceFingerprint());
            device.put("deviceName", binding.getDeviceName());
            device.put("bindTime", binding.getBindTime().toString());
            device.put("lastUsedTime", binding.getLastUsedTime().toString());
            device.put("active", binding.isActive());
            devices.put(device);
        });
        data.put("boundDevices", devices);
        
        return data;
    }
    
    /**
     * 获取当前许可证信息
     */
    public LicenseInfo getCurrentLicense() {
        return legacyService.getCurrentLicense();
    }
    
    /**
     * 获取许可证状态信息
     */
    public String getLicenseStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== 混合许可证服务状态 ===\n");
        info.append("在线验证: ").append(enableOnlineVerification ? "启用" : "禁用").append("\n");
        info.append("离线回退: ").append(fallbackToOffline ? "启用" : "禁用").append("\n");
        info.append("传统模式: ").append(forceLegacyMode ? "强制" : "自动").append("\n");
        info.append("\n");
        
        // 添加缓存统计
        JSONObject cacheStats = offlineCache.getCacheStats();
        info.append("=== 离线缓存统计 ===\n");
        info.append("总缓存条目: ").append(cacheStats.optInt("totalEntries", 0)).append("\n");
        info.append("有效条目: ").append(cacheStats.optInt("validEntries", 0)).append("\n");
        info.append("过期条目: ").append(cacheStats.optInt("expiredEntries", 0)).append("\n");
        info.append("\n");
        
        // 添加传统许可证信息
        info.append(legacyService.getLicenseStatusInfo());
        
        return info.toString();
    }
    
    /**
     * 清除所有缓存
     */
    public void clearCache() {
        offlineCache.clearCache();
        logger.info("已清除所有离线缓存");
    }
    
    /**
     * 重置许可证（包括缓存）
     */
    public void resetLicense() {
        legacyService.resetLicense();
        offlineCache.clearCache();
        logger.info("已重置许可证和缓存");
    }
    
    /**
     * 配置验证策略
     */
    public void configureVerificationStrategy(boolean enableOnline, boolean enableOfflineFallback, boolean forceLegacy) {
        this.enableOnlineVerification = enableOnline;
        this.fallbackToOffline = enableOfflineFallback;
        this.forceLegacyMode = forceLegacy;
        
        logger.info("验证策略已更新: 在线={}, 离线回退={}, 强制传统={}", 
                   enableOnline, enableOfflineFallback, forceLegacy);
    }
    
    /**
     * 检查是否需要续费提醒
     */
    public boolean needsRenewalReminder() {
        return legacyService.needsRenewalReminder();
    }
    
    /**
     * 获取剩余天数
     */
    public long getRemainingDays() {
        return legacyService.getRemainingDays();
    }
}