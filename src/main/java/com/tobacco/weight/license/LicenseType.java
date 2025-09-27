package com.tobacco.weight.license;

/**
 * 许可证类型定义
 * 区分甲方授权与开发者授权的设备策略
 */
public enum LicenseType {
    /**
     * 甲方授权密钥：最多允许绑定两台设备
     */
    CUSTOMER_LIMITED("2025", false, 2, "甲方授权（最多两台设备）"),

    /**
     * 开发者通用密钥：不限设备数量
     */
    DEVELOPER_UNLIMITED("DEV0", true, Integer.MAX_VALUE, "开发者无限制授权");

    private final String tierCode;
    private final boolean unlimitedDevices;
    private final int defaultMaxDevices;
    private final String description;

    LicenseType(String tierCode, boolean unlimitedDevices, int defaultMaxDevices, String description) {
        this.tierCode = tierCode;
        this.unlimitedDevices = unlimitedDevices;
        this.defaultMaxDevices = defaultMaxDevices;
        this.description = description;
    }

    public String getTierCode() {
        return tierCode;
    }

    public boolean isUnlimitedDevices() {
        return unlimitedDevices;
    }

    public int getDefaultMaxDevices() {
        return defaultMaxDevices;
    }

    public String getDescription() {
        return description;
    }

    public static LicenseType fromTierCode(String tierCode) {
        if (tierCode == null) {
            return CUSTOMER_LIMITED;
        }
        String normalized = tierCode.trim().toUpperCase();
        for (LicenseType type : values()) {
            if (type.tierCode.equals(normalized)) {
                return type;
            }
        }
        return CUSTOMER_LIMITED;
    }
}
