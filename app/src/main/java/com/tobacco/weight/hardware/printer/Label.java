package com.tobacco.weight.hardware.printer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 标签打印命令生成器
 * 用于生成标签打印机的控制命令
 */
public class Label {
    
    private static final String TAG = "Label";
    
    // 命令缓冲区
    private ByteArrayOutputStream commandBuffer;
    private List<byte[]> commandList;
    
    // 页面属性
    private int pageWidth = 400;
    private int pageHeight = 300;
    private int pageX = 0;
    private int pageY = 0;
    private int rotation = 0;
    
    public Label() {
        this.commandBuffer = new ByteArrayOutputStream();
        this.commandList = new ArrayList<>();
    }
    
    /**
     * 开始页面设置
     * @param x X坐标
     * @param y Y坐标  
     * @param width 页面宽度
     * @param height 页面高度
     * @param rotation 旋转角度 (0, 90, 180, 270)
     */
    public Label pageStart(int x, int y, int width, int height, int rotation) {
        this.pageX = x;
        this.pageY = y;
        this.pageWidth = width;
        this.pageHeight = height;
        this.rotation = rotation;
        
        // 设置页面大小和方向
        String command = String.format("SIZE %d mm,%d mm\n", 
            mmFromDots(width), mmFromDots(height));
        addTextCommand(command);
        
        // 设置间隙
        addTextCommand("GAP 2 mm,0 mm\n");
        
        // 清除缓冲区
        addTextCommand("CLS\n");
        
        return this;
    }
    
    /**
     * 结束页面
     */
    public Label pageEnd() {
        // 页面结束，准备打印
        return this;
    }
    
    /**
     * 打印文本
     * @param x X坐标
     * @param y Y坐标
     * @param height 字体高度
     * @param bold 是否粗体 (1=粗体, 0=正常)
     * @param underline 是否下划线 (1=是, 0=否)
     * @param text 文本内容
     * @param rotation 旋转角度 (0, 90, 180, 270)
     */
    public Label printText(int x, int y, int height, int bold, int underline, String text, int rotation) {
        if (text != null && !text.isEmpty()) {
            // 选择字体
            String fontName = "TSS24.BF2"; // 默认字体
            if (bold == 1) {
                fontName = "TSS24.BF2"; // 粗体字体
            }
            
            String command = String.format("TEXT %d,%d,\"%s\",%d,%d,%d,\"%s\"\n",
                x, y, fontName, rotation, 1, 1, text);
            addTextCommand(command);
        }
        return this;
    }
    
    /**
     * 打印条形码
     * @param x X坐标
     * @param y Y坐标
     * @param type 条形码类型
     * @param height 条形码高度
     * @param width 条形码宽度 (模块宽度)
     * @param data 条形码数据
     * @param rotation 旋转角度
     */
    public Label printBarCode(int x, int y, int type, int height, int width, String data, int rotation) {
        if (data != null && !data.isEmpty()) {
            String barcodeType = "128"; // 默认使用Code128
            
            // 根据type参数选择条形码类型
            switch (type) {
                case 73: // Code128
                    barcodeType = "128";
                    break;
                case 4: // Code39
                    barcodeType = "39";
                    break;
                case 8: // EAN13
                    barcodeType = "EAN13";
                    break;
                case 9: // EAN8
                    barcodeType = "EAN8";
                    break;
                default:
                    barcodeType = "128";
                    break;
            }
            
            String command = String.format("BARCODE %d,%d,\"%s\",%d,%d,%d,%d,%d,\"%s\"\n",
                x, y, barcodeType, height, 1, rotation, width, width, data);
            addTextCommand(command);
        }
        return this;
    }
    
    /**
     * 打印二维码
     * @param x X坐标
     * @param y Y坐标
     * @param size 二维码大小 (模块大小)
     * @param version 版本
     * @param ecc 纠错等级
     * @param data 二维码数据
     * @param rotation 旋转角度
     */
    public Label printQR(int x, int y, int size, int version, int ecc, String data, int rotation) {
        if (data != null && !data.isEmpty()) {
            // 纠错等级转换
            char eccLevel = 'M'; // 默认中等纠错
            switch (ecc) {
                case 0: eccLevel = 'L'; break; // 低等纠错
                case 1: eccLevel = 'M'; break; // 中等纠错
                case 2: eccLevel = 'Q'; break; // 高等纠错
                case 3: eccLevel = 'H'; break; // 最高纠错
                default: eccLevel = 'M'; break;
            }
            
            String command = String.format("QRCODE %d,%d,%c,%d,A,%d,\"%s\"\n",
                x, y, eccLevel, size, rotation, data);
            addTextCommand(command);
        }
        return this;
    }
    
    /**
     * 打印图片
     * @param x X坐标
     * @param y Y坐标
     * @param width 图片宽度
     * @param height 图片高度
     * @param imageName 图片名称
     */
    public Label printImage(int x, int y, int width, int height, String imageName) {
        if (imageName != null && !imageName.isEmpty()) {
            String command = String.format("PUTBMP %d,%d,\"%s\"\n", x, y, imageName);
            addTextCommand(command);
        }
        return this;
    }
    
    /**
     * 画线
     * @param x1 起始X坐标
     * @param y1 起始Y坐标
     * @param x2 结束X坐标
     * @param y2 结束Y坐标
     * @param width 线宽
     */
    public Label drawLine(int x1, int y1, int x2, int y2, int width) {
        String command = String.format("BAR %d,%d,%d,%d\n", x1, y1, x2 - x1, width);
        addTextCommand(command);
        return this;
    }
    
    /**
     * 画矩形
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param thickness 边框厚度
     */
    public Label drawRect(int x, int y, int width, int height, int thickness) {
        // 画四条边框线
        drawLine(x, y, x + width, y, thickness); // 上边
        drawLine(x, y + height - thickness, x + width, y + height, thickness); // 下边
        drawLine(x, y, x, y + height, thickness); // 左边
        drawLine(x + width - thickness, y, x + width, y + height, thickness); // 右边
        return this;
    }
    
    /**
     * 设置打印浓度
     * @param density 浓度 (0-15)
     */
    public Label setDensity(int density) {
        if (density >= 0 && density <= 15) {
            String command = String.format("DENSITY %d\n", density);
            addTextCommand(command);
        }
        return this;
    }
    
    /**
     * 设置打印速度
     * @param speed 速度 (1-14)
     */
    public Label setSpeed(int speed) {
        if (speed >= 1 && speed <= 14) {
            String command = String.format("SPEED %d\n", speed);
            addTextCommand(command);
        }
        return this;
    }
    
    /**
     * 设置原点
     * @param x X坐标偏移
     * @param y Y坐标偏移
     */
    public Label setReference(int x, int y) {
        String command = String.format("REFERENCE %d,%d\n", x, y);
        addTextCommand(command);
        return this;
    }
    
    /**
     * 偏移设置
     * @param offset 偏移量
     */
    public Label setOffset(int offset) {
        String command = String.format("OFFSET %d mm\n", offset);
        addTextCommand(command);
        return this;
    }
    
    /**
     * 自定义打印页面
     * @param copies 打印份数
     */
    public Label customPrintPage(int copies) {
        String command = String.format("PRINT %d,1\n", copies);
        addTextCommand(command);
        return this;
    }
    
    /**
     * 打印并走纸
     * @param copies 打印份数
     */
    public Label printAndFeed(int copies) {
        customPrintPage(copies);
        return this;
    }
    
    /**
     * 撕纸位置
     */
    public Label tearPosition() {
        addTextCommand("SET TEAR ON\n");
        return this;
    }
    
    /**
     * 设置撕纸偏移
     * @param offset 偏移量
     */
    public Label setTearOffset(int offset) {
        String command = String.format("SET TEAR OFF %d\n", offset);
        addTextCommand(command);
        return this;
    }
    
    /**
     * 回带
     */
    public Label backfeed() {
        addTextCommand("BACKFEED\n");
        return this;
    }
    
    /**
     * 表单进纸
     */
    public Label formfeed() {
        addTextCommand("FORMFEED\n");
        return this;
    }
    
    /**
     * 添加文本命令
     */
    private void addTextCommand(String command) {
        try {
            byte[] commandBytes = command.getBytes("GBK"); // 支持中文
            commandBuffer.write(commandBytes);
            
            // 同时添加到命令列表
            byte[] commandCopy = new byte[commandBytes.length];
            System.arraycopy(commandBytes, 0, commandCopy, 0, commandBytes.length);
            commandList.add(commandCopy);
            
        } catch (Exception e) {
            // 如果编码失败，使用默认编码
            byte[] commandBytes = command.getBytes();
            try {
                commandBuffer.write(commandBytes);
                commandList.add(commandBytes);
            } catch (IOException ex) {
                android.util.Log.e(TAG, "Failed to add text command", ex);
            }
        }
    }
    
    /**
     * 添加二进制命令
     */
    private void addBinaryCommand(byte[] command) {
        try {
            commandBuffer.write(command);
            
            // 同时添加到命令列表
            byte[] commandCopy = new byte[command.length];
            System.arraycopy(command, 0, commandCopy, 0, command.length);
            commandList.add(commandCopy);
            
        } catch (IOException e) {
            android.util.Log.e(TAG, "Failed to add binary command", e);
        }
    }
    
    /**
     * 点数转换为毫米
     */
    private int mmFromDots(int dots) {
        // 假设8点/毫米的分辨率
        return Math.max(1, dots / 8);
    }
    
    /**
     * 毫米转换为点数
     */
    private int dotsFromMm(int mm) {
        // 假设8点/毫米的分辨率
        return mm * 8;
    }
    
    /**
     * 获取所有命令数据
     */
    public byte[] getCommandData() {
        return commandBuffer.toByteArray();
    }
    
    /**
     * 获取命令列表 (用于分批发送)
     */
    public List<byte[]> getCommands() {
        return new ArrayList<>(commandList);
    }
    
    /**
     * 清空命令缓冲区
     */
    public void clear() {
        commandBuffer.reset();
        commandList.clear();
    }
    
    /**
     * 获取命令缓冲区大小
     */
    public int getCommandSize() {
        return commandBuffer.size();
    }
    
    /**
     * 获取页面信息
     */
    public int getPageWidth() { return pageWidth; }
    public int getPageHeight() { return pageHeight; }
    public int getPageX() { return pageX; }
    public int getPageY() { return pageY; }
    public int getRotation() { return rotation; }
} 