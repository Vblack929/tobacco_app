package com.tobacco.weight.hardware.printer;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Windows串口打印机连接器
 * 用于通过串口连接和控制GY-RP801T等打印机
 */
public class WindowsSerialPrinter implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(WindowsSerialPrinter.class);

    private SerialPort serialPort;
    private String portName;
    private boolean isConnected = false;

    // 默认串口参数
    private static final int DEFAULT_BAUD_RATE = 115200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;
    private static final int TIMEOUT = 2000; // 2秒超时

    /**
     * 获取所有可用的串口
     */
    public static String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        return Arrays.stream(ports)
                .map(SerialPort::getSystemPortName)
                .toArray(String[]::new);
    }

    /**
     * 查找可能的打印机端口
     * 自动排除虚拟端口，只返回真实的打印机端口
     */
    public static String[] findPrinterPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();

        // 首先查找明确的打印机端口
        List<String> printerPorts = Arrays.stream(ports)
                .filter(port -> {
                    String desc = port.getDescriptivePortName().toLowerCase();
                    String name = port.getSystemPortName();

                    // 排除虚拟端口（com0com等）
                    if (name.startsWith("CNC") || desc.contains("com0com")) {
                        return false;
                    }

                    // 优先查找明确的打印机相关端口
                    if (desc.contains("printer") ||
                            desc.contains("pos") ||
                            desc.contains("receipt") ||
                            desc.contains("thermal") ||
                            desc.contains("gy-rp") ||
                            desc.contains("gy_rp") ||
                            desc.contains("usb serial")) {
                        return true;
                    }

                    return false;
                })
                .map(SerialPort::getSystemPortName)
                .collect(Collectors.toList());

        // 如果没找到明确的打印机端口，则查找所有非虚拟的串口
        if (printerPorts.isEmpty()) {
            printerPorts = Arrays.stream(ports)
                    .filter(port -> {
                        String desc = port.getDescriptivePortName().toLowerCase();
                        String name = port.getSystemPortName();

                        // 排除虚拟端口
                        if (name.startsWith("CNC") || desc.contains("com0com")) {
                            return false;
                        }

                        // 包含USB串口和标准COM口，但进一步过滤
                        if (desc.contains("usb") || name.matches("COM[0-9]+")) {
                            // 排除明显的非打印机设备
                            if (desc.contains("mouse") || desc.contains("keyboard") ||
                                    desc.contains("bluetooth") || desc.contains("modem")) {
                                return false;
                            }
                            return true;
                        }

                        return false;
                    })
                    .map(SerialPort::getSystemPortName)
                    .collect(Collectors.toList());
        }

        return printerPorts.toArray(new String[0]);
    }

    /**
     * 打开串口连接
     * 
     * @param portName 端口名称 (如 "COM3", "CNCA0")
     */
    public boolean open(String portName) {
        return open(portName, DEFAULT_BAUD_RATE);
    }

    /**
     * 打开串口连接
     * 
     * @param portName 端口名称
     * @param baudRate 波特率
     */
    public boolean open(String portName, int baudRate) {
        try {
            if (isConnected) {
                logger.warn("串口已经连接，先关闭现有连接");
                close();
            }

            this.portName = portName;
            this.serialPort = SerialPort.getCommPort(portName);

            // 设置串口参数
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(DATA_BITS);
            serialPort.setNumStopBits(STOP_BITS);
            serialPort.setParity(PARITY);

            // 设置超时
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, TIMEOUT, TIMEOUT);

            // 打开端口
            if (serialPort.openPort()) {
                isConnected = true;
                logger.info("成功连接到串口: {} (波特率: {})", portName, baudRate);

                // 等待端口稳定
                Thread.sleep(100);

                return true;
            } else {
                logger.error("无法打开串口: {}", portName);
                return false;
            }

        } catch (Exception e) {
            logger.error("打开串口失败: {}", portName, e);
            return false;
        }
    }

    /**
     * 发送数据到打印机
     * 
     * @param data 要发送的数据
     */
    public boolean write(byte[] data) {
        if (!isConnected || serialPort == null) {
            logger.error("串口未连接，无法发送数据");
            return false;
        }

        try {
            int bytesWritten = serialPort.writeBytes(data, data.length);

            if (bytesWritten == data.length) {
                logger.debug("成功发送 {} 字节到串口 {}", bytesWritten, portName);
                return true;
            } else {
                logger.error("发送数据不完整: 期望 {} 字节，实际发送 {} 字节", data.length, bytesWritten);
                return false;
            }

        } catch (Exception e) {
            logger.error("发送数据到串口失败: {}", portName, e);
            return false;
        }
    }

    /**
     * 发送文本到打印机
     * 
     * @param text 要发送的文本
     */
    public boolean write(String text) {
        if (text == null) {
            return false;
        }

        try {
            // 使用GBK编码支持中文
            byte[] data = text.getBytes("GBK");
            return write(data);
        } catch (Exception e) {
            logger.error("文本编码失败", e);
            return false;
        }
    }

    /**
     * 读取打印机响应数据
     * 
     * @param buffer  接收缓冲区
     * @param timeout 超时时间(毫秒)
     */
    public int read(byte[] buffer, int timeout) {
        if (!isConnected || serialPort == null) {
            return -1;
        }

        try {
            // 设置读取超时
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout, timeout);

            return serialPort.readBytes(buffer, buffer.length);

        } catch (Exception e) {
            logger.error("从串口读取数据失败: {}", portName, e);
            return -1;
        }
    }

    /**
     * 测试打印机连接
     */
    public boolean testConnection() {
        if (!isConnected) {
            return false;
        }

        try {
            // 发送打印机状态查询命令
            byte[] statusCmd = { 0x1B, 0x76 }; // ESC v - 查询状态
            write(statusCmd);

            // 尝试读取响应
            byte[] response = new byte[10];
            int bytesRead = read(response, 1000);

            // 如果有响应或者没有错误，认为连接正常
            return bytesRead >= 0;

        } catch (Exception e) {
            logger.error("测试连接失败", e);
            return false;
        }
    }

    /**
     * 获取串口信息
     */
    public String getPortInfo() {
        if (serialPort == null) {
            return "未连接";
        }

        return String.format("端口: %s, 描述: %s, 波特率: %d, 连接状态: %s",
                serialPort.getSystemPortName(),
                serialPort.getDescriptivePortName(),
                serialPort.getBaudRate(),
                isConnected ? "已连接" : "未连接");
    }

    /**
     * 检查是否有数据等待写入
     */
    public int getBytesAwaitingWrite() {
        if (isConnected && serialPort != null) {
            return serialPort.bytesAwaitingWrite();
        }
        return 0;
    }

    /**
     * 检查是否有数据可读
     */
    public int getBytesAvailable() {
        if (isConnected && serialPort != null) {
            return serialPort.bytesAvailable();
        }
        return 0;
    }

    /**
     * 关闭串口连接
     */
    @Override
    public void close() {
        try {
            if (serialPort != null && serialPort.isOpen()) {
                // 等待数据发送完成
                int maxWait = 50; // 最多等待5秒
                while (getBytesAwaitingWrite() > 0 && maxWait-- > 0) {
                    Thread.sleep(100);
                }

                serialPort.closePort();
                logger.info("串口连接已关闭: {}", portName);
            }
        } catch (Exception e) {
            logger.error("关闭串口失败: {}", portName, e);
        } finally {
            isConnected = false;
            serialPort = null;
            portName = null;
        }
    }

    // Getter方法
    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }

    public String getPortName() {
        return portName;
    }

    public int getBaudRate() {
        return serialPort != null ? serialPort.getBaudRate() : 0;
    }
}