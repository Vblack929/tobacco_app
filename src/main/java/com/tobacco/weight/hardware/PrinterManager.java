package com.tobacco.weight.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;

/**
 * 打印机管理器
 * 负责小票打印功能
 */
public class PrinterManager {

    private static final Logger logger = LoggerFactory.getLogger(PrinterManager.class);

    private boolean isConnected = false;
    private String printerName;

    // 回调函数
    private Consumer<Boolean> onConnectionStatusChanged;
    private Consumer<String> onPrintStatus;

    /**
     * 构造函数
     */
    public PrinterManager() {
        initializePrinter();
    }

    /**
     * 初始化打印机
     */
    private void initializePrinter() {
        try {
            // 查找默认打印机
            PrinterJob printerJob = PrinterJob.getPrinterJob();
            if (printerJob.getPrintService() != null) {
                printerName = printerJob.getPrintService().getName();
                isConnected = true;
                logger.info("打印机初始化成功: {}", printerName);

                // 通知连接状态变化
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(true);
                }
            } else {
                logger.warn("未找到可用的打印机");
            }

        } catch (Exception e) {
            logger.error("初始化打印机失败", e);
        }
    }

    /**
     * 打印称重小票
     */
    public boolean printWeighingReceipt(String farmerName, String contractNumber,
            String leafType, double weight, String operator) {
        return printWeighingReceipt(farmerName, contractNumber, leafType, weight, operator, 1);
    }

    /**
     * 打印称重小票（包含捆数）
     */
    public boolean printWeighingReceipt(String farmerName, String contractNumber,
            String leafType, double weight, String operator, int bundleCount) {
        return printWeighingReceipt(farmerName, contractNumber, leafType, weight, operator, bundleCount, null);
    }

    /**
     * 打印称重小票（包含捆数和预检编号）
     */
    public boolean printWeighingReceipt(String farmerName, String contractNumber,
            String leafType, double weight, String operator, int bundleCount, String precheckId) {
        try {
            // 生成小票内容
            String receiptContent = generateReceiptContent(farmerName, contractNumber,
                    leafType, weight, operator, bundleCount, precheckId);

            // 打印小票
            return printText(receiptContent);

        } catch (Exception e) {
            logger.error("打印称重小票失败", e);
            if (onPrintStatus != null) {
                onPrintStatus.accept("打印失败: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 生成小票内容
     */
    private String generateReceiptContent(String farmerName, String contractNumber,
            String leafType, double weight, String operator) {
        return generateReceiptContent(farmerName, contractNumber, leafType, weight, operator, 1, null);
    }

    /**
     * 生成小票内容（包含捆数）
     */
    private String generateReceiptContent(String farmerName, String contractNumber,
            String leafType, double weight, String operator, int bundleCount) {
        return generateReceiptContent(farmerName, contractNumber, leafType, weight, operator, bundleCount, null);
    }

    /**
     * 生成小票内容（包含捆数和预检编号）
     */
    private String generateReceiptContent(String farmerName, String contractNumber,
            String leafType, double weight, String operator, int bundleCount, String precheckId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        StringBuilder content = new StringBuilder();
        content.append("=".repeat(32)).append("\n");
        content.append("        烟叶称重小票\n");
        content.append("=".repeat(32)).append("\n");
        content.append("时间: ").append(currentTime).append("\n");
        if (precheckId != null && !precheckId.isEmpty()) {
            content.append("预检编号: ").append(precheckId).append("\n");
        }
        content.append("烟农: ").append(farmerName).append("\n");
        content.append("合同号: ").append(contractNumber).append("\n");
        content.append("部叶类型: ").append(leafType).append("\n");
        content.append("重量: ").append(String.format("%.2f kg", weight)).append("\n");
        content.append("捆数: ").append(bundleCount).append("\n");
        content.append("操作员: ").append(operator).append("\n");
        content.append("=".repeat(32)).append("\n");
        content.append("        谢谢使用\n");
        content.append("=".repeat(32)).append("\n");

        return content.toString();
    }

    /**
     * 打印文本
     */
    public boolean printText(String text) {
        try {
            // 查找可用的打印服务
            javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
            javax.print.PrintService targetService = null;

            // 优先查找POS80或相关打印机
            for (javax.print.PrintService service : services) {
                String name = service.getName().toLowerCase();
                if (name.contains("pos80") || name.contains("pos") ||
                        name.contains("receipt") || name.contains("thermal")) {
                    targetService = service;
                    logger.info("找到POS打印机: {}", service.getName());
                    break;
                }
            }

            // 如果没找到POS打印机，使用默认打印机
            if (targetService == null && services.length > 0) {
                targetService = javax.print.PrintServiceLookup.lookupDefaultPrintService();
                if (targetService != null) {
                    logger.info("使用默认打印机: {}", targetService.getName());
                } else {
                    targetService = services[0];
                    logger.info("使用第一个可用打印机: {}", targetService.getName());
                }
            }

            if (targetService == null) {
                logger.error("未找到可用的打印机");
                if (onPrintStatus != null) {
                    onPrintStatus.accept("未找到可用打印机");
                }
                return false;
            }

            // 创建打印任务
            javax.print.DocPrintJob printJob = targetService.createPrintJob();

            // 创建文档
            javax.print.attribute.DocAttributeSet docAttribs = new javax.print.attribute.HashDocAttributeSet();
            javax.print.DocFlavor flavor = javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE;

            // 对于POS打印机，尝试发送原始字节数据
            byte[] printData;
            if (targetService.getName().toLowerCase().contains("pos")) {
                // 为POS打印机添加基本的ESC/POS指令
                StringBuilder escText = new StringBuilder();
                escText.append("\u001B@"); // ESC @ - 初始化打印机
                escText.append("\u001Ba\u0001"); // ESC a 1 - 居中对齐
                escText.append("=== 系统打印测试 ===\n");
                escText.append("\u001Ba\u0000"); // ESC a 0 - 左对齐
                escText.append(text);
                escText.append("\n\n\n");
                escText.append("\u001Bi"); // ESC i - 切纸
                printData = escText.toString().getBytes("GBK");
            } else {
                // 普通打印机使用UTF-8编码
                printData = text.getBytes("UTF-8");
            }

            javax.print.SimpleDoc doc = new javax.print.SimpleDoc(printData, flavor, docAttribs);

            // 执行打印
            printJob.print(doc, null);

            logger.info("系统打印任务已提交到: {}", targetService.getName());
            if (onPrintStatus != null) {
                onPrintStatus.accept("打印成功 - " + targetService.getName());
            }
            return true;

        } catch (Exception e) {
            logger.error("系统打印失败", e);
            if (onPrintStatus != null) {
                onPrintStatus.accept("系统打印失败: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * 打印汇总报表
     */
    public boolean printSummaryReport(String farmerName, double totalWeight,
            int totalBundles, String dateRange) {
        try {
            String reportContent = generateSummaryReport(farmerName, totalWeight,
                    totalBundles, dateRange);
            return printText(reportContent);

        } catch (Exception e) {
            logger.error("打印汇总报表失败", e);
            return false;
        }
    }

    /**
     * 生成汇总报表内容
     */
    private String generateSummaryReport(String farmerName, double totalWeight,
            int totalBundles, String dateRange) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        StringBuilder content = new StringBuilder();
        content.append("=".repeat(40)).append("\n");
        content.append("          烟叶称重汇总报表\n");
        content.append("=".repeat(40)).append("\n");
        content.append("生成时间: ").append(currentTime).append("\n");
        content.append("统计期间: ").append(dateRange).append("\n");
        content.append("烟农姓名: ").append(farmerName).append("\n");
        content.append("总重量: ").append(String.format("%.2f kg", totalWeight)).append("\n");
        content.append("总捆数: ").append(totalBundles).append("\n");
        content.append("=".repeat(40)).append("\n");

        return content.toString();
    }

    /**
     * 获取可用打印机列表
     */
    public String[] getAvailablePrinters() {
        try {
            javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);

            String[] printerNames = new String[services.length];
            for (int i = 0; i < services.length; i++) {
                printerNames[i] = services[i].getName();
            }

            return printerNames;

        } catch (Exception e) {
            logger.error("获取打印机列表失败", e);
            return new String[0];
        }
    }

    /**
     * 设置默认打印机
     */
    public boolean setDefaultPrinter(String printerName) {
        try {
            javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);

            for (javax.print.PrintService service : services) {
                if (service.getName().equals(printerName)) {
                    this.printerName = printerName;
                    logger.info("设置默认打印机: {}", printerName);
                    return true;
                }
            }

            logger.error("未找到指定的打印机: {}", printerName);
            return false;

        } catch (Exception e) {
            logger.error("设置默认打印机失败", e);
            return false;
        }
    }

    /**
     * 测试打印机连接
     */
    public boolean testPrinter() {
        try {
            String testContent = "打印机测试\n" + new Date().toString() + "\n";
            return printText(testContent);

        } catch (Exception e) {
            logger.error("打印机测试失败", e);
            return false;
        }
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 获取打印机名称
     */
    public String getPrinterName() {
        return printerName;
    }

    /**
     * 设置连接状态变化回调
     */
    public void setOnConnectionStatusChanged(Consumer<Boolean> callback) {
        this.onConnectionStatusChanged = callback;
    }

    /**
     * 设置打印状态回调
     */
    public void setOnPrintStatus(Consumer<String> callback) {
        this.onPrintStatus = callback;
    }

    /**
     * 获取打印机信息
     */
    public String getPrinterInfo() {
        if (isConnected) {
            return String.format("打印机: %s, 状态: %s",
                    printerName, isConnected ? "已连接" : "未连接");
        }
        return "未连接";
    }
}