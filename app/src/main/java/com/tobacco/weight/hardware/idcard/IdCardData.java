package com.tobacco.weight.hardware.idcard;

import android.graphics.Bitmap;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 身份证数据模型
 * 基于演示项目的JSON结构实现
 */
public class IdCardData {
    
    private String name;           // 姓名
    private String idNumber;       // 身份证号 (number)
    private String gender;         // 性别 (gender int: 1=男, 2=女)
    private String nationality;    // 民族 (race code)
    private String birthDate;      // 出生日期 (birthday)
    private String address;        // 地址 (address)
    private String department;     // 签发机关 (department)
    private String startDate;      // 有效期开始 (startdate)
    private String endDate;        // 有效期结束 (enddate)
    private Bitmap photo;          // 照片 (photo base64)
    
    /**
     * 默认构造函数
     */
    public IdCardData() {
    }
    
    /**
     * 从演示项目SDK的JSON结果解析身份证数据
     * JSON格式示例:
     * {
     *   "name": "张三",
     *   "number": "110101199001011234",
     *   "gender": 1,
     *   "race": "01",
     *   "birthday": "19900101",
     *   "address": "北京市东城区",
     *   "department": "北京市公安局",
     *   "startdate": "20200101",
     *   "enddate": "20300101",
     *   "photo": "base64..."
     * }
     */
    public static IdCardData fromSdkJson(String jsonResult) {
        if (jsonResult == null || jsonResult.trim().isEmpty()) {
            return null;
        }
        
        try {
            JSONObject json = new JSONObject(jsonResult);
            
            IdCardData data = new IdCardData();
            data.name = json.optString("name", "");
            data.idNumber = json.optString("number", "");
            
            // 性别转换 (1=男, 2=女)
            int genderCode = json.optInt("gender", 0);
            data.gender = genderCode == 1 ? "男" : genderCode == 2 ? "女" : "";
            
            // 民族代码转换
            String raceCode = json.optString("race", "");
            data.nationality = convertRaceCode(raceCode);
            
            data.birthDate = json.optString("birthday", "");
            data.address = json.optString("address", "");
            data.department = json.optString("department", "");
            data.startDate = json.optString("startdate", "");
            data.endDate = json.optString("enddate", "");
            
            // 处理照片
            String photoBase64 = json.optString("photo", "");
            if (!photoBase64.isEmpty()) {
                data.photo = decodeBase64ToBitmap(photoBase64);
            }
            
            return data;
            
        } catch (JSONException e) {
            return null;
        }
    }
    
    /**
     * 民族代码转换
     */
    private static String convertRaceCode(String raceCode) {
        if (raceCode == null || raceCode.isEmpty()) return "";
        
        switch (raceCode) {
            case "01": return "汉";
            case "02": return "蒙古";
            case "03": return "回";
            case "04": return "藏";
            case "05": return "维吾尔";
            case "06": return "苗";
            case "07": return "彝";
            case "08": return "壮";
            case "09": return "布依";
            case "10": return "朝鲜";
            default: return "其他";
        }
    }
    
    /**
     * Base64解码为Bitmap
     */
    private static Bitmap decodeBase64ToBitmap(String base64) {
        try {
            byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            return android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 验证数据有效性
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               idNumber != null && !idNumber.trim().isEmpty() &&
               idNumber.length() == 18;
    }
    
    // Getters
    public String getName() { return name; }
    public String getIdNumber() { return idNumber; }
    public String getGender() { return gender; }
    public String getNationality() { return nationality; }
    public String getBirthDate() { return birthDate; }
    public String getAddress() { return address; }
    public String getDepartment() { return department; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Bitmap getPhoto() { return photo; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setGender(String gender) { this.gender = gender; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public void setAddress(String address) { this.address = address; }
    public void setDepartment(String department) { this.department = department; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setPhoto(Bitmap photo) { this.photo = photo; }
    
    @Override
    public String toString() {
        return "IdCardData{" +
               "name='" + name + '\'' +
               ", idNumber='" + (idNumber != null && idNumber.length() >= 14 ? 
                   idNumber.substring(0, 6) + "********" + idNumber.substring(14) : idNumber) + '\'' +
               ", gender='" + gender + '\'' +
               ", nationality='" + nationality + '\'' +
               ", address='" + address + '\'' +
               '}';
    }
} 