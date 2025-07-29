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
                            // 解析重量数据
                            String dataString = new String(data).trim();
                            logger.debug("接收到原始数据: '{}'", dataString);

                            // 按行分割处理数据
                            String[] lines = dataString.split("\n");
                            for (String line : lines) {
                                String trimmedLine = line.trim();
                                if (!trimmedLine.isEmpty()) {
                                    parseWeightData(trimmedLine);
                                }
                            }
                        }

                    } catch (Exception e) {
                        logger.error("读取串口数据失败", e);
                    }
                }
            }
        });
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
                if (Math.abs(scaleData.getWeight() - currentWeight) > 0.01) {
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

            // 检查是否是电子秤数据格式
            if (cleanData.contains(",") && cleanData.contains("kg")) {
                // 格式: "ST, NT, + 0.00kg"
                String[] parts = cleanData.split(",");

                if (parts.length >= 3) {
                    String status = parts[0].trim(); // ST 或 US
                    String type = parts[1].trim(); // NT
                    String weightPart = parts[2].trim(); // + 0.00kg

                    // 提取重量值
                    String weightStr = weightPart.replaceAll("[^\\d.-]", "");
                    if (!weightStr.isEmpty()) {
                        double weight = Double.parseDouble(weightStr);

                        // 判断是否稳定
                        boolean isStable = "ST".equals(status);

                        return new ScaleData(weight, isStable, status, type);
                    }
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
     * 尝试解析其他格式的重量数据
     */
    private ScaleData tryParseOtherFormats(String data) {
        // 格式1: 带kg单位 (如: "12.34kg", "5.67 kg")
        if (data.toLowerCase().contains("kg")) {
            try {
                String weightStr = data.replaceAll("[^\\d.-]", "");
                if (!weightStr.isEmpty()) {
                    double weight = Double.parseDouble(weightStr);
                    return new ScaleData(weight, true, "ST", "NT");
                }
            } catch (NumberFormatException e) {
                logger.warn("解析kg格式失败: {}", data);
            }
        }

        // 格式2: 纯数字格式 (如: "12.34", "-5.67", "0.00")
        try {
            String weightStr = data.replaceAll("[^\\d.-]", "");
            if (!weightStr.isEmpty()) {
                double weight = Double.parseDouble(weightStr);
                return new ScaleData(weight, true, "ST", "NT");
            }
        } catch (NumberFormatException e) {
            logger.warn("解析纯数字格式失败: {}", data);
        }

        return null;
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

        // 通知连接状态变化
        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(false);
        }
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