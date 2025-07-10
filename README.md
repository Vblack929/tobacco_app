# 烟叶称重收购系统

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Version](https://img.shields.io/badge/Version-1.0.0-blue.svg)](https://github.com)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg)](https://www.oracle.com/java/)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM-red.svg)](https://developer.android.com/jetpack/guide)

一款专为烟叶收购站设计的智能称重管理系统，集成电子秤、打印机、身份证读卡器等硬件设备，实现烟叶收购流程的数字化管理。

## 📱 应用概览

### 核心功能

- 🏷️ **智能称重** - 实时电子秤数据采集与显示
- 👤 **身份识别** - 身份证读卡器集成，农户信息自动录入
- 🖨️ **标签打印** - 自动生成并打印烟叶标识标签
- 📊 **数据管理** - 完整的称重记录管理与统计分析
- 📁 **数据导出** - 支持Excel格式数据导出
- 🔧 **硬件模拟** - 内置硬件模拟器，支持无硬件调试

### 技术特色

- **现代Android架构** - 采用MVVM架构模式
- **硬件集成** - 支持USB串口通信
- **本地数据库** - Room数据库持久化存储
- **依赖注入** - Hilt框架管理依赖
- **数据绑定** - DataBinding提升UI开发效率

## 🏗️ 系统架构

```
app/
├── src/main/java/com/tobacco/weight/
│   ├── ui/                    # UI层
│   │   ├── main/             # 主界面（称重界面）
│   │   ├── admin/            # 管理员界面
│   │   ├── records/          # 记录查询界面
│   │   ├── precheck/         # 预检界面
│   │   └── settings/         # 设置界面
│   ├── data/                 # 数据层
│   │   ├── entity/           # 数据库实体
│   │   ├── dao/              # 数据访问对象
│   │   ├── repository/       # 数据仓库
│   │   └── database/         # 数据库配置
│   ├── hardware/             # 硬件层
│   │   ├── scale/            # 电子秤驱动
│   │   ├── printer/          # 打印机驱动
│   │   ├── idcard/           # 身份证读卡器
│   │   ├── serial/           # 串口通信
│   │   └── simulator/        # 硬件模拟器
│   ├── di/                   # 依赖注入模块
│   └── utils/                # 工具类
├── res/                      # 资源文件
└── libs/                     # 第三方库
```

## 🔧 技术栈

### 核心框架

- **Android SDK** `API 26+` - 目标Android 8.0+
- **Java 11** - 开发语言
- **Gradle 8.2** - 构建工具

### 主要依赖

- **Jetpack组件**

  - `Room 2.6.1` - 本地数据库
  - `ViewModel & LiveData` - MVVM架构
  - `Navigation 2.7.5` - 页面导航
  - `DataBinding` - 数据绑定
- **依赖注入**

  - `Hilt 2.56.2` - 依赖注入框架
- **硬件通信**

  - `USB Serial for Android 3.6.0` - USB串口通信
  - `SerialportPrintSDK` - 打印机SDK
- **其他库**

  - `RxJava3` - 响应式编程
  - `Retrofit2` - 网络请求（预留）
  - `ZXing` - 二维码生成
  - `Glide` - 图片加载

## 🚀 快速开始

### 系统要求

- **Android设备**: Android 8.0 (API 26) 及以上
- **硬件要求**: 支持USB Host模式
- **推荐配置**: 10寸平板，横屏显示

### 编译环境

- **Android Studio**: Arctic Fox及以上版本
- **JDK**: OpenJDK 11
- **Gradle**: 8.2+
- **NDK**: 21.4.7075529 (可选)

### 编译步骤

1. **克隆项目**

   ```bash
   git clone <repository-url>
   cd tobacco_app
   ```
2. **导入Android Studio**

   - 打开Android Studio
   - 选择 "Open an existing project"
   - 选择项目根目录
3. **同步依赖**

   ```bash
   ./gradlew build
   ```
4. **运行应用**

   - 连接Android设备或启动模拟器
   - 点击运行按钮

## 📋 功能模块

### 1. 称重界面 (MainActivity)

- **实时重量显示** - 5.00 kg 大字体显示
- **农户信息录入** - 姓名、合同号管理
- **部叶选择** - 上部叶、中部叶、下部叶分类
- **身份证读取** - 一键读取农户身份信息
- **标签打印** - 生成包含二维码的标识标签

### 2. 管理员界面 (AdminActivity)

- **系统统计** - 注册农户数、称重记录数、总重量
- **农户信息表** - 可滚动查看所有农户数据
- **详细记录** - 查看单个农户的所有称重记录
- **数据导出** - 导出Excel格式统计报表

### 3. 记录查询界面 (RecordsFragment)

- **记录搜索** - 按农户姓名、合同号搜索
- **日期筛选** - 按时间范围过滤记录
- **详情查看** - 查看单条记录详细信息

### 4. 硬件集成

- **电子秤** - 串口通信，实时重量采集
- **打印机** - USB连接，标签自动打印
- **身份证读卡器** - USB接口，身份信息读取
- **硬件模拟器** - 无硬件环境下的功能测试

## 🗃️ 数据库设计

### 主要数据表

- **farmer_info** - 农户身份信息表

  - 姓名、身份证号、地址等基本信息
  - 合同号、注册时间等业务信息
- **weight_records** - 称重记录表

  - 称重时间、重量、部叶类型
  - 预检编号、操作员等详细信息

### 数据关系

- 一个农户可以有多条称重记录
- 通过身份证号关联农户信息和称重记录
- 支持数据完整性约束和索引优化

## 🔧 硬件配置

### 支持的设备

- **电子秤**: 支持标准RS232/USB串口协议
- **打印机**: 支持ESC/POS指令的标签打印机
- **身份证读卡器**: 符合公安部标准的二代身份证读卡器

### 连接方式

- **USB Host模式**: 直接USB连接
- **串口转USB**: 通过USB转串口适配器连接
- **网络连接**: 预留TCP/IP接口（未启用）

## 📊 数据导出

### 导出格式

- **Excel文件** (.xlsx格式)
- **CSV文件** (逗号分隔值)

### 导出内容

- 农户基本信息统计
- 称重记录明细表
- 按时间段的汇总报表
- 各部叶重量分布分析

### 导出路径

```
/storage/emulated/0/Documents/TobaccoWeightExports/
├── farmers_export_[timestamp].xlsx
├── records_export_[timestamp].xlsx
└── farmer_[name]_records_[timestamp].xlsx
```

## 🚨 权限说明

应用需要以下Android权限：

- **存储权限** - 数据导出和文件管理
- **USB权限** - 硬件设备通信
- **网络权限** - 数据同步（预留）
- **NFC权限** - 身份证读卡器通信

## 🔧 开发调试

### 硬件模拟

应用内置硬件模拟器，支持：

- 模拟重量数据生成 (1-15kg)
- 模拟身份证信息
- 模拟打印机状态
- 测试模式切换

### 调试功能

- 长按打印按钮切换测试/真实模式
- 管理员界面查看详细调试信息
- 日志输出支持问题诊断

## 📝 版本历史

### v1.0.0 (当前版本)

- ✅ 基础称重功能实现
- ✅ 硬件设备集成
- ✅ 数据库存储和查询
- ✅ 标签打印功能
- ✅ 数据导出功能
- ✅ 硬件模拟器

### 计划功能

- 🔄 网络数据同步
- 📱 移动端管理应用
- 📈 高级数据分析
- 🔐 用户权限管理

## 🤝 参与贡献

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用私有许可证，仅供授权用户使用。

## 📞 技术支持

如有技术问题或需要定制开发，请联系开发团队。

---

**烟叶称重收购系统** - 让烟叶收购管理更智能、更高效
