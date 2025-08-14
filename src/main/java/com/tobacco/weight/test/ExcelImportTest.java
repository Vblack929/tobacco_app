package com.tobacco.weight.test;

import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.service.FarmerImportService;

import java.io.File;

/**
 * Excel导入功能测试类
 * 用于测试农户合同Excel导入功能
 */
public class ExcelImportTest {
    
    public static void main(String[] args) {
        System.out.println("=== Excel导入功能测试 ===");
        
        // 初始化数据库
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        FarmerImportService importService = new FarmerImportService(databaseManager);
        
        // 示例Excel文件路径（需要用户提供实际文件）
        String[] possibleFiles = {"农户合同信息.xlsx", "农户合同信息.xls", "test.xlsx", "test.xls"};
        File excelFile = null;
        
        // 查找可用的Excel文件
        for (String fileName : possibleFiles) {
            File testFile = new File(fileName);
            if (testFile.exists()) {
                excelFile = testFile;
                System.out.println("找到Excel文件: " + fileName);
                break;
            }
        }
        
        if (excelFile == null) {
            System.out.println("请将Excel文件放在项目根目录下，支持的文件名:");
            for (String fileName : possibleFiles) {
                System.out.println("  - " + fileName);
            }
            System.out.println("\nExcel文件格式要求:");
            System.out.println("支持格式: .xlsx (Office 2007+) 和 .xls (Office 97-2003)");
            System.out.println("必需列头:");
            System.out.println("  - 站点名称");
            System.out.println("  - 姓名");
            System.out.println("  - 种植者编号（合同号）");
            System.out.println("  - 身份证号");
            System.out.println("  - 行政区划 (如: 永安镇督正村)");
            System.out.println("  - 合同量");
            return;
        }
        
        try {
            // 测试导入（注意：这是读取测试，不会写入数据库）
            System.out.println("测试导入功能...");
            FarmerImportService.ImportResult result;
            
            try {
                // 首先尝试无密码
                System.out.println("尝试无密码方式打开文件...");
                result = importService.importFromExcel(excelFile, null);
                System.out.println("无密码方式成功");
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                System.out.println("无密码方式失败，错误信息: " + errorMsg);
                if (errorMsg != null && (
                    errorMsg.contains("OLE2") || 
                    errorMsg.contains("encrypted") || 
                    errorMsg.contains("Encrypted") ||
                    errorMsg.contains("decrypted") ||
                    errorMsg.contains("需要提供密码") ||
                    (errorMsg.contains("XSSF") && errorMsg.contains("HSSF")))) {
                    System.out.println("检测到加密文件，尝试使用密码 0807...");
                    // 使用密码 0807
                    result = importService.importFromExcel(excelFile, null, "0807");
                    System.out.println("密码方式成功");
                } else {
                    System.out.println("未检测到加密错误，抛出异常");
                    throw e;
                }
            }
            
            System.out.println("\n=== 测试结果 ===");
            System.out.println(result.getSummary());
            
            System.out.println("总记录数: " + result.getTotalRows());
            System.out.println("成功处理: " + result.getSuccessCount());
            System.out.println("失败记录: " + result.getFailureCount());
            System.out.println("重复记录: " + result.getDuplicateCount());
            
            if (!result.getErrorMessages().isEmpty()) {
                System.out.println("\n=== 错误详情 ===");
                for (int i = 0; i < result.getErrorMessages().size(); i++) {
                    System.out.println((i + 1) + ". " + result.getErrorMessages().get(i));
                }
            }
            
            // 提示
            if (result.getErrorMessages().isEmpty()) {
                System.out.println("\n✅ 测试成功！Excel文件格式正确，可以进行导入。");
                System.out.println("注意：这只是解析测试，实际导入请使用UI界面。");
            } else {
                System.out.println("\n❌ 发现问题，请修复Excel文件后重试。");
            }
            
        } catch (Exception e) {
            System.err.println("导入测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
}
