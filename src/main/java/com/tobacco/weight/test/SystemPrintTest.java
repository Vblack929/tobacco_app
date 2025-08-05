package com.tobacco.weight.test;

import com.tobacco.weight.hardware.PrinterManager;

/**
 * 系统打印功能测试
 * 验证Windows系统打印服务是否能正确工作
 */
public class SystemPrintTest {

    public static void main(String[] args) {
        System.out.println("=== 系统打印功能测试 ===");

        try {
            // 1. 扫描系统打印机
            javax.print.PrintService[] services = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
            System.out.println("\n1. 发现的系统打印机 (" + services.length + "个):");

            boolean foundPOS = false;
            for (int i = 0; i < services.length; i++) {
                String name = services[i].getName();
                String status = "";

                if (name.toLowerCase().contains("pos")) {
                    status = " [POS打印机]";
                    foundPOS = true;
                }

                System.out.println("   " + (i + 1) + ". " + name + status);
            }

            // 2. 检查默认打印机
            javax.print.PrintService defaultService = javax.print.PrintServiceLookup.lookupDefaultPrintService();
            if (defaultService != null) {
                System.out.println("\n2. 默认打印机: " + defaultService.getName());
            } else {
                System.out.println("\n2. 未设置默认打印机");
            }

            // 3. POS打印机检测结果
            System.out.println("\n3. POS打印机检测:");
            if (foundPOS) {
                System.out.println("   ✅ 检测到POS打印机，系统打印功能可用");
            } else {
                System.out.println("   ⚠️  未检测到POS打印机，但仍可测试系统打印");
            }

            // 4. 执行测试打印
            if (services.length > 0) {
                System.out.println("\n4. 执行系统打印测试:");
                PrinterManager printerManager = new PrinterManager();

                boolean result = printerManager.testPrinter();
                if (result) {
                    System.out.println("   ✅ 系统打印测试成功");
                    System.out.println("   💡 请检查打印机输出");
                } else {
                    System.out.println("   ❌ 系统打印测试失败");
                }
            } else {
                System.out.println("\n4. 无可用打印机，跳过测试");
            }

            System.out.println("\n✅ 测试完成");

        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}