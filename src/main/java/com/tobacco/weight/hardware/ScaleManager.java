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
    private String portName = "COM3"; // 默认串口
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
            connectToPort(portName);

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
                            parseWeightData(dataString);
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
     */
    private void parseWeightData(String data) {
        try {
            // 示例：假设数据格式为 "W:123.45kg"
            if (data.startsWith("W:")) {
                String weightStr = data.substring(2).replace("kg", "").trim();
                double weight = Double.parseDouble(weightStr);

                // 更新当前重量
                if (Math.abs(weight - currentWeight) > 0.01) {
                    currentWeight = weight;

                    // 通知重量变化
                    if (onWeightChanged != null) {
                        onWeightChanged.accept(currentWeight);
                    }

                    logger.debug("重量更新: {} kg", currentWeight);
                }
            }

        } catch (Exception e) {
            logger.error("解析重量数据失败: {}", data, e);
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

        // 通知连接状态变化
        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(false);
        }
    }

    /**
     * 获取当前重量
     */
    public double getCurrentWeight() {
        // 模拟模式：始终返回固定重量
        return 12.34;
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