package com.tobacco.weight.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
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
     *
     * @param primaryStage 主舞台（用于显示激活对话框）
     * @return 是否已获得许可
     */
    public boolean ensureLicensed(Stage primaryStage) {
        if (isLicensed()) {
            return true;
        }

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

        ensureLicenseDefaults(currentLicense);

        if (currentLicense.isExpired()) {
            logger.warn("许可证已过期: {}", currentLicense.getExpiryDate());
            isLicensed = false;
            return false;
        }

        String currentFingerprint = HardwareFingerprint.generateFingerprint();
        boolean isDeviceBound = currentLicense.getDeviceBindings().stream()
                .anyMatch(binding -> binding.getDeviceFingerprint().equals(currentFingerprint) && binding.isActive());

        if (!isDeviceBound) {
            logger.warn("当前设备未绑定到许可证");
            isLicensed = false;
            return false;
        }

        updateDeviceLastUsed(currentFingerprint);
        printLicenseStatusInfo();

        isLicensed = true;
        return true;
    }

    /**
     * 激活许可证
     *
     * @param licenseId 许可证ID
     * @return 是否激活成功
     */
    public boolean activateLicense(String licenseId) {
        try {
            if (!LicenseIdGenerator.validateLicenseId(licenseId)) {
                logger.error("无效的许可证ID格式: {}", licenseId);
                return false;
            }

            LicenseInfo parsedInfo = LicenseIdGenerator.parseLicenseId(licenseId);
            if (parsedInfo == null) {
                logger.error("无法解析许可证ID: {}", licenseId);
                return false;
            }

            ensureLicenseDefaults(parsedInfo);
            LicenseInfo licenseInfo = mergeWithExisting(parsedInfo);

            if (licenseInfo.isExpired()) {
                logger.error("许可证已过期: {}", licenseInfo.getExpiryDate());
                return false;
            }

            String currentFingerprint = HardwareFingerprint.generateFingerprint();
            String deviceName = HardwareFingerprint.getDeviceName();

            Optional<DeviceBinding> existingBinding = licenseInfo.getDeviceBindings().stream()
                    .filter(binding -> binding.getDeviceFingerprint().equals(currentFingerprint))
                    .findFirst();

            if (existingBinding.isPresent()) {
                DeviceBinding binding = existingBinding.get();
                binding.setActive(true);
                binding.setLastUsedTime(LocalDateTime.now());
                logger.info("设备重新激活: {} ({})", deviceName,
                        HardwareFingerprint.formatFingerprint(currentFingerprint));
            } else {
                if (!licenseInfo.canBindMoreDevices()) {
                    int activeDevices = licenseInfo.getActiveDeviceCount();
                    String limitLabel = licenseInfo.isUnlimitedDevices() ? "不限制" : String.valueOf(licenseInfo.getMaxDevices());
                    logger.error("许可证已达到最大设备绑定数量: {}/{}", activeDevices, limitLabel);
                    return false;
                }

                DeviceBinding newBinding = new DeviceBinding(
                        currentFingerprint,
                        deviceName,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        true);
                licenseInfo.addDeviceBinding(newBinding);
                logger.info("新设备绑定成功: {} ({})", deviceName,
                        HardwareFingerprint.formatFingerprint(currentFingerprint));
            }

            licenseInfo.setLicenseId(licenseId);
            licenseInfo.setActivated(true);

            this.currentLicense = licenseInfo;
            saveLicense();

            this.isLicensed = true;
            logger.info("许可证激活成功: {} - 客户: {} - 类型: {}", licenseId,
                    licenseInfo.getCustomerName(), licenseInfo.getLicenseType());

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

        ensureLicenseDefaults(currentLicense);

        StringBuilder info = new StringBuilder();
        info.append("许可证ID: ").append(currentLicense.getLicenseId()).append("\n");
        info.append("客户名称: ").append(currentLicense.getCustomerName()).append("\n");
        info.append("许可证类型: ").append(currentLicense.getLicenseType()).append("\n");
        info.append("最大设备数: ")
                .append(currentLicense.isUnlimitedDevices() ? "不限制" : currentLicense.getMaxDevices())
                .append("\n");
        info.append("已绑定设备: ").append(currentLicense.getActiveDeviceCount()).append("\n");
        if (currentLicense.getExpiryDate() != null) {
            info.append("有效期至: ").append(currentLicense.getExpiryDate()).append("\n");
            info.append("状态: ").append(currentLicense.isExpired() ? "已过期" : "有效");
        } else {
            info.append("有效期: 永久有效\n");
            info.append("状态: 有效");
        }
        return info.toString();
    }

    /**
     * 获取设备绑定信息（用于显示）
     */
    public String getDeviceBindingInfo() {
        if (currentLicense == null || currentLicense.getDeviceBindings().isEmpty()) {
            return "暂无任何设备绑定";
        }

        StringBuilder info = new StringBuilder();
        info.append("已绑定设备\n");

        for (DeviceBinding binding : currentLicense.getDeviceBindings()) {
            if (binding.isActive()) {
                info.append("- ").append(binding.getDeviceName())
                        .append(" (").append(binding.getDisplayFingerprint()).append(")")
                        .append(" - 最后使用 ").append(binding.getLastUsedTime())
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
        if (licenseFile.exists() && licenseFile.delete()) {
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
            ensureLicenseDefaults(this.currentLicense);
            logger.info("许可证信息加载成功: {}", currentLicense.getLicenseId());

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

        ensureLicenseDefaults(currentLicense);

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

        if (currentLicense.getExpiryDate() == null) {
            return false;
        }

        LocalDateTime expiryDate = currentLicense.getExpiryDate();
        LocalDateTime reminderDate = expiryDate.minusDays(30);

        return LocalDateTime.now().isAfter(reminderDate);
    }

    /**
     * 获取许可证剩余天数
     */
    public long getRemainingDays() {
        if (currentLicense == null || currentLicense.isExpired() || currentLicense.getExpiryDate() == null) {
            return 0;
        }

        return java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now().toLocalDate(),
                currentLicense.getExpiryDate().toLocalDate());
    }

    /**
     * 打印许可证状态信息到控制台
     */
    private void printLicenseStatusInfo() {
        if (currentLicense == null) {
            return;
        }

        ensureLicenseDefaults(currentLicense);

        System.out.println("\n=== 许可证状态信息 ===");
        System.out.println("许可证ID: " + currentLicense.getLicenseId());
        System.out.println("客户名称: " + currentLicense.getCustomerName());
        System.out.println("许可证类型: " + currentLicense.getLicenseType());
        if (currentLicense.isUnlimitedDevices()) {
            System.out.println("已授权设备: " + currentLicense.getActiveDeviceCount());
        } else {
            System.out.println("最大设备数: " + currentLicense.getMaxDevices());
            System.out.println("已授权设备: " + currentLicense.getActiveDeviceCount() + "/" + currentLicense.getMaxDevices());
        }

        if (currentLicense.getValidDays() == 0 || currentLicense.getExpiryDate() == null) {
            System.out.println("有效期: 永久有效");
        } else {
            long remainingDays = getRemainingDays();
            System.out.println("有效期至: " + currentLicense.getExpiryDate().toLocalDate());
            System.out.println("剩余天数: " + remainingDays + " 天");
        }

        System.out.println("\n=== 已绑定设备信息 ===");
        String currentFingerprint = HardwareFingerprint.generateFingerprint();
        for (DeviceBinding binding : currentLicense.getDeviceBindings()) {
            if (binding.isActive()) {
                boolean isCurrentDevice = binding.getDeviceFingerprint().equals(currentFingerprint);
                String marker = isCurrentDevice ? " [当前设备]" : "";
                System.out.println("- " + binding.getDeviceName() + marker);
                System.out.println("  设备指纹: " + binding.getDisplayFingerprint());
                System.out.println("  绑定时间: " + binding.getBindTime().toLocalDate());
                System.out.println("  最后使用: " + binding.getLastUsedTime().toLocalDate());
            }
        }

        System.out.println("========================\n");
    }

    public synchronized void updateFromRemote(JSONObject licenseData) {
        if (licenseData == null) {
            logger.warn("云端许可证数据为空，跳过同步");
            return;
        }

        try {
            LicenseInfo remoteInfo = convertRemoteLicense(licenseData);
            ensureLicenseDefaults(remoteInfo);
            this.currentLicense = remoteInfo;
            saveLicense();

            boolean bound = remoteInfo.isActive() && !remoteInfo.isExpired()
                    && remoteInfo.isDeviceBound(HardwareFingerprint.generateFingerprint());
            this.isLicensed = bound;
            if (bound) {
                logger.info("云端许可证同步成功，当前设备已获得授权");
            } else {
                logger.warn("云端许可证同步成功，但当前设备不在授权列表中");
            }
        } catch (Exception e) {
            logger.error("同步云端许可证数据失败", e);
        }
    }

    private LicenseInfo convertRemoteLicense(JSONObject licenseData) {
        String licenseId = licenseData.optString("licenseId", currentLicense != null ? currentLicense.getLicenseId() : "");
        String customerName = licenseData.optString("customerName", "未知客户");
        String typeValue = licenseData.optString("licenseType", LicenseType.CUSTOMER_LIMITED.name());
        LicenseType licenseType;
        try {
            licenseType = LicenseType.valueOf(typeValue);
        } catch (IllegalArgumentException ex) {
            licenseType = LicenseType.fromTierCode(typeValue);
        }
        boolean unlimited = licenseData.optBoolean("unlimited", licenseType.isUnlimitedDevices());
        int maxDevices = unlimited ? Integer.MAX_VALUE : licenseData.optInt("maxDevices", licenseType.getDefaultMaxDevices());
        int validDays = licenseData.optInt("validDays", 0);

        LicenseInfo info = new LicenseInfo(licenseId, customerName, maxDevices, validDays, licenseType);
        info.setActive("active".equalsIgnoreCase(licenseData.optString("status", "active")));
        String uuid = licenseData.optString("uuid", "");
        if (!uuid.isBlank()) {
            info.setUuid(uuid);
        }

        LocalDateTime createdAt = parseRemoteDate(licenseData.optString("createdAt", null));
        if (createdAt != null) {
            info.setCreatedAt(createdAt);
        }
        LocalDateTime expiresAt = parseRemoteDate(licenseData.optString("expiryDate", null));
        if (expiresAt != null) {
            info.setExpiresAt(expiresAt);
        }

        JSONArray boundDevices = licenseData.optJSONArray("boundDevices");
        if (boundDevices != null) {
            List<DeviceBinding> bindings = new ArrayList<>();
            for (int i = 0; i < boundDevices.length(); i++) {
                JSONObject device = boundDevices.getJSONObject(i);
                String fingerprint = device.optString("deviceFingerprint");
                String deviceName = device.optString("deviceName", "未命名设备");
                LocalDateTime boundAt = parseRemoteDate(device.optString("bindTime", null));
                LocalDateTime lastUsedAt = parseRemoteDate(device.optString("lastUsedTime", null));
                boolean active = device.optBoolean("active", true);

                DeviceBinding binding = new DeviceBinding(
                        fingerprint,
                        deviceName,
                        boundAt != null ? boundAt : LocalDateTime.now(),
                        lastUsedAt != null ? lastUsedAt : LocalDateTime.now(),
                        active);
                bindings.add(binding);
            }
            info.setBoundDevices(bindings);
        } else {
            info.setBoundDevices(new ArrayList<>());
        }

        return info;
    }

    private LocalDateTime parseRemoteDate(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
        } catch (Exception e) {
            logger.debug("解析远端时间失败: {}", value, e);
            return null;
        }
    }

    private void ensureLicenseDefaults(LicenseInfo license) {
        if (license == null) {
            return;
        }

        if (license.getLicenseType() == null) {
            license.setLicenseType(LicenseType.CUSTOMER_LIMITED);
        } else {
            license.setLicenseType(license.getLicenseType());
        }

        if (license.getBoundDevices() == null) {
            license.setBoundDevices(new ArrayList<>());
        }
    }

    private LicenseInfo mergeWithExisting(LicenseInfo parsedInfo) {
        if (this.currentLicense != null && parsedInfo.getLicenseId().equals(this.currentLicense.getLicenseId())) {
            LicenseInfo existing = this.currentLicense;
            existing.setLicenseType(parsedInfo.getLicenseType());
            existing.setMaxDevices(parsedInfo.getMaxDevices());
            ensureLicenseDefaults(existing);
            return existing;
        }
        return parsedInfo;
    }
}
