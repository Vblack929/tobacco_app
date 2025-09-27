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
    
    private boolean enableOnlineVerification = true;
    private boolean fallbackToOffline = true;
    private boolean forceLegacyMode = false;
    
    private HybridLicenseService() {
        this.config = LicenseConfig.getInstance();
        this.onlineService = new OnlineLicenseService();
        this.offlineCache = new OfflineLicenseCache();
        this.legacyService = LicenseService.getInstance();
        
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
        
        if (config.isStatusPrintEnabled()) {
            logger.info(config.getConfigurationSummary());
        }
        
        if (config.isLegacyModeForced()) {
            logger.info("使用传统许可证验证模式");
            return legacyService.ensureLicensed(primaryStage);
        }
        
        LicenseInfo currentLicense = legacyService.getCurrentLicense();
        if (currentLicense == null) {
            logger.info("未找到本地许可证，需要激活");
            return showActivationDialog(primaryStage);
        }
        
        String licenseId = currentLicense.getLicenseId();
        String deviceFingerprint = HardwareFingerprint.generateFingerprint();
        
        if (config.isOnlineVerificationEnabled()) {
            OnlineLicenseResponse onlineResponse = verifyOnline(licenseId, deviceFingerprint);
            LicenseVerificationResult onlineResult = onlineResponse.getResult();
            
            if (onlineResult == LicenseVerificationResult.SUCCESS) {
                logger.info("在线验证成功");
                JSONObject licenseData = onlineResponse.getLicenseData();
                legacyService.updateFromRemote(licenseData);
                if (licenseData != null) {
                    offlineCache.cacheLicenseVerification(licenseId, deviceFingerprint, onlineResult, licenseData);
                }
                return true;
            }
            
            if (onlineResult.isFatalError()) {
                logger.error("在线验证失败（致命错误）: {}", onlineResult);
                return handleFatalError(onlineResult, primaryStage);
            }
            
            if (onlineResult.isNetworkError() && config.isOfflineFallbackEnabled()) {
            logger.warn("在线激活过程中出现网络问题: {}", onlineResult);
                return verifyOffline(licenseId, deviceFingerprint, primaryStage);
            }
            
            if (onlineResult.isNetworkError()) {
            logger.warn("在线激活过程中出现网络问题: {}", onlineResult);
                return false;
            }
        }
        
        if (config.isOfflineFallbackEnabled()) {
            return verifyOffline(licenseId, deviceFingerprint, primaryStage);
        }
        
        logger.warn("没有可用的在线验证结果，无法回退传统模式，许可证不可用");
        return false;
    }
    
    /**
     * 激活许可证（混合模式）
     */
    public boolean activateLicense(String licenseId) {
        logger.info("开始激活许可证: {}", licenseId);
        
        String deviceFingerprint = HardwareFingerprint.generateFingerprint();
        LicenseType licenseType = resolveLicenseType(licenseId);
        
        if (config.isOnlineVerificationEnabled()) {
            OnlineLicenseResponse onlineResponse = verifyOnline(licenseId, deviceFingerprint);
            LicenseVerificationResult onlineResult = onlineResponse.getResult();
            
            if (onlineResult == LicenseVerificationResult.SUCCESS) {
                logger.info("在线激活成功");
                JSONObject licenseData = onlineResponse.getLicenseData();
                legacyService.updateFromRemote(licenseData);
                if (licenseData != null) {
                    offlineCache.cacheLicenseVerification(licenseId, deviceFingerprint, onlineResult, licenseData);
                }
                return true;
            }
            
            if (onlineResult.isFatalError()) {
                logger.error("在线激活失败: {}", onlineResult);
                return false;
            }
            
            if (onlineResult.isNetworkError()) {
                logger.warn("在线激活过程中出现网络问题: {}", onlineResult);
            } else if (onlineResult != LicenseVerificationResult.SUCCESS) {
                logger.error("在线激活失败: {}", onlineResult);
                return false;
            }
        }
        if (!licenseType.isUnlimitedDevices()) {
            logger.error("甲方授权密钥必须联网激活，当前无法完成激活。");
            return false;
        }
        
        logger.info("回退到传统激活模式（开发者不限设备）");
        return legacyService.activateLicense(licenseId);
    }
    
    private OnlineLicenseResponse verifyOnline(String licenseId, String deviceFingerprint) {
        try {
            return onlineService.verifyLicenseOnline(licenseId, deviceFingerprint);
        } catch (Exception e) {
            logger.error("在线验证异常", e);
            return OnlineLicenseResponse.failure(LicenseVerificationResult.SERVER_ERROR);
        }
    }

    private boolean verifyOffline(String licenseId, String deviceFingerprint, Stage primaryStage) {
        logger.info("尝试离线验证");
        
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
        

        if (legacyService.isLicensed()) {
            return true;
        }

        logger.warn("当前设备暂时无法验证，请联网后再次启动应用");
        return false;
    }
    
    private boolean handleFatalError(LicenseVerificationResult result, Stage primaryStage) {
        switch (result) {
            case LICENSE_NOT_FOUND:
            case LICENSE_INACTIVE:
            case INVALID_LICENSE_FORMAT:
                logger.error("许可证无效，需要重新激活");
                return showActivationDialog(primaryStage);
            case LICENSE_EXPIRED:
                logger.error("许可证已过期");
                return false;
            case MAX_DEVICES_EXCEEDED:
                logger.error("许可证已达到最大设备数量");
                return false;
            default:
                logger.error("未知致命错误: {}", result);
                return false;
        }
    }
    
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
    
    private JSONObject createLicenseDataFromInfo(LicenseInfo licenseInfo) {
        JSONObject data = new JSONObject();
        data.put("licenseId", licenseInfo.getLicenseId());
        data.put("customerName", licenseInfo.getCustomerName());
        data.put("licenseType", licenseInfo.getLicenseType().name());
        data.put("maxDevices", licenseInfo.isUnlimitedDevices() ? Integer.MAX_VALUE : licenseInfo.getMaxDevices());
        data.put("unlimited", licenseInfo.isUnlimitedDevices());
        
        if (licenseInfo.getExpiryDate() != null) {
            data.put("expiryDate", licenseInfo.getExpiryDate().toString());
        } else {
            data.put("expiryDate", JSONObject.NULL);
        }
        
        data.put("status", "active");
        
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
    
    public LicenseInfo getCurrentLicense() {
        return legacyService.getCurrentLicense();
    }
    
    public String getLicenseStatusInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== 混合许可证服务状态 ===\n");
        info.append("在线验证: ").append(enableOnlineVerification ? "启用" : "禁用").append("\n");
        info.append("离线回退: ").append(fallbackToOffline ? "启用" : "禁用").append("\n");
        info.append("传统模式: ").append(forceLegacyMode ? "强制" : "自动").append("\n");
        info.append("\n");
        
        JSONObject cacheStats = offlineCache.getCacheStats();
        info.append("=== 离线缓存统计 ===\n");
        info.append("总缓存条目: ").append(cacheStats.optInt("totalEntries", 0)).append("\n");
        info.append("有效条目: ").append(cacheStats.optInt("validEntries", 0)).append("\n");
        info.append("过期条目: ").append(cacheStats.optInt("expiredEntries", 0)).append("\n");
        info.append("\n");
        
        info.append(legacyService.getLicenseStatusInfo());
        
        return info.toString();
    }
    
    public void clearCache() {
        offlineCache.clearCache();
        logger.info("已清除所有离线缓存");
    }
    
    public void resetLicense() {
        legacyService.resetLicense();
        offlineCache.clearCache();
        logger.info("已重置许可证和缓存");
    }
    
    public void configureVerificationStrategy(boolean enableOnline, boolean enableOfflineFallback, boolean forceLegacy) {
        this.enableOnlineVerification = enableOnline;
        this.fallbackToOffline = enableOfflineFallback;
        this.forceLegacyMode = forceLegacy;
        
        logger.info("验证策略已更新: 在线={}, 离线回退={}, 强制传统={}",
                enableOnline, enableOfflineFallback, forceLegacy);
    }
    
    public boolean needsRenewalReminder() {
        return legacyService.needsRenewalReminder();
    }
    
    public long getRemainingDays() {
        return legacyService.getRemainingDays();
    }
    
    private LicenseType resolveLicenseType(String licenseId) {
        if (licenseId == null || !licenseId.contains("-")) {
            return LicenseType.CUSTOMER_LIMITED;
        }
        String[] parts = licenseId.split("-");
        if (parts.length < 3) {
            return LicenseType.CUSTOMER_LIMITED;
        }
        return LicenseType.fromTierCode(parts[2]);
    }
}


