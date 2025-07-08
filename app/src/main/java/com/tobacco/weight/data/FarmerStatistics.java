package com.tobacco.weight.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烟农统计数据模型
 * 用于存储每个烟农的汇总信息
 */
public class FarmerStatistics {
    private String farmerName; // 烟农姓名
    private String contractNumber; // 合同号
    private int totalBundles; // 预检捆数（总）
    private double totalWeight; // 预检重量（总）
    private Map<String, Double> leafTypeWeights; // 各部叶重量统计
    private Map<String, Integer> leafTypeCounts; // 各部叶捆数统计
    private List<WeighingRecord> records; // 所有称重记录

    public FarmerStatistics() {
        this.leafTypeWeights = new HashMap<>();
        this.leafTypeCounts = new HashMap<>();
        this.records = new ArrayList<>();
        initializeLeafTypes();
    }

    public FarmerStatistics(String farmerName, String contractNumber) {
        this();
        this.farmerName = farmerName;
        this.contractNumber = contractNumber;
    }

    private void initializeLeafTypes() {
        leafTypeWeights.put("上部叶", 0.0);
        leafTypeWeights.put("中部叶", 0.0);
        leafTypeWeights.put("下部叶", 0.0);

        leafTypeCounts.put("上部叶", 0);
        leafTypeCounts.put("中部叶", 0);
        leafTypeCounts.put("下部叶", 0);
    }

    /**
     * 添加称重记录
     */
    public void addWeighingRecord(WeighingRecord record) {
        records.add(record);

        String leafType = record.getLeafType();
        double weight = record.getWeight();

        // 更新总重量
        totalWeight += weight;

        // 更新总捆数
        totalBundles++;

        // 更新部叶重量统计
        if (leafTypeWeights.containsKey(leafType)) {
            leafTypeWeights.put(leafType, leafTypeWeights.get(leafType) + weight);
        }

        // 更新部叶捆数统计
        if (leafTypeCounts.containsKey(leafType)) {
            leafTypeCounts.put(leafType, leafTypeCounts.get(leafType) + 1);
        }
    }

    /**
     * 获取部叶重量比例
     */
    public double getLeafTypePercentage(String leafType) {
        if (totalWeight == 0)
            return 0.0;
        Double weight = leafTypeWeights.get(leafType);
        return weight != null ? (weight / totalWeight) * 100 : 0.0;
    }

    /**
     * 获取部叶重量
     */
    public double getLeafTypeWeight(String leafType) {
        Double weight = leafTypeWeights.get(leafType);
        return weight != null ? weight : 0.0;
    }

    /**
     * 获取部叶捆数
     */
    public int getLeafTypeCount(String leafType) {
        Integer count = leafTypeCounts.get(leafType);
        return count != null ? count : 0;
    }

    // Getters and Setters
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

    public int getTotalBundles() {
        return totalBundles;
    }

    public void setTotalBundles(int totalBundles) {
        this.totalBundles = totalBundles;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(double totalWeight) {
        this.totalWeight = totalWeight;
    }

    public Map<String, Double> getLeafTypeWeights() {
        return leafTypeWeights;
    }

    public void setLeafTypeWeights(Map<String, Double> leafTypeWeights) {
        this.leafTypeWeights = leafTypeWeights;
    }

    public Map<String, Integer> getLeafTypeCounts() {
        return leafTypeCounts;
    }

    public void setLeafTypeCounts(Map<String, Integer> leafTypeCounts) {
        this.leafTypeCounts = leafTypeCounts;
    }

    public List<WeighingRecord> getRecords() {
        return records;
    }

    public void setRecords(List<WeighingRecord> records) {
        this.records = records;
    }

    @Override
    public String toString() {
        return "FarmerStatistics{" +
                "farmerName='" + farmerName + '\'' +
                ", contractNumber='" + contractNumber + '\'' +
                ", totalBundles=" + totalBundles +
                ", totalWeight=" + totalWeight +
                ", records=" + records.size() +
                '}';
    }
}