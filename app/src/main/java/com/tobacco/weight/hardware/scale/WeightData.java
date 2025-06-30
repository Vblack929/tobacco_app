package com.tobacco.weight.hardware.scale;

/**
 * 重量数据模型
 * 封装电子秤返回的重量信息和状态
 */
public class WeightData {
    
    private double weight;           // 重量值（kg）
    private String unit = "kg";      // 重量单位
    private boolean isValid = false; // 数据是否有效
    private boolean isStable = false;// 重量是否稳定
    private boolean isOverload = false;   // 是否超载
    private boolean isUnderload = false;  // 是否欠载
    private boolean isError = false;      // 是否有错误
    private String rawData;          // 原始数据
    private long timestamp;          // 时间戳
    
    /**
     * 构造函数
     */
    public WeightData() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数
     * @param weight 重量值
     */
    public WeightData(double weight) {
        this();
        this.weight = weight;
        this.isValid = true;
    }
    
    /**
     * 构造函数
     * @param weight 重量值
     * @param unit 重量单位
     */
    public WeightData(double weight, String unit) {
        this(weight);
        this.unit = unit;
    }
    
    // Getter 和 Setter 方法
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public boolean isStable() {
        return isStable;
    }
    
    public void setStable(boolean stable) {
        isStable = stable;
    }
    
    public boolean isOverload() {
        return isOverload;
    }
    
    public void setOverload(boolean overload) {
        isOverload = overload;
    }
    
    public boolean isUnderload() {
        return isUnderload;
    }
    
    public void setUnderload(boolean underload) {
        isUnderload = underload;
    }
    
    public boolean isError() {
        return isError;
    }
    
    public void setError(boolean error) {
        isError = error;
    }
    
    public String getRawData() {
        return rawData;
    }
    
    public void setRawData(String rawData) {
        this.rawData = rawData;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 获取格式化的重量字符串
     * @return 格式化后的重量显示
     */
    public String getFormattedWeight() {
        if (!isValid) {
            return "---";
        }
        
        if (isError) {
            return "ERR";
        }
        
        if (isOverload) {
            return "OVER";
        }
        
        if (isUnderload) {
            return "UNDER";
        }
        
        return String.format("%.3f %s", weight, unit);
    }
    
    /**
     * 获取状态描述
     * @return 状态描述字符串
     */
    public String getStatusDescription() {
        if (!isValid) {
            return "无效数据";
        }
        
        if (isError) {
            return "设备错误";
        }
        
        if (isOverload) {
            return "超载";
        }
        
        if (isUnderload) {
            return "欠载";
        }
        
        if (isStable) {
            return "稳定";
        } else {
            return "不稳定";
        }
    }
    
    /**
     * 检查重量是否可用于记录
     * @return 是否可记录
     */
    public boolean isRecordable() {
        return isValid && !isError && !isOverload && !isUnderload && isStable && weight > 0;
    }
    
    /**
     * 复制重量数据
     * @return 新的重量数据对象
     */
    public WeightData copy() {
        WeightData copy = new WeightData();
        copy.weight = this.weight;
        copy.unit = this.unit;
        copy.isValid = this.isValid;
        copy.isStable = this.isStable;
        copy.isOverload = this.isOverload;
        copy.isUnderload = this.isUnderload;
        copy.isError = this.isError;
        copy.rawData = this.rawData;
        copy.timestamp = this.timestamp;
        return copy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        WeightData that = (WeightData) obj;
        return Double.compare(that.weight, weight) == 0 &&
                isValid == that.isValid &&
                isStable == that.isStable &&
                isOverload == that.isOverload &&
                isUnderload == that.isUnderload &&
                isError == that.isError;
    }
    
    @Override
    public int hashCode() {
        int result = Double.hashCode(weight);
        result = 31 * result + (isValid ? 1 : 0);
        result = 31 * result + (isStable ? 1 : 0);
        result = 31 * result + (isOverload ? 1 : 0);
        result = 31 * result + (isUnderload ? 1 : 0);
        result = 31 * result + (isError ? 1 : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "WeightData{" +
                "weight=" + weight +
                ", unit='" + unit + '\'' +
                ", isValid=" + isValid +
                ", isStable=" + isStable +
                ", isOverload=" + isOverload +
                ", isUnderload=" + isUnderload +
                ", isError=" + isError +
                ", rawData='" + rawData + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 