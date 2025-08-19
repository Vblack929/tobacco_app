package com.tobacco.weight.test;

import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.FarmerInfoDao;
import com.tobacco.weight.data.FarmerInfo;

import java.sql.SQLException;
import java.util.List;

/**
 * 测试合同量搜索功能
 */
public class ContractAmountSearchTest {

    public static void main(String[] args) {
        try {
            // 初始化数据库连接
            DatabaseManager.getInstance();

            // 测试合同量搜索
            testContractAmountSearch();

        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试合同量搜索功能
     */
    private static void testContractAmountSearch() throws SQLException {
        FarmerInfoDao dao = new FarmerInfoDao(DatabaseManager.getInstance());

        System.out.println("=== 测试合同量搜索功能 ===");

        // 测试搜索包含"1000"的合同量
        System.out.println("\n1. 搜索合同量包含'1000'的农户:");
        List<FarmerInfo> results1 = dao.searchByNameOrContract("1000");
        System.out.println("找到 " + results1.size() + " 个农户");
        for (FarmerInfo farmer : results1) {
            System.out.println("  - " + farmer.getFarmerName() + " (合同号: " + farmer.getContractNumber() + ")");
        }

        // 测试搜索包含"5000"的合同量
        System.out.println("\n2. 搜索合同量包含'5000'的农户:");
        List<FarmerInfo> results2 = dao.searchByNameOrContract("5000");
        System.out.println("找到 " + results2.size() + " 个农户");
        for (FarmerInfo farmer : results2) {
            System.out.println("  - " + farmer.getFarmerName() + " (合同号: " + farmer.getContractNumber() + ")");
        }

        // 测试搜索包含"张三"的农户
        System.out.println("\n3. 搜索姓名包含'张三'的农户:");
        List<FarmerInfo> results3 = dao.searchByNameOrContract("张三");
        System.out.println("找到 " + results3.size() + " 个农户");
        for (FarmerInfo farmer : results3) {
            System.out.println("  - " + farmer.getFarmerName() + " (合同号: " + farmer.getContractNumber() + ")");
        }

        System.out.println("\n=== 测试完成 ===");
    }
}
