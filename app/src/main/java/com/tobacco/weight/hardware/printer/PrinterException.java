package com.tobacco.weight.hardware.printer;

/**
 * 打印机异常类
 * 处理打印机操作过程中的各种错误
 */
public class PrinterException extends Exception {
    
    public enum ErrorType {
        CONNECTION_FAILED("连接失败"),
        NO_PERMISSION("无设备权限"),
        DEVICE_NOT_FOUND("设备未找到"),
        PRINT_FAILED("打印失败"),
        PAPER_OUT("缺纸"),
        PAPER_JAM("卡纸"),
        TEMPERATURE_ERROR("温度异常"),
        COMMUNICATION_ERROR("通信错误"),
        INVALID_DATA("数据无效"),
        TIMEOUT("操作超时"),
        UNKNOWN_ERROR("未知错误");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private ErrorType errorType;
    private String devicePath;
    private int errorCode;
    
    public PrinterException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.errorCode = -1;
    }
    
    public PrinterException(ErrorType errorType, String message, String devicePath) {
        super(message);
        this.errorType = errorType;
        this.devicePath = devicePath;
        this.errorCode = -1;
    }
    
    public PrinterException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.errorCode = -1;
    }
    
    public PrinterException(ErrorType errorType, String message, String devicePath, int errorCode) {
        super(message);
        this.errorType = errorType;
        this.devicePath = devicePath;
        this.errorCode = errorCode;
    }
    
    public PrinterException(ErrorType errorType, String message, String devicePath, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.devicePath = devicePath;
        this.errorCode = -1;
    }
    
    public ErrorType getErrorType() { 
        return errorType; 
    }
    
    public String getDevicePath() { 
        return devicePath; 
    }
    
    public int getErrorCode() { 
        return errorCode; 
    }
    
    /**
     * 获取用户友好的错误信息
     */
    public String getUserMessage() {
        StringBuilder message = new StringBuilder();
        
        if (errorType != null) {
            message.append(errorType.getDescription());
        }
        
        if (devicePath != null) {
            message.append(" (设备: ").append(devicePath).append(")");
        }
        
        if (errorCode != -1) {
            message.append(" (错误码: ").append(errorCode).append(")");
        }
        
        String originalMessage = getMessage();
        if (originalMessage != null && !originalMessage.trim().isEmpty()) {
            message.append(" - ").append(originalMessage);
        }
        
        return message.toString();
    }
    
    /**
     * 创建连接失败异常
     */
    public static PrinterException connectionFailed(String devicePath, String reason) {
        return new PrinterException(ErrorType.CONNECTION_FAILED, reason, devicePath);
    }
    
    /**
     * 创建权限异常
     */
    public static PrinterException noPermission(String devicePath) {
        return new PrinterException(ErrorType.NO_PERMISSION, 
            "无法访问设备，请检查权限设置", devicePath);
    }
    
    /**
     * 创建设备未找到异常
     */
    public static PrinterException deviceNotFound() {
        return new PrinterException(ErrorType.DEVICE_NOT_FOUND, 
            "未找到可用的打印机设备");
    }
    
    /**
     * 创建打印失败异常
     */
    public static PrinterException printFailed(String reason) {
        return new PrinterException(ErrorType.PRINT_FAILED, reason);
    }
    
    /**
     * 创建缺纸异常
     */
    public static PrinterException paperOut() {
        return new PrinterException(ErrorType.PAPER_OUT, 
            "打印机缺纸，请添加纸张后重试");
    }
    
    /**
     * 创建卡纸异常
     */
    public static PrinterException paperJam() {
        return new PrinterException(ErrorType.PAPER_JAM, 
            "打印机卡纸，请清理后重试");
    }
    
    /**
     * 创建温度异常
     */
    public static PrinterException temperatureError() {
        return new PrinterException(ErrorType.TEMPERATURE_ERROR, 
            "打印机温度异常，请等待冷却或检查设备");
    }
    
    /**
     * 创建通信错误异常
     */
    public static PrinterException communicationError(String reason) {
        return new PrinterException(ErrorType.COMMUNICATION_ERROR, reason);
    }
    
    /**
     * 创建数据无效异常
     */
    public static PrinterException invalidData(String reason) {
        return new PrinterException(ErrorType.INVALID_DATA, reason);
    }
    
    /**
     * 创建超时异常
     */
    public static PrinterException timeout(String operation) {
        return new PrinterException(ErrorType.TIMEOUT, 
            operation + "操作超时");
    }
    
    /**
     * 从系统异常创建打印机异常
     */
    public static PrinterException fromSystemError(Throwable cause) {
        String message = cause.getMessage();
        
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            
            if (lowerMessage.contains("permission")) {
                return new PrinterException(ErrorType.NO_PERMISSION, message, cause);
            } else if (lowerMessage.contains("not found") || lowerMessage.contains("no such file")) {
                return new PrinterException(ErrorType.DEVICE_NOT_FOUND, message, cause);
            } else if (lowerMessage.contains("timeout")) {
                return new PrinterException(ErrorType.TIMEOUT, message, cause);
            } else if (lowerMessage.contains("communication") || lowerMessage.contains("io")) {
                return new PrinterException(ErrorType.COMMUNICATION_ERROR, message, cause);
            }
        }
        
        return new PrinterException(ErrorType.UNKNOWN_ERROR, 
            message != null ? message : "未知错误", cause);
    }
    
    /**
     * 检查是否为可重试的错误
     */
    public boolean isRetryable() {
        switch (errorType) {
            case COMMUNICATION_ERROR:
            case TIMEOUT:
            case UNKNOWN_ERROR:
                return true;
            case CONNECTION_FAILED:
                return true; // 连接失败通常可以重试
            case PRINT_FAILED:
                return true; // 打印失败可能是临时性的
            default:
                return false;
        }
    }
    
    /**
     * 检查是否为硬件故障
     */
    public boolean isHardwareError() {
        switch (errorType) {
            case PAPER_OUT:
            case PAPER_JAM:
            case TEMPERATURE_ERROR:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 检查是否为配置错误
     */
    public boolean isConfigurationError() {
        switch (errorType) {
            case NO_PERMISSION:
            case DEVICE_NOT_FOUND:
            case INVALID_DATA:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 获取建议的解决方案
     */
    public String getSuggestedSolution() {
        switch (errorType) {
            case CONNECTION_FAILED:
                return "1. 检查设备连接\n2. 确认设备电源\n3. 重新插拔USB线";
            case NO_PERMISSION:
                return "1. 检查应用权限\n2. 尝试以管理员身份运行\n3. 检查SELinux设置";
            case DEVICE_NOT_FOUND:
                return "1. 确认设备已连接\n2. 检查驱动程序\n3. 重启设备";
            case PAPER_OUT:
                return "1. 添加打印纸\n2. 确认纸张正确安装\n3. 重新开始打印";
            case PAPER_JAM:
                return "1. 打开打印机盖子\n2. 小心取出卡住的纸张\n3. 关闭盖子重试";
            case TEMPERATURE_ERROR:
                return "1. 等待设备冷却\n2. 检查通风是否良好\n3. 联系技术支持";
            case COMMUNICATION_ERROR:
                return "1. 检查连接线\n2. 重新连接设备\n3. 检查波特率设置";
            case TIMEOUT:
                return "1. 检查设备响应\n2. 重新连接\n3. 减少打印数据量";
            case INVALID_DATA:
                return "1. 检查打印数据格式\n2. 验证命令参数\n3. 使用标准模板";
            default:
                return "1. 重新尝试操作\n2. 重启设备\n3. 联系技术支持";
        }
    }
    
    @Override
    public String toString() {
        return "PrinterException{" +
                "errorType=" + errorType +
                ", devicePath='" + devicePath + '\'' +
                ", errorCode=" + errorCode +
                ", message='" + getMessage() + '\'' +
                '}';
    }
} 