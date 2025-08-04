package com.tobacco.weight.hardware.printer;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 打印机模拟器
 * 用于在无实物打印机的情况下模拟打印机行为，方便开发和测试
 */
public class PrinterSimulator implements Runnable, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(PrinterSimulator.class);

    private SerialPort serialPort;
    private String portName;
    private Thread simulatorThread;
    private volatile boolean running = false;

    // ESC/POS控制字符
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte LF = 0x0A;
    private static final byte CR = 0x0D;

    // 模拟器状态
    private int alignment = 0; // 0=左对齐, 1=居中, 2=右对齐
    private boolean boldMode = false;
    private boolean underlineMode = false;
    private int fontSize = 1;

    /**
     * 启动打印机模拟器
     * 
     * @param portName 监听的串口名称 (如 "CNCB0")
     */
    public boolean start(String portName) {
        try {
            this.portName = portName;
            this.serialPort = SerialPort.getCommPort(portName);

            // 设置串口参数
            serialPort.setBaudRate(115200);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serialPort.setParity(SerialPort.NO_PARITY);

            // 设置超时
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 100);

            // 打开端口
            if (serialPort.openPort()) {
                running = true;
                simulatorThread = new Thread(this, "PrinterSimulator-" + portName);
                simulatorThread.start();

                logger.info("打印机模拟器已启动，监听端口: {}", portName);
                printToConsole("=".repeat(50));
                printToConsole("  打印机模拟器已启动");
                printToConsole("  监听端口: " + portName);
                printToConsole(
                        "  时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                printToConsole("=".repeat(50));

                return true;
            } else {
                logger.error("无法打开模拟器端口: {}", portName);
                return false;
            }

        } catch (Exception e) {
            logger.error("启动打印机模拟器失败: {}", portName, e);
            return false;
        }
    }

    /**
     * 模拟器主循环
     */
    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        while (running && serialPort != null && serialPort.isOpen()) {
            try {
                int bytesRead = serialPort.readBytes(buffer, buffer.length);

                if (bytesRead > 0) {
                    // 处理接收到的数据
                    processReceivedData(buffer, bytesRead);
                }

                // 短暂休眠避免CPU占用过高
                Thread.sleep(10);

            } catch (Exception e) {
                if (running) {
                    logger.error("模拟器运行错误", e);
                }
                break;
            }
        }

        logger.info("打印机模拟器已停止");
    }

    /**
     * 处理接收到的数据
     */
    private void processReceivedData(byte[] data, int length) {
        try {
            for (int i = 0; i < length; i++) {
                byte b = data[i];

                // 检查ESC/POS命令
                if (b == ESC && i + 1 < length) {
                    i = processEscCommand(data, i, length);
                } else if (b == GS && i + 1 < length) {
                    i = processGsCommand(data, i, length);
                } else if (b == LF) {
                    // 换行
                    printToConsole("");
                } else if (b == CR) {
                    // 回车 - 忽略
                } else if (b >= 0x20) {
                    // 可打印字符
                    try {
                        // 尝试GBK解码
                        String text = new String(new byte[] { b }, "GBK");
                        printFormattedText(text);
                    } catch (Exception e) {
                        // 如果解码失败，显示为字符
                        System.out.print((char) b);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("处理接收数据失败", e);
        }
    }

    /**
     * 处理ESC命令
     */
    private int processEscCommand(byte[] data, int index, int length) {
        if (index + 1 >= length)
            return index;

        byte command = data[index + 1];

        switch (command) {
            case 0x40: // ESC @ - 重置
                reset();
                printToConsole("[重置打印机]");
                return index + 1;

            case 0x61: // ESC a - 设置对齐
                if (index + 2 < length) {
                    alignment = data[index + 2];
                    String alignText = alignment == 0 ? "左对齐" : (alignment == 1 ? "居中" : "右对齐");
                    printToConsole("[设置对齐: " + alignText + "]");
                    return index + 2;
                }
                break;

            case 0x45: // ESC E - 设置粗体
                if (index + 2 < length) {
                    boldMode = data[index + 2] != 0;
                    printToConsole("[设置粗体: " + (boldMode ? "开启" : "关闭") + "]");
                    return index + 2;
                }
                break;

            case 0x2D: // ESC - - 设置下划线
                if (index + 2 < length) {
                    underlineMode = data[index + 2] != 0;
                    printToConsole("[设置下划线: " + (underlineMode ? "开启" : "关闭") + "]");
                    return index + 2;
                }
                break;

            case 0x33: // ESC 3 - 设置行间距
                if (index + 2 < length) {
                    int spacing = data[index + 2] & 0xFF;
                    printToConsole("[设置行间距: " + spacing + "点]");
                    return index + 2;
                }
                break;

            case 0x76: // ESC v - 状态查询
                printToConsole("[收到状态查询]");
                // 模拟发送状态响应
                sendStatusResponse();
                return index + 1;
        }

        return index + 1;
    }

    /**
     * 处理GS命令
     */
    private int processGsCommand(byte[] data, int index, int length) {
        if (index + 1 >= length)
            return index;

        byte command = data[index + 1];

        switch (command) {
            case 0x21: // GS ! - 设置字符大小
                if (index + 2 < length) {
                    int size = data[index + 2] & 0xFF;
                    int width = (size & 0x0F) + 1;
                    int height = ((size & 0xF0) >> 4) + 1;
                    fontSize = Math.max(width, height);
                    printToConsole("[设置字体大小: " + width + "x" + height + "]");
                    return index + 2;
                }
                break;

            case 0x56: // GS V - 切纸
                if (index + 2 < length) {
                    byte cutType = data[index + 2];
                    String cutText = cutType == 0x41 ? "半切" : "全切";
                    printToConsole("");
                    printToConsole("--- " + cutText + "纸 ---");
                    printToConsole("");
                    return index + 3; // GS V还有一个参数字节
                }
                break;

            case 0x6B: // GS k - 打印条形码
                printToConsole("[开始打印条形码]");
                // 查找条形码数据（以0结尾）
                int dataStart = index + 3;
                int dataEnd = dataStart;
                while (dataEnd < length && data[dataEnd] != 0) {
                    dataEnd++;
                }
                if (dataEnd < length) {
                    String barcodeData = new String(data, dataStart, dataEnd - dataStart, StandardCharsets.UTF_8);
                    printToConsole("条形码: " + barcodeData);
                    printBarcode(barcodeData);
                    return dataEnd;
                }
                break;

            case 0x28: // GS ( - 扩展命令（如二维码）
                if (index + 4 < length && data[index + 2] == 0x6B) { // QR码命令
                    printToConsole("[处理二维码命令]");
                    return processQRCommand(data, index, length);
                }
                break;
        }

        return index + 1;
    }

    /**
     * 处理二维码命令
     */
    private int processQRCommand(byte[] data, int index, int length) {
        // 简化处理，查找二维码数据
        if (index + 10 < length) {
            printToConsole("[打印二维码]");
            printQRCode("QR_CODE_DATA");
            return index + 10; // 跳过固定长度
        }
        return index + 1;
    }

    /**
     * 发送状态响应
     */
    private void sendStatusResponse() {
        try {
            // 模拟发送正常状态
            byte[] response = { 0x12 }; // 正常状态
            serialPort.writeBytes(response, response.length);
        } catch (Exception e) {
            logger.error("发送状态响应失败", e);
        }
    }

    /**
     * 重置模拟器状态
     */
    private void reset() {
        alignment = 0;
        boldMode = false;
        underlineMode = false;
        fontSize = 1;
    }

    /**
     * 打印格式化文本
     */
    private void printFormattedText(String text) {
        String prefix = "";
        String suffix = "";

        if (boldMode) {
            prefix += "**";
            suffix = "**" + suffix;
        }

        if (underlineMode) {
            prefix += "_";
            suffix = "_" + suffix;
        }

        if (fontSize > 1) {
            prefix += "[" + fontSize + "x]";
        }

        String alignPrefix = "";
        if (alignment == 1) {
            alignPrefix = "    "; // 简单居中
        } else if (alignment == 2) {
            alignPrefix = "        "; // 简单右对齐
        }

        System.out.print(alignPrefix + prefix + text + suffix);
    }

    /**
     * 打印条形码（ASCII艺术）
     */
    private void printBarcode(String data) {
        printToConsole("┌" + "─".repeat(data.length() + 2) + "┐");
        printToConsole("│ " + data + " │");
        printToConsole("│" + "█".repeat(data.length() + 2) + "│");
        printToConsole("└" + "─".repeat(data.length() + 2) + "┘");
    }

    /**
     * 打印二维码（ASCII艺术）
     */
    private void printQRCode(String data) {
        printToConsole("┌────────┐");
        printToConsole("│██  ██ │");
        printToConsole("│  ██  ██│");
        printToConsole("│██  ██ │");
        printToConsole("│  ██  ██│");
        printToConsole("└────────┘");
        printToConsole("QR: " + data);
    }

    /**
     * 打印到控制台
     */
    private void printToConsole(String text) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] " + text);
    }

    /**
     * 停止模拟器
     */
    public void stop() {
        running = false;

        if (simulatorThread != null) {
            try {
                simulatorThread.interrupt();
                simulatorThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        close();
    }

    /**
     * 关闭模拟器
     */
    @Override
    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            printToConsole("模拟器端口已关闭: " + portName);
        }
    }

    // Getter方法
    public boolean isRunning() {
        return running && serialPort != null && serialPort.isOpen();
    }

    public String getPortName() {
        return portName;
    }
}