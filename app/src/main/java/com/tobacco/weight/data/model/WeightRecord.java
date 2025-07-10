package com.tobacco.weight.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * 称重记录数据模型
 * 用于存储烟叶收购称重相关的所有信息
 * 包含完整的身份证信息，便于保存和显示
 */
@Entity(tableName = "weight_records")
public class WeightRecord {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "record_number")
    private String recordNumber; // 记录编号

    @ColumnInfo(name = "farmer_name")
    private String farmerName; // 农户姓名

    @ColumnInfo(name = "id_card_number")
    private String idCardNumber; // 身份证号

    @ColumnInfo(name = "farmer_address")
    private String farmerAddress; // 农户地址（新增）

    @ColumnInfo(name = "farmer_gender")
    private String farmerGender; // 农户性别（新增）

    // === 替换单一烟叶部位字段为详细的各部位数据 ===
    
    // 上部叶数据
    @ColumnInfo(name = "upper_leaf_bundles")
    private int upperLeafBundles; // 上部叶捆数
    
    @ColumnInfo(name = "upper_leaf_weight")
    private double upperLeafWeight; // 上部叶重量(kg)
    
    // 中部叶数据
    @ColumnInfo(name = "middle_leaf_bundles")
    private int middleLeafBundles; // 中部叶捆数
    
    @ColumnInfo(name = "middle_leaf_weight")
    private double middleLeafWeight; // 中部叶重量(kg)
    
    // 下部叶数据
    @ColumnInfo(name = "lower_leaf_bundles")
    private int lowerLeafBundles; // 下部叶捆数
    
    @ColumnInfo(name = "lower_leaf_weight")
    private double lowerLeafWeight; // 下部叶重量(kg)
    
    // 总计数据（计算字段）
    @ColumnInfo(name = "total_bundles")
    private int totalBundles; // 总捆数
    
    @ColumnInfo(name = "total_weight")
    private double totalWeight; // 总重量(kg)
    
    // 主要烟叶部位（用于向后兼容和简单显示）
    @ColumnInfo(name = "primary_tobacco_part")
    private String primaryTobaccoPart; // 主要烟叶部位（捆数最多的部位）

    @ColumnInfo(name = "weight")
    private double weight; // 重量(kg) - 保留用于兼容，等同于totalWeight

    @ColumnInfo(name = "total_amount")
    private double totalAmount; // 总金额

    @ColumnInfo(name = "create_time")
    private Date createTime; // 创建时间

    @ColumnInfo(name = "timestamp")
    private long timestamp; // 时间戳

    @ColumnInfo(name = "qr_code")
    private String qrCode; // 二维码内容

    @ColumnInfo(name = "operator_name")
    private String operatorName; // 操作员姓名

    @ColumnInfo(name = "warehouse_number")
    private String warehouseNumber; // 仓库编号

    @ColumnInfo(name = "pre_check_number")
    private String preCheckNumber; // 预检编号

    @ColumnInfo(name = "status")
    private String status; // 状态

    // 新增字段以匹配数据库schema
    @ColumnInfo(name = "tobacco_grade")
    private String tobaccoGrade; // 烟叶等级

    @ColumnInfo(name = "purchase_price")
    private double purchasePrice; // 收购价格

    @ColumnInfo(name = "moisture_content")
    private double moistureContent; // 水分含量

    @ColumnInfo(name = "impurity_rate")
    private double impurityRate; // 杂质率

    @ColumnInfo(name = "remark")
    private String remark; // 备注

    @ColumnInfo(name = "update_time")
    private Date updateTime; // 更新时间

    @ColumnInfo(name = "is_printed")
    private boolean isPrinted; // 是否已打印

    @ColumnInfo(name = "print_count")
    private int printCount; // 打印次数

    @ColumnInfo(name = "is_exported")
    private boolean isExported; // 是否已导出

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
        this.primaryTobaccoPart = tobaccoPart; // Map old tobacco_part to primaryTobaccoPart
        this.totalBundles = tobaccoBundles; // Map old tobacco_bundles to totalBundles
        this.totalWeight = weight;
        this.weight = weight; // Keep both for compatibility
        this.totalAmount = totalAmount;
        this.operatorName = operatorName;
        this.warehouseNumber = warehouseNumber;
        
        // Initialize individual leaf data to zero (will be set properly later)
        this.upperLeafBundles = 0;
        this.upperLeafWeight = 0.0;
        this.middleLeafBundles = 0;
        this.middleLeafWeight = 0.0;
        this.lowerLeafBundles = 0;
        this.lowerLeafWeight = 0.0;
    }

    // 完整构造函数（包含ID card信息）
    public WeightRecord(String recordNumber, String farmerName, String idCardNumber,
            String farmerAddress, String farmerGender, String tobaccoPart,
            int tobaccoBundles, double weight, double totalAmount,
            String operatorName, String warehouseNumber) {
        this(recordNumber, farmerName, idCardNumber, tobaccoPart, tobaccoBundles,
                weight, totalAmount, operatorName, warehouseNumber);
        this.farmerAddress = farmerAddress;
        this.farmerGender = farmerGender;
    }
    
    // 新增：烟叶详细数据构造函数
    public WeightRecord(String recordNumber, String farmerName, String idCardNumber,
            String farmerAddress, String farmerGender,
            int upperBundles, double upperWeight,
            int middleBundles, double middleWeight, 
            int lowerBundles, double lowerWeight,
            String operatorName, String warehouseNumber) {
        this();
        this.recordNumber = recordNumber;
        this.farmerName = farmerName;
        this.idCardNumber = idCardNumber;
        this.farmerAddress = farmerAddress;
        this.farmerGender = farmerGender;
        this.operatorName = operatorName;
        this.warehouseNumber = warehouseNumber;
        
        // 设置各部位数据
        this.upperLeafBundles = upperBundles;
        this.upperLeafWeight = upperWeight;
        this.middleLeafBundles = middleBundles;
        this.middleLeafWeight = middleWeight;
        this.lowerLeafBundles = lowerBundles;
        this.lowerLeafWeight = lowerWeight;
        
        // 自动计算总计
        updateTotalsAndPrimaryPart();
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

    public String getFarmerAddress() {
        return farmerAddress;
    }

    public void setFarmerAddress(String farmerAddress) {
        this.farmerAddress = farmerAddress;
    }

    public String getFarmerGender() {
        return farmerGender;
    }

    public void setFarmerGender(String farmerGender) {
        this.farmerGender = farmerGender;
    }

    public String getTobaccoPart() {
        return primaryTobaccoPart; // Keep for compatibility
    }

    public void setTobaccoPart(String tobaccoPart) {
        this.primaryTobaccoPart = tobaccoPart;
    }

    public int getTobaccoBundles() {
        return totalBundles; // Keep for compatibility
    }

    public void setTobaccoBundles(int tobaccoBundles) {
        this.totalBundles = tobaccoBundles;
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

    // ID字段的getter和setter
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // 新增字段的getter和setter方法
    public String getTobaccoGrade() {
        return tobaccoGrade;
    }

    public void setTobaccoGrade(String tobaccoGrade) {
        this.tobaccoGrade = tobaccoGrade;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getMoistureContent() {
        return moistureContent;
    }

    public void setMoistureContent(double moistureContent) {
        this.moistureContent = moistureContent;
    }

    public double getImpurityRate() {
        return impurityRate;
    }

    public void setImpurityRate(double impurityRate) {
        this.impurityRate = impurityRate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isPrinted() {
        return isPrinted;
    }

    public void setPrinted(boolean printed) {
        isPrinted = printed;
    }

    public int getPrintCount() {
        return printCount;
    }

    public void setPrintCount(int printCount) {
        this.printCount = printCount;
    }

    public boolean isExported() {
        return isExported;
    }

    public void setExported(boolean exported) {
        isExported = exported;
    }

    // === 新增：烟叶详细数据的getter和setter方法 ===
    
    // 上部叶数据
    public int getUpperLeafBundles() {
        return upperLeafBundles;
    }

    public void setUpperLeafBundles(int upperLeafBundles) {
        this.upperLeafBundles = upperLeafBundles;
        updateTotalsAndPrimaryPart();
    }

    public double getUpperLeafWeight() {
        return upperLeafWeight;
    }

    public void setUpperLeafWeight(double upperLeafWeight) {
        this.upperLeafWeight = upperLeafWeight;
        updateTotalsAndPrimaryPart();
    }

    // 中部叶数据
    public int getMiddleLeafBundles() {
        return middleLeafBundles;
    }

    public void setMiddleLeafBundles(int middleLeafBundles) {
        this.middleLeafBundles = middleLeafBundles;
        updateTotalsAndPrimaryPart();
    }

    public double getMiddleLeafWeight() {
        return middleLeafWeight;
    }

    public void setMiddleLeafWeight(double middleLeafWeight) {
        this.middleLeafWeight = middleLeafWeight;
        updateTotalsAndPrimaryPart();
    }

    // 下部叶数据
    public int getLowerLeafBundles() {
        return lowerLeafBundles;
    }

    public void setLowerLeafBundles(int lowerLeafBundles) {
        this.lowerLeafBundles = lowerLeafBundles;
        updateTotalsAndPrimaryPart();
    }

    public double getLowerLeafWeight() {
        return lowerLeafWeight;
    }

    public void setLowerLeafWeight(double lowerLeafWeight) {
        this.lowerLeafWeight = lowerLeafWeight;
        updateTotalsAndPrimaryPart();
    }

    // 总计数据
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
        this.weight = totalWeight; // Keep weight field in sync
    }

    // 主要烟叶部位
    public String getPrimaryTobaccoPart() {
        return primaryTobaccoPart;
    }

    public void setPrimaryTobaccoPart(String primaryTobaccoPart) {
        this.primaryTobaccoPart = primaryTobaccoPart;
    }

    // === 便利方法：设置完整的烟叶数据 ===
    
    /**
     * 一次性设置所有烟叶数据，避免多次触发重计算
     */
    public void setTobaccoLeafData(int upperBundles, double upperWeight,
                                   int middleBundles, double middleWeight,
                                   int lowerBundles, double lowerWeight) {
        this.upperLeafBundles = upperBundles;
        this.upperLeafWeight = upperWeight;
        this.middleLeafBundles = middleBundles;
        this.middleLeafWeight = middleWeight;
        this.lowerLeafBundles = lowerBundles;
        this.lowerLeafWeight = lowerWeight;
        
        updateTotalsAndPrimaryPart();
    }

    /**
     * 获取指定烟叶部位的捆数
     */
    public int getBundlesByTobaccoPart(String tobaccoPart) {
        if (tobaccoPart == null) return 0;
        
        switch (tobaccoPart) {
            case "上部叶":
                return upperLeafBundles;
            case "中部叶":
                return middleLeafBundles;
            case "下部叶":
                return lowerLeafBundles;
            default:
                return 0;
        }
    }

    /**
     * 获取指定烟叶部位的重量
     */
    public double getWeightByTobaccoPart(String tobaccoPart) {
        if (tobaccoPart == null) return 0.0;
        
        switch (tobaccoPart) {
            case "上部叶":
                return upperLeafWeight;
            case "中部叶":
                return middleLeafWeight;
            case "下部叶":
                return lowerLeafWeight;
            default:
                return 0.0;
        }
    }

    /**
     * 检查是否有多个烟叶部位的数据
     */
    public boolean hasMultipleTobaccoParts() {
        int partsWithData = 0;
        if (upperLeafBundles > 0) partsWithData++;
        if (middleLeafBundles > 0) partsWithData++;
        if (lowerLeafBundles > 0) partsWithData++;
        return partsWithData > 1;
    }

    /**
     * 获取烟叶部位统计描述
     */
    public String getTobaccoPartsDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (upperLeafBundles > 0) {
            desc.append(String.format("上部叶:%d捆(%.1fkg)", upperLeafBundles, upperLeafWeight));
        }
        if (middleLeafBundles > 0) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append(String.format("中部叶:%d捆(%.1fkg)", middleLeafBundles, middleLeafWeight));
        }
        if (lowerLeafBundles > 0) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append(String.format("下部叶:%d捆(%.1fkg)", lowerLeafBundles, lowerLeafWeight));
        }
        
        if (desc.length() == 0) {
            return "无烟叶数据";
        }
        
        return desc.toString();
    }

    /**
     * 检查ID card信息是否完整
     */
    public boolean hasCompleteIdCardInfo() {
        return farmerName != null && !farmerName.trim().isEmpty() &&
                idCardNumber != null && !idCardNumber.trim().isEmpty() &&
                farmerAddress != null && !farmerAddress.trim().isEmpty() &&
                farmerGender != null && !farmerGender.trim().isEmpty();
    }

    /**
     * 获取格式化的农户信息
     */
    public String getFormattedFarmerInfo() {
        StringBuilder sb = new StringBuilder();
        if (farmerName != null)
            sb.append("姓名: ").append(farmerName);
        if (farmerGender != null)
            sb.append(" | 性别: ").append(farmerGender);
        if (idCardNumber != null)
            sb.append(" | 身份证: ").append(formatIdCard(idCardNumber));
        if (farmerAddress != null)
            sb.append(" | 地址: ").append(farmerAddress);
        return sb.toString();
    }

    /**
     * 格式化身份证号显示
     */
    private String formatIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "****" + idCard.substring(14);
    }

    // 工具方法
    @Override
    public String toString() {
        return "WeightRecord{" +
                "recordNumber='" + recordNumber + '\'' +
                ", farmerName='" + farmerName + '\'' +
                ", idCardNumber='" + (idCardNumber != null ? formatIdCard(idCardNumber) : null) + '\'' +
                ", farmerAddress='" + farmerAddress + '\'' +
                ", farmerGender='" + farmerGender + '\'' +
                ", tobaccoPart='" + primaryTobaccoPart + '\'' +
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

    // 新增：更新总计和主要部位的方法
    private void updateTotalsAndPrimaryPart() {
        // 计算总计
        this.totalBundles = upperLeafBundles + middleLeafBundles + lowerLeafBundles;
        this.totalWeight = upperLeafWeight + middleLeafWeight + lowerLeafWeight;
        this.weight = this.totalWeight; // 保持向后兼容

        // 确定主要烟叶部位（捆数最多的部位）
        if (totalBundles > 0) {
            if (upperLeafBundles >= middleLeafBundles && upperLeafBundles >= lowerLeafBundles) {
                this.primaryTobaccoPart = "上部叶";
            } else if (middleLeafBundles >= upperLeafBundles && middleLeafBundles >= lowerLeafBundles) {
                this.primaryTobaccoPart = "中部叶";
            } else {
                this.primaryTobaccoPart = "下部叶";
            }
        } else {
            this.primaryTobaccoPart = "无烟叶";
        }
    }
}