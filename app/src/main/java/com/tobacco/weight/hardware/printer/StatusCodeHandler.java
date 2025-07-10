package com.tobacco.weight.hardware.printer;

import java.util.HashMap;
import java.util.Map;

/**
 * 打印机状态码处理器
 * 解析和处理打印机返回的状态信息
 */
public class StatusCodeHandler {
    
    private static final String TAG = "StatusCodeHandler";
    
    /**
     * 打印机状态枚举
     */
    public enum PrinterStatus {
        // 打印状态
        PRINT_COMPLETE("FC4F4B", "打印完成"),
        PRINT_INCOMPLETE("FC6E6F", "打印未完成"),
        
        // 纸张状态
        PAPER_OUT("EF231A", "缺纸"),
        PAPER_PRESENT("FE2312", "有纸"),
        PAPER_LOW("FE2410", "纸张不足"),
        PAPER_SUFFICIENT("FE2411", "纸张充足"),
        
        // 温度状态
        NORMAL_TEMPERATURE("FE2510", "温度正常"),
        ABNORMAL_TEMPERATURE("FE2511", "温度异常"),
        
        // 切刀状态
        CUTTER_RESET("FE2610", "切刀复位"),
        CUTTER_NOT_RESET("FE2611", "切刀未复位"),
        
        // 滚轮状态
        ROLLER_CLOSED("FE2710", "滚轮关闭"),
        ROLLER_OPEN("FE2711", "滚轮打开"),
        
        // 纸张传感器状态
        PAPER_NORMAL("FE2810", "纸张传感器正常"),
        PAPER_JAM("FE2811", "卡纸"),
        
        // 电压状态
        NORMAL_VOLTAGE("FE2B10", "电压正常"),
        ABNORMAL_VOLTAGE("FE2B11", "电压异常"),
        
        // 连接状态
        CONNECTED("CONNECTED", "设备已连接"),
        DISCONNECTED("DISCONNECTED", "设备未连接"),
        
        // 通用状态
        READY("READY", "就绪"),
        BUSY("BUSY", "忙碌"),
        ERROR("ERROR", "错误"),
        UNKNOWN("UNKNOWN", "未知状态");
        
        private final String hexCode;
        private final String description;
        
        PrinterStatus(String hexCode, String description) {
            this.hexCode = hexCode;
            this.description = description;
        }
        
        public String getHexCode() { return hexCode; }
        public String getDescription() { return description; }
    }
    
    // 状态码映射表
    private static final Map<String, PrinterStatus> STATUS_MAP = new HashMap<>();
    
    static {
        for (PrinterStatus status : PrinterStatus.values()) {
            STATUS_MAP.put(status.getHexCode().toUpperCase(), status);
        }
    }
    
    /**
     * 解析十六进制状态字符串
     * @param hexString 十六进制状态字符串
     * @return 打印机状态
     */
    public static PrinterStatus parseStatus(String hexString) {
        if (hexString == null || hexString.trim().isEmpty()) {
            return PrinterStatus.UNKNOWN;
        }
        
        String upperHex = hexString.toUpperCase().trim();
        
        // 直接匹配
        PrinterStatus status = STATUS_MAP.get(upperHex);
        if (status != null) {
            return status;
        }
        
        // 模糊匹配 - 检查是否包含已知的状态码
        for (PrinterStatus knownStatus : PrinterStatus.values()) {
            if (upperHex.contains(knownStatus.getHexCode().toUpperCase())) {
                return knownStatus;
            }
        }
        
        return PrinterStatus.UNKNOWN;
    }
    
    /**
     * 解析字节数组状态
     * @param statusBytes 状态字节数组
     * @return 打印机状态
     */
    public static PrinterStatus parseStatus(byte[] statusBytes) {
        if (statusBytes == null || statusBytes.length == 0) {
            return PrinterStatus.UNKNOWN;
        }
        
        String hexString = bytesToHex(statusBytes);
        return parseStatus(hexString);
    }
    
    /**
     * 检查是否为错误状态
     * @param status 打印机状态
     * @return 是否为错误状态
     */
    public static boolean isErrorStatus(PrinterStatus status) {
        if (status == null) return true;
        
        switch (status) {
            case PRINT_INCOMPLETE:
            case PAPER_OUT:
            case PAPER_LOW:
            case ABNORMAL_TEMPERATURE:
            case CUTTER_NOT_RESET:
            case PAPER_JAM:
            case ABNORMAL_VOLTAGE:
            case DISCONNECTED:
            case ERROR:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 检查是否为警告状态
     * @param status 打印机状态
     * @return 是否为警告状态
     */
    public static boolean isWarningStatus(PrinterStatus status) {
        if (status == null) return false;
        
        switch (status) {
            case PAPER_LOW:
            case ROLLER_OPEN:
            case BUSY:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 检查是否为正常状态
     * @param status 打印机状态
     * @return 是否为正常状态
     */
    public static boolean isNormalStatus(PrinterStatus status) {
        if (status == null) return false;
        
        switch (status) {
            case PRINT_COMPLETE:
            case PAPER_PRESENT:
            case PAPER_SUFFICIENT:
            case NORMAL_TEMPERATURE:
            case CUTTER_RESET:
            case ROLLER_CLOSED:
            case PAPER_NORMAL:
            case NORMAL_VOLTAGE:
            case CONNECTED:
            case READY:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 获取状态信息
     * @param status 打印机状态
     * @return 状态信息字符串
     */
    public static String getStatusMessage(PrinterStatus status) {
        if (status == null) {
            return "状态未知";
        }
        return status.getDescription();
    }
    
    /**
     * 获取详细状态信息
     * @param status 打印机状态
     * @return 详细状态信息
     */
    public static String getDetailedStatusMessage(PrinterStatus status) {
        if (status == null) {
            return "状态: 未知";
        }
        
        StringBuilder message = new StringBuilder();
        message.append("状态: ").append(status.getDescription());
        
        if (isErrorStatus(status)) {
            message.append(" (错误)");
        } else if (isWarningStatus(status)) {
            message.append(" (警告)");
        } else if (isNormalStatus(status)) {
            message.append(" (正常)");
        }
        
        return message.toString();
    }
    
    /**
     * 获取状态对应的建议操作
     * @param status 打印机状态
     * @return 建议操作
     */
    public static String getSuggestedAction(PrinterStatus status) {
        if (status == null) {
            return "检查设备连接和状态";
        }
        
        switch (status) {
            case PRINT_COMPLETE:
                return "打印成功完成";
            case PRINT_INCOMPLETE:
                return "重新开始打印";
            case PAPER_OUT:
                return "请添加打印纸";
            case PAPER_LOW:
                return "建议补充打印纸";
            case ABNORMAL_TEMPERATURE:
                return "等待设备冷却或检查散热";
            case CUTTER_NOT_RESET:
                return "检查切刀机构";
            case PAPER_JAM:
                return "清除卡纸后重试";
            case ABNORMAL_VOLTAGE:
                return "检查电源供应";
            case ROLLER_OPEN:
                return "关闭打印机滚轮";
            case DISCONNECTED:
                return "重新连接设备";
            case BUSY:
                return "等待当前操作完成";
            case ERROR:
                return "检查设备状态并重启";
            case READY:
                return "设备就绪，可以开始打印";
            default:
                return "继续操作";
        }
    }
    
    /**
     * 获取状态的严重程度
     * @param status 打印机状态
     * @return 严重程度 (0-正常, 1-警告, 2-错误)
     */
    public static int getStatusSeverity(PrinterStatus status) {
        if (status == null) return 2;
        
        if (isErrorStatus(status)) {
            return 2; // 错误
        } else if (isWarningStatus(status)) {
            return 1; // 警告
        } else {
            return 0; // 正常
        }
    }
    
    /**
     * 字节数组转十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
    
    /**
     * 十六进制字符串转字节数组
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[0];
        }
        
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return result;
    }
    
    /**
     * 创建状态查询命令
     * @return ESC/POS状态查询命令
     */
    public static byte[] createStatusQueryCommand() {
        // ESC/POS实时状态查询命令
        return new byte[] {
            0x10, 0x04, 0x01  // DLE EOT n (查询打印机状态)
        };
    }
    
    /**
     * 创建纸张状态查询命令
     * @return 纸张状态查询命令
     */
    public static byte[] createPaperStatusQueryCommand() {
        return new byte[] {
            0x10, 0x04, 0x04  // DLE EOT n (查询纸张状态)
        };
    }
    
    /**
     * 批量解析状态
     * @param statusData 状态数据
     * @return 状态结果
     */
    public static StatusResult parseMultipleStatus(byte[] statusData) {
        StatusResult result = new StatusResult();
        
        if (statusData == null || statusData.length == 0) {
            result.addStatus(PrinterStatus.UNKNOWN);
            return result;
        }
        
        // 尝试按不同长度分段解析
        for (int i = 0; i < statusData.length; i += 3) {
            int endIndex = Math.min(i + 3, statusData.length);
            byte[] segment = new byte[endIndex - i];
            System.arraycopy(statusData, i, segment, 0, segment.length);
            
            PrinterStatus status = parseStatus(segment);
            if (status != PrinterStatus.UNKNOWN) {
                result.addStatus(status);
            }
        }
        
        if (result.getStatusList().isEmpty()) {
            result.addStatus(PrinterStatus.UNKNOWN);
        }
        
        return result;
    }
    
    /**
     * 状态结果类
     */
    public static class StatusResult {
        private java.util.List<PrinterStatus> statusList;
        private long timestamp;
        
        public StatusResult() {
            this.statusList = new java.util.ArrayList<>();
            this.timestamp = System.currentTimeMillis();
        }
        
        public void addStatus(PrinterStatus status) {
            this.statusList.add(status);
        }
        
        public java.util.List<PrinterStatus> getStatusList() {
            return statusList;
        }
        
        public PrinterStatus getPrimaryStatus() {
            if (statusList.isEmpty()) {
                return PrinterStatus.UNKNOWN;
            }
            
            // 优先返回错误状态
            for (PrinterStatus status : statusList) {
                if (isErrorStatus(status)) {
                    return status;
                }
            }
            
            // 其次返回警告状态
            for (PrinterStatus status : statusList) {
                if (isWarningStatus(status)) {
                    return status;
                }
            }
            
            // 最后返回第一个状态
            return statusList.get(0);
        }
        
        public boolean hasErrors() {
            return statusList.stream().anyMatch(StatusCodeHandler::isErrorStatus);
        }
        
        public boolean hasWarnings() {
            return statusList.stream().anyMatch(StatusCodeHandler::isWarningStatus);
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "StatusResult{" +
                    "statusList=" + statusList +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
} 