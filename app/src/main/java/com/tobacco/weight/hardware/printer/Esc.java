package com.tobacco.weight.hardware.printer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * ESC/POS命令生成器
 * 用于生成热敏收据打印机的控制命令
 */
public class Esc {
    
    private static final String TAG = "Esc";
    
    // ESC/POS控制字符
    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte LF = 0x0A;
    private static final byte CR = 0x0D;
    private static final byte SPACE = 0x20;
    
    // 命令缓冲区
    private ByteArrayOutputStream commandBuffer;
    private List<byte[]> commandList;
    
    public Esc() {
        this.commandBuffer = new ByteArrayOutputStream();
        this.commandList = new ArrayList<>();
    }
    
    /**
     * 重置打印机
     */
    public Esc reset() {
        addCommand(new byte[]{ESC, 0x40});
        return this;
    }
    
    /**
     * 设置对齐方式
     * @param align 0=左对齐, 1=居中, 2=右对齐
     */
    public Esc align(int align) {
        addCommand(new byte[]{ESC, 0x61, (byte) align});
        return this;
    }
    
    /**
     * 设置字体类型
     * @param font 字体类型 (0-2)
     * @param type 字符类型
     * @param style 字符样式
     * @param bold 是否粗体 (1=粗体, 0=正常)
     * @param doubleWidth 是否双倍宽度 (1=是, 0=否)
     * @param doubleHeight 是否双倍高度 (1=是, 0=否)
     * @param underline 是否下划线 (1=是, 0=否)
     */
    public Esc textType(int font, int type, int style, int bold, int doubleWidth, int doubleHeight, int underline) {
        // 设置字体
        addCommand(new byte[]{ESC, 0x4D, (byte) font});
        
        // 设置字符属性
        int attributes = 0;
        if (bold == 1) attributes |= 0x08;
        if (doubleHeight == 1) attributes |= 0x10;
        if (doubleWidth == 1) attributes |= 0x20;
        if (underline == 1) attributes |= 0x80;
        
        addCommand(new byte[]{ESC, 0x21, (byte) attributes});
        
        return this;
    }
    
    /**
     * 打印文本
     * @param text 要打印的文本
     */
    public Esc printText(String text) {
        if (text != null) {
            try {
                byte[] textBytes = text.getBytes("GBK"); // 使用GBK编码支持中文
                addCommand(textBytes);
            } catch (Exception e) {
                // 如果GBK编码失败，使用默认编码
                addCommand(text.getBytes());
            }
        }
        return this;
    }
    
    /**
     * 换行
     * @param lines 换行数量
     */
    public Esc formfeedY(int lines) {
        for (int i = 0; i < lines; i++) {
            addCommand(new byte[]{LF});
        }
        return this;
    }
    
    /**
     * 切纸
     */
    public Esc cutPaper() {
        addCommand(new byte[]{GS, 0x56, 0x42, 0x00});
        return this;
    }
    
    /**
     * 打印条形码
     * @param type 条形码类型
     * @param data 条形码数据
     */
    public Esc printBarCode(int type, String data) {
        if (data != null && !data.isEmpty()) {
            // 设置条形码高度
            addCommand(new byte[]{GS, 0x68, 50}); // 高度50点
            
            // 设置条形码宽度
            addCommand(new byte[]{GS, 0x77, 2}); // 宽度2
            
            // 设置HRI字符打印位置
            addCommand(new byte[]{GS, 0x48, 2}); // 在条形码下方打印
            
            // 打印条形码
            try {
                byte[] dataBytes = data.getBytes("ASCII");
                ByteArrayOutputStream barcodeCmd = new ByteArrayOutputStream();
                barcodeCmd.write(GS);
                barcodeCmd.write(0x6B);
                barcodeCmd.write(type);
                barcodeCmd.write(dataBytes.length);
                barcodeCmd.write(dataBytes);
                addCommand(barcodeCmd.toByteArray());
            } catch (IOException e) {
                // 如果编码失败，打印原始数据
                addCommand(data.getBytes());
            }
        }
        return this;
    }
    
    /**
     * 创建二维码
     * @param data 二维码数据
     */
    public Esc createQR(String data) {
        if (data != null && !data.isEmpty()) {
            try {
                byte[] dataBytes = data.getBytes("UTF-8");
                
                // 选择二维码模型
                addCommand(new byte[]{GS, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00});
                
                // 设置二维码模块大小
                addCommand(new byte[]{GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x05});
                
                // 设置纠错等级
                addCommand(new byte[]{GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31});
                
                // 存储二维码数据
                ByteArrayOutputStream qrCmd = new ByteArrayOutputStream();
                qrCmd.write(GS);
                qrCmd.write(0x28);
                qrCmd.write(0x6B);
                qrCmd.write((dataBytes.length + 3) & 0xFF);
                qrCmd.write(((dataBytes.length + 3) >> 8) & 0xFF);
                qrCmd.write(0x31);
                qrCmd.write(0x50);
                qrCmd.write(0x30);
                qrCmd.write(dataBytes);
                addCommand(qrCmd.toByteArray());
                
            } catch (IOException e) {
                // 如果失败，打印文本替代
                printText("[QR: " + data + "]");
            }
        }
        return this;
    }
    
    /**
     * 设置二维码大小
     * @param size 大小 (1-16)
     */
    public Esc QRSize(int size) {
        if (size >= 1 && size <= 16) {
            addCommand(new byte[]{GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, (byte) size});
        }
        return this;
    }
    
    /**
     * 打印二维码
     */
    public Esc printQR() {
        addCommand(new byte[]{GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30});
        return this;
    }
    
    /**
     * 设置字符间距
     * @param spacing 间距
     */
    public Esc setCharSpacing(int spacing) {
        addCommand(new byte[]{ESC, 0x20, (byte) spacing});
        return this;
    }
    
    /**
     * 设置行间距
     * @param spacing 间距
     */
    public Esc setLineSpacing(int spacing) {
        addCommand(new byte[]{ESC, 0x33, (byte) spacing});
        return this;
    }
    
    /**
     * 打印图片 (简化版本)
     * @param width 宽度
     * @param height 高度
     * @param data 图片数据
     */
    public Esc printImage(int width, int height, byte[] data) {
        if (data != null && data.length > 0) {
            // 使用位图模式打印
            addCommand(new byte[]{ESC, 0x2A, 0x21, (byte) (width & 0xFF), (byte) ((width >> 8) & 0xFF)});
            addCommand(data);
            formfeedY(1);
        }
        return this;
    }
    
    /**
     * 蜂鸣器
     * @param times 蜂鸣次数
     * @param duration 持续时间
     */
    public Esc beep(int times, int duration) {
        addCommand(new byte[]{ESC, 0x42, (byte) times, (byte) duration});
        return this;
    }
    
    /**
     * 开钱箱
     * @param pin 引脚 (0或1)
     * @param onTime 开启时间
     * @param offTime 关闭时间
     */
    public Esc openCashDrawer(int pin, int onTime, int offTime) {
        addCommand(new byte[]{ESC, 0x70, (byte) pin, (byte) onTime, (byte) offTime});
        return this;
    }
    
    /**
     * 设置字符大小
     * @param width 宽度倍数 (1-8)
     * @param height 高度倍数 (1-8)
     */
    public Esc setCharSize(int width, int height) {
        int size = ((width - 1) << 4) | (height - 1);
        addCommand(new byte[]{GS, 0x21, (byte) size});
        return this;
    }
    
    /**
     * 设置反显模式
     * @param enable 是否启用反显
     */
    public Esc setInvertMode(boolean enable) {
        addCommand(new byte[]{GS, 0x42, (byte) (enable ? 1 : 0)});
        return this;
    }
    
    /**
     * 添加命令到缓冲区
     */
    private void addCommand(byte[] command) {
        try {
            commandBuffer.write(command);
            
            // 同时添加到命令列表以便分批发送
            byte[] commandCopy = new byte[command.length];
            System.arraycopy(command, 0, commandCopy, 0, command.length);
            commandList.add(commandCopy);
            
        } catch (IOException e) {
            // 记录错误但继续执行
            android.util.Log.e(TAG, "Failed to add command", e);
        }
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
} 