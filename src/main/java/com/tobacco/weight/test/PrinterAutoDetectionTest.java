package com.tobacco.weight.test;

import com.tobacco.weight.hardware.printer.WindowsSerialPrinter;

/**
 * 打印机自动检测测试程序
 * 验证系统能够智能识别开发环境和生产环境
 */
public class PrinterAutoDetectionTest {

    public static void main(String[] args) {
        System.out.println("=== 打印机自动检测测试 ===");

        try {
            // 1. 扫描所有串口
            String[] allPorts = WindowsSerialPrinter.getAvailablePorts();
            System.out.println("\n1. 发现的所有串口 (" + allPorts.length + "个):");
            for (String port : allPorts) {
                System.out.println("   - " + port);
            }

            // 2. 扫描打印机端口
            String[] printerPorts = WindowsSerialPrinter.findPrinterPorts();
            System.out.println("\n2. 可能的打印机端口 (" + printerPorts.length + "个):");
            for (String port : printerPorts) {
                System.out.println("   - " + port);
            }

            // 3. 检测虚拟端口
            boolean hasVirtualPorts = false;
            for (String port : allPorts) {
                if (port.startsWith("CNCA") || port.startsWith("CNCB")) {
                    hasVirtualPorts = true;
                    break;
                }
            }

            System.out.println("\n3. 环境检测结果:");
            System.out.println("   com0com虚拟端口: " + (hasVirtualPorts ? "已安装" : "未安装"));
            System.out.println("   真实打印机端口: " + printerPorts.length + "个");

            // 4. 智能决策
            System.out.println("\n4. 推荐连接策略:");
            if (hasVirtualPorts && printerPorts.length == 0) {
                System.out.println("   🔧 开发环境检测：启动打印机模拟器");
                System.out.println("   📍 建议使用：CNCA0/CNCB0 或 CNCA4/CNCB4 端口对");
            } else if (printerPorts.length > 0) {
                System.out.println("   🖨️  生产环境检测：连接真实打印机");
                System.out.println("   📍 推荐端口顺序：");
                for (int i = 0; i < printerPorts.length; i++) {
                    System.out.println("     " + (i + 1) + ". " + printerPorts[i]);
                }
            } else {
                System.out.println("   ❌ 未检测到可用的打印机端口或虚拟端口");
                System.out.println("   💡 建议：安装com0com用于开发测试");
            }

            // 5. 测试连接示例
            if (printerPorts.length > 0) {
                System.out.println("\n5. 连接测试示例:");
                WindowsSerialPrinter printer = new WindowsSerialPrinter();

                // 推荐的波特率顺序
                int[] baudRates = { 115200, 9600, 19200, 38400, 57600 };

                System.out.println("   将尝试以下连接方式:");
                for (String port : printerPorts) {
                    for (int baudRate : baudRates) {
                        System.out.println("   - " + port + " @ " + baudRate + " baud");
                    }
                }

                printer.close();
            }

            System.out.println("\n✅ 检测完成");

        } catch (Exception e) {
            System.out.println("❌ 检测失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}