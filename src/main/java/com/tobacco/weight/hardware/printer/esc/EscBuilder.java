package com.tobacco.weight.hardware.printer.esc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * ESC/POS指令生成器
 * 基于Android USB打印demo移植，用于生成ESC/POS打印指令
 */
public class EscBuilder {

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    // ESC/POS控制字符
    private static final byte ESC = 0x1B; // ESC
    private static final byte GS = 0x1D; // GS
    private static final byte LF = 0x0A; // 换行
    private static final byte CR = 0x0D; // 回车

    /**
     * 重置打印机
     */
    public EscBuilder reset() {
        write(ESC, 0x40);
        return this;
    }

    /**
     * 设置对齐方式
     * 
     * @param align 0=左对齐, 1=居中, 2=右对齐
     */
    public EscBuilder align(int align) {
        write(ESC, 0x61, (byte) align);
        return this;
    }

    /**
     * 设置字体大小
     * 
     * @param width  宽度倍数 (1-8)
     * @param height 高度倍数 (1-8)
     */
    public EscBuilder fontSize(int width, int height) {
        int size = ((height - 1) << 4) | (width - 1);
        write(GS, 0x21, (byte) size);
        return this;
    }

    /**
     * 设置粗体
     * 
     * @param bold true=粗体, false=正常
     */
    public EscBuilder bold(boolean bold) {
        write(ESC, 0x45, bold ? (byte) 1 : (byte) 0);
        return this;
    }

    /**
     * 设置下划线
     * 
     * @param underline true=下划线, false=无下划线
     */
    public EscBuilder underline(boolean underline) {
        write(ESC, 0x2D, underline ? (byte) 1 : (byte) 0);
        return this;
    }

    /**
     * 打印文本
     * 
     * @param text 要打印的文本
     */
    public EscBuilder text(String text) {
        if (text != null) {
            try {
                // 使用GBK编码支持中文
                byte[] textBytes = text.getBytes("GBK");
                buffer.write(textBytes);
            } catch (Exception e) {
                // 如果GBK编码失败，使用UTF-8
                write(text.getBytes(StandardCharsets.UTF_8));
            }
        }
        return this;
    }

    /**
     * 换行
     * 
     * @param lines 换行数量，默认1行
     */
    public EscBuilder feed(int lines) {
        for (int i = 0; i < lines; i++) {
            write(LF);
        }
        return this;
    }

    /**
     * 换行（默认1行）
     */
    public EscBuilder feed() {
        return feed(1);
    }

    /**
     * 设置行间距
     * 
     * @param dots 点数 (0-255)
     */
    public EscBuilder lineSpacing(int dots) {
        write(ESC, 0x33, (byte) dots);
        return this;
    }

    /**
     * 打印条形码
     * 
     * @param type 条形码类型 (0-6)
     * @param data 条形码数据
     */
    public EscBuilder barcode(int type, String data) {
        // 设置条形码高度
        write(GS, 0x68, (byte) 162); // 高度162点

        // 设置条形码宽度
        write(GS, 0x77, (byte) 3); // 宽度3

        // 设置条形码字符位置
        write(GS, 0x48, (byte) 2); // 在条形码下方显示

        // 打印条形码
        write(GS, 0x6B, (byte) type);
        write(data.getBytes(StandardCharsets.UTF_8));
        write((byte) 0); // 结束符

        return this;
    }

    /**
     * 打印二维码
     * 
     * @param data 二维码数据
     */
    public EscBuilder qrCode(String data) {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // 设置二维码模块大小
        write(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x08);

        // 设置纠错等级
        write(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31);

        // 存储二维码数据
        int len = dataBytes.length + 3;
        write(GS, 0x28, 0x6B, (byte) (len & 0xFF), (byte) ((len >> 8) & 0xFF), 0x31, 0x50, 0x30);
        write(dataBytes);

        // 打印二维码
        write(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30);

        return this;
    }

    /**
     * 切纸
     */
    public EscBuilder cut() {
        write(GS, 0x56, 0x42, 0x00); // 全切
        return this;
    }

    /**
     * 半切纸
     */
    public EscBuilder halfCut() {
        write(GS, 0x56, 0x41, 0x00); // 半切
        return this;
    }

    /**
     * 开钱箱
     */
    public EscBuilder openDrawer() {
        write(ESC, 0x70, 0x00, 0x3C, 0xFF);
        return this;
    }

    /**
     * 打印分隔线
     * 
     * @param char   分隔字符
     * @param length 长度
     */
    public EscBuilder separator(char ch, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ch);
        }
        return text(sb.toString()).feed();
    }

    /**
     * 打印烟叶称重小票
     * 
     * @param farmerName     烟农姓名
     * @param contractNumber 合同号
     * @param leafType       叶片类型
     * @param weight         重量
     * @param operator       操作员
     */
    public EscBuilder tobaccoReceipt(String farmerName, String contractNumber,
            String leafType, double weight, String operator) {
        return reset()
                .align(1).bold(true).fontSize(2, 2)
                .text("烟叶称重小票").feed(2)
                .reset().align(0).fontSize(1, 1)
                .separator('=', 32)
                .text("时间: " + new java.util.Date().toString()).feed()
                .text("烟农: " + farmerName).feed()
                .text("合同号: " + contractNumber).feed()
                .text("叶片类型: " + leafType).feed()
                .text("重量: " + String.format("%.2f kg", weight)).feed()
                .text("操作员: " + operator).feed()
                .separator('=', 32)
                .align(1).text("谢谢使用").feed(3)
                .cut();
    }

    /**
     * 构建最终的字节数组
     */
    public byte[] build() {
        return buffer.toByteArray();
    }

    /**
     * 清空缓冲区
     */
    public EscBuilder clear() {
        buffer.reset();
        return this;
    }

    /**
     * 写入字节到缓冲区
     */
    private void write(int... bytes) {
        for (int b : bytes) {
            buffer.write(b);
        }
    }

    /**
     * 写入字节数组到缓冲区
     */
    private void write(byte[] data) {
        try {
            buffer.write(data);
        } catch (IOException e) {
            // ByteArrayOutputStream不会抛出IOException
        }
    }
}