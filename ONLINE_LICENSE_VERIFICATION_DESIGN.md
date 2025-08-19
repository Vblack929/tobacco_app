# 在线许可证验证系统改进方案

## 概述

本方案旨在改进现有的本地许可证系统，实现真正的全网在线验证和全局设备数量控制，确保相同的激活码有且仅能在指定数量的设备上激活。

## 核心设计理念

### 1. 轻量级云端存储
- **使用GitHub Gist作为许可证数据库**
- 无需搭建专门的许可证服务器
- 利用GitHub的高可用性和全球CDN
- 支持版本控制和历史记录

### 2. 混合验证模式
- **在线验证为主**：每次启动时进行云端验证
- **离线缓存为辅**：支持短期离线使用（最多7天）
- **定期同步**：每24小时强制在线验证一次

## 技术架构

### 1. 云端数据结构

#### GitHub Gist 文件结构
```json
{
  "licenses": {
    "YC-TWW-2025-0249-FB50-G1HH": {
      "licenseId": "YC-TWW-2025-0249-FB50-G1HH",
      "customerName": "测试客户",
      "maxDevices": 2,
      "validDays": 365,
      "createdDate": "2025-01-20T10:30:00Z",
      "expiryDate": "2026-01-20T10:30:00Z",
      "status": "active",
      "boundDevices": [
        {
          "deviceFingerprint": "a1b2c3d4e5f6...",
          "deviceName": "DESKTOP-ABC123",
          "bindTime": "2025-01-20T10:35:00Z",
          "lastUsedTime": "2025-01-20T15:20:00Z",
          "ipAddress": "192.168.1.100",
          "active": true
        }
      ],
      "activationHistory": [
        {
          "timestamp": "2025-01-20T10:35:00Z",
          "action": "device_bind",
          "deviceFingerprint": "a1b2c3d4e5f6...",
          "deviceName": "DESKTOP-ABC123",
          "ipAddress": "192.168.1.100"
        }
      ]
    }
  },
  "metadata": {
    "lastUpdated": "2025-01-20T15:20:00Z",
    "version": "1.0",
    "totalLicenses": 1,
    "totalActiveDevices": 1
  }
}
```

### 2. 本地缓存结构

#### license_cache.json
```json
{
  "licenseId": "YC-TWW-2025-0249-FB50-G1HH",
  "lastOnlineVerification": "2025-01-20T15:20:00Z",
  "cacheExpiryTime": "2025-01-27T15:20:00Z",
  "deviceFingerprint": "a1b2c3d4e5f6...",
  "verificationToken": "encrypted_token_here",
  "offlineUsageCount": 3,
  "maxOfflineUsage": 7
}
```

## 实现方案

### 1. 云端许可证管理服务

#### OnlineLicenseService.java
```java
public class OnlineLicenseService {
    private static final String GIST_API_URL = "https://api.github.com/gists/";
    private static final String GIST_ID = "your_gist_id_here";
    private static final String GITHUB_TOKEN = "your_github_token_here";
    
    /**
     * 在线验证许可证
     */
    public LicenseVerificationResult verifyLicenseOnline(String licenseId, String deviceFingerprint) {
        try {
            // 1. 从GitHub Gist获取最新许可证数据
            String gistContent = fetchGistContent();
            JSONObject licenses = new JSONObject(gistContent).getJSONObject("licenses");
            
            // 2. 检查许可证是否存在
            if (!licenses.has(licenseId)) {
                return LicenseVerificationResult.LICENSE_NOT_FOUND;
            }
            
            JSONObject license = licenses.getJSONObject(licenseId);
            
            // 3. 检查许可证状态和有效期
            if (!"active".equals(license.getString("status"))) {
                return LicenseVerificationResult.LICENSE_INACTIVE;
            }
            
            if (isExpired(license.getString("expiryDate"))) {
                return LicenseVerificationResult.LICENSE_EXPIRED;
            }
            
            // 4. 检查设备绑定状态
            JSONArray boundDevices = license.getJSONArray("boundDevices");
            boolean deviceFound = false;
            
            for (int i = 0; i < boundDevices.length(); i++) {
                JSONObject device = boundDevices.getJSONObject(i);
                if (deviceFingerprint.equals(device.getString("deviceFingerprint"))) {
                    deviceFound = true;
                    // 更新最后使用时间
                    device.put("lastUsedTime", Instant.now().toString());
                    break;
                }
            }
            
            // 5. 如果设备未绑定，检查是否可以绑定新设备
            if (!deviceFound) {
                int maxDevices = license.getInt("maxDevices");
                int activeDevices = countActiveDevices(boundDevices);
                
                if (activeDevices >= maxDevices) {
                    return LicenseVerificationResult.MAX_DEVICES_EXCEEDED;
                }
                
                // 绑定新设备
                bindNewDevice(license, deviceFingerprint);
            }
            
            // 6. 更新云端数据
            updateGistContent(licenses.toString());
            
            return LicenseVerificationResult.SUCCESS;
            
        } catch (Exception e) {
            logger.error("在线验证失败", e);
            return LicenseVerificationResult.NETWORK_ERROR;
        }
    }
    
    /**
     * 绑定新设备
     */
    private void bindNewDevice(JSONObject license, String deviceFingerprint) {
        JSONArray boundDevices = license.getJSONArray("boundDevices");
        
        JSONObject newDevice = new JSONObject();
        newDevice.put("deviceFingerprint", deviceFingerprint);
        newDevice.put("deviceName", HardwareFingerprint.getDeviceName());
        newDevice.put("bindTime", Instant.now().toString());
        newDevice.put("lastUsedTime", Instant.now().toString());
        newDevice.put("ipAddress", getPublicIP());
        newDevice.put("active", true);
        
        boundDevices.put(newDevice);
        
        // 记录激活历史
        JSONArray history = license.getJSONArray("activationHistory");
        JSONObject historyEntry = new JSONObject();
        historyEntry.put("timestamp", Instant.now().toString());
        historyEntry.put("action", "device_bind");
        historyEntry.put("deviceFingerprint", deviceFingerprint);
        historyEntry.put("deviceName", HardwareFingerprint.getDeviceName());
        historyEntry.put("ipAddress", getPublicIP());
        
        history.put(historyEntry);
    }
}
```

### 2. 离线缓存管理

#### OfflineLicenseCache.java
```java
public class OfflineLicenseCache {
    private static final String CACHE_FILE = "license_cache.json";
    private static final int MAX_OFFLINE_DAYS = 7;
    
    /**
     * 保存在线验证结果到本地缓存
     */
    public void saveVerificationResult(String licenseId, String deviceFingerprint) {
        try {
            JSONObject cache = new JSONObject();
            cache.put("licenseId", licenseId);
            cache.put("lastOnlineVerification", Instant.now().toString());
            cache.put("cacheExpiryTime", Instant.now().plus(MAX_OFFLINE_DAYS, ChronoUnit.DAYS).toString());
            cache.put("deviceFingerprint", deviceFingerprint);
            cache.put("verificationToken", generateVerificationToken(licenseId, deviceFingerprint));
            cache.put("offlineUsageCount", 0);
            cache.put("maxOfflineUsage", MAX_OFFLINE_DAYS);
            
            Files.write(Paths.get(CACHE_FILE), cache.toString().getBytes());
            
        } catch (Exception e) {
            logger.error("保存缓存失败", e);
        }
    }
    
    /**
     * 验证离线缓存
     */
    public boolean validateOfflineCache(String licenseId, String deviceFingerprint) {
        try {
            if (!Files.exists(Paths.get(CACHE_FILE))) {
                return false;
            }
            
            String content = new String(Files.readAllBytes(Paths.get(CACHE_FILE)));
            JSONObject cache = new JSONObject(content);
            
            // 检查许可证ID和设备指纹
            if (!licenseId.equals(cache.getString("licenseId")) ||
                !deviceFingerprint.equals(cache.getString("deviceFingerprint"))) {
                return false;
            }
            
            // 检查缓存是否过期
            Instant expiryTime = Instant.parse(cache.getString("cacheExpiryTime"));
            if (Instant.now().isAfter(expiryTime)) {
                return false;
            }
            
            // 检查离线使用次数
            int offlineUsageCount = cache.getInt("offlineUsageCount");
            int maxOfflineUsage = cache.getInt("maxOfflineUsage");
            
            if (offlineUsageCount >= maxOfflineUsage) {
                return false;
            }
            
            // 更新离线使用次数
            cache.put("offlineUsageCount", offlineUsageCount + 1);
            Files.write(Paths.get(CACHE_FILE), cache.toString().getBytes());
            
            return true;
            
        } catch (Exception e) {
            logger.error("验证离线缓存失败", e);
            return false;
        }
    }
}
```

### 3. 集成的许可证服务

#### 修改现有的 LicenseService.java
```java
public class LicenseService {
    private OnlineLicenseService onlineService = new OnlineLicenseService();
    private OfflineLicenseCache offlineCache = new OfflineLicenseCache();
    
    /**
     * 确保许可证有效（集成在线和离线验证）
     */
    public boolean ensureLicensed(Stage primaryStage) {
        try {
            String licenseId = getCurrentLicenseId();
            String deviceFingerprint = HardwareFingerprint.generateFingerprint();
            
            // 1. 尝试在线验证
            LicenseVerificationResult onlineResult = onlineService.verifyLicenseOnline(licenseId, deviceFingerprint);
            
            if (onlineResult == LicenseVerificationResult.SUCCESS) {
                // 在线验证成功，更新本地缓存
                offlineCache.saveVerificationResult(licenseId, deviceFingerprint);
                printLicenseStatusInfo();
                return true;
            }
            
            // 2. 在线验证失败，尝试离线缓存验证
            if (onlineResult == LicenseVerificationResult.NETWORK_ERROR) {
                boolean offlineValid = offlineCache.validateOfflineCache(licenseId, deviceFingerprint);
                if (offlineValid) {
                    logger.warn("使用离线缓存验证许可证");
                    printLicenseStatusInfo();
                    return true;
                }
            }
            
            // 3. 验证失败，显示激活对话框
            return showActivationDialog(primaryStage, onlineResult);
            
        } catch (Exception e) {
            logger.error("许可证验证失败", e);
            return false;
        }
    }
}
```

## 部署步骤

### 1. 创建GitHub Gist
1. 登录GitHub账户
2. 创建新的Gist（设置为Private）
3. 创建文件 `licenses.json`，初始内容为空的许可证数据结构
4. 记录Gist ID

### 2. 生成GitHub Personal Access Token
1. 进入GitHub Settings > Developer settings > Personal access tokens
2. 生成新token，权限选择 `gist`
3. 记录token值

### 3. 配置应用程序
1. 在配置文件中添加Gist ID和GitHub Token
2. 更新许可证生成工具，支持云端数据同步
3. 部署更新的应用程序

## 优势

### 1. 真正的全网验证
- 所有设备共享同一个云端许可证数据库
- 实时同步设备绑定状态
- 防止超额激活

### 2. 高可用性
- 利用GitHub的全球CDN和高可用性
- 支持离线使用，避免网络问题影响用户体验
- 自动故障恢复

### 3. 易于管理
- 供应商可通过GitHub界面直接查看和管理许可证
- 支持许可证状态的实时更新
- 详细的激活历史记录

### 4. 成本低廉
- 无需搭建专门的服务器
- GitHub Gist免费使用
- 维护成本极低

### 5. 安全性
- 使用HTTPS加密传输
- GitHub的安全保障
- 支持访问token控制

## 注意事项

### 1. 网络依赖
- 首次激活必须联网
- 定期需要在线验证
- 需要处理网络异常情况

### 2. GitHub限制
- API调用频率限制（每小时5000次）
- Gist文件大小限制（100MB）
- 需要有效的GitHub账户

### 3. 数据安全
- 建议对敏感信息进行加密
- 定期备份许可证数据
- 监控异常访问

## 总结

这个方案通过使用GitHub Gist作为轻量级云端数据库，实现了真正的全网在线许可证验证，确保相同激活码的全局设备数量控制。同时保持了系统的简单性和易维护性，无需搭建复杂的许可证服务器基础设施。