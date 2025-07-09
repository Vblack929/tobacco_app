package com.tobacco.weight.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

/**
 * 烟农身份信息数据库实体
 * 
 * 设计原则：
 * - 每个身份证号只存储一次
 * - 所有身份信息不可变
 * - 通过身份证号与称重记录关联
 * - 支持查询和统计操作
 */
@Entity(
    tableName = "farmer_info",
    indices = {
        @Index(value = "id_card_number", unique = true), // 身份证号唯一索引
        @Index(value = "farmer_name")                    // 姓名索引，提高查询性能
    }
)
public class FarmerInfoEntity {
    
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    
    // 基本信息
    @ColumnInfo(name = "farmer_name")
    private String farmerName;        // 烟农姓名
    
    @ColumnInfo(name = "contract_number")
    private String contractNumber;    // 合同号
    
    // 完整身份证信息
    @ColumnInfo(name = "id_card_number")
    private String idCardNumber;      // 身份证号（唯一）
    
    @ColumnInfo(name = "gender")
    private String gender;            // 性别
    
    @ColumnInfo(name = "nationality")
    private String nationality;       // 民族
    
    @ColumnInfo(name = "birth_date")
    private String birthDate;         // 出生日期
    
    @ColumnInfo(name = "address")
    private String address;           // 地址
    
    @ColumnInfo(name = "department")
    private String department;        // 签发机关
    
    @ColumnInfo(name = "start_date")
    private String startDate;         // 有效期开始
    
    @ColumnInfo(name = "end_date")
    private String endDate;           // 有效期结束
    
    @ColumnInfo(name = "photo")
    private byte[] photo;             // 照片数据
    
    // 元数据
    @ColumnInfo(name = "create_time")
    private Date createTime;          // 创建时间
    
    @ColumnInfo(name = "update_time")
    private Date updateTime;          // 更新时间
    
    @ColumnInfo(name = "first_record_time")
    private Date firstRecordTime;     // 首次称重时间
    
    @ColumnInfo(name = "is_active")
    private boolean isActive;         // 是否激活状态
    
    @ColumnInfo(name = "remark")
    private String remark;            // 备注

    /**
     * 构造函数
     */
    public FarmerInfoEntity() {
        this.createTime = new Date();
        this.updateTime = new Date();
        this.isActive = true;
    }
    
    /**
     * 完整构造函数
     */
    public FarmerInfoEntity(String farmerName, String contractNumber, String idCardNumber, 
                           String gender, String nationality, String birthDate, String address,
                           String department, String startDate, String endDate, byte[] photo) {
        this();
        this.farmerName = farmerName;
        this.contractNumber = contractNumber;
        this.idCardNumber = idCardNumber;
        this.gender = gender;
        this.nationality = nationality;
        this.birthDate = birthDate;
        this.address = address;
        this.department = department;
        this.startDate = startDate;
        this.endDate = endDate;
        this.photo = photo;
        this.firstRecordTime = new Date();
    }
    
    /**
     * 从FarmerInfo对象创建实体
     */
    public static FarmerInfoEntity fromFarmerInfo(com.tobacco.weight.data.FarmerInfo farmerInfo) {
        FarmerInfoEntity entity = new FarmerInfoEntity();
        entity.setFarmerName(farmerInfo.getFarmerName());
        entity.setContractNumber(farmerInfo.getContractNumber());
        entity.setIdCardNumber(farmerInfo.getIdCardNumber());
        entity.setGender(farmerInfo.getGender());
        entity.setNationality(farmerInfo.getNationality());
        entity.setBirthDate(farmerInfo.getBirthDate());
        entity.setAddress(farmerInfo.getAddress());
        entity.setDepartment(farmerInfo.getDepartment());
        entity.setStartDate(farmerInfo.getStartDate());
        entity.setEndDate(farmerInfo.getEndDate());
        entity.setPhoto(farmerInfo.getPhoto());
        entity.setFirstRecordTime(new Date());
        return entity;
    }
    
    /**
     * 转换为FarmerInfo对象
     */
    public com.tobacco.weight.data.FarmerInfo toFarmerInfo() {
        return com.tobacco.weight.data.FarmerInfo.createWithIdCard(
            farmerName, contractNumber, idCardNumber, gender, nationality,
            birthDate, address, department, startDate, endDate, photo
        );
    }
    
    /**
     * 检查是否有完整的身份证信息
     */
    public boolean hasCompleteIdCardInfo() {
        return idCardNumber != null && !idCardNumber.trim().isEmpty() &&
               farmerName != null && !farmerName.trim().isEmpty() &&
               address != null && !address.trim().isEmpty();
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
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

    public Date getFirstRecordTime() {
        return firstRecordTime;
    }

    public void setFirstRecordTime(Date firstRecordTime) {
        this.firstRecordTime = firstRecordTime;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return "FarmerInfoEntity{" +
                "id=" + id +
                ", farmerName='" + farmerName + '\'' +
                ", contractNumber='" + contractNumber + '\'' +
                ", idCardNumber='" + (idCardNumber != null && idCardNumber.length() >= 14 ? 
                    idCardNumber.substring(0, 6) + "********" + idCardNumber.substring(14) : idCardNumber) + '\'' +
                ", gender='" + gender + '\'' +
                ", address='" + address + '\'' +
                ", isActive=" + isActive +
                ", createTime=" + createTime +
                '}';
    }
} 