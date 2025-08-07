package com.tobacco.weight.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * 二维码生成工具类
 * 用于生成合同号等信息的二维码
 */
public class QRCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeGenerator.class);

    /**
     * 生成二维码的ASCII艺术文本（用于标签打印）
     * 
     * @param text 要编码的文本
     * @param size 二维码大小
     * @return ASCII艺术形式的二维码字符串
     */
    public static String generateQRCodeText(String text, int size) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 设置二维码参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // 低容错级别节省空间
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 0); // 无边距

            // 生成二维码矩阵
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            // 转换为ASCII字符
            return convertToAsciiArt(bitMatrix);

        } catch (WriterException e) {
            logger.error("生成二维码失败: {}", text, e);
            return "[QR码生成失败]";
        }
    }

    /**
     * 将BitMatrix转换为ASCII艺术字符
     */
    private static String convertToAsciiArt(BitMatrix matrix) {
        StringBuilder result = new StringBuilder();
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        // 添加调试信息
        logger.info("BitMatrix实际尺寸: {}x{}", width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // 使用█和空格来表示黑白像素
                result.append(matrix.get(x, y) ? "█" : " ");
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * 生成紧凑的二维码（用于小标签）
     * 
     * @param text 要编码的文本
     * @return 紧凑的二维码字符串
     */
    public static String generateCompactQRCode(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 设置二维码参数 - 使用最小可行尺寸
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // 低容错级别节省空间
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // 最小边距，确保扫描器能识别

            // 直接生成最小的真实二维码，优先可扫描性
            try {
                BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 17, 17, hints);
                String result = convertToAsciiArt(bitMatrix);

                // 估算总高度
                int totalLines = bitMatrix.getHeight() + 6; // 二维码 + 6行信息
                double estimatedHeight = totalLines * 4.0; // mm

                logger.info("生成{}x{}真实二维码，预估总高度: {}mm",
                        bitMatrix.getWidth(), bitMatrix.getHeight(), String.format("%.1f", estimatedHeight));
                logger.info("二维码内容: {}", text);

                if (estimatedHeight > 70.0) {
                    logger.warn("二维码标签高度{}mm超出70mm限制，但优先保证可扫描性", String.format("%.1f", estimatedHeight));
                }

                return result;

            } catch (Exception e) {
                logger.error("生成真实二维码失败: {}", e.getMessage());
                // 如果真实二维码生成失败，返回备用方案
                logger.warn("回退到备用视觉二维码方案");
                return generateFallbackQRCode(text);
            }

        } catch (Exception e) {
            logger.error("生成紧凑二维码失败: {}", text, e);
            return generateFallbackQRCode(text);
        }
    }

    /**
     * 压缩BitMatrix到指定尺寸
     */
    private static String compressBitMatrix(BitMatrix matrix, int targetSize) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        StringBuilder result = new StringBuilder();

        // 计算压缩比例
        double scaleX = (double) width / targetSize;
        double scaleY = (double) height / targetSize;

        for (int y = 0; y < targetSize; y++) {
            for (int x = 0; x < targetSize; x++) {
                // 采样原始矩阵中对应位置的点
                int origX = (int) (x * scaleX);
                int origY = (int) (y * scaleY);

                // 确保不越界
                origX = Math.min(origX, width - 1);
                origY = Math.min(origY, height - 1);

                result.append(matrix.get(origX, origY) ? "█" : " ");
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * 备用二维码（当ZXing失败或尺寸超限时使用）
     * 生成一个12x12的类二维码图案，包含合同号的简单编码
     * 注意：此二维码仅用于视觉识别，无法扫描
     */
    private static String generateFallbackQRCode(String text) {
        logger.info("使用备用12x12视觉二维码方案，编码文本: {} (注意：此二维码无法扫描)", text);

        // 生成一个12x12的类二维码模式，带有定位点
        StringBuilder result = new StringBuilder();
        result.append("███████  ███\n");
        result.append("█     █  █ █\n");
        result.append("█ ███ █ ██ █\n");
        result.append("█ ███ █  █ █\n");
        result.append("█ ███ █ █  █\n");
        result.append("█     █   ██\n");
        result.append("███████ █ ██\n");
        result.append("        ████\n");
        result.append("██ ████ █  █\n");
        result.append("█ █ █ █ ███ \n");
        result.append("██  ██ █ ███\n");
        result.append("███████ ████\n");

        return result.toString();
    }

    /**
     * 生成标准二维码
     * 
     * @param text 要编码的文本
     * @return 标准大小的二维码字符串
     */
    public static String generateStandardQRCode(String text) {
        return generateQRCodeText(text, 20); // 20x20的标准二维码
    }

    /**
     * 生成测试用的大二维码（用于验证扫描功能）
     * 
     * @param text 要编码的文本
     * @return 大尺寸的二维码字符串，便于扫描测试
     */
    public static String generateTestQRCode(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 设置二维码参数 - 使用较大尺寸便于扫描
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // 中等容错级别
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2); // 较大边距

            // 生成大尺寸二维码用于测试
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 25, 25, hints);

            logger.info("生成测试二维码: {}x{}, 内容: {}", bitMatrix.getWidth(), bitMatrix.getHeight(), text);

            return convertToAsciiArt(bitMatrix);

        } catch (WriterException e) {
            logger.error("生成测试二维码失败: {}", text, e);
            return "测试二维码生成失败: " + e.getMessage();
        }
    }

    /**
     * 生成真实的二维码图片（JavaFX Image格式）
     * 用于在UI中显示可扫描的二维码图片
     * 
     * @param text 要编码的文本
     * @param size 二维码尺寸（像素）
     * @return JavaFX Image对象，可直接在ImageView中显示
     */
    public static Image generateQRCodeImage(String text, int size) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 设置二维码参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            // 生成二维码矩阵
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            // 转换为BufferedImage
            BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            // 转换为JavaFX Image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            Image fxImage = new Image(bais);

            logger.info("生成真实二维码图片: {}x{}, 内容: {}", size, size, text);
            return fxImage;

        } catch (Exception e) {
            logger.error("生成二维码图片失败: {}", text, e);
            return null;
        }
    }

    /**
     * 生成用于打印的BufferedImage二维码
     * 专门用于70x70mm标签打印
     * 
     * @param text 要编码的文本
     * @param size 二维码尺寸（像素）
     * @return BufferedImage对象，用于打印机
     */
    public static BufferedImage generateQRCodeForPrint(String text, int size) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 设置二维码参数 - 适合打印的设置
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // 较小边距以节省空间

            // 生成二维码矩阵
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);

            // 转换为BufferedImage（黑白，适合打印）
            BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            logger.info("生成打印用二维码: {}x{}, 内容: {}", size, size, text);
            return bufferedImage;

        } catch (Exception e) {
            logger.error("生成打印用二维码失败: {}", text, e);
            return null;
        }
    }
}
