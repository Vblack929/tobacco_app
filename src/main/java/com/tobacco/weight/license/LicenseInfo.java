package com.tobacco.weight.license;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 许可证信息类
 * 存储动态绑定许可证的详细信息
 */
public class LicenseInfo {

    private String licenseId; // 许可证ID
    private String uuid; // 唯一标识符
    private String customerName; // 客户名称
    private int maxDevices; // 最大设备数量
    private int validDays; // 有效天数（0表示永久）
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime expiresAt; // 过期时间
    private boolean active; // 是否激活
    private List<DeviceBinding> boundDevices; // 已绑定的设备列表

    public LicenseInfo() {
        this.boundDevices = new ArrayList<>();
        this.active = true;
    }

    public LicenseInfo(String licenseId, String customerName, int maxDevices, int validDays) {
        this();
        this.licenseId = licenseId;
        this.customerName = customerName;
        this.maxDevices = maxDevices;
        this.validDays = validDays;
        this.createdAt = LocalDateTime.now();

        if (validDays > 0) {
            this.expiresAt = this.createdAt.plusDays(validDays);
        }
    }

    /**
     * 检查许可证是否已过期
     */
    @JsonIgnore
    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // 永久许可证
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查是否还能绑定更多设备
     */
    public boolean canBindMoreDevices() {
        return boundDevices.size() < maxDevices;
    }

    /**
     * 获取剩余可绑定设备数量
     */
    @JsonIgnore
    public int getRemainingDeviceSlots() {
        return Math.max(0, maxDevices - boundDevices.size());
    }

    /**
     * 检查设备是否已绑定
     */
    public boolean isDeviceBound(String deviceFingerprint) {
        return boundDevices.stream()
                .anyMatch(device -> device.getDeviceFingerprint().equals(deviceFingerprint));
    }

    /**
     * 添加设备绑定
     */
    public boolean addDeviceBinding(String deviceFingerprint, String deviceName) {
        if (!canBindMoreDevices() || isDeviceBound(deviceFingerprint)) {
            return false;
        }

        DeviceBinding binding = new DeviceBinding(deviceFingerprint, deviceName);
        boundDevices.add(binding);
        return true;
    }

    /**
     * 移除设备绑定
     */
    public boolean removeDeviceBinding(String deviceFingerprint) {
        return boundDevices.removeIf(device -> device.getDeviceFingerprint().equals(deviceFingerprint));
    }

    /**
     * 获取设备绑定信息
     */
    public DeviceBinding getDeviceBinding(String deviceFingerprint) {
        return boundDevices.stream()
                .filter(device -> device.getDeviceFingerprint().equals(deviceFingerprint))
                .findFirst()
                .orElse(null);
    }

    // Getters and Setters

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getMaxDevices() {
        return maxDevices;
    }

    public void setMaxDevices(int maxDevices) {
        this.maxDevices = maxDevices;
    }

    public int getValidDays() {
        return validDays;
    }

    public void setValidDays(int validDays) {
        this.validDays = validDays;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<DeviceBinding> getBoundDevices() {
        return new ArrayList<>(boundDevices);
    }

    public void setBoundDevices(List<DeviceBinding> boundDevices) {
        this.boundDevices = new ArrayList<>(boundDevices);
    }

    /**
     * 获取过期时间（兼容方法）
     */
    @JsonIgnore
    public LocalDateTime getExpiryDate() {
        return expiresAt;
    }

    /**
     * 获取创建时间（兼容方法）
     */
    @JsonIgnore
    public LocalDateTime getCreatedDate() {
        return createdAt;
    }

    /**
     * 获取设备绑定列表（兼容方法）
     */
    @JsonIgnore
    public List<DeviceBinding> getDeviceBindings() {
        return new ArrayList<>(boundDevices);
    }

    /**
     * 获取激活设备数量
     */
    @JsonIgnore
    public int getActiveDeviceCount() {
        return (int) boundDevices.stream().filter(DeviceBinding::isActive).count();
    }

    /**
     * 设置激活状态（兼容方法）
     */
    public void setActivated(boolean activated) {
        this.active = activated;
    }

    /**
     * 添加设备绑定（重载方法）
     */
    public void addDeviceBinding(DeviceBinding deviceBinding) {
        if (!boundDevices.contains(deviceBinding)) {
            boundDevices.add(deviceBinding);
        }
    }

    @Override
    public String toString() {
        return String.format("LicenseInfo{licenseId='%s', customer='%s', maxDevices=%d, boundDevices=%d, expired=%s}",
                licenseId, customerName, maxDevices, boundDevices.size(), isExpired());
    }
}