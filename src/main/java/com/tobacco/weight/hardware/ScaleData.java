package com.tobacco.weight.hardware;

/**
 * 电子秤数据模型
 * 存储解析后的电子秤数据信息
 */
public class ScaleData {

    private double weight; // 重量值（kg）
    private boolean isStable; // 是否稳定
    private String status; // 状态（ST=稳定，US=不稳定）
    private String type; // 类型（NT=净重）
    private boolean isValid; // 数据是否有效
    private long timestamp; // 时间戳

    /**
     * 构造函数
     */
    public ScaleData() {
        this.timestamp = System.currentTimeMillis();
        this.isValid = false;
    }

    /**
     * 构造函数
     * 
     * @param weight   重量值
     * @param isStable 是否稳定
     * @param status   状态
     * @param type     类型
     */
    public ScaleData(double weight, boolean isStable, String status, String type) {
        this();
        this.weight = weight;
        this.isStable = isStable;
        this.status = status;
        this.type = type;
        this.isValid = true;
    }

    // Getter 和 Setter 方法
    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isStable() {
        return isStable;
    }

    public void setStable(boolean stable) {
        isStable = stable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format("ScaleData{weight=%.2f, stable=%s, status='%s', type='%s', valid=%s}",
                weight, isStable, status, type, isValid);
    }
}