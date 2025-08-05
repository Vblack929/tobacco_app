package com.tobacco.weight.test;

import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.LocationInfoDao;

/**
 * 数据库地区数据测试
 * 验证地区数据是否正确自动初始化到数据库
 */
public class DatabaseLocationTest {

    public static void main(String[] args) {
        System.out.println("🔍 测试数据库地区数据自动初始化...");
        
        try {
            // 初始化数据库（会自动创建表和初始化数据）
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            LocationInfoDao dao = new LocationInfoDao(databaseManager);
            
            // 检查数据库中的数据
            System.out.println("\n📊 数据库统计:");
            int totalRecords = dao.getCount();
            System.out.println("总记录数: " + totalRecords);
            
            // 检查乡镇数据
            var townships = dao.findAllTownships();
            System.out.println("乡镇数量: " + townships.size());
            
            if (!townships.isEmpty()) {
                System.out.println("第一个乡镇: " + townships.get(0).getTownshipName());
                
                // 检查第一个乡镇的村庄
                String firstTownship = townships.get(0).getTownshipName();
                var villages = dao.findVillagesByTownship(firstTownship);
                System.out.println(firstTownship + " 的村庄数量: " + villages.size());
                
                if (!villages.isEmpty()) {
                    System.out.println("第一个村庄: " + villages.get(0).getVillageName());
                }
            }
            
            // 测试LocationData API
            System.out.println("\n🔧 测试LocationData API:");
            var allTownships = LocationData.getAllTownships();
            System.out.println("通过API获取的乡镇数量: " + allTownships.size());
            
            if (!allTownships.isEmpty()) {
                String firstTownshipName = allTownships.get(0);
                var townshipVillages = LocationData.getVillagesByTownship(firstTownshipName);
                System.out.println(firstTownshipName + " 通过API获取的村庄数量: " + townshipVillages.size());
            }
            
            // 测试地址解析
            System.out.println("\n📍 测试地址解析:");
            var locationInfo = LocationData.parseAddress("永安镇 永和村");
            System.out.println("解析 '永安镇 永和村' -> 乡镇: " + locationInfo.getTownship() + ", 村庄: " + locationInfo.getVillage());
            
            System.out.println("\n✅ 数据库地区数据测试完成！");
            
            // 关闭数据库连接
            databaseManager.closeConnection();
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}