package com.tobacco.weight.data;

import java.util.Objects;

/**
 * 烟农身份信息模型
 * 
 * 设计原则：
 * - 包含完整的身份证信息和基本农户信息
 * - 所有字段不可变（immutable）
 * - 一旦创建后不能修改任何信息
 * - 可以安全地在不同模块间共享和引用
 */
public class FarmerInfo {

    // 基本信息
    private final String farmerName; // 烟农姓名
    private final String contractNumber; // 合同号

    // 完整身份证信息（不可变）
    private final String idCardNumber; // 身份证号
    private final String gender; // 性别
    private final String nationality; // 民族
    private final String birthDate; // 出生日期
    private final String address; // 地址
    private final String department; // 签发机关
    private final String startDate; // 有效期开始
    private final String endDate; // 有效期结束
    private final byte[] photo; // 照片数据

    /**
     * 完整构造函数（包含所有身份信息）
     */
    public FarmerInfo(String farmerName, String contractNumber, String idCardNumber,
            String gender, String nationality, String birthDate, String address,
            String department, String startDate, String endDate, byte[] photo) {
        this.farmerName = farmerName != null ? farmerName.trim() : "";
        this.contractNumber = contractNumber != null ? contractNumber.trim() : "";

        // 设置不可变的身份证信息
        this.idCardNumber = idCardNumber != null ? idCardNumber.trim() : "";
        this.gender = gender != null ? gender.trim() : "";
        this.nationality = nationality != null ? nationality.trim() : "";
        this.birthDate = birthDate != null ? birthDate.trim() : "";
        this.address = address != null ? address.trim() : "";
        this.department = department != null ? department.trim() : "";
        this.startDate = startDate != null ? startDate.trim() : "";
        this.endDate = endDate != null ? endDate.trim() : "";
        this.photo = photo; // 可以为null
    }

    /**
     * 简化构造函数（仅基本信息）
     */
    public FarmerInfo(String farmerName, String contractNumber, String idCardNumber) {
        this(farmerName, contractNumber, idCardNumber, "", "", "", "", "", "", "", null);
    }

    /**
     * 工厂方法：创建带有完整身份证信息的农户
     */
    public static FarmerInfo createWithIdCard(String farmerName, String contractNumber,
            String idCardNumber, String gender, String nationality,
            String birthDate, String address, String department,
            String startDate, String endDate, byte[] photo) {
        return new FarmerInfo(farmerName, contractNumber, idCardNumber, gender, nationality,
                birthDate, address, department, startDate, endDate, photo);
    }

    /**
     * 工厂方法：创建基本农户信息
     */
    public static FarmerInfo createBasic(String farmerName, String contractNumber, String idCardNumber) {
        return new FarmerInfo(farmerName, contractNumber, idCardNumber);
    }

    /**
     * 检查是否有完整的身份证信息
     */
    public boolean hasCompleteIdCardInfo() {
        return idCardNumber != null && !idCardNumber.trim().isEmpty() &&
                farmerName != null && !farmerName.trim().isEmpty() &&
                address != null && !address.trim().isEmpty();
    }

    /**
     * 检查身份证号是否匹配
     */
    public boolean matchesIdCard(String idCardNumber) {
        return this.idCardNumber != null && this.idCardNumber.equals(idCardNumber);
    }

    /**
     * 检查农户姓名是否匹配
     */
    public boolean matchesName(String farmerName) {
        return this.farmerName != null && this.farmerName.equals(farmerName);
    }

    /**
     * 检查身份信息一致性
     */
    public boolean isIdentityConsistent(String farmerName, String idCardNumber) {
        return matchesName(farmerName) && matchesIdCard(idCardNumber);
    }

    /**
     * 获取脱敏的身份证号（用于显示）
     */
    public String getMaskedIdCardNumber() {
        if (idCardNumber == null || idCardNumber.length() < 8) {
            return "****";
        }
        return idCardNumber.substring(0, 4) + "****" + idCardNumber.substring(idCardNumber.length() - 4);
    }

    /**
     * 获取显示信息
     */
    public String getDisplayInfo() {
        return farmerName + " (" + getMaskedIdCardNumber() + ")";
    }

    // Getters
    public String getFarmerName() {
        return farmerName;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public String getGender() {
        return gender;
    }

    public String getNationality() {
        return nationality;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }

    public String getDepartment() {
        return department;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public byte[] getPhoto() {
        return photo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        FarmerInfo that = (FarmerInfo) obj;
        return Objects.equals(farmerName, that.farmerName) &&
                Objects.equals(contractNumber, that.contractNumber) &&
                Objects.equals(idCardNumber, that.idCardNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmerName, contractNumber, idCardNumber);
    }

    @Override
    public String toString() {
        return "FarmerInfo{" +
                "farmerName='" + farmerName + '\'' +
                ", contractNumber='" + contractNumber + '\'' +
                ", idCardNumber='" + getMaskedIdCardNumber() + '\'' +
                '}';
    }
}