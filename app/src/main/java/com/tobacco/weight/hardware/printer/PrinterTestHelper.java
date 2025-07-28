package com.tobacco.weight.hardware.printer;

import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * 打印机测试辅助类
 * 用于验证打印机模块的功能
 */
public class PrinterTestHelper {
    
    private static final String TAG = "PrinterTestHelper";
    
    /**
     * 测试打印机模块的基本功能
     */
    public static void testPrinterModule(Context context) {
        Log.i(TAG, "Starting printer module test...");
        
        // 创建打印机管理器
        PrinterManager printerManager = new PrinterManager(context);
        
        // 设置回调
        printerManager.setCallback(new PrinterManager.PrinterCallback() {
            @Override
            public void onConnectionSuccess(String devicePath) {
                Log.i(TAG, "Connection success: " + devicePath);
            }
            
            @Override
            public void onConnectionFailed(String error) {
                Log.w(TAG, "Connection failed: " + error);
            }
            
            @Override
            public void onPrintComplete() {
                Log.i(TAG, "Print completed");
            }
            
            @Override
            public void onPrintError(String error) {
                Log.e(TAG, "Print error: " + error);
            }
            
            @Override
            public void onStatusUpdate(String status) {
                Log.i(TAG, "Status update: " + status);
            }
        });
        
        // 测试设备发现
        List<PrinterManager.Device> devices = printerManager.getAvailablePrinters();
        Log.i(TAG, "Found " + devices.size() + " devices");
        
        for (PrinterManager.Device device : devices) {
            Log.i(TAG, "Device: " + device.toString());
        }
        
        // 如果有设备，测试连接
        if (!devices.isEmpty()) {
            PrinterManager.Device testDevice = devices.get(0);
            Log.i(TAG, "Testing connection to: " + testDevice.getName());
            
            boolean connected = printerManager.connectToDevice(testDevice);
            if (connected) {
                Log.i(TAG, "Connection test successful");
                
                // 测试收据打印
                testReceiptPrinting(printerManager);
                
                // 测试标签打印
                testLabelPrinting(printerManager);
                
                // 关闭连接
                printerManager.closeConnection();
            } else {
                Log.w(TAG, "Connection test failed");
            }
        }
        
        // 释放资源
        printerManager.release();
        
        Log.i(TAG, "Printer module test completed");
    }
    
    /**
     * 测试收据打印
     */
    private static void testReceiptPrinting(PrinterManager printerManager) {
        Log.i(TAG, "Testing receipt printing...");
        
        // 创建测试收据
        ReceiptData receipt = ReceiptData.createTest();
        
        // 打印收据
        printerManager.printReceipt(receipt);
        
        Log.i(TAG, "Receipt printing test completed");
    }
    
    /**
     * 测试标签打印
     */
    private static void testLabelPrinting(PrinterManager printerManager) {
        Log.i(TAG, "Testing label printing...");
        
        // 创建测试标签
        LabelData label = LabelData.createTest();
        
        // 打印标签
        printerManager.printLabel(label);
        
        Log.i(TAG, "Label printing test completed");
    }
    
    /**
     * 创建烟叶收据
     */
    public static ReceiptData createTobaccoReceipt(String farmerName, String idCard, String weight, String totalAmount) {
        ReceiptData receipt = new ReceiptData("烟叶收购凭证");
        
        receipt.addSeparator();
        receipt.addKeyValue("农户姓名", farmerName != null ? farmerName : "");
        receipt.addKeyValue("身份证号", idCard != null ? hideIdCard(idCard) : "");
        receipt.addKeyValue("重量", weight != null ? weight + " kg" : "");
        receipt.addKeyValue("金额", totalAmount != null ? "¥" + totalAmount : "");
        receipt.addSeparator();
        receipt.addCenterItem("感谢您的合作！");
        
        // 添加二维码（包含完整信息）
        if (idCard != null && weight != null) {
            String qrData = String.format("农户:%s,身份证:%s,重量:%s", 
                farmerName != null ? farmerName : "", idCard, weight);
            receipt.setQrCode(qrData);
        }
        
        return receipt;
    }
    
    /**
     * 创建烟叶标签
     */
    public static LabelData createTobaccoLabel(String farmerName, String idCard, String weight) {
        return LabelData.createTobaccoLabel(farmerName, idCard, weight, 
            String.format("农户:%s,重量:%s", farmerName, weight));
    }
    
    /**
     * 隐藏身份证中间部分
     */
    private static String hideIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }
    
    /**
     * 测试异常处理
     */
    public static void testErrorHandling() {
        Log.i(TAG, "Testing error handling...");
        
        // 测试PrinterException
        try {
            throw PrinterException.connectionFailed("/dev/ttyUSB0", "Device not found");
        } catch (PrinterException e) {
            Log.i(TAG, "Caught expected exception: " + e.getUserMessage());
            Log.i(TAG, "Suggested solution: " + e.getSuggestedSolution());
            Log.i(TAG, "Is retryable: " + e.isRetryable());
        }
        
        // 测试StatusCodeHandler
        StatusCodeHandler.PrinterStatus status = StatusCodeHandler.parseStatus("FC4F4B");
        Log.i(TAG, "Parsed status: " + StatusCodeHandler.getStatusMessage(status));
        Log.i(TAG, "Is error: " + StatusCodeHandler.isErrorStatus(status));
        
        Log.i(TAG, "Error handling test completed");
    }
} 