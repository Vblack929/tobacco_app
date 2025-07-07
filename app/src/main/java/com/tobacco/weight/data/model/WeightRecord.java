package com.tobacco.weight.data.model;

import java.util.Date;

/**
 * 称重记录数据模型
 * 用于存储烟叶收购称重相关的所有信息
 */
public class WeightRecord {

    private String recordNumber; // 记录编号
    private String farmerName; // 农户姓名
    private String idCardNumber; // 身份证号
    private String tobaccoPart; // 烟叶部位
    private int tobaccoBundles; // 捆数
    private double weight; // 重量(kg)
    private double totalAmount; // 总金额
    private Date createTime; // 创建时间
    private long timestamp; // 时间戳
    private String qrCode; // 二维码内容
    private String operatorName; // 操作员姓名
    private String warehouseNumber; // 仓库编号
    private String preCheckNumber; // 预检编号
    private String status; // 状态

    // 构造函数
    public WeightRecord() {
        this.createTime = new Date();
        this.timestamp = System.currentTimeMillis();
    }

    public WeightRecord(String recordNumber, String farmerName, String idCardNumber,
            String tobaccoPart, int tobaccoBundles, double weight,
            double totalAmount, String operatorName, String warehouseNumber) {
        this();
        this.recordNumber = recordNumber;
        this.farmerName = farmerName;
        this.idCardNumber = idCardNumber;
        this.tobaccoPart = tobaccoPart;
        this.tobaccoBundles = tobaccoBundles;
        this.weight = weight;
        this.totalAmount = totalAmount;
        this.operatorName = operatorName;
        this.warehouseNumber = warehouseNumber;
    }

    // Getter 和 Setter 方法
    public String getRecordNumber() {
        return recordNumber;
    }

    public void setRecordNumber(String recordNumber) {
        this.recordNumber = recordNumber;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getTobaccoPart() {
        return tobaccoPart;
    }

    public void setTobaccoPart(String tobaccoPart) {
        this.tobaccoPart = tobaccoPart;
    }

    public int getTobaccoBundles() {
        return tobaccoBundles;
    }

    public void setTobaccoBundles(int tobaccoBundles) {
        this.tobaccoBundles = tobaccoBundles;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
        if (createTime != null) {
            this.timestamp = createTime.getTime();
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.createTime = new Date(timestamp);
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getWarehouseNumber() {
        return warehouseNumber;
    }

    public void setWarehouseNumber(String warehouseNumber) {
        this.warehouseNumber = warehouseNumber;
    }

    public String getPreCheckNumber() {
        return preCheckNumber;
    }

    public void setPreCheckNumber(String preCheckNumber) {
        this.preCheckNumber = preCheckNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // 工具方法
    @Override
    public String toString() {
        return "WeightRecord{" +
                "recordNumber='" + recordNumber + '\'' +
                ", farmerName='" + farmerName + '\'' +
                ", tobaccoPart='" + tobaccoPart + '\'' +
                ", weight=" + weight +
                ", totalAmount=" + totalAmount +
                ", createTime=" + createTime +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        WeightRecord that = (WeightRecord) obj;
        return recordNumber != null ? recordNumber.equals(that.recordNumber) : that.recordNumber == null;
    }

    @Override
    public int hashCode() {
        return recordNumber != null ? recordNumber.hashCode() : 0;
    }
}