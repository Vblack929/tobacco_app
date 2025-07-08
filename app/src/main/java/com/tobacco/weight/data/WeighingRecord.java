package com.tobacco.weight.data;

import java.util.Date;

/**
 * 称重记录数据模型
 * 用于存储每次称重的详细信息
 */
public class WeighingRecord {
    private String precheckId; // 预检标号
    private String farmerName; // 烟农姓名
    private String contractNumber; // 合同号
    private String leafType; // 部叶类型（上部叶/中部叶/下部叶）
    private double weight; // 称重重量
    private Date timestamp; // 称重时间
    private String operator; // 操作员

    public WeighingRecord() {
        this.timestamp = new Date();
    }

    public WeighingRecord(String precheckId, String farmerName, String contractNumber,
            String leafType, double weight) {
        this();
        this.precheckId = precheckId;
        this.farmerName = farmerName;
        this.contractNumber = contractNumber;
        this.leafType = leafType;
        this.weight = weight;
    }

    // Getters and Setters
    public String getPrecheckId() {
        return precheckId;
    }

    public void setPrecheckId(String precheckId) {
        this.precheckId = precheckId;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getLeafType() {
        return leafType;
    }

    public void setLeafType(String leafType) {
        this.leafType = leafType;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "WeighingRecord{" +
                "precheckId='" + precheckId + '\'' +
                ", farmerName='" + farmerName + '\'' +
                ", contractNumber='" + contractNumber + '\'' +
                ", leafType='" + leafType + '\'' +
                ", weight=" + weight +
                ", timestamp=" + timestamp +
                '}';
    }
}