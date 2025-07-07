package com.tobacco.weight.hardware.idcard;

/**
 * 简单的测试运行器
 * 演示ID card reader功能测试
 */
public class TestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== ID Card Reader Integration Test Demo ===\n");
        
        try {
            // 测试JSON解析功能
            testJsonParsing();
            
            // 测试数据验证
            testDataValidation();
            
            // 测试边界情况
            testEdgeCases();
            
            System.out.println("✅ All tests passed! ID card reader integration is working correctly.\n");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试JSON解析功能
     */
    private static void testJsonParsing() {
        System.out.println("🧪 Testing JSON parsing...");
        
        // 测试成功场景
        String successJson = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"data\": {\n" +
                "    \"name\": \"张三\",\n" +
                "    \"idNumber\": \"123456789012345678\",\n" +
                "    \"gender\": \"男\",\n" +
                "    \"birthDate\": \"19900101\",\n" +
                "    \"address\": \"北京市东城区\",\n" +
                "    \"nationality\": \"汉\"\n" +
                "  }\n" +
                "}";
        
        IdCardData result = IdCardData.fromJson(successJson);
        
        assert result != null : "JSON解析失败";
        assert "张三".equals(result.getName()) : "姓名解析错误";
        assert "123456789012345678".equals(result.getIdNumber()) : "身份证号解析错误";
        assert "男".equals(result.getGender()) : "性别解析错误";
        assert "19900101".equals(result.getBirthDate()) : "出生日期解析错误";
        assert "北京市东城区".equals(result.getAddress()) : "地址解析错误";
        assert "汉".equals(result.getNationality()) : "民族解析错误";
        
        System.out.println("   ✓ Success case JSON parsing works");
        
        // 测试直接格式
        String directJson = "{\n" +
                "  \"name\": \"李四\",\n" +
                "  \"idNumber\": \"987654321098765432\",\n" +
                "  \"gender\": \"女\",\n" +
                "  \"birthDate\": \"19850315\",\n" +
                "  \"address\": \"上海市浦东新区\",\n" +
                "  \"nationality\": \"汉\"\n" +
                "}";
        
        IdCardData directResult = IdCardData.fromJson(directJson);
        assert directResult != null : "直接格式JSON解析失败";
        assert "李四".equals(directResult.getName()) : "直接格式姓名解析错误";
        
        System.out.println("   ✓ Direct format JSON parsing works");
    }
    
    /**
     * 测试数据验证
     */
    private static void testDataValidation() {
        System.out.println("🧪 Testing data validation...");
        
        // 测试有效数据
        IdCardData validData = new IdCardData();
        validData.setName("张三");
        validData.setIdNumber("123456789012345678");
        
        assert validData.isValid() : "有效数据验证失败";
        System.out.println("   ✓ Valid data validation works");
        
        // 测试无效数据
        IdCardData invalidData = new IdCardData();
        invalidData.setName("");
        invalidData.setIdNumber("123");
        
        assert !invalidData.isValid() : "无效数据应该被拒绝";
        System.out.println("   ✓ Invalid data rejection works");
        
        // 测试格式化功能
        String formatted = validData.getFormattedIdNumber();
        assert "123456********5678".equals(formatted) : "身份证号格式化错误";
        System.out.println("   ✓ ID number formatting works");
    }
    
    /**
     * 测试边界情况
     */
    private static void testEdgeCases() {
        System.out.println("🧪 Testing edge cases...");
        
        // 测试null输入
        IdCardData nullResult = IdCardData.fromJson(null);
        assert nullResult == null : "null输入应返回null";
        System.out.println("   ✓ Null input handling works");
        
        // 测试空字符串
        IdCardData emptyResult = IdCardData.fromJson("");
        assert emptyResult == null : "空字符串应返回null";
        System.out.println("   ✓ Empty string handling works");
        
        // 测试无效JSON
        IdCardData invalidJsonResult = IdCardData.fromJson("invalid json");
        assert invalidJsonResult == null : "无效JSON应返回null";
        System.out.println("   ✓ Invalid JSON handling works");
        
        // 测试失败状态
        String failureJson = "{\n" +
                "  \"status\": \"error\",\n" +
                "  \"message\": \"读卡失败\"\n" +
                "}";
        
        IdCardData failureResult = IdCardData.fromJson(failureJson);
        assert failureResult == null : "失败状态应返回null";
        System.out.println("   ✓ Failure status handling works");
    }
} 