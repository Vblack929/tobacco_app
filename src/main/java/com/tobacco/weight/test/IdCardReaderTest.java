package com.tobacco.weight.test;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.hardware.IdCardReader;
import com.tobacco.weight.hardware.IdCardReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 身份证读卡器测试类
 * 演示如何使用模拟服务器进行测试
 */
public class IdCardReaderTest {

    private static final Logger logger = LoggerFactory.getLogger(IdCardReaderTest.class);

    public static void main(String[] args) {
        MockIdCardReaderServer mockServer = new MockIdCardReaderServer();
        IdCardReader idCardReader = new IdCardReader();
        
        try {
            // 启动模拟服务器
            logger.info("=== 启动身份证读卡器模拟服务器 ===");
            mockServer.start();
            Thread.sleep(1000); // 等待服务器启动
            
            // 设置回调
            idCardReader.setOnConnectionStatusChanged(connected -> {
                logger.info("连接状态变化: {}", connected ? "已连接" : "已断开");
            });
            
            idCardReader.setOnIdCardRead(farmerInfo -> {
                if (farmerInfo != null) {
                    logger.info("=== 身份证读取成功 ===");
                    logger.info("姓名: {}", farmerInfo.getFarmerName());
                    logger.info("身份证号: {}", farmerInfo.getIdCardNumber());
                    logger.info("性别: {}", farmerInfo.getGender());
                    logger.info("民族: {}", farmerInfo.getNationality());
                    logger.info("出生日期: {}", farmerInfo.getBirthDate());
                    logger.info("地址: {}", farmerInfo.getAddress());
                    logger.info("签发机关: {}", farmerInfo.getDepartment());
                    logger.info("有效期: {} - {}", farmerInfo.getStartDate(), farmerInfo.getEndDate());
                    logger.info("合同号: {}", farmerInfo.getContractNumber());
                    
                    if (farmerInfo.getPhoto() != null) {
                        logger.info("照片数据大小: {} 字节", farmerInfo.getPhoto().length);
                    }
                } else {
                    logger.error("身份证读取失败");
                }
            });
            
            idCardReader.setOnErrorOccurred(error -> {
                logger.error("读卡器错误: {}", error);
            });
            
            // 测试连接
            logger.info("=== 测试连接身份证读卡器 ===");
            boolean connected = idCardReader.connect();
            if (connected) {
                logger.info("连接成功！");
                
                // 测试读卡器
                logger.info("=== 测试读卡器功能 ===");
                boolean testResult = idCardReader.testReader();
                logger.info("读卡器测试结果: {}", testResult ? "通过" : "失败");
                
                // 读取SAM ID
                logger.info("=== 读取SAM ID ===");
                String samId = idCardReader.readSamId();
                logger.info("SAM ID: {}", samId);
                
                // 读取身份证物理卡号
                logger.info("=== 读取身份证物理卡号 ===");
                String cardNumber = idCardReader.readCardNumber();
                logger.info("物理卡号: {}", cardNumber);
                
                // 模拟多次读卡
                logger.info("=== 模拟读取身份证 ===");
                for (int i = 1; i <= 3; i++) {
                    logger.info("第 {} 次读卡:", i);
                    idCardReader.readIdCard();
                    Thread.sleep(2000); // 等待读卡完成
                }
                
                // 断开连接
                logger.info("=== 断开连接 ===");
                idCardReader.disconnect();
                
            } else {
                logger.error("连接失败！错误信息: {}", idCardReader.getDetailedErrorMessage());
            }
            
        } catch (Exception e) {
            logger.error("测试过程中发生异常", e);
        } finally {
            // 停止模拟服务器
            logger.info("=== 停止模拟服务器 ===");
            mockServer.stop();
        }
    }
}
