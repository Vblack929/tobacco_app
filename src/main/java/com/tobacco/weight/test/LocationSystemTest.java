package com.tobacco.weight.test;

import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.database.DatabaseManager;

import java.util.List;

/**
 * 地区系统测试类
 * 用于验证数据库地区系统是否正常工作
 */
public class LocationSystemTest {

    public static void main(String[] args) {
        System.out.println("开始测试地区系统...");
        
        try {
            // 测试基本功能
            testBasicFunctions();
            
            // 测试地址解析
            testAddressParsing();
            
            System.out.println("✅ 地区系统测试通过！");
            
        } catch (Exception e) {
            System.err.println("❌ 地区系统测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试基本功能
     */
    private static void testBasicFunctions() {
        System.out.println("\n🔍 测试基本功能...");
        
        // 测试获取所有乡镇
        List<String> townships = LocationData.getAllTownships();
        System.out.println("乡镇数量: " + townships.size());
        if (!townships.isEmpty()) {
            System.out.println("第一个乡镇: " + townships.get(0));
        }
        
        // 测试获取村庄
        if (!townships.isEmpty()) {
            String firstTownship = townships.get(0);
            List<String> villages = LocationData.getVillagesByTownship(firstTownship);
            System.out.println(firstTownship + " 下的村庄数量: " + villages.size());
            if (!villages.isEmpty()) {
                System.out.println("第一个村庄: " + villages.get(0));
            }
        }
        
        // 测试验证功能
        if (!townships.isEmpty()) {
            String firstTownship = townships.get(0);
            boolean isValid = LocationData.isTownshipValid(firstTownship);
            System.out.println(firstTownship + " 是否有效: " + isValid);
        }
    }

    /**
     * 测试地址解析
     */
    private static void testAddressParsing() {
        System.out.println("\n📍 测试地址解析...");
        
        // 测试不同的地址格式
        String[] testAddresses = {
            "永安镇 永和村",
            "沙市 长春",
            "社港 合盛",
            "北盛 百塘",
            "永安镇",
            "未知地址"
        };
        
        for (String address : testAddresses) {
            LocationData.LocationInfo locationInfo = LocationData.parseAddress(address);
            System.out.println(String.format("地址: '%s' -> 乡镇: '%s', 村庄: '%s'", 
                address, locationInfo.getTownship(), locationInfo.getVillage()));
        }
    }
}