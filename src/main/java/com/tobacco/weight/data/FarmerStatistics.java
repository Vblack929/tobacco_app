package com.tobacco.weight.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 烟农统计数据模型
 * 
 * 设计原则：
 * - 只包含可变的统计数据
 * - 通过FarmerInfo引用获取不可变的身份信息
 * - 职责单一：仅负责称重统计计算
 * - 与身份信息完全分离
 */
public class FarmerStatistics {

    // 农户身份信息引用（不可变）
    private final FarmerInfo farmerInfo;

    // 可变的统计数据
    private int totalBundles; // 预检捆数（总）
    private double totalWeight; // 预检重量（总）
    private Map<String, Double> leafTypeWeights; // 各部叶重量统计
    private Map<String, Integer> leafTypeCounts; // 各部叶捆数统计
    private List<WeighingRecord> records; // 所有称重记录

    /**
     * 主构造函数
     */
    public FarmerStatistics(FarmerInfo farmerInfo) {
        if (farmerInfo == null) {
            throw new IllegalArgumentException("FarmerInfo cannot be null");
        }

        this.farmerInfo = farmerInfo;
        this.leafTypeWeights = new HashMap<>();
        this.leafTypeCounts = new HashMap<>();
        this.records = new ArrayList<>();
        initializeLeafTypes();
    }

    /**
     * 工厂方法：创建带有完整身份信息的统计
     */
    public static FarmerStatistics createWithFullInfo(String farmerName, String contractNumber,
            String idCardNumber, String gender, String nationality,
            String birthDate, String address, String department,
            String startDate, String endDate, byte[] photo) {
        FarmerInfo farmerInfo = FarmerInfo.createWithIdCard(farmerName, contractNumber, idCardNumber,
                gender, nationality, birthDate, address,
                department, startDate, endDate, photo);
        return new FarmerStatistics(farmerInfo);
    }

    /**
     * 工厂方法：创建基本统计信息
     */
    public static FarmerStatistics createBasic(String farmerName, String contractNumber, String idCardNumber) {
        FarmerInfo farmerInfo = FarmerInfo.createBasic(farmerName, contractNumber, idCardNumber);
        return new FarmerStatistics(farmerInfo);
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
     * 添加称重记录（更新统计数据）
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
     * 重置统计数据
     */
    public void resetStatistics() {
        totalBundles = 0;
        totalWeight = 0.0;
        records.clear();
        initializeLeafTypes();
    }

    /**
     * 获取指定部叶的重量百分比
     */
    public double getLeafTypePercentage(String leafType) {
        if (totalWeight <= 0)
            return 0.0;
        Double weight = leafTypeWeights.get(leafType);
        return weight != null ? (weight / totalWeight) * 100.0 : 0.0;
    }

    /**
     * 获取指定部叶的重量
     */
    public double getLeafTypeWeight(String leafType) {
        Double weight = leafTypeWeights.get(leafType);
        return weight != null ? weight : 0.0;
    }

    /**
     * 获取指定部叶的捆数
     */
    public int getLeafTypeCount(String leafType) {
        Integer count = leafTypeCounts.get(leafType);
        return count != null ? count : 0;
    }

    /**
     * 验证记录是否有效
     */
    public boolean isRecordValid(WeighingRecord record) {
        return record != null &&
                record.getFarmerName() != null &&
                record.getFarmerName().equals(farmerInfo.getFarmerName());
    }

    // Getters for FarmerInfo
    public FarmerInfo getFarmerInfo() {
        return farmerInfo;
    }

    public String getFarmerName() {
        return farmerInfo.getFarmerName();
    }

    public String getContractNumber() {
        return farmerInfo.getContractNumber();
    }

    public String getIdCardNumber() {
        return farmerInfo.getIdCardNumber();
    }

    public String getGender() {
        return farmerInfo.getGender();
    }

    public String getAddress() {
        return farmerInfo.getAddress();
    }

    public String getNationality() {
        return farmerInfo.getNationality();
    }

    public String getBirthDate() {
        return farmerInfo.getBirthDate();
    }

    public String getDepartment() {
        return farmerInfo.getDepartment();
    }

    public String getStartDate() {
        return farmerInfo.getStartDate();
    }

    public String getEndDate() {
        return farmerInfo.getEndDate();
    }

    public byte[] getPhoto() {
        return farmerInfo.getPhoto();
    }

    public boolean matchesIdCard(String idCardNumber) {
        return farmerInfo.matchesIdCard(idCardNumber);
    }

    public boolean hasCompleteIdCardInfo() {
        return farmerInfo.hasCompleteIdCardInfo();
    }

    // Getters and Setters for statistics
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
        return new HashMap<>(leafTypeWeights);
    }

    public void setLeafTypeWeights(Map<String, Double> leafTypeWeights) {
        this.leafTypeWeights = new HashMap<>(leafTypeWeights);
    }

    public Map<String, Integer> getLeafTypeCounts() {
        return new HashMap<>(leafTypeCounts);
    }

    public void setLeafTypeCounts(Map<String, Integer> leafTypeCounts) {
        this.leafTypeCounts = new HashMap<>(leafTypeCounts);
    }

    public List<WeighingRecord> getRecords() {
        return new ArrayList<>(records);
    }

    public void setRecords(List<WeighingRecord> records) {
        this.records = new ArrayList<>(records);
    }

    @Override
    public String toString() {
        return "FarmerStatistics{" +
                "farmerName='" + getFarmerName() + '\'' +
                ", contractNumber='" + getContractNumber() + '\'' +
                ", totalBundles=" + totalBundles +
                ", totalWeight=" + totalWeight +
                ", leafTypeWeights=" + leafTypeWeights +
                '}';
    }
}