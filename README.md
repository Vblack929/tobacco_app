# 烟叶称重系统 (Tobacco Weight Windows)

## 项目概述

烟叶称重系统是一个基于JavaFX的桌面应用程序，专为烟叶收购和称重管理设计。系统采用动态绑定许可证模式，支持硬件集成，提供完整的称重、数据管理和报表功能。

## 主要特性

- **动态许可证管理** - 类似Typora的许可证绑定模式，支持多设备激活
- **硬件集成** - 支持电子秤、身份证读卡器、打印机等设备
- **数据管理** - 完整的称重数据记录、查询和导出功能
- **在线/离线模式** - 基于GitHub Gist的在线许可证验证，支持离线回退
- **便携式部署** - 支持生成独立的便携版程序包

## 快速开始

### 环境要求

- Java 11 或更高版本
- Maven 3.6 或更高版本
- Windows 操作系统（推荐）

### 编译和运行

1. **编译项目**
   ```bash
   mvn clean compile
   ```

2. **运行应用程序**
   ```bash
   mvn javafx:run
   ```

3. **生成便携版**
   ```bash
   # Windows
   .\build-portable-simple.bat
   
   # Linux/Mac
   ./run.sh
   ```

### 许可证管理

1. **生成许可证**
   ```bash
   .\generate_license.bat
   ```

2. **启动许可证管理界面**
   ```bash
   .\run_license_manager.bat
   ```

## 项目结构

```
tobacco_weight_windows/
├── src/                    # 源代码目录
│   ├── main/java/         # Java源代码
│   └── main/resources/    # 资源文件
├── config/                # 硬件驱动和配置文件
├── lib/                   # 外部依赖库
├── data/                  # 运行时数据目录
├── image/                 # 项目图片资源
├── docs/                  # 详细文档（见下方文档链接）
└── scripts/               # 构建和管理脚本
```

## 文档导航

### 核心文档
- [许可证系统说明](LICENSE_SYSTEM_README.md) - 许可证系统详细介绍
- [部署指南](DEPLOYMENT_GUIDE.md) - 在线许可证验证系统部署
- [便携版构建指南](PORTABLE_BUILD_GUIDE.md) - 便携版程序包生成
- [硬件集成指南](GY_RP801T_PRINTER_GUIDE.md) - 打印机等硬件集成说明

### 项目管理
- [项目整理报告](PROJECT_CLEANUP_REPORT.md) - 项目结构分析和整理建议

## 许可证格式

许可证ID格式：`YC-TWW-2025-XXXX-XXXX-XXXX`

- `YC`: 公司标识
- `TWW`: 产品标识（Tobacco Weight Windows）
- `2025`: 年份
- `XXXX-XXXX-XXXX`: 编码信息（包含客户信息、设备数量、有效期等）

## 硬件支持

### 支持的设备
- **电子秤** - 串口通信，支持多种协议
- **身份证读卡器** - 基于idreader.jar库
- **打印机** - 支持GY-RP801T等热敏打印机
- **条码扫描器** - USB HID设备支持

### 驱动文件
所有必要的驱动文件位于 `config/` 目录：
- `idreader.dll` - 身份证读卡器驱动
- `CH9326DLL64.dll` - 串口通信驱动
- 其他硬件相关DLL文件

## 开发和贡献

### 开发环境设置
1. 克隆项目到本地
2. 使用IDE（推荐IntelliJ IDEA）打开项目
3. 确保Java和Maven环境配置正确
4. 运行 `mvn compile` 编译项目

### 代码结构
- `com.tobacco.weight.main` - 主应用程序入口
- `com.tobacco.weight.license` - 许可证管理模块
- `com.tobacco.weight.hardware` - 硬件集成模块
- `com.tobacco.weight.data` - 数据管理模块
- `com.tobacco.weight.ui` - 用户界面模块

## 故障排除

### 常见问题

1. **许可证验证失败**
   - 检查网络连接
   - 验证GitHub Token配置
   - 查看许可证配置文件

2. **硬件设备无法识别**
   - 确认驱动文件完整
   - 检查设备连接
   - 查看系统日志

3. **程序启动失败**
   - 验证Java版本
   - 检查依赖库完整性
   - 查看错误日志

### 日志文件
- 应用程序日志：`logs/application.log`
- 许可证日志：`logs/license.log`

## 技术栈

- **前端**: JavaFX 17
- **后端**: Java 11+
- **构建工具**: Maven
- **数据库**: SQLite
- **日志**: Logback
- **JSON处理**: Jackson
- **Excel处理**: Apache POI
- **串口通信**: jSerialComm

## 版本信息

当前版本：1.0.0

## 联系方式

如有问题或建议，请联系技术支持团队。

---

*本项目为烟叶收购管理系统的Windows桌面版本，专为提高烟叶收购效率和数据管理准确性而设计。*