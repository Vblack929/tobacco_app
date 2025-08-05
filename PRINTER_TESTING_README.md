# 打印机功能测试指南

## 📋 功能概述

本系统实现了智能打印机检测和连接功能，支持两种打印方式：
- **串口打印**：直接通过COM端口发送ESC/POS指令
- **系统打印**：通过Windows打印服务发送到打印队列

## 🚀 快速测试

### 1. 打印机自动检测测试
```bash
./test-printer-detection.bat
```
**功能**：扫描串口，智能识别开发/生产环境，给出连接建议

### 2. 系统打印功能测试  
```bash
./test-system-print.bat
```
**功能**：测试Windows系统打印服务，验证POS80 Printer等驱动打印机

### 3. GY-RP801T完整测试
```bash
./test-gy-rp801t.bat
```
**功能**：启动完整的打印机测试程序，支持串口连接、模拟器、打印测试

## 📁 核心文件结构

```
tobacco_weight_windows/
├── src/main/java/com/tobacco/weight/
│   ├── hardware/
│   │   ├── PrinterManager.java                    # 系统打印管理
│   │   └── printer/
│   │       ├── WindowsSerialPrinter.java          # 串口连接
│   │       ├── PrinterSimulator.java              # 打印机模拟器  
│   │       ├── GY_RP801T_PrinterTest.java         # 完整测试程序
│   │       └── esc/
│   │           ├── EscBuilder.java                # ESC/POS指令生成
│   │           └── LabelBuilder.java              # 标签指令生成
│   ├── test/
│   │   ├── PrinterAutoDetectionTest.java          # 自动检测测试
│   │   └── SystemPrintTest.java                   # 系统打印测试
│   └── ui/
│       └── MainController.java                    # UI集成
├── test-printer-detection.bat                     # 检测测试脚本
├── test-system-print.bat                          # 系统打印测试脚本
├── test-gy-rp801t.bat                             # 完整测试脚本
├── test-gy-rp801t.sh                              # Git Bash版本
├── PRINTER_AUTO_DETECTION_GUIDE.md                # 自动检测详细指南
└── GY_RP801T_PRINTER_GUIDE.md                     # GY-RP801T使用指南
```

## 🔧 开发环境 vs 生产环境

### 开发环境（无实体打印机）
- **检测**：有com0com虚拟端口，无真实打印机端口
- **行为**：自动启动打印机模拟器
- **测试**：虚拟串口对通信 + 模拟器解析

### 生产环境（有实体打印机）
#### 情况A：USB-Serial打印机
- **检测**：设备管理器中出现新的COM端口
- **行为**：自动尝试连接所有可能的端口和波特率
- **测试**：真实串口通信 + ESC/POS指令

#### 情况B：Windows驱动打印机  
- **检测**：系统打印机列表中出现POS80 Printer等
- **行为**：通过系统打印服务发送
- **测试**：Java Print Service + 打印队列

## 💡 使用建议

1. **首次使用**：运行`test-printer-detection.bat`了解当前环境
2. **开发调试**：确保安装com0com，使用模拟器测试
3. **生产部署**：连接实体打印机，系统自动适配连接方式
4. **故障排除**：查看应用日志，关注连接状态信息

## 📖 详细文档

- [打印机自动检测指南](PRINTER_AUTO_DETECTION_GUIDE.md)
- [GY-RP801T使用指南](GY_RP801T_PRINTER_GUIDE.md) 