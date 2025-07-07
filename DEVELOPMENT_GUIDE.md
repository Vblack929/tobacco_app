# 烟叶称重应用开发指南

## 📖 概述

本指南详细说明如何在没有实际硬件设备的情况下开发和测试烟叶称重应用。

## 🔧 开发环境配置

### 1. Android Studio平板模拟器设置

#### 创建平板模拟器
1. 打开Android Studio，点击**Tools** → **AVD Manager**
2. 点击**Create Virtual Device**
3. 选择**Tablet**分类，推荐：**Pixel C** (10.2英寸)
4. 选择**API Level 33** (Android 13.0)
5. 配置参数：
   - 设备名称：`Tobacco_Tablet_Android13`
   - 方向：**Landscape** (横屏)
   - RAM：4GB
   - 内存：8GB

#### 启动模拟器
```bash
emulator -avd Tobacco_Tablet_Android13 -gpu host -memory 4096
```

### 2. 应用架构说明

#### 核心组件
- **HardwareSimulator**：模拟硬件设备
- **WeightingViewModel**：业务逻辑处理
- **WeightingFragment**：UI界面
- **MainActivity**：主活动

#### 模拟功能
- ✅ 电子秤重量数据模拟
- ✅ 身份证读卡器模拟
- ✅ 热敏打印机模拟
- ✅ 实时数据更新

## 🎯 功能测试指南

### 1. 重量模拟测试
- 点击"模拟5kg"按钮：测试轻量烟叶
- 点击"模拟10kg"按钮：测试中等重量
- 点击"模拟20kg"按钮：测试重量烟叶
- 观察重量显示实时更新

### 2. 身份证读取测试
- 点击"读取身份证"按钮
- 系统会生成随机身份证信息
- 农户姓名会自动更新
- 合同号会自动生成

### 3. 等级选择测试
- 点击"A级"、"B级"、"C级"、"D级"按钮
- 观察按钮状态变化
- 确认等级选择反馈

### 4. 设备状态监控
- 启动后2秒内模拟设备连接
- 设备状态显示：电子秤✓、打印机✓、身份证✓
- 实时监控连接状态

## 🏗️ 代码结构

### 硬件模拟器 (HardwareSimulator)
```java
// 模拟重量数据
public void simulateAddWeight(double weight)

// 模拟身份证读取
public void simulateIdCardRead()

// 模拟打印操作
public boolean simulatePrint(String content)
```

### ViewModel数据绑定
```java
// 实时重量显示
LiveData<String> currentWeight

// 农户信息
LiveData<String> farmerName

// 设备状态
LiveData<String> deviceStatus
```

## 🚀 快速开始

### 1. 构建项目
```bash
./gradlew assembleDebug
```

### 2. 安装到模拟器
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 运行测试
1. 启动应用
2. 等待设备连接（2秒）
3. 点击测试按钮验证功能
4. 观察日志输出

## 📝 开发要点

### 1. UI适配
- 界面采用横屏布局
- 字体大小适配平板
- 按钮尺寸满足触摸要求

### 2. 数据流管理
- 使用LiveData进行数据绑定
- RxJava处理异步数据流
- 状态管理和错误处理

### 3. 模拟真实性
- 重量数据包含稳定性判断
- 身份证信息符合真实格式
- 设备连接状态模拟

## 🔍 调试技巧

### 1. 日志查看
```bash
adb logcat -s "HardwareSimulator"
adb logcat -s "WeightingViewModel"
```

### 2. 数据验证
- 检查重量数据格式
- 验证身份证信息完整性
- 确认设备状态更新

### 3. UI测试
- 验证按钮点击反馈
- 检查数据绑定效果
- 测试界面响应性

## 💡 下一步计划

### 1. 硬件集成
- 替换模拟器为真实硬件接口
- 添加串口通信功能
- 集成USB设备驱动

### 2. 功能扩展
- 数据库存储
- 报表生成
- 云端同步

### 3. 性能优化
- 内存使用优化
- 响应速度提升
- 电池续航考虑

---

通过本指南，您可以在无硬件环境下完整开发和测试烟叶称重应用的所有核心功能！ 