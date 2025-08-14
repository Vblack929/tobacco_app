package com.tobacco.weight.data;

import java.util.Objects;

/**
 * Excel导入记录模型
 * 表示从Excel中解析的单行数据
 */
public class ImportRecord {
    
    // 原始数据
    private final int rowNumber;           // 行号
    private final String station;          // 站点名称
    private final String farmerName;       // 姓名
    private final String contractNo;       // 种植者编号（合同号）
    private final String nationalId;       // 身份证号
    private final String adminDivision;    // 行政区划
    private final double contractAmount;   // 合同量
    
    // 解析后的数据
    private final String township;         // 乡镇
    private final String village;          // 村庄
    
    // 状态信息
    private ImportStatus status;
    private String message;
    
    public enum ImportStatus {
        OK,           // 成功
        WARNING,      // 警告（处理了但有问题）
        ERROR         // 错误（无法处理）
    }
    
    /**
     * 构造函数
     */
    public ImportRecord(int rowNumber, String station, String farmerName, 
                       String contractNo, String nationalId, String adminDivision, 
                       double contractAmount, String township, String village) {
        this.rowNumber = rowNumber;
        this.station = station != null ? station.trim() : "";
        this.farmerName = farmerName != null ? farmerName.trim() : "";
        this.contractNo = contractNo != null ? contractNo.trim() : "";
        this.nationalId = nationalId != null ? nationalId.trim() : "";
        this.adminDivision = adminDivision != null ? adminDivision.trim() : "";
        this.contractAmount = contractAmount;
        this.township = township != null ? township.trim() : "";
        this.village = village != null ? village.trim() : "";
        this.status = ImportStatus.OK;
        this.message = "";
    }
    
    /**
     * 检查是否有必填字段为空
     */
    public boolean hasRequiredFields() {
        return !farmerName.isEmpty() && 
               !contractNo.isEmpty() && 
               !nationalId.isEmpty() && 
               !adminDivision.isEmpty() && 
               contractAmount >= 0;
    }
    
    /**
     * 获取脱敏的身份证号
     */
    public String getMaskedNationalId() {
        if (nationalId == null || nationalId.length() < 8) {
            return "******";
        }
        return "******" + nationalId.substring(nationalId.length() - 4);
    }
    
    /**
     * 创建农户信息
     */
    public FarmerInfo createFarmerInfo() {
        return new FarmerInfo(farmerName, contractNo, nationalId, "", "", "", 
                            township + " " + village, "", "", "", null);
    }
    
    // Getters
    public int getRowNumber() { return rowNumber; }
    public String getStation() { return station; }
    public String getFarmerName() { return farmerName; }
    public String getContractNo() { return contractNo; }
    public String getNationalId() { return nationalId; }
    public String getAdminDivision() { return adminDivision; }
    public double getContractAmount() { return contractAmount; }
    public String getTownship() { return township; }
    public String getVillage() { return village; }
    
    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImportRecord that = (ImportRecord) obj;
        return Objects.equals(nationalId, that.nationalId) && 
               Objects.equals(contractNo, that.contractNo);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nationalId, contractNo);
    }
    
    @Override
    public String toString() {
        return "ImportRecord{" +
                "rowNumber=" + rowNumber +
                ", station='" + station + '\'' +
                ", farmerName='" + farmerName + '\'' +
                ", contractNo='" + contractNo + '\'' +
                ", nationalId='" + getMaskedNationalId() + '\'' +
                ", adminDivision='" + adminDivision + '\'' +
                ", contractAmount=" + contractAmount +
                ", status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
