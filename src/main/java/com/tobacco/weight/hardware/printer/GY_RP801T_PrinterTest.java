package com.tobacco.weight.hardware.printer;

import com.tobacco.weight.hardware.printer.esc.EscBuilder;
import com.tobacco.weight.hardware.printer.esc.LabelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * GY-RP801T打印机连接测试程序
 * 专门用于验证GY-RP801T打印机连接和打印功能
 */
public class GY_RP801T_PrinterTest {

    private static final Logger logger = LoggerFactory.getLogger(GY_RP801T_PrinterTest.class);
    private static final Scanner scanner = new Scanner(System.in);

    private static WindowsSerialPrinter serialPrinter;
    private static PrinterSimulator simulator;
    private static boolean useSimulator = false;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    GY-RP801T打印机连接验证程序");
        System.out.println("========================================");
        System.out.println();

        try {
            // 初始化
            serialPrinter = new WindowsSerialPrinter();
            simulator = new PrinterSimulator();

            // 显示主菜单
            showMainMenu();

        } catch (Exception e) {
            System.out.println("❌ 程序运行错误: " + e.getMessage());
            logger.error("程序运行错误", e);
        } finally {
            cleanup();
        }
    }

    /**
     * 显示主菜单
     */
    private static void showMainMenu() {
        while (true) {
            System.out.println("\n=== 主菜单 ===");
            System.out.println("1. 查找系统打印机");
            System.out.println("2. 扫描串口设备");
            System.out.println("3. 连接串口打印机");
            System.out.println("4. 启动打印机模拟器");
            System.out.println("5. 测试打印功能");
            System.out.println("6. 打印机状态");
            System.out.println("7. 烟叶称重测试");
            System.out.println("8. 断开连接");
            System.out.println("0. 退出程序");
            System.out.print("\n请选择操作 (0-8): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    findSystemPrinters();
                    break;
                case "2":
                    scanSerialPorts();
                    break;
                case "3":
                    connectSerialPrinter();
                    break;
                case "4":
                    startSimulator();
                    break;
                case "5":
                    testPrintFunctions();
                    break;
                case "6":
                    showPrinterStatus();
                    break;
                case "7":
                    tobaccoWeighingTest();
                    break;
                case "8":
                    disconnectAll();
                    break;
                case "0":
                    System.out.println("退出程序...");
                    return;
                default:
                    System.out.println("❌ 无效选择，请重新输入");
            }
        }
    }

    /**
     * 查找系统打印机
     */
    private static void findSystemPrinters() {
        System.out.println("\n=== 查找系统打印机 ===");

        try {
            PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);

            if (services.length == 0) {
                System.out.println("❌ 未找到任何系统打印机");
                return;
            }

            System.out.println("发现 " + services.length + " 个系统打印机:");

            boolean foundGY = false;
            for (int i = 0; i < services.length; i++) {
                String name = services[i].getName();
                System.out.println((i + 1) + ". " + name);

                // 检查是否是GY-RP801T
                if (name.toLowerCase().contains("gy") ||
                        name.toLowerCase().contains("rp801") ||
                        name.toLowerCase().contains("801")) {
                    System.out.println("   ⭐ 可能是GY-RP801T打印机");
                    foundGY = true;
                }
            }

            if (foundGY) {
                System.out.println("\n✅ 找到可能的GY-RP801T打印机");
            } else {
                System.out.println("\n⚠️  未找到GY-RP801T打印机，可能需要安装驱动或使用串口连接");
            }

        } catch (Exception e) {
            System.out.println("❌ 查找打印机失败: " + e.getMessage());
            logger.error("查找打印机失败", e);
        }
    }

    /**
     * 扫描串口设备
     */
    private static void scanSerialPorts() {
        System.out.println("\n=== 扫描串口设备 ===");

        try {
            String[] allPorts = WindowsSerialPrinter.getAvailablePorts();
            String[] printerPorts = WindowsSerialPrinter.findPrinterPorts();

            System.out.println("所有可用串口 (" + allPorts.length + "个):");
            for (String port : allPorts) {
                System.out.println("  - " + port);
            }

            System.out.println("\n可能的打印机串口 (" + printerPorts.length + "个):");
            if (printerPorts.length > 0) {
                for (String port : printerPorts) {
                    System.out.println("  ⭐ " + port + " (推荐尝试)");
                }
            } else {
                System.out.println("  (未找到明显的打印机串口)");
            }

            // 检查com0com虚拟串口
            boolean foundCom0com = false;
            for (String port : allPorts) {
                if (port.toUpperCase().startsWith("CNC")) {
                    foundCom0com = true;
                    break;
                }
            }

            if (foundCom0com) {
                System.out.println("\n✅ 发现com0com虚拟串口，可以使用模拟器测试");
            } else {
                System.out.println("\n💡 提示: 安装com0com可以创建虚拟串口用于测试");
            }

        } catch (Exception e) {
            System.out.println("❌ 扫描串口失败: " + e.getMessage());
            logger.error("扫描串口失败", e);
        }
    }

    /**
     * 连接串口打印机
     */
    private static void connectSerialPrinter() {
        System.out.println("\n=== 连接串口打印机 ===");

        if (serialPrinter.isConnected()) {
            System.out.println("⚠️  打印机已连接: " + serialPrinter.getPortName());
            System.out.print("是否重新连接? (y/N): ");
            if (!scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                return;
            }
            serialPrinter.close();
        }

        try {
            // 显示可用端口
            String[] ports = WindowsSerialPrinter.getAvailablePorts();
            if (ports.length == 0) {
                System.out.println("❌ 没有可用的串口");
                return;
            }

            System.out.println("可用串口:");
            for (int i = 0; i < ports.length; i++) {
                System.out.println((i + 1) + ". " + ports[i]);
            }

            System.out.print("\n选择串口 (1-" + ports.length + ") 或直接输入端口名: ");
            String input = scanner.nextLine().trim();

            String selectedPort;
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < ports.length) {
                    selectedPort = ports[index];
                } else {
                    System.out.println("❌ 无效选择");
                    return;
                }
            } catch (NumberFormatException e) {
                selectedPort = input.toUpperCase();
            }

            // 选择波特率
            System.out.print("选择波特率 (1=9600, 2=115200, 默认=115200): ");
            String baudInput = scanner.nextLine().trim();
            int baudRate = 115200;
            if ("1".equals(baudInput)) {
                baudRate = 9600;
            }

            System.out.println("正在连接到 " + selectedPort + " (波特率: " + baudRate + ")...");

            if (serialPrinter.open(selectedPort, baudRate)) {
                System.out.println("✅ 成功连接到串口打印机");
                System.out.println("   端口信息: " + serialPrinter.getPortInfo());

                // 测试连接
                if (serialPrinter.testConnection()) {
                    System.out.println("✅ 打印机响应正常");
                } else {
                    System.out.println("⚠️  打印机无响应，但连接已建立");
                }

                useSimulator = false;
            } else {
                System.out.println("❌ 连接失败");
            }

        } catch (Exception e) {
            System.out.println("❌ 连接过程出错: " + e.getMessage());
            logger.error("连接串口打印机失败", e);
        }
    }

    /**
     * 启动打印机模拟器
     */
    private static void startSimulator() {
        System.out.println("\n=== 启动打印机模拟器 ===");

        if (simulator.isRunning()) {
            System.out.println("⚠️  模拟器已在运行: " + simulator.getPortName());
            return;
        }

        try {
            // 查找com0com端口
            String[] ports = WindowsSerialPrinter.getAvailablePorts();
            String[] com0comPorts = java.util.Arrays.stream(ports)
                    .filter(p -> p.toUpperCase().startsWith("CNC"))
                    .toArray(String[]::new);

            if (com0comPorts.length == 0) {
                System.out.println("❌ 未找到com0com虚拟串口");
                System.out.println("请先安装com0com并创建虚拟串口对 (如CNCA0<->CNCB0)");
                return;
            }

            System.out.println("发现com0com端口:");
            for (int i = 0; i < com0comPorts.length; i++) {
                System.out.println((i + 1) + ". " + com0comPorts[i]);
            }

            System.out.print("选择模拟器监听端口 (1-" + com0comPorts.length + "): ");
            String input = scanner.nextLine().trim();

            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < com0comPorts.length) {
                    String selectedPort = com0comPorts[index];

                    if (simulator.start(selectedPort)) {
                        System.out.println("✅ 打印机模拟器已启动");
                        System.out.println("   监听端口: " + selectedPort);

                        // 推荐连接端口
                        String pairPort = selectedPort.replace("CNCB", "CNCA").replace("CNCA", "CNCB");
                        System.out.println("💡 建议连接到: " + pairPort);

                        useSimulator = true;
                    } else {
                        System.out.println("❌ 启动模拟器失败");
                    }
                } else {
                    System.out.println("❌ 无效选择");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ 无效输入");
            }

        } catch (Exception e) {
            System.out.println("❌ 启动模拟器失败: " + e.getMessage());
            logger.error("启动模拟器失败", e);
        }
    }

    /**
     * 测试打印功能
     */
    private static void testPrintFunctions() {
        System.out.println("\n=== 测试打印功能 ===");

        if (!serialPrinter.isConnected()) {
            System.out.println("❌ 请先连接打印机或启动模拟器");
            return;
        }

        while (true) {
            System.out.println("\n选择测试类型:");
            System.out.println("1. 基本文本打印");
            System.out.println("2. 格式化文本测试");
            System.out.println("3. 条形码测试");
            System.out.println("4. 二维码测试");
            System.out.println("5. 标签打印测试");
            System.out.println("6. ESC/POS指令测试");
            System.out.println("0. 返回主菜单");
            System.out.print("请选择 (0-6): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    testBasicPrint();
                    break;
                case "2":
                    testFormattedPrint();
                    break;
                case "3":
                    testBarcodePrint();
                    break;
                case "4":
                    testQRCodePrint();
                    break;
                case "5":
                    testLabelPrint();
                    break;
                case "6":
                    testEscPosPrint();
                    break;
                case "0":
                    return;
                default:
                    System.out.println("❌ 无效选择");
            }
        }
    }

    /**
     * 基本文本打印测试
     */
    private static void testBasicPrint() {
        System.out.println("\n--- 基本文本打印测试 ---");

        String testText = "打印机连接测试\n" +
                "Test Printer Connection\n" +
                "时间: " + new Date() + "\n" +
                "状态: 正常\n";

        if (serialPrinter.write(testText)) {
            System.out.println("✅ 文本发送成功");
        } else {
            System.out.println("❌ 文本发送失败");
        }
    }

    /**
     * 格式化文本测试
     */
    private static void testFormattedPrint() {
        System.out.println("\n--- 格式化文本测试 ---");

        EscBuilder esc = new EscBuilder();
        byte[] commands = esc.reset()
                .align(1).bold(true).fontSize(2, 2)
                .text("烟叶称重系统").feed(2)
                .reset().align(0)
                .text("测试项目:").feed()
                .text("1. 字体大小测试").feed()
                .fontSize(1, 2).text("  - 高度2倍").feed()
                .fontSize(2, 1).text("  - 宽度2倍").feed()
                .reset()
                .text("2. 格式测试").feed()
                .bold(true).text("  - 粗体文本").feed()
                .reset().underline(true).text("  - 下划线文本").feed()
                .reset()
                .separator('=', 32)
                .align(1).text("测试完成").feed(3)
                .build();

        if (serialPrinter.write(commands)) {
            System.out.println("✅ 格式化文本发送成功");
        } else {
            System.out.println("❌ 格式化文本发送失败");
        }
    }

    /**
     * 条形码测试
     */
    private static void testBarcodePrint() {
        System.out.println("\n--- 条形码测试 ---");

        System.out.print("输入条形码内容 (默认: 1234567890): ");
        String barcodeData = scanner.nextLine().trim();
        if (barcodeData.isEmpty()) {
            barcodeData = "1234567890";
        }

        EscBuilder esc = new EscBuilder();
        byte[] commands = esc.reset()
                .align(1).text("条形码测试").feed(2)
                .barcode(0, barcodeData) // Code128
                .feed(2)
                .align(1).text("条形码: " + barcodeData).feed(3)
                .build();

        if (serialPrinter.write(commands)) {
            System.out.println("✅ 条形码发送成功");
        } else {
            System.out.println("❌ 条形码发送失败");
        }
    }

    /**
     * 二维码测试
     */
    private static void testQRCodePrint() {
        System.out.println("\n--- 二维码测试 ---");

        System.out.print("输入二维码内容 (默认: GY-RP801T测试): ");
        String qrData = scanner.nextLine().trim();
        if (qrData.isEmpty()) {
            qrData = "GY-RP801T测试";
        }

        EscBuilder esc = new EscBuilder();
        byte[] commands = esc.reset()
                .align(1).text("二维码测试").feed(2)
                .qrCode(qrData)
                .feed(2)
                .align(1).text("内容: " + qrData).feed(3)
                .build();

        if (serialPrinter.write(commands)) {
            System.out.println("✅ 二维码发送成功");
        } else {
            System.out.println("❌ 二维码发送失败");
        }
    }

    /**
     * 标签打印测试
     */
    private static void testLabelPrint() {
        System.out.println("\n--- 标签打印测试 ---");

        LabelBuilder label = new LabelBuilder();
        byte[] commands = label.size(80, 60)
                .clear()
                .text(10, 10, "3", 0, 2, 2, "测试标签")
                .text(10, 40, "GY-RP801T打印机")
                .text(10, 60, "时间: " + new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                .barcode(10, 80, "TEST123456")
                .box(5, 5, 70, 110)
                .print()
                .build();

        if (serialPrinter.write(commands)) {
            System.out.println("✅ 标签指令发送成功");
        } else {
            System.out.println("❌ 标签指令发送失败");
        }
    }

    /**
     * ESC/POS指令测试
     */
    private static void testEscPosPrint() {
        System.out.println("\n--- ESC/POS指令测试 ---");

        EscBuilder esc = new EscBuilder();
        byte[] commands = esc.tobaccoReceipt(
                "张三",
                "HT202401001",
                "上部叶",
                25.6,
                "操作员001").build();

        if (serialPrinter.write(commands)) {
            System.out.println("✅ 烟叶称重小票发送成功");
        } else {
            System.out.println("❌ 烟叶称重小票发送失败");
        }
    }

    /**
     * 显示打印机状态
     */
    private static void showPrinterStatus() {
        System.out.println("\n=== 打印机状态 ===");

        System.out.println("串口打印机:");
        if (serialPrinter.isConnected()) {
            System.out.println("  ✅ 已连接");
            System.out.println("  " + serialPrinter.getPortInfo());
            System.out.println("  等待写入: " + serialPrinter.getBytesAwaitingWrite() + " 字节");
            System.out.println("  可读数据: " + serialPrinter.getBytesAvailable() + " 字节");
        } else {
            System.out.println("  ❌ 未连接");
        }

        System.out.println("\n打印机模拟器:");
        if (simulator.isRunning()) {
            System.out.println("  ✅ 运行中");
            System.out.println("  监听端口: " + simulator.getPortName());
        } else {
            System.out.println("  ❌ 未运行");
        }

        System.out.println("\n当前模式: " + (useSimulator ? "模拟器模式" : "实际连接模式"));
    }

    /**
     * 烟叶称重测试
     */
    private static void tobaccoWeighingTest() {
        System.out.println("\n=== 烟叶称重测试 ===");

        if (!serialPrinter.isConnected()) {
            System.out.println("❌ 请先连接打印机");
            return;
        }

        // 模拟称重数据
        String farmerName = "李四";
        String contractNumber = "HT202401002";
        String leafType = "中部叶";
        double weight = 18.75;
        String operator = "测试员";

        System.out.println("模拟烟叶称重数据:");
        System.out.println("  烟农: " + farmerName);
        System.out.println("  合同号: " + contractNumber);
        System.out.println("  叶片类型: " + leafType);
        System.out.println("  重量: " + weight + " kg");
        System.out.println("  操作员: " + operator);

        System.out.print("\n是否打印称重小票? (Y/n): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.isEmpty() || confirm.toLowerCase().startsWith("y")) {
            // 打印ESC/POS小票
            EscBuilder esc = new EscBuilder();
            byte[] escCommands = esc.tobaccoReceipt(farmerName, contractNumber, leafType, weight, operator).build();

            if (serialPrinter.write(escCommands)) {
                System.out.println("✅ 称重小票打印完成");
            } else {
                System.out.println("❌ 称重小票打印失败");
            }

            // 询问是否打印标签
            System.out.print("是否打印称重标签? (y/N): ");
            String labelConfirm = scanner.nextLine().trim();

            if (labelConfirm.toLowerCase().startsWith("y")) {
                LabelBuilder label = new LabelBuilder();
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                byte[] labelCommands = label.tobaccoLabel(farmerName, contractNumber, leafType, weight, date).build();

                if (serialPrinter.write(labelCommands)) {
                    System.out.println("✅ 称重标签打印完成");
                } else {
                    System.out.println("❌ 称重标签打印失败");
                }
            }
        }
    }

    /**
     * 断开所有连接
     */
    private static void disconnectAll() {
        System.out.println("\n=== 断开连接 ===");

        if (serialPrinter.isConnected()) {
            serialPrinter.close();
            System.out.println("✅ 串口连接已断开");
        }

        if (simulator.isRunning()) {
            simulator.stop();
            System.out.println("✅ 模拟器已停止");
        }

        useSimulator = false;
    }

    /**
     * 清理资源
     */
    private static void cleanup() {
        try {
            if (serialPrinter != null) {
                serialPrinter.close();
            }
            if (simulator != null) {
                simulator.stop();
            }
        } catch (Exception e) {
            logger.error("清理资源失败", e);
        }
    }
}