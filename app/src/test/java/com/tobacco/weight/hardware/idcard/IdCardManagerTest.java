package com.tobacco.weight.hardware.idcard;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * 身份证读卡器测试类
 * 测试JSON解析和数据流处理
 */
@RunWith(MockitoJUnitRunner.class)
public class IdCardManagerTest {
    
    private IdCardData testIdCardData;
    
    @Before
    public void setUp() {
        // 准备测试数据
        testIdCardData = new IdCardData();
        testIdCardData.setName("张三");
        testIdCardData.setIdNumber("123456789012345678");
        testIdCardData.setGender("男");
        testIdCardData.setBirthDate("19900101");
        testIdCardData.setAddress("北京市东城区");
        testIdCardData.setNationality("汉");
    }
    
    /**
     * 测试成功场景的JSON解析
     */
    @Test
    public void testFromJson_Success() {
        // 模拟真实的JSON响应
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
        
        // 验证解析结果
        assertNotNull("解析结果不应为null", result);
        assertEquals("张三", result.getName());
        assertEquals("123456789012345678", result.getIdNumber());
        assertEquals("男", result.getGender());
        assertEquals("19900101", result.getBirthDate());
        assertEquals("北京市东城区", result.getAddress());
        assertEquals("汉", result.getNationality());
        
        // 验证数据有效性
        assertTrue("身份证数据应该有效", result.isValid());
    }
    
    /**
     * 测试直接数据格式的JSON解析（无data包装）
     */
    @Test
    public void testFromJson_DirectFormat() {
        // 模拟直接格式的JSON响应
        String directJson = "{\n" +
                "  \"name\": \"李四\",\n" +
                "  \"idNumber\": \"987654321098765432\",\n" +
                "  \"gender\": \"女\",\n" +
                "  \"birthDate\": \"19850315\",\n" +
                "  \"address\": \"上海市浦东新区\",\n" +
                "  \"nationality\": \"汉\"\n" +
                "}";
        
        IdCardData result = IdCardData.fromJson(directJson);
        
        // 验证解析结果
        assertNotNull("解析结果不应为null", result);
        assertEquals("李四", result.getName());
        assertEquals("987654321098765432", result.getIdNumber());
        assertEquals("女", result.getGender());
        assertEquals("19850315", result.getBirthDate());
        assertEquals("上海市浦东新区", result.getAddress());
        assertEquals("汉", result.getNationality());
    }
    
    /**
     * 测试失败状态的JSON解析
     */
    @Test
    public void testFromJson_FailureStatus() {
        String failureJson = "{\n" +
                "  \"status\": \"error\",\n" +
                "  \"message\": \"读卡失败\"\n" +
                "}";
        
        IdCardData result = IdCardData.fromJson(failureJson);
        
        assertNull("失败状态时应返回null", result);
    }
    
    /**
     * 测试空数据和无效JSON
     */
    @Test
    public void testFromJson_InvalidInputs() {
        // 测试null输入
        assertNull("null输入应返回null", IdCardData.fromJson(null));
        
        // 测试空字符串
        assertNull("空字符串应返回null", IdCardData.fromJson(""));
        
        // 测试无效JSON
        assertNull("无效JSON应返回null", IdCardData.fromJson("invalid json"));
        
        // 测试不完整的JSON
        assertNull("不完整JSON应返回null", IdCardData.fromJson("{\"name\":"));
    }
    
    /**
     * 测试部分字段缺失的JSON
     */
    @Test
    public void testFromJson_PartialData() {
        String partialJson = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"data\": {\n" +
                "    \"name\": \"王五\",\n" +
                "    \"idNumber\": \"111111111111111111\"\n" +
                "  }\n" +
                "}";
        
        IdCardData result = IdCardData.fromJson(partialJson);
        
        assertNotNull("部分数据应能解析", result);
        assertEquals("王五", result.getName());
        assertEquals("111111111111111111", result.getIdNumber());
        assertEquals("", result.getGender());
        assertEquals("", result.getBirthDate());
        assertEquals("", result.getAddress());
        assertEquals("", result.getNationality());
    }
    
    /**
     * 测试IdCardData数据验证
     */
    @Test
    public void testIdCardDataValidation() {
        // 测试有效数据
        assertTrue("完整数据应该有效", testIdCardData.isValid());
        
        // 测试姓名为空
        IdCardData invalidName = new IdCardData();
        invalidName.setName("");
        invalidName.setIdNumber("123456789012345678");
        assertFalse("姓名为空应该无效", invalidName.isValid());
        
        // 测试身份证号为空
        IdCardData invalidId = new IdCardData();
        invalidId.setName("张三");
        invalidId.setIdNumber("");
        assertFalse("身份证号为空应该无效", invalidId.isValid());
        
        // 测试身份证号长度不足
        IdCardData shortId = new IdCardData();
        shortId.setName("张三");
        shortId.setIdNumber("123");
        assertFalse("身份证号长度不足应该无效", shortId.isValid());
    }
    
    /**
     * 测试格式化身份证号
     */
    @Test
    public void testFormattedIdNumber() {
        assertEquals("123456********5678", testIdCardData.getFormattedIdNumber());
        
        // 测试短身份证号
        IdCardData shortId = new IdCardData();
        shortId.setIdNumber("123");
        assertEquals("123", shortId.getFormattedIdNumber());
        
        // 测试null身份证号
        IdCardData nullId = new IdCardData();
        assertNull(nullId.getFormattedIdNumber());
    }
    
    /**
     * 测试toString方法
     */
    @Test
    public void testToString() {
        String result = testIdCardData.toString();
        
        assertNotNull("toString结果不应为null", result);
        assertTrue("应包含姓名", result.contains("张三"));
        assertTrue("应包含格式化的身份证号", result.contains("123456********5678"));
        assertTrue("应包含地址", result.contains("北京市东城区"));
    }
    
    /**
     * 测试equals和hashCode方法
     */
    @Test
    public void testEqualsAndHashCode() {
        IdCardData same = new IdCardData("张三", "123456789012345678", "北京市东城区", 
                                        "男", "19900101", "汉");
        IdCardData different = new IdCardData("李四", "987654321098765432", "上海市浦东新区", 
                                            "女", "19850315", "汉");
        
        // 测试equals
        assertEquals("相同数据应该相等", testIdCardData, same);
        assertNotEquals("不同数据应该不相等", testIdCardData, different);
        assertNotEquals("与null应该不相等", testIdCardData, null);
        assertNotEquals("与不同类型应该不相等", testIdCardData, "string");
        
        // 测试hashCode
        assertEquals("相同对象应该有相同hashCode", testIdCardData.hashCode(), same.hashCode());
    }
    
    /**
     * 测试构造函数
     */
    @Test
    public void testConstructors() {
        // 测试默认构造函数
        IdCardData empty = new IdCardData();
        assertNull("默认构造函数的姓名应为null", empty.getName());
        assertNull("默认构造函数的身份证号应为null", empty.getIdNumber());
        
        // 测试完整构造函数
        IdCardData complete = new IdCardData("测试", "123456789012345678", "测试地址", 
                                           "男", "19900101", "汉");
        assertEquals("测试", complete.getName());
        assertEquals("123456789012345678", complete.getIdNumber());
        assertEquals("测试地址", complete.getAddress());
        assertEquals("男", complete.getGender());
        assertEquals("19900101", complete.getBirthDate());
        assertEquals("汉", complete.getNationality());
    }
    
    /**
     * 集成测试：完整的JSON解析到数据验证流程
     */
    @Test
    public void testCompleteFlow() {
        // 模拟完整的读卡流程
        String completeJson = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"message\": \"读卡成功\",\n" +
                "  \"data\": {\n" +
                "    \"name\": \"赵六\",\n" +
                "    \"idNumber\": \"110101199001011234\",\n" +
                "    \"gender\": \"男\",\n" +
                "    \"birthDate\": \"19900101\",\n" +
                "    \"address\": \"北京市东城区东华门街道\",\n" +
                "    \"nationality\": \"汉\"\n" +
                "  },\n" +
                "  \"timestamp\": \"2024-03-15T10:30:00Z\"\n" +
                "}";
        
        // 解析JSON
        IdCardData result = IdCardData.fromJson(completeJson);
        
        // 验证解析成功
        assertNotNull("解析应该成功", result);
        
        // 验证数据完整性
        assertEquals("赵六", result.getName());
        assertEquals("110101199001011234", result.getIdNumber());
        assertEquals("男", result.getGender());
        assertEquals("19900101", result.getBirthDate());
        assertEquals("北京市东城区东华门街道", result.getAddress());
        assertEquals("汉", result.getNationality());
        
        // 验证数据有效性
        assertTrue("数据应该有效", result.isValid());
        
        // 验证格式化功能
        assertEquals("110101********1234", result.getFormattedIdNumber());
        
        // 验证toString包含关键信息
        String stringResult = result.toString();
        assertTrue("toString应包含姓名", stringResult.contains("赵六"));
        assertTrue("toString应包含地址", stringResult.contains("北京市东城区东华门街道"));
    }
} 