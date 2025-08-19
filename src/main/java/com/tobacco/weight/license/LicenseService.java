package com.tobacco.weight.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 动态绑定许可证服务
 * 负责许可证的验证、设备绑定和状态管理
 */
public class LicenseService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);
    private static final String LICENSE_FILE = "license.json";
    private static LicenseService instance;
    
    private final ObjectMapper objectMapper;
    private LicenseInfo currentLicense;
    private boolean isLicensed = false;
    
    private LicenseService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        loadLicense();
    }
    
    public static synchronized LicenseService getInstance() {
        if (instance == null) {
            instance = new LicenseService();
        }
        return instance;
    }
    
    /**
     * 确保应用程序已获得许可
     * @param primaryStage 主舞台（用于显示激活对话框）
     * @return 是否已获得许可
     */
    public boolean ensureLicensed(Stage primaryStage) {
        if (isLicensed()) {
            return true;
        }
        
        // 显示许可证激活对话框
        LicenseActivationDialog dialog = new LicenseActivationDialog(primaryStage);
        Optional<String> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            String licenseId = result.get();
            return activateLicense(licenseId);
        }
        
        return false;
    }
    
    /**
     * 检查当前是否已获得许可
     */
    public boolean isLicensed() {
        if (currentLicense == null) {
            return false;
        }
        
        // 检查许可证是否过期
        if (currentLicense.isExpired()) {
            logger.warn("许可证已过期: {}", currentLicense.getExpiryDate());
            isLicensed = false;
            return false;
        }
        
        // 检查当前设备是否已绑定
        String currentFingerprint = HardwareFingerprint.generateFingerprint();
        boolean isDeviceBound = currentLicense.getDeviceBindings().stream()
                .anyMatch(binding -> binding.getDeviceFingerprint().equals(currentFingerprint) && binding.isActive());
        
        if (!isDeviceBound) {
            logger.warn("当前设备未绑定到许可证");
            isLicensed = false;
            return false;
        }
        
        // 更新设备最后使用时间
        updateDeviceLastUsed(currentFingerprint);
        
        isLicensed = true;
        return true;
    }
    
    /**
     * 激活许可证
     * @param licenseId 许可证ID
     * @return 是否激活成功
     */
    public boolean activateLicense(String licenseId) {
        try {
            // 验证许可证ID格式
            if (!LicenseIdGenerator.validateLicenseId(licenseId)) {
                logger.error("无效的许可证ID格式: {}", licenseId);
                return false;
            }
            
            // 解析许可证信息
            LicenseInfo licenseInfo = LicenseIdGenerator.parseLicenseId(licenseId);
            if (licenseInfo == null) {
                logger.error("无法解析许可证ID: {}", licenseId);
                return false;
            }
            
            // 检查许可证是否过期
            if (licenseInfo.isExpired()) {
                logger.error("许可证已过期: {}", licenseInfo.getExpiryDate());
                return false;
            }
            
            // 获取当前设备信息
            String currentFingerprint = HardwareFingerprint.generateFingerprint();
            String deviceName = HardwareFingerprint.getDeviceName();
            
            // 检查设备是否已绑定
            Optional<DeviceBinding> existingBinding = licenseInfo.getDeviceBindings().stream()
                    .filter(binding -> binding.getDeviceFingerprint().equals(currentFingerprint))
                    .findFirst();
            
            if (existingBinding.isPresent()) {
                // 设备已绑定，激活设备
                DeviceBinding binding = existingBinding.get();
                binding.setActive(true);
                binding.setLastUsedTime(LocalDateTime.now());
                logger.info("设备重新激活: {} ({})", deviceName, HardwareFingerprint.formatFingerprint(currentFingerprint));
            } else {
                // 新设备绑定
                if (!licenseInfo.canBindMoreDevices()) {
                    logger.error("许可证已达到最大设备绑定数量: {}/{}", 
                            licenseInfo.getActiveDeviceCount(), licenseInfo.getMaxDevices());
                    return false;
                }
                
                // 创建新的设备绑定
                DeviceBinding newBinding = new DeviceBinding(
                        currentFingerprint,
                        deviceName,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        true
                );
                
                licenseInfo.addDeviceBinding(newBinding);
                logger.info("新设备绑定成功: {} ({})", deviceName, HardwareFingerprint.formatFingerprint(currentFingerprint));
            }
            
            // 标记许可证为已激活
            licenseInfo.setActivated(true);
            
            // 保存许可证信息
            this.currentLicense = licenseInfo;
            saveLicense();
            
            this.isLicensed = true;
            logger.info("许可证激活成功: {} - 客户: {}", licenseId, licenseInfo.getCustomerName());
            
            return true;
            
        } catch (Exception e) {
            logger.error("激活许可证失败: {}", licenseId, e);
            return false;
        }
    }
    
    /**
     * 获取当前许可证信息
     */
    public LicenseInfo getCurrentLicense() {
        return currentLicense;
    }
    
    /**
     * 获取许可证状态信息（用于显示）
     */
    public String getLicenseStatusInfo() {
        if (currentLicense == null) {
            return "未激活";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("许可证ID: ").append(currentLicense.getLicenseId()).append("\n");
        info.append("客户名称: ").append(currentLicense.getCustomerName()).append("\n");
        info.append("最大设备数: ").append(currentLicense.getMaxDevices()).append("\n");
        info.append("已绑定设备: ").append(currentLicense.getActiveDeviceCount()).append("\n");
        info.append("有效期至: ").append(currentLicense.getExpiryDate()).append("\n");
        info.append("状态: ").append(currentLicense.isExpired() ? "已过期" : "有效");
        
        return info.toString();
    }
    
    /**
     * 获取设备绑定信息（用于显示）
     */
    public String getDeviceBindingInfo() {
        if (currentLicense == null || currentLicense.getDeviceBindings().isEmpty()) {
            return "无设备绑定";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("已绑定设备:\n");
        
        for (DeviceBinding binding : currentLicense.getDeviceBindings()) {
            if (binding.isActive()) {
                info.append("- ").append(binding.getDeviceName())
                    .append(" (").append(binding.getDisplayFingerprint()).append(")")
                    .append(" - 最后使用: ").append(binding.getLastUsedTime())
                    .append("\n");
            }
        }
        
        return info.toString();
    }
    
    /**
     * 重置许可证（清除本地许可证信息）
     */
    public void resetLicense() {
        this.currentLicense = null;
        this.isLicensed = false;
        
        File licenseFile = new File(LICENSE_FILE);
        if (licenseFile.exists()) {
            licenseFile.delete();
            logger.info("许可证信息已清除");
        }
    }
    
    /**
     * 更新设备最后使用时间
     */
    private void updateDeviceLastUsed(String deviceFingerprint) {
        if (currentLicense != null) {
            currentLicense.getDeviceBindings().stream()
                    .filter(binding -> binding.getDeviceFingerprint().equals(deviceFingerprint))
                    .findFirst()
                    .ifPresent(binding -> {
                        binding.setLastUsedTime(LocalDateTime.now());
                        saveLicense();
                    });
        }
    }
    
    /**
     * 加载本地许可证信息
     */
    private void loadLicense() {
        File licenseFile = new File(LICENSE_FILE);
        if (!licenseFile.exists()) {
            logger.debug("许可证文件不存在: {}", LICENSE_FILE);
            return;
        }
        
        try {
            this.currentLicense = objectMapper.readValue(licenseFile, LicenseInfo.class);
            logger.info("许可证信息加载成功: {}", currentLicense.getLicenseId());
            
            // 验证加载的许可证
            if (isLicensed()) {
                logger.info("许可证验证通过");
            } else {
                logger.warn("许可证验证失败");
            }
            
        } catch (IOException e) {
            logger.error("加载许可证文件失败: {}", LICENSE_FILE, e);
            this.currentLicense = null;
        }
    }
    
    /**
     * 保存许可证信息到本地
     */
    private void saveLicense() {
        if (currentLicense == null) {
            return;
        }
        
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(LICENSE_FILE), currentLicense);
            logger.debug("许可证信息保存成功: {}", LICENSE_FILE);
        } catch (IOException e) {
            logger.error("保存许可证文件失败: {}", LICENSE_FILE, e);
        }
    }
    
    /**
     * 检查许可证是否需要续期提醒
     */
    public boolean needsRenewalReminder() {
        if (currentLicense == null || currentLicense.isExpired()) {
            return false;
        }
        
        // 如果距离过期时间少于30天，显示续期提醒
        LocalDateTime expiryDate = currentLicense.getExpiryDate();
        LocalDateTime reminderDate = expiryDate.minusDays(30);
        
        return LocalDateTime.now().isAfter(reminderDate);
    }
    
    /**
     * 获取许可证剩余天数
     */
    public long getRemainingDays() {
        if (currentLicense == null || currentLicense.isExpired()) {
            return 0;
        }
        
        return java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                currentLicense.getExpiryDate().toLocalDate()
        );
    }
}