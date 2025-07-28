package com.tobacco.weight.hardware.printer;

import java.util.ArrayList;
import java.util.List;

/**
 * 收据打印数据模型
 * 包含收据打印所需的所有信息
 */
public class ReceiptData {
    
    private String header;
    private List<ReceiptItem> items;
    private String barcode;
    private String qrCode;
    private String footer;
    private boolean enableCut;
    private int feedLines;
    
    public ReceiptData() {
        this.items = new ArrayList<>();
        this.enableCut = true;
        this.feedLines = 3;
    }
    
    public ReceiptData(String header) {
        this();
        this.header = header;
    }
    
    /**
     * 收据项目类
     */
    public static class ReceiptItem {
        private String text;
        private int alignment; // 0=左对齐, 1=居中, 2=右对齐
        private boolean bold;
        private boolean doubleSize;
        private boolean underline;
        private int fontSize; // 字体大小倍数
        
        public ReceiptItem(String text) {
            this(text, 0); // 默认左对齐
        }
        
        public ReceiptItem(String text, int alignment) {
            this.text = text;
            this.alignment = alignment;
            this.bold = false;
            this.doubleSize = false;
            this.underline = false;
            this.fontSize = 1;
        }
        
        public ReceiptItem(String text, int alignment, boolean bold) {
            this(text, alignment);
            this.bold = bold;
        }
        
        public ReceiptItem(String text, int alignment, boolean bold, boolean doubleSize) {
            this(text, alignment, bold);
            this.doubleSize = doubleSize;
        }
        
        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public int getAlignment() { return alignment; }
        public void setAlignment(int alignment) { this.alignment = alignment; }
        
        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }
        
        public boolean isDoubleSize() { return doubleSize; }
        public void setDoubleSize(boolean doubleSize) { this.doubleSize = doubleSize; }
        
        public boolean isUnderline() { return underline; }
        public void setUnderline(boolean underline) { this.underline = underline; }
        
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        
        @Override
        public String toString() {
            return "ReceiptItem{" +
                    "text='" + text + '\'' +
                    ", alignment=" + alignment +
                    ", bold=" + bold +
                    ", doubleSize=" + doubleSize +
                    '}';
        }
    }
    
    // Getters and Setters
    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }
    
    public List<ReceiptItem> getItems() { return items; }
    public void setItems(List<ReceiptItem> items) { this.items = items != null ? items : new ArrayList<>(); }
    
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    
    public String getFooter() { return footer; }
    public void setFooter(String footer) { this.footer = footer; }
    
    public boolean isEnableCut() { return enableCut; }
    public void setEnableCut(boolean enableCut) { this.enableCut = enableCut; }
    
    public int getFeedLines() { return feedLines; }
    public void setFeedLines(int feedLines) { this.feedLines = feedLines; }
    
    // 便利方法
    
    /**
     * 添加文本项目
     */
    public ReceiptData addItem(String text) {
        return addItem(text, 0);
    }
    
    /**
     * 添加文本项目（指定对齐方式）
     */
    public ReceiptData addItem(String text, int alignment) {
        this.items.add(new ReceiptItem(text, alignment));
        return this;
    }
    
    /**
     * 添加粗体文本项目
     */
    public ReceiptData addBoldItem(String text) {
        return addBoldItem(text, 0);
    }
    
    /**
     * 添加粗体文本项目（指定对齐方式）
     */
    public ReceiptData addBoldItem(String text, int alignment) {
        this.items.add(new ReceiptItem(text, alignment, true));
        return this;
    }
    
    /**
     * 添加居中文本项目
     */
    public ReceiptData addCenterItem(String text) {
        return addItem(text, 1);
    }
    
    /**
     * 添加分割线
     */
    public ReceiptData addSeparator() {
        return addCenterItem("--------------------------------");
    }
    
    /**
     * 添加分割线（指定长度）
     */
    public ReceiptData addSeparator(int length) {
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < length; i++) {
            separator.append("-");
        }
        return addCenterItem(separator.toString());
    }
    
    /**
     * 添加空行
     */
    public ReceiptData addEmptyLine() {
        return addItem("");
    }
    
    /**
     * 添加空行（指定数量）
     */
    public ReceiptData addEmptyLines(int count) {
        for (int i = 0; i < count; i++) {
            addEmptyLine();
        }
        return this;
    }
    
    /**
     * 添加键值对项目
     */
    public ReceiptData addKeyValue(String key, String value) {
        return addKeyValue(key, value, 32); // 默认32字符宽度
    }
    
    /**
     * 添加键值对项目（指定总宽度）
     */
    public ReceiptData addKeyValue(String key, String value, int totalWidth) {
        if (key == null) key = "";
        if (value == null) value = "";
        
        int keyLength = key.length();
        int valueLength = value.length();
        int spacesNeeded = totalWidth - keyLength - valueLength;
        
        StringBuilder line = new StringBuilder();
        line.append(key);
        
        // 添加空格
        for (int i = 0; i < Math.max(1, spacesNeeded); i++) {
            line.append(" ");
        }
        
        line.append(value);
        
        return addItem(line.toString());
    }
    
    /**
     * 验证数据完整性
     */
    public boolean isValid() {
        // 至少需要有一个项目或者header
        return (header != null && !header.trim().isEmpty()) || 
               (items != null && !items.isEmpty());
    }
    
    /**
     * 获取收据总行数估计
     */
    public int getEstimatedLines() {
        int lines = 0;
        
        if (header != null && !header.trim().isEmpty()) {
            lines += 3; // 标题通常占用3行（包括前后空行）
        }
        
        if (items != null) {
            lines += items.size();
        }
        
        if (barcode != null && !barcode.trim().isEmpty()) {
            lines += 5; // 条形码占用约5行
        }
        
        if (qrCode != null && !qrCode.trim().isEmpty()) {
            lines += 8; // 二维码占用约8行
        }
        
        if (footer != null && !footer.trim().isEmpty()) {
            lines += 2;
        }
        
        lines += feedLines; // 最后的走纸行数
        
        return lines;
    }
    
    /**
     * 创建简单收据
     */
    public static ReceiptData createSimple(String title, String content) {
        ReceiptData receipt = new ReceiptData(title);
        receipt.addSeparator();
        receipt.addItem(content);
        receipt.addSeparator();
        return receipt;
    }
    
    /**
     * 创建测试收据
     */
    public static ReceiptData createTest() {
        ReceiptData receipt = new ReceiptData("TEST RECEIPT");
        receipt.addSeparator();
        receipt.addKeyValue("Date", "2024-01-01 12:00:00");
        receipt.addKeyValue("Operator", "System");
        receipt.addSeparator();
        receipt.addItem("Item 1", 0);
        receipt.addKeyValue("Quantity", "1");
        receipt.addKeyValue("Price", "$10.00");
        receipt.addSeparator();
        receipt.addBoldItem("TOTAL: $10.00", 2);
        receipt.addSeparator();
        receipt.addCenterItem("Thank you!");
        receipt.setBarcode("1234567890");
        receipt.setQrCode("Test QR Code Content");
        return receipt;
    }
    
    @Override
    public String toString() {
        return "ReceiptData{" +
                "header='" + header + '\'' +
                ", itemCount=" + (items != null ? items.size() : 0) +
                ", hasBarcode=" + (barcode != null && !barcode.isEmpty()) +
                ", hasQrCode=" + (qrCode != null && !qrCode.isEmpty()) +
                ", enableCut=" + enableCut +
                '}';
    }
} 