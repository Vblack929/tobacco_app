package com.tobacco.weight.test;

import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.service.FarmerImportService;

import java.io.File;

/**
 * 专门用于调试加密Excel文件导入的测试类
 */
public class EncryptedExcelDebugTest {
    
    public static void main(String[] args) {
        System.out.println("=== 加密Excel调试测试 ===");
        
        if (args.length == 0) {
            System.out.println("用法: java EncryptedExcelDebugTest <excel文件路径>");
            System.out.println("示例: java EncryptedExcelDebugTest farmerInfo.xlsx");
            return;
        }
        
        String filePath = args[0];
        File excelFile = new File(filePath);
        
        if (!excelFile.exists()) {
            System.out.println("文件不存在: " + filePath);
            return;
        }
        
        System.out.println("调试文件: " + excelFile.getAbsolutePath());
        System.out.println("文件大小: " + excelFile.length() + " 字节");
        
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        FarmerImportService importService = new FarmerImportService(databaseManager);
        
        // 测试步骤1: 尝试无密码
        System.out.println("\n=== 步骤1: 尝试无密码导入 ===");
        try {
            FarmerImportService.ImportResult result1 = importService.importFromExcel(excelFile, null);
            System.out.println("✅ 无密码导入成功!");
            System.out.println("记录数: " + result1.getTotalRows());
            System.out.println("成功数: " + result1.getSuccessCount());
            System.out.println("错误数: " + result1.getFailureCount());
            if (result1.getFailureCount() > 0) {
                System.out.println("错误详情:");
                for (String error : result1.getErrorMessages()) {
                    System.out.println("  - " + error);
                }
            }
            return; // 如果无密码成功，直接返回
        } catch (Exception e) {
            System.out.println("❌ 无密码导入失败");
            System.out.println("异常类型: " + e.getClass().getSimpleName());
            System.out.println("错误信息: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("原因: " + e.getCause().getMessage());
            }
        }
        
        // 测试步骤2: 尝试使用密码 0807
        System.out.println("\n=== 步骤2: 尝试密码 0807 ===");
        try {
            FarmerImportService.ImportResult result2 = importService.importFromExcel(excelFile, null, "0807");
            System.out.println("✅ 密码导入成功!");
            System.out.println("记录数: " + result2.getTotalRows());
            System.out.println("成功数: " + result2.getSuccessCount());
            System.out.println("失败数: " + result2.getFailureCount());
            System.out.println("重复数: " + result2.getDuplicateCount());
            
            if (result2.getFailureCount() > 0) {
                System.out.println("\n错误详情:");
                for (String error : result2.getErrorMessages()) {
                    System.out.println("  - " + error);
                }
            } else {
                System.out.println("\n✅ 所有记录处理成功!");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 密码导入也失败");
            System.out.println("异常类型: " + e.getClass().getSimpleName());
            System.out.println("错误信息: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("原因: " + e.getCause().getMessage());
            }
            e.printStackTrace();
        }
        
        // 测试步骤3: 尝试其他常见密码
        String[] commonPasswords = {"", "123456", "password", "admin", "0000", "1234"};
        System.out.println("\n=== 步骤3: 尝试其他常见密码 ===");
        for (String pwd : commonPasswords) {
            try {
                System.out.println("尝试密码: '" + pwd + "'");
                FarmerImportService.ImportResult result3 = importService.importFromExcel(excelFile, null, pwd);
                System.out.println("✅ 密码 '" + pwd + "' 成功! 记录数: " + result3.getTotalRows());
                break;
            } catch (Exception e) {
                System.out.println("❌ 密码 '" + pwd + "' 失败: " + e.getMessage());
            }
        }
        
        System.out.println("\n=== 调试完成 ===");
    }
}
