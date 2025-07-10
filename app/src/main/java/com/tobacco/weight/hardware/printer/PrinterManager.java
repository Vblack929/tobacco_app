package com.tobacco.weight.hardware.printer;

import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.util.Log;

import com.tobacco.weight.hardware.serial.SerialPortManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 打印机管理器
 * 独立的USB/串口打印机通信模块，支持ESC/POS和Label命令
 */
@Singleton
public class PrinterManager {
    
    private static final String TAG = "PrinterManager";
    
    // 常用串口路径
    private static final String[] COMMON_SERIAL_PATHS = {
        "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2",
        "/dev/ttyS0", "/dev/ttyS1", "/dev/ttyS2"
    };
    
    // 常用波特率
    private static final int[] COMMON_BAUD_RATES = {
        9600, 19200, 38400, 57600, 115200
    };
    
    private Context context;
    private SerialPortManager serialPortManager;
    private PrinterCallback callback;
    private Device connectedDevice;
    private boolean isConnected = false;
    
    // Test mode - always simulate successful printing
    private boolean testMode = true;

    // Removed old constructor that was conflicting with dependency injection
    
    /**
     * 设备信息类
     */
    public static class Device {
        private String name;
        private String path;
        private int baudRate;
        private boolean canRead;
        private boolean canWrite;
        
        public Device(String name, String path, int baudRate) {
            this.name = name;
            this.path = path;
            this.baudRate = baudRate;
            
            // 检查设备权限
            java.io.File file = new java.io.File(path);
            this.canRead = file.canRead();
            this.canWrite = file.canWrite();
        }
        
        public String getName() { return name; }
        public String getPath() { return path; }
        public int getBaudRate() { return baudRate; }
        public boolean canRead() { return canRead; }
        public boolean canWrite() { return canWrite; }
        public java.io.File getFile() { return new java.io.File(path); }
        
        @Override
        public String toString() {
            return name + " (" + path + " @ " + baudRate + " baud)";
        }
    }
    
    /**
     * 打印机回调接口
     */
    public interface PrinterCallback {
        void onConnectionSuccess(String devicePath);
        void onConnectionFailed(String error);
        void onPrintComplete();
        void onPrintError(String error);
        void onStatusUpdate(String status);
    }
    
    @Inject
    public PrinterManager(Context context) {
        this.context = context.getApplicationContext();
        this.serialPortManager = new SerialPortManager();
    }
    
    /**
     * 初始化打印机模块
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        Log.i(TAG, "PrinterManager initialized");
    }
    
    /**
     * 设置回调监听器
     */
    public void setCallback(PrinterCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 获取可用的打印机设备列表
     */
    public List<Device> getAvailablePrinters() {
        List<Device> devices = new ArrayList<>();
        
        // 扫描常见串口路径
        for (String path : COMMON_SERIAL_PATHS) {
            java.io.File deviceFile = new java.io.File(path);
            if (deviceFile.exists()) {
                // 为每个可用路径尝试不同波特率
                for (int baudRate : COMMON_BAUD_RATES) {
                    String deviceName = "Serial Printer (" + 
                        deviceFile.getName() + " @ " + baudRate + ")";
                    devices.add(new Device(deviceName, path, baudRate));
                }
                // 只为存在的设备添加一次，使用默认波特率115200
                break;
            }
        }
        
        // 如果没有找到设备，添加模拟设备用于测试
        if (devices.isEmpty()) {
            devices.add(new Device("Simulated Printer", "/dev/null", 115200));
            Log.w(TAG, "No physical devices found, added simulated device");
        }
        
        Log.i(TAG, "Found " + devices.size() + " potential printer devices");
        return devices;
    }
    
    /**
     * 连接到指定设备
     */
    public boolean connectToDevice(Device device) {
        if (device == null) {
            notifyConnectionFailed("Device is null");
            return false;
        }
        
        Log.i(TAG, "Attempting to connect to: " + device.toString());
        
        // 断开现有连接
        if (isConnected) {
            closeConnection();
        }
        
        try {
            // 特殊处理模拟设备
            if (device.getPath().equals("/dev/null")) {
                Log.i(TAG, "Connected to simulated printer");
                this.connectedDevice = device;
                this.isConnected = true;
                notifyConnectionSuccess(device.getPath());
                return true;
            }
            
            // 尝试打开串口连接
            boolean success = serialPortManager.openSerialPort(device.getPath(), device.getBaudRate());
            
            if (success) {
                this.connectedDevice = device;
                this.isConnected = true;
                
                // 发送初始化命令
                initializePrinter();
                
                Log.i(TAG, "Successfully connected to printer: " + device.getPath());
                notifyConnectionSuccess(device.getPath());
                return true;
            } else {
                Log.e(TAG, "Failed to open serial port: " + device.getPath());
                notifyConnectionFailed("Failed to open serial port");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception connecting to device", e);
            notifyConnectionFailed("Connection exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 初始化打印机
     */
    private void initializePrinter() {
        try {
            if (isConnected && !connectedDevice.getPath().equals("/dev/null")) {
                // 发送ESC/POS初始化命令
                byte[] initCmd = {0x1B, 0x40}; // ESC @
                serialPortManager.sendData(initCmd);
                Thread.sleep(100);
                
                Log.d(TAG, "Printer initialized");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize printer", e);
        }
    }
    
    /**
     * 检查打印机连接状态
     */
    public boolean isConnected() {
        return isConnected && connectedDevice != null;
    }
    
    /**
     * 打印收据 (简单版本 - 向后兼容)
     */
    public void printReceipt(String content) {
        ReceiptData receipt = ReceiptData.createSimple("RECEIPT", content);
        printReceipt(receipt);
    }
    
    /**
     * 打印收据 (完整版本)
     */
    public void printReceipt(ReceiptData receiptData) {
        if (!isConnected()) {
            notifyPrintError("Printer not connected");
            return;
        }
        
        if (receiptData == null || !receiptData.isValid()) {
            notifyPrintError("Invalid receipt data");
            return;
        }
        
        try {
            Log.i(TAG, "Printing receipt...");
            
            if (connectedDevice.getPath().equals("/dev/null")) {
                // 模拟打印
                Log.i(TAG, "Simulated receipt print: " + receiptData.toString());
                Thread.sleep(500); // 模拟打印时间
                notifyPrintComplete();
                return;
            }
            
            // 创建ESC命令
            Esc esc = new Esc();
            esc.reset();
            
            // 打印标题
            if (receiptData.getHeader() != null && !receiptData.getHeader().trim().isEmpty()) {
                esc.align(1); // 居中对齐
                esc.textType(0, 0, 0, 1, 1, 1, 0); // 粗体，双倍大小
                esc.printText(receiptData.getHeader());
                esc.formfeedY(2);
            }
            
            // 打印内容项目
            if (receiptData.getItems() != null) {
                for (ReceiptData.ReceiptItem item : receiptData.getItems()) {
                    // 设置对齐方式
                    esc.align(item.getAlignment());
                    
                    // 设置字体属性
                    esc.textType(0, 0, 0, 
                        item.isBold() ? 1 : 0,
                        item.isDoubleSize() ? 1 : 0,
                        item.isDoubleSize() ? 1 : 0,
                        item.isUnderline() ? 1 : 0);
                    
                    // 打印文本
                    esc.printText(item.getText());
                    esc.formfeedY(1);
                }
            }
            
            // 打印条形码
            if (receiptData.getBarcode() != null && !receiptData.getBarcode().trim().isEmpty()) {
                esc.align(1); // 居中对齐
                esc.formfeedY(1);
                esc.printBarCode(73, receiptData.getBarcode()); // Code128
                esc.formfeedY(2);
            }
            
            // 打印二维码
            if (receiptData.getQrCode() != null && !receiptData.getQrCode().trim().isEmpty()) {
                esc.align(1); // 居中对齐
                esc.createQR(receiptData.getQrCode());
                esc.QRSize(5);
                esc.printQR();
                esc.formfeedY(2);
            }
            
            // 打印页脚
            if (receiptData.getFooter() != null && !receiptData.getFooter().trim().isEmpty()) {
                esc.align(1); // 居中对齐
                esc.printText(receiptData.getFooter());
                esc.formfeedY(2);
            }
            
            // 最终走纸
            esc.formfeedY(receiptData.getFeedLines());
            
            // 切纸 (如果启用)
            if (receiptData.isEnableCut()) {
                esc.cutPaper();
            }
            
            // 发送命令到打印机
            sendEscCommands(esc);
            esc.clear();
            
            notifyPrintComplete();
            Log.i(TAG, "Receipt printed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to print receipt", e);
            notifyPrintError("Print failed: " + e.getMessage());
        }
    }
    
    /**
     * 打印标签 (简单版本 - 向后兼容)
     */
    public void printLabel(String name, String id, String qrCodeData) {
        LabelData label = LabelData.createSimple(name, "ID: " + id);
        if (qrCodeData != null && !qrCodeData.isEmpty()) {
            label.addQRCode(250, 80, qrCodeData);
        }
        if (id != null && !id.isEmpty()) {
            label.addBarcode(10, 100, id);
        }
        printLabel(label);
    }
    
    /**
     * 打印标签 (完整版本)
     */
    public void printLabel(LabelData labelData) {
        // Test mode - always simulate successful printing
        if (testMode) {
            Log.i(TAG, "TEST MODE: Simulating successful print operation");
            
            // Simulate connection success first
            notifyConnectionSuccess("TEST-PRINTER-USB");
            
            // Simulate printing process with realistic delays
            new Thread(() -> {
                try {
                    // Simulate connection and preparation time
                    Thread.sleep(500);
                    notifyStatusUpdate("正在准备打印机...");
                    
                    Thread.sleep(300);
                    notifyStatusUpdate("正在发送标签数据...");
                    
                    Thread.sleep(800);
                    notifyStatusUpdate("正在打印标签...");
                    
                    Thread.sleep(1000);
                    Log.i(TAG, "TEST MODE: Simulated label print: " + (labelData != null ? labelData.toString() : "null data"));
                    notifyPrintComplete();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    notifyPrintError("测试模式被中断");
                }
            }).start();
            
            return;
        }
        
        if (!isConnected()) {
            notifyPrintError("Printer not connected");
            return;
        }
        
        if (labelData == null || !labelData.isValid()) {
            notifyPrintError("Invalid label data");
            return;
        }
        
        try {
            Log.i(TAG, "Printing label...");
            
            if (connectedDevice.getPath().equals("/dev/null")) {
                // 模拟打印
                Log.i(TAG, "Simulated label print: " + labelData.toString());
                Thread.sleep(800); // 模拟打印时间
                notifyPrintComplete();
                return;
            }
            
            // 创建标签命令
            Label label = new Label();
            
            // 设置页面属性
            label.pageStart(labelData.getX(), labelData.getY(), 
                          labelData.getWidth(), labelData.getHeight(), 
                          labelData.getRotation());
            
            // 设置打印参数
            if (labelData.getDensity() > 0) {
                label.setDensity(labelData.getDensity());
            }
            if (labelData.getSpeed() > 0) {
                label.setSpeed(labelData.getSpeed());
            }
            
            // 处理所有元素
            for (LabelData.LabelElement element : labelData.getElements()) {
                switch (element.getType()) {
                    case TEXT:
                        label.printText(element.getX(), element.getY(), 
                                      element.getFontSize(), 
                                      element.isBold() ? 1 : 0, 
                                      element.isUnderline() ? 1 : 0, 
                                      element.getValue(), 
                                      element.getRotation());
                        break;
                        
                    case BARCODE:
                        label.printBarCode(element.getX(), element.getY(), 
                                         element.getBarcodeType(),
                                         element.getHeight(), 
                                         element.getWidth(), 
                                         element.getValue(), 
                                         element.getRotation());
                        break;
                        
                    case QR_CODE:
                        label.printQR(element.getX(), element.getY(), 
                                    element.getQrSize(),
                                    element.getQrVersion(), 
                                    element.getQrEcc(), 
                                    element.getValue(), 
                                    element.getRotation());
                        break;
                        
                    case LINE:
                        label.drawLine(element.getX(), element.getY(),
                                     element.getX() + element.getWidth(),
                                     element.getY() + element.getHeight(),
                                     element.getThickness());
                        break;
                        
                    case RECTANGLE:
                        label.drawRect(element.getX(), element.getY(),
                                     element.getWidth(), element.getHeight(),
                                     element.getThickness());
                        break;
                        
                    case IMAGE:
                        if (element.getImagePath() != null) {
                            label.printImage(element.getX(), element.getY(),
                                           element.getWidth(), element.getHeight(),
                                           element.getImagePath());
                        }
                        break;
                }
            }
            
            label.pageEnd();
            label.customPrintPage(labelData.getCopies());
            
            // 发送命令到打印机
            sendLabelCommands(label);
            label.clear();
            
            notifyPrintComplete();
            Log.i(TAG, "Label printed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to print label", e);
            notifyPrintError("Print failed: " + e.getMessage());
        }
    }
    
    /**
     * 发送ESC命令到打印机
     */
    private void sendEscCommands(Esc esc) throws IOException {
        if (serialPortManager != null && serialPortManager.isOpen()) {
            List<byte[]> commands = esc.getCommands();
            for (byte[] command : commands) {
                serialPortManager.sendData(command);
                try {
                    Thread.sleep(10); // 小延迟确保命令按序发送
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * 发送Label命令到打印机
     */
    private void sendLabelCommands(Label label) throws IOException {
        if (serialPortManager != null && serialPortManager.isOpen()) {
            List<byte[]> commands = label.getCommands();
            for (byte[] command : commands) {
                serialPortManager.sendData(command);
                try {
                    Thread.sleep(10); // 小延迟确保命令按序发送
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    /**
     * 关闭打印机连接
     */
    public void closeConnection() {
        try {
            if (serialPortManager != null && serialPortManager.isOpen()) {
                serialPortManager.closeSerialPort();
            }
            
            isConnected = false;
            connectedDevice = null;
            
            Log.i(TAG, "Printer connection closed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error closing printer connection", e);
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        closeConnection();
        if (serialPortManager != null) {
            serialPortManager.release();
        }
        callback = null;
        Log.i(TAG, "PrinterManager resources released");
    }
    
    // 通知方法
    private void notifyConnectionSuccess(String devicePath) {
        if (callback != null) {
            callback.onConnectionSuccess(devicePath);
        }
    }
    
    private void notifyConnectionFailed(String error) {
        if (callback != null) {
            callback.onConnectionFailed(error);
        }
    }
    
    private void notifyPrintComplete() {
        if (callback != null) {
            callback.onPrintComplete();
        }
    }
    
    private void notifyPrintError(String error) {
        if (callback != null) {
            callback.onPrintError(error);
        }
    }
    
    private void notifyStatusUpdate(String status) {
        if (callback != null) {
            callback.onStatusUpdate(status);
        }
    }
    
    // Test mode control methods
    public void setTestMode(boolean enabled) {
        this.testMode = enabled;
        Log.i(TAG, "Test mode " + (enabled ? "ENABLED" : "DISABLED"));
    }
    
    public boolean isTestMode() {
        return testMode;
    }
    
    /**
     * 测试打印失败场景
     */
    public void simulatePrintFailure(String errorMessage) {
        if (!testMode) {
            Log.w(TAG, "simulatePrintFailure called but test mode is disabled");
            return;
        }
        
        Log.i(TAG, "TEST MODE: Simulating print failure");
        
        new Thread(() -> {
            try {
                Thread.sleep(500);
                notifyStatusUpdate("正在连接打印机...");
                
                Thread.sleep(800);
                notifyPrintError(errorMessage != null ? errorMessage : "测试打印失败场景");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}