package com.tobacco.weight.data.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.tobacco.weight.data.database.DateConverter;

import java.util.Date;

/**
 * 称重记录数据模型
 * 对应数据库中的weight_records表
 */
@Entity(tableName = "weight_records")
@TypeConverters({DateConverter.class})
public class WeightRecord {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    @ColumnInfo(name = "record_number")
    private String recordNumber; // 记录编号（自动生成）
    
    @ColumnInfo(name = "weight")
    private double weight; // 称重重量（kg）
    
    @ColumnInfo(name = "id_card_number")
    private String idCardNumber; // 身份证号码
    
    @ColumnInfo(name = "farmer_name")
    private String farmerName; // 农户姓名
    
    @ColumnInfo(name = "farmer_address")
    private String farmerAddress; // 农户地址
    
    @ColumnInfo(name = "tobacco_bundles")
    private int tobaccoBundles; // 烟叶捆数
    
    @ColumnInfo(name = "tobacco_part")
    private String tobaccoPart; // 烟叶部位（上叶、中叶、下叶等）
    
    @ColumnInfo(name = "pre_check_number")
    private String preCheckNumber; // 预检编号
    
    @ColumnInfo(name = "tobacco_grade")
    private String tobaccoGrade; // 烟叶等级
    
    @ColumnInfo(name = "purchase_price")
    private double purchasePrice; // 收购单价（元/kg）
    
    @ColumnInfo(name = "total_amount")
    private double totalAmount; // 总金额（元）
    
    @ColumnInfo(name = "moisture_content")
    private double moistureContent; // 含水率（%）
    
    @ColumnInfo(name = "impurity_rate")
    private double impurityRate; // 杂质率（%）
    
    @ColumnInfo(name = "warehouse_number")
    private String warehouseNumber; // 仓库编号
    
    @ColumnInfo(name = "operator_name")
    private String operatorName; // 操作员姓名
    
    @ColumnInfo(name = "remark")
    private String remark; // 备注
    
    @ColumnInfo(name = "create_time")
    private Date createTime; // 创建时间
    
    @ColumnInfo(name = "update_time")
    private Date updateTime; // 更新时间
    
    @ColumnInfo(name = "is_printed")
    private boolean isPrinted; // 是否已打印标签
    
    @ColumnInfo(name = "print_count")
    private int printCount; // 打印次数
    
    @ColumnInfo(name = "is_exported")
    private boolean isExported; // 是否已导出
    
    @ColumnInfo(name = "qr_code")
    private String qrCode; // 二维码内容
    
    // 构造函数
    public WeightRecord() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.isPrinted = false;
        this.printCount = 0;
        this.isExported = false;
    }
    
    // Getter 和 Setter 方法
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getRecordNumber() {
        return recordNumber;
    }
    
    public void setRecordNumber(String recordNumber) {
        this.recordNumber = recordNumber;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public String getIdCardNumber() {
        return idCardNumber;
    }
    
    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }
    
    public String getFarmerName() {
        return farmerName;
    }
    
    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }
    
    public String getFarmerAddress() {
        return farmerAddress;
    }
    
    public void setFarmerAddress(String farmerAddress) {
        this.farmerAddress = farmerAddress;
    }
    
    public int getTobaccoBundles() {
        return tobaccoBundles;
    }
    
    public void setTobaccoBundles(int tobaccoBundles) {
        this.tobaccoBundles = tobaccoBundles;
    }
    
    public String getTobaccoPart() {
        return tobaccoPart;
    }
    
    public void setTobaccoPart(String tobaccoPart) {
        this.tobaccoPart = tobaccoPart;
    }
    
    public String getPreCheckNumber() {
        return preCheckNumber;
    }
    
    public void setPreCheckNumber(String preCheckNumber) {
        this.preCheckNumber = preCheckNumber;
    }
    
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
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
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
    
    public String getWarehouseNumber() {
        return warehouseNumber;
    }
    
    public void setWarehouseNumber(String warehouseNumber) {
        this.warehouseNumber = warehouseNumber;
    }
    
    public String getOperatorName() {
        return operatorName;
    }
    
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
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
    
    public String getQrCode() {
        return qrCode;
    }
    
    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }
    
    /**
     * 计算总金额
     */
    public void calculateTotalAmount() {
        this.totalAmount = this.weight * this.purchasePrice;
    }
    
    /**
     * 生成二维码内容
     */
    public void generateQrCode() {
        StringBuilder qrBuilder = new StringBuilder();
        qrBuilder.append("记录编号:").append(recordNumber).append("\n");
        qrBuilder.append("农户姓名:").append(farmerName).append("\n");
        qrBuilder.append("重量:").append(weight).append("kg\n");
        qrBuilder.append("总金额:").append(totalAmount).append("元\n");
        qrBuilder.append("时间:").append(createTime.toString());
        this.qrCode = qrBuilder.toString();
    }
    
    @Override
    public String toString() {
        return "WeightRecord{" +
                "id=" + id +
                ", recordNumber='" + recordNumber + '\'' +
                ", weight=" + weight +
                ", farmerName='" + farmerName + '\'' +
                ", totalAmount=" + totalAmount +
                ", createTime=" + createTime +
                '}';
    }
} 