package com.tobacco.weight.hardware.printer.esc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * 标签打印指令生成器
 * 基于Android USB打印demo移植，用于生成Label打印指令
 */
public class LabelBuilder {

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /**
     * 设置标签大小
     * 
     * @param width  宽度(mm)
     * @param height 高度(mm)
     */
    public LabelBuilder size(int width, int height) {
        String cmd = String.format("SIZE %d mm,%d mm\r\n", width, height);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 设置标签间隙
     * 
     * @param gap    间隙(mm)
     * @param offset 偏移(mm)
     */
    public LabelBuilder gap(int gap, int offset) {
        String cmd = String.format("GAP %d mm,%d mm\r\n", gap, offset);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 清除缓冲区
     */
    public LabelBuilder clear() {
        write("CLS\r\n".getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 设置打印方向
     * 
     * @param direction 0=正常, 1=旋转180度
     */
    public LabelBuilder direction(int direction) {
        String cmd = String.format("DIRECTION %d\r\n", direction);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 设置打印浓度
     * 
     * @param density 浓度 (0-15)
     */
    public LabelBuilder density(int density) {
        String cmd = String.format("DENSITY %d\r\n", density);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 设置打印速度
     * 
     * @param speed 速度 (1-6)
     */
    public LabelBuilder speed(int speed) {
        String cmd = String.format("SPEED %d\r\n", speed);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 打印文本
     * 
     * @param x        X坐标
     * @param y        Y坐标
     * @param font     字体 ("1"-"8")
     * @param rotation 旋转角度 (0, 90, 180, 270)
     * @param xScale   X缩放比例 (1-10)
     * @param yScale   Y缩放比例 (1-10)
     * @param text     文本内容
     */
    public LabelBuilder text(int x, int y, String font, int rotation, int xScale, int yScale, String text) {
        String cmd = String.format("TEXT %d,%d,\"%s\",%d,%d,%d,\"%s\"\r\n",
                x, y, font, rotation, xScale, yScale, text);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 打印文本（简化版）
     * 
     * @param x    X坐标
     * @param y    Y坐标
     * @param text 文本内容
     */
    public LabelBuilder text(int x, int y, String text) {
        return text(x, y, "1", 0, 1, 1, text);
    }

    /**
     * 打印条形码
     * 
     * @param x        X坐标
     * @param y        Y坐标
     * @param type     条形码类型 ("128", "39", "93"等)
     * @param height   高度
     * @param rotation 旋转角度
     * @param narrow   窄条宽度
     * @param wide     宽条宽度
     * @param data     条形码数据
     */
    public LabelBuilder barcode(int x, int y, String type, int height, int rotation,
            int narrow, int wide, String data) {
        String cmd = String.format("BARCODE %d,%d,\"%s\",%d,%d,%d,%d,%d,\"%s\"\r\n",
                x, y, type, height, rotation, narrow, wide, 2, data);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 打印条形码（简化版）
     * 
     * @param x    X坐标
     * @param y    Y坐标
     * @param data 条形码数据
     */
    public LabelBuilder barcode(int x, int y, String data) {
        return barcode(x, y, "128", 40, 0, 2, 2, data);
    }

    /**
     * 打印二维码
     * 
     * @param x        X坐标
     * @param y        Y坐标
     * @param level    纠错等级 ("L", "M", "Q", "H")
     * @param cell     模块大小 (1-10)
     * @param mode     模式 ("A"=自动, "M"=手动)
     * @param rotation 旋转角度
     * @param data     二维码数据
     */
    public LabelBuilder qrcode(int x, int y, String level, int cell, String mode, int rotation, String data) {
        String cmd = String.format("QRCODE %d,%d,%s,%d,%s,%d,\"%s\"\r\n",
                x, y, level, cell, mode, rotation, data);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 打印二维码（简化版）
     * 
     * @param x    X坐标
     * @param y    Y坐标
     * @param data 二维码数据
     */
    public LabelBuilder qrcode(int x, int y, String data) {
        return qrcode(x, y, "M", 4, "A", 0, data);
    }

    /**
     * 画线
     * 
     * @param x1 起始X坐标
     * @param y1 起始Y坐标
     * @param x2 结束X坐标
     * @param y2 结束Y坐标
     */
    public LabelBuilder line(int x1, int y1, int x2, int y2) {
        String cmd = String.format("BAR %d,%d,%d,%d\r\n", x1, y1, x2 - x1, y2 - y1);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 画矩形框
     * 
     * @param x         X坐标
     * @param y         Y坐标
     * @param width     宽度
     * @param height    高度
     * @param thickness 线条粗细
     */
    public LabelBuilder box(int x, int y, int width, int height, int thickness) {
        String cmd = String.format("BOX %d,%d,%d,%d,%d\r\n", x, y, width, height, thickness);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 画矩形框（简化版）
     * 
     * @param x      X坐标
     * @param y      Y坐标
     * @param width  宽度
     * @param height 高度
     */
    public LabelBuilder box(int x, int y, int width, int height) {
        return box(x, y, width, height, 1);
    }

    /**
     * 打印标签
     * 
     * @param copies 打印份数
     * @param sets   打印组数
     */
    public LabelBuilder print(int copies, int sets) {
        String cmd = String.format("PRINT %d,%d\r\n", copies, sets);
        write(cmd.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    /**
     * 打印标签（默认1份）
     */
    public LabelBuilder print() {
        return print(1, 1);
    }

    /**
     * 创建烟叶称重标签
     * 
     * @param farmerName     烟农姓名
     * @param contractNumber 合同号
     * @param leafType       叶片类型
     * @param weight         重量
     * @param date           日期
     */
    public LabelBuilder tobaccoLabel(String farmerName, String contractNumber,
            String leafType, double weight, String date) {
        return size(80, 60)
                .gap(2, 0)
                .clear()
                .density(8)
                .speed(4)
                // 标题
                .text(10, 10, "3", 0, 2, 2, "烟叶称重标签")
                // 分隔线
                .line(5, 35, 75, 35)
                // 内容
                .text(5, 45, "烟农: " + farmerName)
                .text(5, 65, "合同: " + contractNumber)
                .text(5, 85, "类型: " + leafType)
                .text(5, 105, "重量: " + String.format("%.2f kg", weight))
                .text(5, 125, "日期: " + date)
                // 条形码
                .barcode(5, 145, contractNumber)
                // 边框
                .box(2, 2, 76, 185)
                .print();
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
    public LabelBuilder reset() {
        buffer.reset();
        return this;
    }

    /**
     * 写入字节数组到缓冲区
     */
    private void write(byte[] data) {
        try {
            buffer.write(data);
        } catch (Exception e) {
            // ByteArrayOutputStream不会抛出异常
        }
    }
}