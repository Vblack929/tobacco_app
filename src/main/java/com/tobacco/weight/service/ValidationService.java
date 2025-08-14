package com.tobacco.weight.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * 数据验证服务
 * 提供身份证号、合同金额等数据的格式验证和校验功能
 */
public class ValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    
    // 身份证号正则表达式
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    
    // 身份证校验位权重
    private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] ID_CARD_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
    
    /**
     * 验证身份证号格式和校验位
     */
    public static ValidationResult validateNationalId(String nationalId) {
        if (nationalId == null || nationalId.trim().isEmpty()) {
            return ValidationResult.error("身份证号不能为空");
        }
        
        String trimmed = nationalId.trim().toUpperCase();
        
        // 基本格式检查
        if (!ID_CARD_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.error("身份证号格式不正确");
        }
        
        // 校验位检查
        if (!validateIdCardChecksum(trimmed)) {
            return ValidationResult.error("身份证号校验位不正确");
        }
        
        // 日期合理性检查
        String birthDate = trimmed.substring(6, 14);
        if (!isValidBirthDate(birthDate)) {
            return ValidationResult.warning("身份证号中的出生日期可能不合理: " + birthDate);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 验证身份证校验位
     */
    private static boolean validateIdCardChecksum(String idCard) {
        if (idCard.length() != 18) {
            return false;
        }
        
        try {
            int sum = 0;
            for (int i = 0; i < 17; i++) {
                int digit = Character.getNumericValue(idCard.charAt(i));
                sum += digit * ID_CARD_WEIGHTS[i];
            }
            
            int checkIndex = sum % 11;
            char expectedCheck = ID_CARD_CHECK_CODES[checkIndex];
            char actualCheck = idCard.charAt(17);
            
            return expectedCheck == actualCheck;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 验证出生日期合理性
     */
    private static boolean isValidBirthDate(String birthDate) {
        try {
            int year = Integer.parseInt(birthDate.substring(0, 4));
            int month = Integer.parseInt(birthDate.substring(4, 6));
            int day = Integer.parseInt(birthDate.substring(6, 8));
            
            // 年份范围检查
            int currentYear = java.time.LocalDate.now().getYear();
            if (year < 1900 || year > currentYear) {
                return false;
            }
            
            // 月份检查
            if (month < 1 || month > 12) {
                return false;
            }
            
            // 日期检查
            if (day < 1 || day > 31) {
                return false;
            }
            
            // 简单的月日组合检查
            if ((month == 4 || month == 6 || month == 9 || month == 11) && day > 30) {
                return false;
            }
            
            if (month == 2 && day > 29) {
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 验证合同金额
     */
    public static ValidationResult validateContractAmount(double amount) {
        if (amount < 0) {
            return ValidationResult.error("合同金额不能为负数");
        }
        
        if (amount == 0) {
            return ValidationResult.warning("合同金额为0，请确认是否正确");
        }
        
        if (amount > 1000000) {
            return ValidationResult.warning("合同金额过大(" + amount + ")，请确认是否正确");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 验证必填字段
     */
    public static ValidationResult validateRequiredField(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.error(fieldName + "不能为空");
        }
        return ValidationResult.success();
    }
    
    /**
     * 验证合同号格式
     */
    public static ValidationResult validateContractNo(String contractNo) {
        if (contractNo == null || contractNo.trim().isEmpty()) {
            return ValidationResult.error("合同号不能为空");
        }
        
        String trimmed = contractNo.trim();
        
        // 基本长度检查
        if (trimmed.length() < 5 || trimmed.length() > 50) {
            return ValidationResult.warning("合同号长度异常(" + trimmed.length() + "字符)");
        }
        
        // 检查是否包含特殊字符
        if (trimmed.matches(".*[<>\"'&].*")) {
            return ValidationResult.error("合同号包含非法字符");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 解析并规范化数值字符串
     * 处理 "6,130.00" → 6130.00 这样的格式
     */
    public static double parseNumericValue(String value) throws NumberFormatException {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        
        // 移除千分位分隔符和空格
        String cleaned = value.trim()
                              .replace(",", "")
                              .replace(" ", "")
                              .replace("，", ""); // 中文逗号
        
        return Double.parseDouble(cleaned);
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final boolean warning;
        private final String message;
        
        private ValidationResult(boolean valid, boolean warning, String message) {
            this.valid = valid;
            this.warning = warning;
            this.message = message;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, false, "");
        }
        
        public static ValidationResult warning(String message) {
            return new ValidationResult(true, true, message);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, false, message);
        }
        
        public boolean isValid() { return valid; }
        public boolean isWarning() { return warning; }
        public String getMessage() { return message; }
        
        public boolean hasIssue() { return !valid || warning; }
    }
}
