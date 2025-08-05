package com.tobacco.weight.test;

import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.hardware.PrinterManager;

import java.util.Date;

/**
 * 打印标签功能测试
 * 测试从称重记录打印标签的功能
 */
public class PrintLabelTest {

    public static void main(String[] args) {
        System.out.println("🖨️ 测试打印标签功能...");
        
        try {
            // 创建测试用的称重记录
            WeighingRecord testRecord = new WeighingRecord();
            testRecord.setPrecheckId("YJ100000001");
            testRecord.setFarmerName("张三");
            testRecord.setContractNumber("2024001");
            testRecord.setLeafType("上部叶");
            testRecord.setWeight(25.5);
            testRecord.setOperator("测试员");
            testRecord.setBundleCount(5);
            testRecord.setTimestamp(new Date());
            testRecord.setStatus("正常");

            // 初始化打印机管理器
            PrinterManager printerManager = new PrinterManager();

            // 测试打印功能
            System.out.println("📋 测试记录信息:");
            System.out.println("预检编号: " + testRecord.getPrecheckId());
            System.out.println("农户姓名: " + testRecord.getFarmerName());
            System.out.println("合同号: " + testRecord.getContractNumber());
            System.out.println("部叶类型: " + testRecord.getLeafType());
            System.out.println("重量: " + testRecord.getWeight() + "kg");
            System.out.println("捆数: " + testRecord.getBundleCount());

            System.out.println("\n🖨️ 开始打印测试...");
            
            boolean printSuccess = printerManager.printWeighingReceipt(
                testRecord.getFarmerName(),
                testRecord.getContractNumber(),
                testRecord.getLeafType(),
                testRecord.getWeight(),
                testRecord.getOperator(),
                testRecord.getBundleCount(),
                testRecord.getPrecheckId()
            );

            if (printSuccess) {
                System.out.println("✅ 打印测试成功！");
            } else {
                System.out.println("❌ 打印测试失败 - 请检查打印机连接");
            }

        } catch (Exception e) {
            System.err.println("❌ 测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}