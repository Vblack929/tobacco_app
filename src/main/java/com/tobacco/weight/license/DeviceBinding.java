package com.tobacco.weight.license;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * 设备绑定信息类
 * 存储许可证与设备的绑定关系
 */
public class DeviceBinding {

    private String deviceFingerprint; // 设备指纹
    private String deviceName; // 设备名称
    private LocalDateTime boundAt; // 绑定时间
    private LocalDateTime lastUsedAt; // 最后使用时间
    private boolean active; // 是否激活

    public DeviceBinding() {
        this.boundAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.active = true;
    }

    public DeviceBinding(String deviceFingerprint, String deviceName) {
        this();
        this.deviceFingerprint = deviceFingerprint;
        this.deviceName = deviceName;
    }

    public DeviceBinding(String deviceFingerprint, String deviceName, LocalDateTime boundAt, LocalDateTime lastUsedAt,
            boolean active) {
        this.deviceFingerprint = deviceFingerprint;
        this.deviceName = deviceName;
        this.boundAt = boundAt;
        this.lastUsedAt = lastUsedAt;
        this.active = active;
    }

    /**
     * 更新最后使用时间
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 获取设备指纹的显示格式（前8位+...）
     */
    @JsonIgnore
    public String getDisplayFingerprint() {
        if (deviceFingerprint == null || deviceFingerprint.length() < 8) {
            return deviceFingerprint;
        }
        return deviceFingerprint.substring(0, 8) + "...";
    }

    /**
     * 检查设备是否长时间未使用
     * 
     * @param days 天数阈值
     * @return 是否超过指定天数未使用
     */
    public boolean isUnusedForDays(int days) {
        if (lastUsedAt == null) {
            return false;
        }
        return lastUsedAt.isBefore(LocalDateTime.now().minusDays(days));
    }

    // Getters and Setters

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public LocalDateTime getBoundAt() {
        return boundAt;
    }

    public void setBoundAt(LocalDateTime boundAt) {
        this.boundAt = boundAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    /**
     * 设置最后使用时间（兼容方法）
     */
    public void setLastUsedTime(LocalDateTime lastUsedTime) {
        this.lastUsedAt = lastUsedTime;
    }

    /**
     * 获取最后使用时间（兼容方法）
     */
    public LocalDateTime getLastUsedTime() {
        return lastUsedAt;
    }

    /**
     * 获取绑定时间（兼容方法）
     */
    @JsonIgnore
    public LocalDateTime getBindTime() {
        return boundAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return String.format("DeviceBinding{fingerprint='%s', name='%s', boundAt=%s, active=%s}",
                getDisplayFingerprint(), deviceName, boundAt, active);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        DeviceBinding that = (DeviceBinding) obj;
        return deviceFingerprint != null ? deviceFingerprint.equals(that.deviceFingerprint)
                : that.deviceFingerprint == null;
    }

    @Override
    public int hashCode() {
        return deviceFingerprint != null ? deviceFingerprint.hashCode() : 0;
    }
}