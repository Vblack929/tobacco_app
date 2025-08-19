package com.tobacco.weight.license;

/**
 * 许可证验证结果枚举
 * 定义各种验证状态和错误码
 */
public enum LicenseVerificationResult {
    /**
     * 验证成功
     */
    SUCCESS("验证成功"),
    
    /**
     * 许可证未找到
     */
    LICENSE_NOT_FOUND("许可证不存在"),
    
    /**
     * 许可证已失效
     */
    LICENSE_INACTIVE("许可证已失效"),
    
    /**
     * 许可证已过期
     */
    LICENSE_EXPIRED("许可证已过期"),
    
    /**
     * 超过最大设备数量限制
     */
    MAX_DEVICES_EXCEEDED("已达到最大设备数量限制"),
    
    /**
     * 网络错误
     */
    NETWORK_ERROR("网络连接错误"),
    
    /**
     * 设备指纹不匹配
     */
    DEVICE_FINGERPRINT_MISMATCH("设备指纹不匹配"),
    
    /**
     * 许可证格式错误
     */
    INVALID_LICENSE_FORMAT("许可证格式错误"),
    
    /**
     * 服务器错误
     */
    SERVER_ERROR("服务器错误"),
    
    /**
     * 认证失败
     */
    AUTHENTICATION_FAILED("认证失败"),
    
    /**
     * 未知错误
     */
    UNKNOWN_ERROR("未知错误");
    
    private final String message;
    
    LicenseVerificationResult(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * 判断是否为网络相关错误（可以尝试离线验证）
     */
    public boolean isNetworkError() {
        return this == NETWORK_ERROR || this == SERVER_ERROR;
    }
    
    /**
     * 判断是否为致命错误（不允许继续使用）
     */
    public boolean isFatalError() {
        return this == LICENSE_NOT_FOUND || 
               this == LICENSE_INACTIVE || 
               this == LICENSE_EXPIRED || 
               this == MAX_DEVICES_EXCEEDED ||
               this == DEVICE_FINGERPRINT_MISMATCH ||
               this == INVALID_LICENSE_FORMAT;
    }
}