package com.tobacco.weight.test;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.hardware.IdCardReader;
import com.tobacco.weight.hardware.IdCardReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 身份证读卡器使用示例
 * 展示如何在实际应用中集成身份证读卡器功能
 */
public class IdCardReaderExample {

    private static final Logger logger = LoggerFactory.getLogger(IdCardReaderExample.class);

    public static void main(String[] args) {
        demonstrateUsage();
    }

    /**
     * 演示不同的使用方式
     */
    public static void demonstrateUsage() {
        logger.info("=== 身份证读卡器使用示例 ===");

        // 方式1: 直接指定使用模拟设备
        demonstrateMethod1();

        // 方式2: 通过系统属性控制
        demonstrateMethod2();

        // 方式3: 智能检测并自动回退
        demonstrateMethod3();
    }

    /**
     * 方式1: 直接指定使用模拟设备
     */
    private static void demonstrateMethod1() {
        logger.info("\n--- 方式1: 直接指定使用模拟设备 ---");
        
        IdCardReader reader = IdCardReaderFactory.createIdCardReader(true); // true = 使用模拟设备
        
        setupCallbacks(reader);
        
        if (reader.connect()) {
            reader.readIdCard();
            sleep(1000);
            reader.disconnect();
        }
        
        IdCardReaderFactory.stopMockServer();
    }

    /**
     * 方式2: 通过系统属性控制
     */
    private static void demonstrateMethod2() {
        logger.info("\n--- 方式2: 通过系统属性控制 ---");
        
        // 设置系统属性
        System.setProperty("tobacco.idcard.mock", "true");
        
        IdCardReader reader = IdCardReaderFactory.createAutoIdCardReader();
        
        setupCallbacks(reader);
        
        if (reader.connect()) {
            reader.readIdCard();
            sleep(1000);
            reader.disconnect();
        }
        
        IdCardReaderFactory.stopMockServer();
    }

    /**
     * 方式3: 智能检测并自动回退
     */
    private static void demonstrateMethod3() {
        logger.info("\n--- 方式3: 智能检测并自动回退 ---");
        
        IdCardReader reader = IdCardReaderFactory.createSmartIdCardReader();
        
        setupCallbacks(reader);
        
        if (reader.connect()) {
            logger.info("使用模式: {}", IdCardReaderFactory.isMockMode() ? "模拟设备" : "真实设备");
            reader.readIdCard();
            sleep(1000);
            reader.disconnect();
        }
        
        IdCardReaderFactory.stopMockServer();
    }

    /**
     * 设置回调函数
     */
    private static void setupCallbacks(IdCardReader reader) {
        reader.setOnConnectionStatusChanged(connected -> {
            logger.info("连接状态: {}", connected ? "已连接" : "已断开");
        });

        reader.setOnIdCardRead(farmerInfo -> {
            if (farmerInfo != null) {
                displayFarmerInfo(farmerInfo);
            } else {
                logger.error("身份证读取失败");
            }
        });

        reader.setOnErrorOccurred(error -> {
            logger.error("读卡器错误: {}", error);
        });
    }

    /**
     * 显示农户信息
     */
    private static void displayFarmerInfo(FarmerInfo farmerInfo) {
        logger.info("=== 读取到农户信息 ===");
        logger.info("姓名: {}", farmerInfo.getFarmerName());
        logger.info("身份证号: {}", maskIdNumber(farmerInfo.getIdCardNumber()));
        logger.info("性别: {}", farmerInfo.getGender());
        logger.info("民族: {}", farmerInfo.getNationality());
        logger.info("出生日期: {}", farmerInfo.getBirthDate());
        logger.info("地址: {}", farmerInfo.getAddress());
        logger.info("签发机关: {}", farmerInfo.getDepartment());
        logger.info("有效期: {} 至 {}", farmerInfo.getStartDate(), farmerInfo.getEndDate());
        logger.info("合同号: {}", farmerInfo.getContractNumber());
        
        if (farmerInfo.getPhoto() != null) {
            logger.info("照片数据: {} 字节", farmerInfo.getPhoto().length);
        }
    }

    /**
     * 脱敏显示身份证号
     */
    private static String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 10) {
            return idNumber;
        }
        return idNumber.substring(0, 6) + "****" + idNumber.substring(idNumber.length() - 4);
    }

    /**
     * 简单的睡眠方法
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
