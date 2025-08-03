package com.tobacco.weight.hardware;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 电子秤管理器
 * 负责与电子秤的串口通信和重量数据读取
 */
public class ScaleManager {

    private static final Logger logger = LoggerFactory.getLogger(ScaleManager.class);

    private SerialPort serialPort;
    private String portName = "COM5"; // 默认串口
    private int baudRate = 9600; // 默认波特率
    private boolean isConnected = false;
    private double currentWeight = 0.0;

    // 数据缓冲相关
    private StringBuilder dataBuffer = new StringBuilder();
    private long lastDataTime = 0;
    private static final long DATA_TIMEOUT = 100; // 100ms超时

    // 回调函数
    private Consumer<Double> onWeightChanged;
    private Consumer<Boolean> onConnectionStatusChanged;

    /**
     * 构造函数
     */
    public ScaleManager() {
        initializeScale();
    }

    /**
     * 初始化电子秤
     */
    private void initializeScale() {
        try {
            // 查找可用的串口
            SerialPort[] ports = SerialPort.getCommPorts();
            if (ports.length == 0) {
                logger.warn("未找到可用的串口设备");
                return;
            }

            // 尝试连接默认串口
            if (!connectToPort(portName)) {
                logger.warn("无法连接到默认串口: {}", portName);

                // 尝试其他可用串口
                logger.info("尝试连接其他可用串口...");

                for (SerialPort port : ports) {
                    String portName = port.getSystemPortName();
                    if (!portName.equals(this.portName)) {
                        logger.info("尝试连接串口: {}", portName);
                        if (connectToPort(portName)) {
                            logger.info("成功连接到备用串口: {}", portName);
                            this.portName = portName; // 更新当前使用的串口
                            break;
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("初始化电子秤失败", e);
        }
    }

    /**
     * 连接到指定串口
     */
    public boolean connectToPort(String portName) {
        try {
            // 关闭现有连接
            disconnect();

            this.portName = portName;
            serialPort = SerialPort.getCommPort(portName);

            // 设置串口参数
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);

            // 打开串口
            if (serialPort.openPort()) {
                isConnected = true;
                logger.info("电子秤连接成功: {}", portName);

                // 设置数据监听器
                setupDataListener();

                // 通知连接状态变化
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(true);
                }

                return true;
            } else {
                logger.error("无法打开串口: {}", portName);
                logger.error("可能的原因：");
                logger.error("1. 串口被其他程序占用");
                logger.error("2. 串口不存在或未正确创建");
                logger.error("3. 权限不足");

                // 检查串口是否存在
                SerialPort[] availablePorts = SerialPort.getCommPorts();
                logger.info("可用串口列表:");
                for (SerialPort port : availablePorts) {
                    logger.info("  - {}: {}", port.getSystemPortName(), port.getDescriptivePortName());
                }

                return false;
            }

        } catch (Exception e) {
            logger.error("连接电子秤失败: {}", portName, e);
            return false;
        }
    }

    /**
     * 设置数据监听器
     */
    private void setupDataListener() {
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    try {
                        // 读取数据
                        byte[] data = new byte[serialPort.bytesAvailable()];
                        int bytesRead = serialPort.readBytes(data, data.length);

                        if (bytesRead > 0) {
                            // 将新数据添加到缓冲区
                            String newData = new String(data);
                            dataBuffer.append(newData);

                            long currentTime = System.currentTimeMillis();

                            // 检查是否有完整的数据行
                            processBufferedData(currentTime);
                        }

                    } catch (Exception e) {
                        logger.error("读取串口数据失败", e);
                    }
                }
            }
        });
    }

    /**
     * 处理缓冲的数据
     */
    private void processBufferedData(long currentTime) {
        String bufferContent = dataBuffer.toString();

        // 查找完整的数据行（以换行符结尾）
        int newlineIndex = bufferContent.indexOf('\n');

        if (newlineIndex >= 0) {
            // 提取完整的数据行
            String completeLine = bufferContent.substring(0, newlineIndex);
            String remainingData = bufferContent.substring(newlineIndex + 1);

            // 更新缓冲区
            dataBuffer.setLength(0);
            dataBuffer.append(remainingData);

            // 处理完整的数据行
            if (!completeLine.trim().isEmpty()) {
                logger.debug("处理完整数据行: '{}'", completeLine);
                parseWeightData(completeLine.trim());
            }
        } else if (currentTime - lastDataTime > DATA_TIMEOUT && bufferContent.trim().length() > 0) {
            // 超时处理：如果超过100ms没有新数据，且缓冲区有内容，尝试处理
            String timeoutData = bufferContent.trim();
            logger.debug("超时处理数据: '{}'", timeoutData);

            // 检查是否是完整的重量数据
            if (isCompleteWeightData(timeoutData)) {
                parseWeightData(timeoutData);
            } else {
                logger.warn("超时数据不完整，丢弃: '{}'", timeoutData);
            }

            // 清空缓冲区
            dataBuffer.setLength(0);
        }

        lastDataTime = currentTime;
    }

    /**
     * 检查是否是完整的重量数据
     */
    private boolean isCompleteWeightData(String data) {
        if (data == null || data.trim().isEmpty()) {
            return false;
        }

        String trimmed = data.trim();

        // 检查标准格式: "ST, NT, + 0.00kg"
        if (trimmed.contains(",") && trimmed.contains("kg")) {
            return true;
        }

        // 检查简单格式: "5.52kg", "5.52 kg"
        if (trimmed.toLowerCase().contains("kg")) {
            // 确保kg前面有数字
            String beforeKg = trimmed.toLowerCase().substring(0, trimmed.toLowerCase().indexOf("kg"));
            return beforeKg.matches(".*\\d+.*");
        }

        // 检查纯数字格式（至少包含小数点）
        if (trimmed.matches(".*\\d+\\.\\d+.*")) {
            return true;
        }

        return false;
    }

    /**
     * 解析重量数据
     * 根据电子秤协议解析重量值
     * 支持格式: "ST, NT, + 0.00kg", "US, NT, + 1.25kg" 等
     */
    private void parseWeightData(String data) {
        try {
            logger.debug("正在解析数据: '{}'", data);

            // 尝试解析电子秤数据格式
            ScaleData scaleData = parseScaleData(data);

            if (scaleData != null && scaleData.isValid()) {
                logger.debug("解析成功: 重量={}, 状态={}", scaleData.getWeight(), scaleData.getStatus());

                // 更新当前重量
                if (Math.abs(scaleData.getWeight() - currentWeight) > 0.005) {
                    currentWeight = scaleData.getWeight();
                    logger.info("重量变化: {} -> {} kg", currentWeight, scaleData.getWeight());

                    // 通知重量变化
                    if (onWeightChanged != null) {
                        logger.debug("调用重量变化回调: {} kg", currentWeight);
                        onWeightChanged.accept(currentWeight);
                    } else {
                        logger.warn("重量变化回调未设置");
                    }

                    logger.info("重量更新: {} kg, 状态: {}", currentWeight, scaleData.getStatus());
                } else {
                    logger.debug("重量无变化: {} kg", currentWeight);
                }
            } else {
                logger.warn("无法解析重量数据: '{}'", data);
            }

        } catch (Exception e) {
            logger.error("解析重量数据失败: {}", data, e);
        }
    }

    /**
     * 解析电子秤数据
     * 格式: "ST, NT, + 0.00kg" 或 "US, NT, + 1.25kg"
     */
    private ScaleData parseScaleData(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanData = data.trim();
            logger.debug("开始解析数据: '{}'", cleanData);

            // 检查是否是电子秤数据格式
            if (cleanData.contains(",") && cleanData.contains("kg")) {
                // 格式: "ST, NT, + 0.00kg"
                String[] parts = cleanData.split(",");

                if (parts.length >= 3) {
                    String status = parts[0].trim(); // ST 或 US
                    String type = parts[1].trim(); // NT
                    String weightPart = parts[2].trim(); // + 0.00kg

                    // 验证数据完整性
                    if (!isValidStatus(status) || !isValidType(type) || !isValidWeightPart(weightPart)) {
                        logger.warn("数据格式验证失败: status='{}', type='{}', weightPart='{}'", status, type, weightPart);
                        return null;
                    }

                    // 提取重量值
                    String weightStr = extractWeightFromPart(weightPart);
                    if (weightStr != null && !weightStr.isEmpty()) {
                        double weight = Double.parseDouble(weightStr);

                        // 验证重量值的合理性
                        if (weight < 0 || weight > 9999) {
                            logger.warn("重量值超出合理范围: {} kg", weight);
                            return null;
                        }

                        // 判断是否稳定
                        boolean isStable = "ST".equals(status);

                        logger.debug("解析成功: 重量={} kg, 状态={}, 类型={}", weight, status, type);
                        return new ScaleData(weight, isStable, status, type);
                    } else {
                        logger.warn("无法从重量部分提取数值: '{}'", weightPart);
                    }
                } else {
                    logger.warn("数据部分数量不足: 期望>=3, 实际={}", parts.length);
                }
            }

            // 尝试其他格式
            return tryParseOtherFormats(cleanData);

        } catch (Exception e) {
            logger.warn("解析电子秤数据异常: {}", data, e);
            return null;
        }
    }

    /**
     * 验证状态字段
     */
    private boolean isValidStatus(String status) {
        return "ST".equals(status) || "US".equals(status);
    }

    /**
     * 验证类型字段
     */
    private boolean isValidType(String type) {
        return "NT".equals(type) || "GT".equals(type);
    }

    /**
     * 验证重量部分格式
     */
    private boolean isValidWeightPart(String weightPart) {
        if (weightPart == null || weightPart.trim().isEmpty()) {
            return false;
        }

        // 检查是否包含kg单位
        if (!weightPart.toLowerCase().contains("kg")) {
            return false;
        }

        // 检查是否包含数字
        return weightPart.matches(".*\\d+.*");
    }

    /**
     * 从重量部分提取数值
     */
    private String extractWeightFromPart(String weightPart) {
        if (weightPart == null) {
            return null;
        }

        try {
            // 移除所有非数字、小数点和负号的字符
            String weightStr = weightPart.replaceAll("[^\\d.-]", "");

            // 验证提取的字符串
            if (weightStr.isEmpty()) {
                return null;
            }

            // 检查是否包含有效的小数点
            if (weightStr.contains(".")) {
                String[] parts = weightStr.split("\\.");
                if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                    logger.warn("小数点格式无效: '{}'", weightStr);
                    return null;
                }
            }

            // 尝试解析为数字验证格式
            Double.parseDouble(weightStr);
            return weightStr;

        } catch (NumberFormatException e) {
            logger.warn("重量字符串格式无效: '{}'", weightPart);
            return null;
        }
    }

    /**
     * 尝试解析其他格式的重量数据
     */
    private ScaleData tryParseOtherFormats(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        String trimmed = data.trim();
        logger.debug("尝试解析其他格式: '{}'", trimmed);

        // 格式1: 带kg单位 (如: "12.34kg", "5.67 kg")
        if (trimmed.toLowerCase().contains("kg")) {
            try {
                // 确保kg前面有完整的数字
                String beforeKg = trimmed.toLowerCase().substring(0, trimmed.toLowerCase().indexOf("kg"));
                String weightStr = beforeKg.replaceAll("[^\\d.-]", "");

                if (!weightStr.isEmpty() && isValidWeightString(weightStr)) {
                    double weight = Double.parseDouble(weightStr);

                    // 验证重量值的合理性
                    if (weight >= 0 && weight <= 9999) {
                        logger.debug("解析kg格式成功: {} kg", weight);
                        return new ScaleData(weight, true, "ST", "NT");
                    } else {
                        logger.warn("kg格式重量值超出范围: {} kg", weight);
                    }
                } else {
                    logger.warn("kg格式数据不完整: '{}'", trimmed);
                }
            } catch (NumberFormatException e) {
                logger.warn("解析kg格式失败: {}", trimmed);
            } catch (Exception e) {
                logger.warn("解析kg格式异常: {}", trimmed, e);
            }
        }

        // 格式2: 纯数字格式 (如: "12.34", "-5.67", "0.00")
        // 只处理包含小数点的数字，避免处理不完整的整数
        if (trimmed.matches("^[+-]?\\d+\\.\\d+$")) {
            try {
                double weight = Double.parseDouble(trimmed);

                // 验证重量值的合理性
                if (weight >= 0 && weight <= 9999) {
                    logger.debug("解析纯数字格式成功: {} kg", weight);
                    return new ScaleData(weight, true, "ST", "NT");
                } else {
                    logger.warn("纯数字格式重量值超出范围: {} kg", weight);
                }
            } catch (NumberFormatException e) {
                logger.warn("解析纯数字格式失败: {}", trimmed);
            }
        } else if (trimmed.matches(".*\\d+.*")) {
            // 包含数字但不是完整格式，可能是分割的数据
            logger.warn("检测到可能的分割数据，跳过: '{}'", trimmed);
        }

        return null;
    }

    /**
     * 验证重量字符串的有效性
     */
    private boolean isValidWeightString(String weightStr) {
        if (weightStr == null || weightStr.isEmpty()) {
            return false;
        }

        try {
            // 检查是否包含有效的小数点
            if (weightStr.contains(".")) {
                String[] parts = weightStr.split("\\.");
                if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                    return false;
                }
            }

            // 尝试解析为数字
            double weight = Double.parseDouble(weightStr);
            return weight >= 0 && weight <= 9999;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            logger.info("电子秤连接已断开");
        }

        isConnected = false;

        // 清理数据缓冲区
        clearDataBuffer();

        // 通知连接状态变化
        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(false);
        }
    }

    /**
     * 清理数据缓冲区
     */
    private void clearDataBuffer() {
        dataBuffer.setLength(0);
        lastDataTime = 0;
        logger.debug("数据缓冲区已清理");
    }

    /**
     * 获取当前重量
     */
    public double getCurrentWeight() {
        return currentWeight;
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }

    /**
     * 获取可用串口列表
     */
    public String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];

        for (int i = 0; i < ports.length; i++) {
            portNames[i] = ports[i].getSystemPortName();
        }

        return portNames;
    }

    /**
     * 设置串口参数
     */
    public void setPortParameters(String portName, int baudRate) {
        this.portName = portName;
        this.baudRate = baudRate;

        // 重新连接
        if (isConnected) {
            connectToPort(portName);
        }
    }

    /**
     * 发送命令到电子秤
     */
    public boolean sendCommand(String command) {
        if (!isConnected()) {
            logger.warn("电子秤未连接，无法发送命令");
            return false;
        }

        try {
            byte[] data = command.getBytes();
            int bytesWritten = serialPort.writeBytes(data, data.length);

            if (bytesWritten == data.length) {
                logger.debug("命令发送成功: {}", command);
                return true;
            } else {
                logger.error("命令发送失败: {}", command);
                return false;
            }

        } catch (Exception e) {
            logger.error("发送命令失败: {}", command, e);
            return false;
        }
    }

    /**
     * 去皮操作
     */
    public boolean tare() {
        return sendCommand("TARE\n");
    }

    /**
     * 清零操作
     */
    public boolean zero() {
        return sendCommand("ZERO\n");
    }

    /**
     * 设置重量变化回调
     */
    public void setOnWeightChanged(Consumer<Double> callback) {
        this.onWeightChanged = callback;
    }

    /**
     * 设置连接状态变化回调
     */
    public void setOnConnectionStatusChanged(Consumer<Boolean> callback) {
        this.onConnectionStatusChanged = callback;
    }

    /**
     * 获取串口信息
     */
    public String getPortInfo() {
        if (serialPort != null) {
            return String.format("串口: %s, 波特率: %d, 状态: %s",
                    portName, baudRate, isConnected() ? "已连接" : "未连接");
        }
        return "未连接";
    }

    /**
     * 测试连接
     */
    public boolean testConnection() {
        if (!isConnected()) {
            return false;
        }

        try {
            // 发送测试命令
            return sendCommand("TEST\n");
        } catch (Exception e) {
            logger.error("测试连接失败", e);
            return false;
        }
    }
}