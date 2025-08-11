package com.tobacco.weight.hardware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.print.PrinterJob;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.Paper;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.Color;
import java.awt.image.BufferedImage;
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

import com.tobacco.weight.util.QRCodeGenerator;

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

    /**
     * 生成标签预览图像
     * 
     * @param qrCodeImage 二维码图片
     * @param labelInfo   标签信息
     * @return 标签预览图像
     */
    public BufferedImage generateLabelPreview(BufferedImage qrCodeImage, LabelInfo labelInfo) {
        // 目标DPI：热敏打印机常见分辨率 203DPI，提升清晰度
        final int targetDpi = 203;
        final double scale = targetDpi / 72.0; // 从点（pt，72DPI）到像素的缩放

        // 70x70mm at 72 DPI = 198x198 points
        int widthPt = (int) (70 * 72.0 / 25.4);
        int heightPt = (int) (70 * 72.0 / 25.4);

        // 转成像素尺寸
        int widthPx = (int) Math.round(widthPt * scale);
        int heightPx = (int) Math.round(heightPt * scale);

        BufferedImage previewImage = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = previewImage.createGraphics();

        // 背景白色
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, widthPx, heightPx);

        // 渲染参数：避免模糊
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_SPEED);

        // 将坐标系从点（pt）缩放到像素
        g2d.scale(scale, scale);

        // 模拟可成像区域（2mm边距，单位：pt）
        int marginPt = (int) (2 * 72.0 / 25.4);
        int imageableWidthPt = widthPt - 2 * marginPt;
        int imageableHeightPt = heightPt - 2 * marginPt;

        // 绘制可成像区域边框（灰色虚线），用于预览
        g2d.setColor(Color.LIGHT_GRAY);
        float[] dash = { 2.0f, 2.0f };
        g2d.setStroke(new java.awt.BasicStroke(1.0f, java.awt.BasicStroke.CAP_BUTT,
                java.awt.BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        g2d.drawRect(marginPt, marginPt, imageableWidthPt - 1, imageableHeightPt - 1);

        // 转换到可成像区域坐标系（单位：pt）
        g2d.translate(marginPt, marginPt);

        // 使用与打印相同的绘制逻辑，但提供高分辨率二维码
        BufferedImage hiResQR = qrCodeImage;
        try {
            int qrSizePt = 55; // 与drawLabelContent中的逻辑保持一致（单位：pt）
            int qrSizePx = (int) Math.round(qrSizePt * scale);
            if (labelInfo != null && labelInfo.getContractNumber() != null) {
                hiResQR = QRCodeGenerator.generateQRCodeForPrint(labelInfo.getContractNumber(), qrSizePx);
            }
        } catch (Exception ignore) {
        }
        drawLabelContent(g2d, imageableWidthPt, imageableHeightPt, hiResQR, labelInfo);

        // 添加尺寸标注（在图像底部，回到页面坐标）
        g2d.translate(-marginPt, -marginPt);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString(String.format("70x70mm (~%dx%d px @ %dDPI)", widthPx, heightPx, targetDpi), 5, heightPt - 5);

        g2d.dispose();
        return previewImage;
    }

    /**
     * 保存标签预览图像到文件
     * 
     * @param qrCodeImage 二维码图片
     * @param labelInfo   标签信息
     * @param filePath    保存路径
     * @return 是否保存成功
     */
    public boolean saveLabelPreview(BufferedImage qrCodeImage, LabelInfo labelInfo, String filePath) {
        try {
            BufferedImage preview = generateLabelPreview(qrCodeImage, labelInfo);
            File outputFile = new File(filePath);

            // 确保父目录存在
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            // 保存为PNG格式
            javax.imageio.ImageIO.write(preview, "PNG", outputFile);
            logger.info("标签预览已保存到: {}", filePath);
            return true;
        } catch (Exception e) {
            logger.error("保存标签预览失败", e);
            return false;
        }
    }

    /**
     * 绘制标签内容（共享的绘制逻辑）
     */
    private void drawLabelContent(Graphics2D g2d, int labelWidth, int labelHeight,
            BufferedImage qrCodeImage, LabelInfo labelInfo) {
        // 调试信息
        logger.debug("绘制区域: {}x{}", labelWidth, labelHeight);

        // 绘制边框（调试用，生产环境请注释掉）
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, labelWidth - 1, labelHeight - 1);

        // 统一左侧内边距（与文本对齐）
        final int leftPadding = 2;

        // 绘制二维码 (上半部分) - 极度紧凑以适应70x70mm，且与文本左对齐
        if (qrCodeImage != null) {
            int qrSize = 55; // 极度缩小二维码 (从60->55)
            int qrX = leftPadding; // 左对齐，与文本同边距
            int qrY = 1; // 距离顶部1像素
            g2d.drawImage(qrCodeImage, qrX, qrY, qrSize, qrSize, null);
        }

        // 设置字体 - 极小字体
        Font font = new Font("SimSun", Font.PLAIN, 5); // 从6->5
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);

        // 绘制文本信息 (下半部分) - 极度紧凑布局
        int textY = 59; // 二维码下方，极度减少间距 (从66->59)
        int lineHeight = 7; // 极度减少行高 (从8->7)

        // 绘制各个字段，确保不超出底部边界 - 使用更紧凑的格式
        int bottomMargin = 3; // 底部预留3像素（从5->3）

        if (labelInfo.getLocation() != null && textY + lineHeight <= labelHeight - bottomMargin) {
            g2d.drawString("地址:" + truncateString(labelInfo.getLocation(), 16), leftPadding, textY);
            textY += lineHeight;
        }

        if (labelInfo.getContractNumber() != null && textY + lineHeight <= labelHeight - bottomMargin) {
            g2d.drawString("合同:" + truncateString(labelInfo.getContractNumber(), 16), leftPadding, textY);
            textY += lineHeight;
        }

        if (labelInfo.getFarmerName() != null && textY + lineHeight <= labelHeight - bottomMargin) {
            g2d.drawString("姓名:" + truncateString(labelInfo.getFarmerName(), 16), leftPadding, textY);
            textY += lineHeight;
        }

        if (labelInfo.getPrecheckId() != null && textY + lineHeight <= labelHeight - bottomMargin) {
            g2d.drawString("预检:" + truncateString(labelInfo.getPrecheckId(), 16), leftPadding, textY);
            textY += lineHeight;
        }

        if (labelInfo.getLeafType() != null && textY + lineHeight <= labelHeight - bottomMargin) {
            g2d.drawString("部位:" + truncateString(labelInfo.getLeafType(), 16), leftPadding, textY);
            textY += lineHeight;
        }

        if (labelInfo.getInspector() != null && textY + lineHeight <= labelHeight - bottomMargin) {
            g2d.drawString("检验:" + truncateString(labelInfo.getInspector(), 16), leftPadding, textY);
        }
    }

    /**
     * 打印带二维码的70x70mm标签
     * 
     * @param qrCodeImage 二维码图片
     * @param labelInfo   标签信息
     * @return 打印是否成功
     */
    public boolean printLabelWithQRCode(BufferedImage qrCodeImage, LabelInfo labelInfo) {
        try {
            if (!isConnected) {
                logger.error("打印机未连接，无法打印标签");
                return false;
            }

            PrinterJob printerJob = PrinterJob.getPrinterJob();

            // 创建自定义的Printable对象
            Printable printable = new Printable() {
                @Override
                public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                    if (pageIndex > 0) {
                        return NO_SUCH_PAGE;
                    }

                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                    // 使用实际的可成像区域尺寸
                    int labelWidth = (int) pageFormat.getImageableWidth();
                    int labelHeight = (int) pageFormat.getImageableHeight();

                    // 使用共享的绘制逻辑
                    drawLabelContent(g2d, labelWidth, labelHeight, qrCodeImage, labelInfo);

                    return PAGE_EXISTS;
                }
            };

            // 设置纸张尺寸
            PageFormat pageFormat = printerJob.defaultPage();
            Paper paper = new Paper();

            // 70x70mm转换为点数 (1英寸 = 72点 = 25.4mm)
            double width = 70 * 72.0 / 25.4; // 约198.4点
            double height = 70 * 72.0 / 25.4; // 约198.4点

            paper.setSize(width, height);
            // 设置可成像区域，留出小边距（2mm = 约5.7点）
            double margin = 2 * 72.0 / 25.4;
            paper.setImageableArea(margin, margin, width - 2 * margin, height - 2 * margin);

            pageFormat.setPaper(paper);
            printerJob.setPrintable(printable, pageFormat);
            printerJob.print();

            logger.info("标签打印完成");
            return true;

        } catch (Exception e) {
            logger.error("打印标签失败", e);
            return false;
        }
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncateString(String str, int maxLength) {
        if (str == null)
            return "";
        if (str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength - 2) + "..";
    }

    /**
     * 标签信息类
     */
    public static class LabelInfo {
        private String location;
        private String contractNumber;
        private String farmerName;
        private String precheckId;
        private String leafType;
        private String inspector;

        public LabelInfo(String location, String contractNumber, String farmerName,
                String precheckId, String leafType, String inspector) {
            this.location = location;
            this.contractNumber = contractNumber;
            this.farmerName = farmerName;
            this.precheckId = precheckId;
            this.leafType = leafType;
            this.inspector = inspector;
        }

        // Getters
        public String getLocation() {
            return location;
        }

        public String getContractNumber() {
            return contractNumber;
        }

        public String getFarmerName() {
            return farmerName;
        }

        public String getPrecheckId() {
            return precheckId;
        }

        public String getLeafType() {
            return leafType;
        }

        public String getInspector() {
            return inspector;
        }
    }
}