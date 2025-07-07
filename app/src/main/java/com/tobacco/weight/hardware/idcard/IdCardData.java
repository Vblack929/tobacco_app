package com.tobacco.weight.hardware.idcard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * 身份证数据模型
 * 存储从身份证读卡器读取的信息
 */
public class IdCardData {
    
    private String name;        // 姓名
    private String idNumber;    // 身份证号
    private String address;     // 地址
    private String gender;      // 性别
    private String birthDate;   // 出生日期
    private String nationality; // 民族
    
    /**
     * 默认构造函数
     */
    public IdCardData() {
    }
    
    /**
     * 完整构造函数
     */
    public IdCardData(String name, String idNumber, String address, 
                     String gender, String birthDate, String nationality) {
        this.name = name;
        this.idNumber = idNumber;
        this.address = address;
        this.gender = gender;
        this.birthDate = birthDate;
        this.nationality = nationality;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getIdNumber() {
        return idNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getGender() {
        return gender;
    }
    
    public String getBirthDate() {
        return birthDate;
    }
    
    public String getNationality() {
        return nationality;
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }
    
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
    
    /**
     * 验证身份证数据是否有效
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               idNumber != null && !idNumber.trim().isEmpty() &&
               idNumber.length() == 18;
    }
    
    /**
     * 获取格式化的身份证号（隐藏中间部分）
     */
    public String getFormattedIdNumber() {
        if (idNumber == null || idNumber.length() < 18) {
            return idNumber;
        }
        return idNumber.substring(0, 6) + "********" + idNumber.substring(14);
    }
    
    /**
     * 从JSON字符串解析身份证数据
     * @param jsonString JSON字符串
     * @return 解析后的身份证数据，失败时返回null
     */
    public static IdCardData fromJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        try {
            JSONObject json = new JSONObject(jsonString);
            
            // 检查响应状态
            if (json.has("status") && !"success".equals(json.getString("status"))) {
                return null;
            }
            
            // 解析身份证信息
            JSONObject data = json.optJSONObject("data");
            if (data == null) {
                // 如果没有data字段，直接从根对象解析
                data = json;
            }
            
            IdCardData idCardData = new IdCardData();
            idCardData.setName(data.optString("name", ""));
            idCardData.setIdNumber(data.optString("idNumber", ""));
            idCardData.setAddress(data.optString("address", ""));
            idCardData.setGender(data.optString("gender", ""));
            idCardData.setBirthDate(data.optString("birthDate", ""));
            idCardData.setNationality(data.optString("nationality", ""));
            
            return idCardData;
            
        } catch (JSONException e) {
            return null;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdCardData that = (IdCardData) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(idNumber, that.idNumber) &&
               Objects.equals(address, that.address) &&
               Objects.equals(gender, that.gender) &&
               Objects.equals(birthDate, that.birthDate) &&
               Objects.equals(nationality, that.nationality);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, idNumber, address, gender, birthDate, nationality);
    }
    
    @Override
    public String toString() {
        return "IdCardData{" +
               "name='" + name + '\'' +
               ", idNumber='" + getFormattedIdNumber() + '\'' +
               ", address='" + address + '\'' +
               ", gender='" + gender + '\'' +
               ", birthDate='" + birthDate + '\'' +
               ", nationality='" + nationality + '\'' +
               '}';
    }
} 