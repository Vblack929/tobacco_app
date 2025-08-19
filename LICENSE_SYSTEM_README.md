# 烟叶称重系统 - 动态绑定许可证系统

## 概述

本系统采用动态绑定许可证模式，类似于 Typora 等软件的许可证管理方式。用户购买许可证后获得一个许可证密钥，可以在指定数量的设备上激活使用，无需预先获取设备硬件指纹。

## 系统特点

### 1. 动态设备绑定
- 无需预先获取设备硬件指纹
- 用户可自主在设备上激活许可证
- 支持设备解绑和重新绑定
- 自动生成设备唯一标识

### 2. 灵活的设备管理
- 支持多设备许可证（可配置最大设备数量）
- 设备使用状态跟踪
- 设备绑定历史记录
- 长期未使用设备自动管理

### 3. 安全可靠
- 基于硬件指纹的设备识别
- 许可证密钥加密存储
- 防止许可证滥用
- 支持许可证有效期管理

## 许可证格式

许可证ID格式：`YC-TWW-2025-XXXX-XXXX-XXXX`

- `YC`: 公司标识
- `TWW`: 产品标识（Tobacco Weight Windows）
- `2025`: 年份
- `XXXX-XXXX-XXXX`: 编码信息（包含客户信息、设备数量、有效期等）

## 核心组件

### 1. LicenseIdGenerator
- **功能**: 生成和验证许可证ID
- **位置**: `src/main/java/com/tobacco/weight/license/LicenseIdGenerator.java`
- **主要方法**:
  - `generateLicenseId()`: 生成许可证ID
  - `validateLicenseId()`: 验证许可证ID格式
  - `parseLicenseId()`: 解析许可证信息

### 2. LicenseInfo
- **功能**: 存储许可证详细信息
- **位置**: `src/main/java/com/tobacco/weight/license/LicenseInfo.java`
- **包含信息**:
  - 许可证ID
  - 客户名称
  - 最大设备数量
  - 有效期
  - 设备绑定列表

### 3. DeviceBinding
- **功能**: 存储设备绑定信息
- **位置**: `src/main/java/com/tobacco/weight/license/DeviceBinding.java`
- **包含信息**:
  - 设备硬件指纹
  - 设备名称
  - 绑定时间
  - 最后使用时间
  - 激活状态

### 4. HardwareFingerprint
- **功能**: 生成设备硬件指纹
- **位置**: `src/main/java/com/tobacco/weight/license/HardwareFingerprint.java`
- **指纹组成**:
  - CPU信息
  - 主板序列号
  - 硬盘序列号
  - 网卡MAC地址

### 5. LicenseService
- **功能**: 许可证验证和管理服务
- **位置**: `src/main/java/com/tobacco/weight/license/LicenseService.java`
- **主要功能**:
  - 许可证激活
  - 设备绑定管理
  - 许可证状态验证
  - 本地许可证存储

### 6. LicenseActivationDialog
- **功能**: 许可证激活用户界面
- **位置**: `src/main/java/com/tobacco/weight/license/LicenseActivationDialog.java`
- **界面功能**:
  - 显示设备信息
  - 许可证密钥输入
  - 激活状态反馈

### 7. LicenseManagementDialog
- **功能**: 许可证管理用户界面
- **位置**: `src/main/java/com/tobacco/weight/license/LicenseManagementDialog.java`
- **管理功能**:
  - 查看许可证信息
  - 设备绑定管理
  - 设备解绑操作

## 使用流程

### 1. 许可证生成（供应商端）

```bash
# 运行许可证生成工具
generate_license.bat

# 或者直接使用Java命令
java -cp "target/classes;target/lib/*" com.tobacco.weight.license.LicenseIdGenerator "客户名称" 2 365
```

**参数说明**:
- 客户名称: 购买许可证的客户名称
- 设备数量: 允许绑定的最大设备数量
- 有效天数: 许可证有效期（天）

**输出示例**:
```
许可证生成成功！
==================
客户名称: 测试客户
最大设备数: 2
有效天数: 365
许可证ID: YC-TWW-2025-A1B2-C3D4-E5F6
==================
```

### 2. 许可证激活（用户端）

1. **启动应用程序**
   - 首次启动或许可证失效时，自动显示激活对话框

2. **查看设备信息**
   - 对话框显示当前设备的硬件指纹信息
   - 用户可将此信息提供给供应商（可选）

3. **输入许可证密钥**
   - 在输入框中输入从供应商获得的许可证ID
   - 系统自动格式化输入内容

4. **激活许可证**
   - 点击"激活"按钮
   - 系统验证许可证并绑定当前设备
   - 激活成功后可正常使用软件

### 3. 设备管理

**查看许可证信息**:
- 在应用程序菜单中选择"许可证管理"
- 查看许可证详细信息和设备绑定状态

**设备解绑**:
- 在许可证管理界面选择要解绑的设备
- 点击"解绑设备"按钮
- 确认后该设备将无法继续使用许可证

**重新绑定**:
- 解绑设备后，可在其他设备上重新激活许可证
- 只要不超过最大设备数量限制

## 技术实现

### 1. 硬件指纹生成

```java
// 获取设备硬件指纹
String fingerprint = HardwareFingerprint.generateFingerprint();

// 获取可读格式的指纹
String readableFingerprint = HardwareFingerprint.getReadableFingerprint();

// 获取设备名称
String deviceName = HardwareFingerprint.getDeviceName();
```

### 2. 许可证生成

```java
// 生成许可证
LicenseInfo license = LicenseIdGenerator.generateLicense(
    "客户名称",    // 客户名称
    2,            // 最大设备数
    365           // 有效天数
);

String licenseId = license.getLicenseId();
```

### 3. 许可证激活

```java
// 激活许可证
LicenseService licenseService = LicenseService.getInstance();
boolean success = licenseService.activateLicense(licenseId);

if (success) {
    System.out.println("许可证激活成功");
} else {
    System.out.println("许可证激活失败");
}
```

### 4. 许可证验证

```java
// 检查许可证状态
LicenseService licenseService = LicenseService.getInstance();
boolean isLicensed = licenseService.isLicensed();

if (isLicensed) {
    // 许可证有效，继续执行
} else {
    // 许可证无效，显示激活对话框
    licenseService.ensureLicensed(primaryStage);
}
```

## 数据存储

### 1. 本地许可证文件
- **文件名**: `license.json`
- **位置**: 应用程序根目录
- **格式**: JSON格式
- **内容**: 许可证信息和设备绑定数据

### 2. 许可证文件结构

```json
{
  "licenseId": "YC-TWW-2025-A1B2-C3D4-E5F6",
  "customerName": "测试客户",
  "maxDevices": 2,
  "validDays": 365,
  "createdDate": "2025-01-20T10:30:00",
  "expiryDate": "2026-01-20T10:30:00",
  "activated": true,
  "deviceBindings": [
    {
      "deviceFingerprint": "a1b2c3d4e5f6...",
      "deviceName": "DESKTOP-ABC123",
      "bindTime": "2025-01-20T10:35:00",
      "lastUsedTime": "2025-01-20T15:20:00",
      "active": true
    }
  ]
}
```

## 安全考虑

### 1. 硬件指纹安全
- 使用SHA-256哈希算法生成指纹
- 组合多种硬件信息提高唯一性
- 支持备用指纹生成机制

### 2. 许可证密钥安全
- 许可证ID包含校验信息
- 支持有效期验证
- 防止许可证伪造

### 3. 设备绑定安全
- 设备指纹唯一性验证
- 设备数量限制检查
- 异常设备绑定检测

## 故障排除

### 1. 常见问题

**Q: 许可证激活失败**
A: 检查以下项目：
- 许可证ID格式是否正确
- 许可证是否已过期
- 是否已达到最大设备绑定数量
- 网络连接是否正常（如果需要在线验证）

**Q: 设备指纹获取失败**
A: 可能的解决方案：
- 以管理员权限运行应用程序
- 检查系统权限设置
- 确保硬件信息可正常访问

**Q: 许可证文件损坏**
A: 解决步骤：
- 删除 `license.json` 文件
- 重新激活许可证
- 如果问题持续，联系技术支持

### 2. 日志调试

应用程序会记录详细的许可证操作日志，可通过以下方式查看：

```bash
# 查看应用程序日志
tail -f logs/application.log

# 搜索许可证相关日志
grep "License" logs/application.log
```

## 版本历史

### v2.0.0 (2025-01-20)
- 实现动态绑定许可证系统
- 支持多设备许可证管理
- 添加设备绑定和解绑功能
- 优化用户激活体验
- 完善许可证管理界面

### v1.0.0 (之前版本)
- 静态硬件指纹许可证系统
- 预授权设备绑定模式
- 基础许可证验证功能

## 技术支持

如果在使用过程中遇到问题，请联系技术支持：

- 邮箱: support@example.com
- 电话: 400-xxx-xxxx
- 在线支持: https://support.example.com

提供问题描述时，请包含以下信息：
- 应用程序版本
- 操作系统信息
- 错误信息截图
- 许可证ID（如果相关）
- 设备硬件指纹信息