package com.tobacco.weight.hardware.printer;

/**
 * 打印模板配置类
 * 用于定义打印标签的布局和样式参数
 */
public class PrintTemplate {

    private String name; // 模板名称
    private int width; // 标签宽度(mm)
    private int height; // 标签高度(mm)
    private boolean showQRCode; // 是否显示二维码
    private boolean showLogo; // 是否显示Logo
    private String logoPath; // Logo图片路径
    private int fontSize; // 字体大小
    private String fontFamily; // 字体类型
    private int marginTop; // 上边距
    private int marginBottom; // 下边距
    private int marginLeft; // 左边距
    private int marginRight; // 右边距

    // 构造函数
    public PrintTemplate() {
        // 设置默认值
        this.name = "默认模板";
        this.width = 70;
        this.height = 70;
        this.showQRCode = true;
        this.showLogo = false;
        this.fontSize = 12;
        this.fontFamily = "宋体";
        this.marginTop = 5;
        this.marginBottom = 5;
        this.marginLeft = 5;
        this.marginRight = 5;
    }

    public PrintTemplate(String name, int width, int height) {
        this();
        this.name = name;
        this.width = width;
        this.height = height;
    }

    // Getter 和 Setter 方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isShowQRCode() {
        return showQRCode;
    }

    public void setShowQRCode(boolean showQRCode) {
        this.showQRCode = showQRCode;
    }

    public boolean isShowLogo() {
        return showLogo;
    }

    public void setShowLogo(boolean showLogo) {
        this.showLogo = showLogo;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getMarginTop() {
        return marginTop;
    }

    public void setMarginTop(int marginTop) {
        this.marginTop = marginTop;
    }

    public int getMarginBottom() {
        return marginBottom;
    }

    public void setMarginBottom(int marginBottom) {
        this.marginBottom = marginBottom;
    }

    public int getMarginLeft() {
        return marginLeft;
    }

    public void setMarginLeft(int marginLeft) {
        this.marginLeft = marginLeft;
    }

    public int getMarginRight() {
        return marginRight;
    }

    public void setMarginRight(int marginRight) {
        this.marginRight = marginRight;
    }

    // 工具方法
    /**
     * 获取标签宽度（像素）
     * 
     * @param dpi 分辨率
     * @return 像素宽度
     */
    public int getWidthInPixels(int dpi) {
        return (int) (width * dpi / 25.4); // 25.4mm = 1英寸
    }

    /**
     * 获取标签高度（像素）
     * 
     * @param dpi 分辨率
     * @return 像素高度
     */
    public int getHeightInPixels(int dpi) {
        return (int) (height * dpi / 25.4); // 25.4mm = 1英寸
    }

    /**
     * 验证模板参数是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
                && width > 0 && height > 0
                && fontSize > 0;
    }

    /**
     * 创建模板副本
     * 
     * @return 模板副本
     */
    public PrintTemplate copy() {
        PrintTemplate template = new PrintTemplate();
        template.setName(this.name);
        template.setWidth(this.width);
        template.setHeight(this.height);
        template.setShowQRCode(this.showQRCode);
        template.setShowLogo(this.showLogo);
        template.setLogoPath(this.logoPath);
        template.setFontSize(this.fontSize);
        template.setFontFamily(this.fontFamily);
        template.setMarginTop(this.marginTop);
        template.setMarginBottom(this.marginBottom);
        template.setMarginLeft(this.marginLeft);
        template.setMarginRight(this.marginRight);
        return template;
    }

    @Override
    public String toString() {
        return "PrintTemplate{" +
                "name='" + name + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", showQRCode=" + showQRCode +
                ", showLogo=" + showLogo +
                ", fontSize=" + fontSize +
                ", fontFamily='" + fontFamily + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        PrintTemplate template = (PrintTemplate) obj;
        return name != null ? name.equals(template.name) : template.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}