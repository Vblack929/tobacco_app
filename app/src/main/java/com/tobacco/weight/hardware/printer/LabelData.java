package com.tobacco.weight.hardware.printer;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签打印数据模型
 * 包含标签打印所需的所有信息和元素
 */
public class LabelData {
    
    private int x, y, width, height, rotation, copies;
    private List<LabelElement> elements;
    private int density; // 打印浓度 0-15
    private int speed;   // 打印速度 1-14
    
    public LabelData() {
        this.elements = new ArrayList<>();
        this.x = 0;
        this.y = 0;
        this.width = 400;  // 默认400点宽度
        this.height = 300; // 默认300点高度
        this.rotation = 0;
        this.copies = 1;
        this.density = 8;  // 默认中等浓度
        this.speed = 4;    // 默认中等速度
    }
    
    public LabelData(int width, int height) {
        this();
        this.width = width;
        this.height = height;
    }
    
    /**
     * 标签元素类
     */
    public static class LabelElement {
        
        public enum Type { 
            TEXT,       // 文本
            BARCODE,    // 条形码
            QR_CODE,    // 二维码
            IMAGE,      // 图片
            LINE,       // 线条
            RECTANGLE   // 矩形
        }
        
        private Type type;
        private int x, y, width, height, rotation;
        private String value;
        private boolean bold, underline;
        private int fontSize;
        
        // 条形码专用属性
        private int barcodeType;
        
        // 二维码专用属性
        private int qrSize, qrVersion, qrEcc;
        
        // 线条和矩形专用属性
        private int thickness;
        
        // 图片专用属性
        private String imagePath;
        
        public LabelElement(Type type) {
            this.type = type;
            this.rotation = 0;
            this.bold = false;
            this.underline = false;
            this.fontSize = 12;
            this.barcodeType = 73; // 默认Code128
            this.qrSize = 5;
            this.qrVersion = 3;
            this.qrEcc = 1; // 中等纠错
            this.thickness = 1;
        }
        
        // 创建文本元素
        public static LabelElement createText(int x, int y, String text) {
            LabelElement element = new LabelElement(Type.TEXT);
            element.setX(x);
            element.setY(y);
            element.setValue(text);
            return element;
        }
        
        // 创建粗体文本元素
        public static LabelElement createBoldText(int x, int y, String text) {
            LabelElement element = createText(x, y, text);
            element.setBold(true);
            return element;
        }
        
        // 创建条形码元素
        public static LabelElement createBarcode(int x, int y, String data) {
            LabelElement element = new LabelElement(Type.BARCODE);
            element.setX(x);
            element.setY(y);
            element.setValue(data);
            element.setWidth(2);
            element.setHeight(50);
            return element;
        }
        
        // 创建二维码元素
        public static LabelElement createQRCode(int x, int y, String data) {
            LabelElement element = new LabelElement(Type.QR_CODE);
            element.setX(x);
            element.setY(y);
            element.setValue(data);
            return element;
        }
        
        // 创建线条元素
        public static LabelElement createLine(int x1, int y1, int x2, int y2) {
            LabelElement element = new LabelElement(Type.LINE);
            element.setX(x1);
            element.setY(y1);
            element.setWidth(x2 - x1);
            element.setHeight(y2 - y1);
            return element;
        }
        
        // 创建矩形元素
        public static LabelElement createRectangle(int x, int y, int width, int height) {
            LabelElement element = new LabelElement(Type.RECTANGLE);
            element.setX(x);
            element.setY(y);
            element.setWidth(width);
            element.setHeight(height);
            return element;
        }
        
        // Getters and Setters
        public Type getType() { return type; }
        public void setType(Type type) { this.type = type; }
        
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public int getRotation() { return rotation; }
        public void setRotation(int rotation) { this.rotation = rotation; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }
        
        public boolean isUnderline() { return underline; }
        public void setUnderline(boolean underline) { this.underline = underline; }
        
        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
        
        public int getBarcodeType() { return barcodeType; }
        public void setBarcodeType(int barcodeType) { this.barcodeType = barcodeType; }
        
        public int getQrSize() { return qrSize; }
        public void setQrSize(int qrSize) { this.qrSize = qrSize; }
        
        public int getQrVersion() { return qrVersion; }
        public void setQrVersion(int qrVersion) { this.qrVersion = qrVersion; }
        
        public int getQrEcc() { return qrEcc; }
        public void setQrEcc(int qrEcc) { this.qrEcc = qrEcc; }
        
        public int getThickness() { return thickness; }
        public void setThickness(int thickness) { this.thickness = thickness; }
        
        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
        
        @Override
        public String toString() {
            return "LabelElement{" +
                    "type=" + type +
                    ", x=" + x +
                    ", y=" + y +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
    
    // Getters and Setters
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    
    public int getRotation() { return rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }
    
    public int getCopies() { return copies; }
    public void setCopies(int copies) { this.copies = copies; }
    
    public List<LabelElement> getElements() { return elements; }
    public void setElements(List<LabelElement> elements) { this.elements = elements != null ? elements : new ArrayList<>(); }
    
    public int getDensity() { return density; }
    public void setDensity(int density) { this.density = density; }
    
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
    
    // 便利方法
    
    /**
     * 添加文本元素
     */
    public LabelData addText(int x, int y, String text) {
        this.elements.add(LabelElement.createText(x, y, text));
        return this;
    }
    
    /**
     * 添加粗体文本元素
     */
    public LabelData addBoldText(int x, int y, String text) {
        this.elements.add(LabelElement.createBoldText(x, y, text));
        return this;
    }
    
    /**
     * 添加条形码元素
     */
    public LabelData addBarcode(int x, int y, String data) {
        this.elements.add(LabelElement.createBarcode(x, y, data));
        return this;
    }
    
    /**
     * 添加二维码元素
     */
    public LabelData addQRCode(int x, int y, String data) {
        this.elements.add(LabelElement.createQRCode(x, y, data));
        return this;
    }
    
    /**
     * 添加线条元素
     */
    public LabelData addLine(int x1, int y1, int x2, int y2) {
        this.elements.add(LabelElement.createLine(x1, y1, x2, y2));
        return this;
    }
    
    /**
     * 添加矩形元素
     */
    public LabelData addRectangle(int x, int y, int width, int height) {
        this.elements.add(LabelElement.createRectangle(x, y, width, height));
        return this;
    }
    
    /**
     * 添加水平分割线
     */
    public LabelData addHorizontalLine(int y) {
        return addLine(10, y, width - 10, y);
    }
    
    /**
     * 添加垂直分割线
     */
    public LabelData addVerticalLine(int x) {
        return addLine(x, 10, x, height - 10);
    }
    
    /**
     * 添加边框
     */
    public LabelData addBorder() {
        return addRectangle(0, 0, width, height);
    }
    
    /**
     * 添加边框（指定边距）
     */
    public LabelData addBorder(int margin) {
        return addRectangle(margin, margin, width - 2 * margin, height - 2 * margin);
    }
    
    /**
     * 验证数据完整性
     */
    public boolean isValid() {
        // 检查基本尺寸
        if (width <= 0 || height <= 0) {
            return false;
        }
        
        // 检查是否有元素
        if (elements == null || elements.isEmpty()) {
            return false;
        }
        
        // 检查每个元素是否有效
        for (LabelElement element : elements) {
            if (element.getType() == null) {
                return false;
            }
            
            // 检查文本和条形码是否有内容
            if ((element.getType() == LabelElement.Type.TEXT || 
                 element.getType() == LabelElement.Type.BARCODE || 
                 element.getType() == LabelElement.Type.QR_CODE) && 
                (element.getValue() == null || element.getValue().trim().isEmpty())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取标签尺寸（毫米）
     */
    public String getSizeMm() {
        // 假设8点/毫米的分辨率
        int widthMm = width / 8;
        int heightMm = height / 8;
        return widthMm + "x" + heightMm + "mm";
    }
    
    /**
     * 计算元素数量
     */
    public int getElementCount() {
        return elements != null ? elements.size() : 0;
    }
    
    /**
     * 获取指定类型的元素数量
     */
    public int getElementCount(LabelElement.Type type) {
        if (elements == null || type == null) {
            return 0;
        }
        
        int count = 0;
        for (LabelElement element : elements) {
            if (element.getType() == type) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 创建简单标签
     */
    public static LabelData createSimple(String title, String content) {
        LabelData label = new LabelData();
        label.addBoldText(10, 10, title);
        label.addText(10, 40, content);
        return label;
    }
    
    /**
     * 创建测试标签
     */
    public static LabelData createTest() {
        LabelData label = new LabelData(400, 300);
        
        // 添加边框
        label.addBorder(5);
        
        // 添加标题
        label.addBoldText(10, 15, "TEST LABEL");
        
        // 添加分割线
        label.addHorizontalLine(35);
        
        // 添加内容
        label.addText(10, 50, "Name: Test Product");
        label.addText(10, 70, "ID: 12345");
        label.addText(10, 90, "Date: 2024-01-01");
        
        // 添加条形码
        label.addBarcode(10, 120, "1234567890");
        
        // 添加二维码
        LabelElement qr = LabelElement.createQRCode(250, 120, "Test QR Data");
        qr.setQrSize(5);
        label.getElements().add(qr);
        
        return label;
    }
    
    /**
     * 创建烟叶标签
     */
    public static LabelData createTobaccoLabel(String farmerName, String idCard, String weight, String qrData) {
        LabelData label = new LabelData(560, 400); // 70x50mm
        
        // 标题
        label.addBoldText(20, 20, "烟叶收购凭证");
        
        // 分割线
        label.addHorizontalLine(45);
        
        // 农户信息
        label.addText(20, 60, "农户: " + (farmerName != null ? farmerName : ""));
        label.addText(20, 80, "身份证: " + (idCard != null ? hideIdCard(idCard) : ""));
        label.addText(20, 100, "重量: " + (weight != null ? weight : "") + " kg");
        
        // 添加条形码（身份证号）
        if (idCard != null && !idCard.isEmpty()) {
            label.addBarcode(20, 140, idCard);
        }
        
        // 添加二维码
        if (qrData != null && !qrData.isEmpty()) {
            LabelElement qr = LabelElement.createQRCode(350, 140, qrData);
            qr.setQrSize(6);
            label.getElements().add(qr);
        }
        
        // 时间戳
        label.addText(20, 220, "时间: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()));
        
        return label;
    }
    
    /**
     * 创建烟叶称重标签 - 专用于称重记录
     */
    public static LabelData createTobaccoWeighingLabel(String farmerName, String precheckId, 
                                                      String tobaccoLevel, String date, String contractNumber) {
        LabelData label = new LabelData(560, 400); // 70x50mm
        
        // 标题
        label.addBoldText(20, 20, "烟叶称重标签");
        
        // 分割线
        label.addHorizontalLine(45);
        
        // 称重信息 - 处理空值显示
        label.addText(20, 60, "农户姓名: " + (farmerName != null && !farmerName.isEmpty() ? farmerName : "未填写"));
        label.addText(20, 80, "预检编号: " + (precheckId != null && !precheckId.isEmpty() ? precheckId : "未生成"));
        label.addText(20, 100, "烟叶等级: " + (tobaccoLevel != null && !tobaccoLevel.isEmpty() ? tobaccoLevel : "未选择"));
        label.addText(20, 120, "称重日期: " + (date != null && !date.isEmpty() ? date : "未记录"));
        
        // 合同号
        String contractText = "合同编号: " + (contractNumber != null && !contractNumber.isEmpty() ? contractNumber : "未设置");
        label.addText(20, 140, contractText);
        
        // 添加预检号条形码（即使为空也显示占位符）
        String barcodeData = (precheckId != null && !precheckId.isEmpty() && !precheckId.equals("未生成")) ? precheckId : "NO-PRECHECK";
        label.addBarcode(20, 170, barcodeData);
        
        // 添加二维码（包含所有信息）
        String qrData = "农户:" + (farmerName != null ? farmerName : "未填写") + 
                       "|预检号:" + (precheckId != null ? precheckId : "未生成") + 
                       "|等级:" + (tobaccoLevel != null ? tobaccoLevel : "未选择") + 
                       "|合同:" + (contractNumber != null ? contractNumber : "未设置") +
                       "|日期:" + (date != null ? date : "未记录");
        
        LabelElement qr = LabelElement.createQRCode(350, 170, qrData);
        qr.setQrSize(5);
        label.getElements().add(qr);
        
        // 底部时间戳
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", java.util.Locale.CHINA);
        label.addText(20, 320, "打印时间: " + formatter.format(new java.util.Date()));
        
        return label;
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
    
    @Override
    public String toString() {
        return "LabelData{" +
                "size=" + width + "x" + height +
                ", rotation=" + rotation +
                ", copies=" + copies +
                ", elements=" + getElementCount() +
                ", density=" + density +
                ", speed=" + speed +
                '}';
    }
} 