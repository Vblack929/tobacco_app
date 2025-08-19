package com.tobacco.weight.license;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * 在线许可证验证服务
 * 使用GitHub Gist作为云端许可证数据库
 */
public class OnlineLicenseService {
    private static final Logger logger = LoggerFactory.getLogger(OnlineLicenseService.class);
    
    // 配置管理
    private final LicenseConfig config = LicenseConfig.getInstance();
    
    /**
     * 在线验证许可证
     * @param licenseId 许可证ID
     * @param deviceFingerprint 设备指纹
     * @return 验证结果
     */
    public LicenseVerificationResult verifyLicenseOnline(String licenseId, String deviceFingerprint) {
        logger.info("开始在线验证许可证: {}", licenseId);
        
        try {
            // 1. 检查网络连接
            if (!isNetworkAvailable()) {
                logger.warn("网络不可用，无法进行在线验证");
                return LicenseVerificationResult.NETWORK_ERROR;
            }
            
            // 2. 从GitHub Gist获取最新许可证数据
            String gistContent = fetchGistContent();
            if (gistContent == null) {
                logger.error("无法获取云端许可证数据");
                return LicenseVerificationResult.SERVER_ERROR;
            }
            
            // 3. 解析许可证数据
            JSONObject rootData = new JSONObject(gistContent);
            if (!rootData.has("licenses")) {
                logger.error("云端数据格式错误：缺少licenses字段");
                return LicenseVerificationResult.SERVER_ERROR;
            }
            
            JSONObject licenses = rootData.getJSONObject("licenses");
            
            // 4. 检查许可证是否存在
            if (!licenses.has(licenseId)) {
                logger.error("许可证不存在: {}", licenseId);
                return LicenseVerificationResult.LICENSE_NOT_FOUND;
            }
            
            JSONObject license = licenses.getJSONObject(licenseId);
            
            // 5. 检查许可证状态
            String status = license.optString("status", "inactive");
            if (!"active".equals(status)) {
                logger.error("许可证状态无效: {}", status);
                return LicenseVerificationResult.LICENSE_INACTIVE;
            }
            
            // 6. 检查许可证有效期
            if (isLicenseExpired(license)) {
                logger.error("许可证已过期");
                return LicenseVerificationResult.LICENSE_EXPIRED;
            }
            
            // 7. 检查设备绑定状态
            JSONArray boundDevices = license.optJSONArray("boundDevices");
            if (boundDevices == null) {
                boundDevices = new JSONArray();
                license.put("boundDevices", boundDevices);
            }
            
            boolean deviceFound = false;
            int deviceIndex = -1;
            
            // 查找当前设备
            for (int i = 0; i < boundDevices.length(); i++) {
                JSONObject device = boundDevices.getJSONObject(i);
                if (deviceFingerprint.equals(device.optString("deviceFingerprint"))) {
                    deviceFound = true;
                    deviceIndex = i;
                    break;
                }
            }
            
            // 8. 处理设备绑定
            if (deviceFound) {
                // 设备已绑定，更新最后使用时间
                JSONObject device = boundDevices.getJSONObject(deviceIndex);
                device.put("lastUsedTime", Instant.now().toString());
                logger.info("设备已绑定，更新使用时间");
            } else {
                // 设备未绑定，检查是否可以绑定新设备
                int maxDevices = license.optInt("maxDevices", 1);
                int activeDevices = countActiveDevices(boundDevices);
                
                if (activeDevices >= maxDevices) {
                    logger.error("已达到最大设备数量限制: {}/{}", activeDevices, maxDevices);
                    return LicenseVerificationResult.MAX_DEVICES_EXCEEDED;
                }
                
                // 绑定新设备
                bindNewDevice(license, deviceFingerprint);
                logger.info("成功绑定新设备");
            }
            
            // 9. 更新云端数据
            boolean updateSuccess = updateGistContent(rootData.toString());
            if (!updateSuccess) {
                logger.warn("更新云端数据失败，但验证成功");
                // 不返回错误，因为验证本身是成功的
            }
            
            logger.info("在线验证成功");
            return LicenseVerificationResult.SUCCESS;
            
        } catch (Exception e) {
            logger.error("在线验证过程中发生异常", e);
            return LicenseVerificationResult.UNKNOWN_ERROR;
        }
    }
    
    /**
     * 从GitHub Gist获取内容
     */
    public String fetchGistContent() {
        try {
            URL url = new URL(config.getGithubApiUrl() + config.getGistId());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求头
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + config.getGithubToken());
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "TobaccoWeight-LicenseSystem/1.0");
            
            // 设置超时
            connection.setConnectTimeout(config.getConnectionTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.error("GitHub API请求失败，响应码: {}", responseCode);
                return null;
            }
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // 解析Gist响应，获取文件内容
            JSONObject gistResponse = new JSONObject(response.toString());
            JSONObject files = gistResponse.getJSONObject("files");
            
            if (!files.has(config.getGistFilename())) {
                logger.error("Gist中未找到许可证文件: {}", config.getGistFilename());
                return null;
            }
            
            JSONObject licenseFile = files.getJSONObject(config.getGistFilename());
            return licenseFile.getString("content");
            
        } catch (Exception e) {
            logger.error("获取Gist内容失败", e);
            return null;
        }
    }
    
    /**
     * 更新GitHub Gist内容
     */
    public boolean updateGistContent(String content) {
        try {
            URL url = new URL(config.getGithubApiUrl() + config.getGistId());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 使用PUT方法替代PATCH方法（更兼容）
            connection.setRequestMethod("POST");
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            connection.setRequestProperty("Authorization", "token " + config.getGithubToken());
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "TobaccoWeight-LicenseSystem/1.0");
            
            // 设置超时
            connection.setConnectTimeout(config.getConnectionTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            connection.setDoOutput(true);
            
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            JSONObject files = new JSONObject();
            JSONObject licenseFile = new JSONObject();
            licenseFile.put("content", content);
            files.put(config.getGistFilename(), licenseFile);
            requestBody.put("files", files);
            
            // 发送请求
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.error("更新Gist失败，响应码: {}", responseCode);
                return false;
            }
            
            logger.info("成功更新云端许可证数据");
            return true;
            
        } catch (Exception e) {
            logger.error("更新Gist内容失败", e);
            return false;
        }
    }
    
    /**
     * 检查许可证是否过期
     */
    private boolean isLicenseExpired(JSONObject license) {
        try {
            String expiryDateStr = license.optString("expiryDate");
            if (expiryDateStr.isEmpty()) {
                return false; // 没有过期时间表示永久有效
            }
            
            Instant expiryDate = Instant.parse(expiryDateStr);
            return Instant.now().isAfter(expiryDate);
            
        } catch (DateTimeParseException e) {
            logger.error("解析过期时间失败: {}", license.optString("expiryDate"), e);
            return true; // 解析失败认为已过期
        }
    }
    
    /**
     * 统计活跃设备数量
     */
    private int countActiveDevices(JSONArray boundDevices) {
        int count = 0;
        for (int i = 0; i < boundDevices.length(); i++) {
            JSONObject device = boundDevices.getJSONObject(i);
            if (device.optBoolean("active", true)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 绑定新设备
     */
    private void bindNewDevice(JSONObject license, String deviceFingerprint) {
        JSONArray boundDevices = license.getJSONArray("boundDevices");
        
        // 创建新设备记录
        JSONObject newDevice = new JSONObject();
        newDevice.put("deviceFingerprint", deviceFingerprint);
        newDevice.put("deviceName", HardwareFingerprint.getDeviceName());
        newDevice.put("bindTime", Instant.now().toString());
        newDevice.put("lastUsedTime", Instant.now().toString());
        newDevice.put("ipAddress", getPublicIP());
        newDevice.put("active", true);
        
        boundDevices.put(newDevice);
        
        // 记录激活历史
        JSONArray history = license.optJSONArray("activationHistory");
        if (history == null) {
            history = new JSONArray();
            license.put("activationHistory", history);
        }
        
        JSONObject historyEntry = new JSONObject();
        historyEntry.put("timestamp", Instant.now().toString());
        historyEntry.put("action", "device_bind");
        historyEntry.put("deviceFingerprint", deviceFingerprint);
        historyEntry.put("deviceName", HardwareFingerprint.getDeviceName());
        historyEntry.put("ipAddress", getPublicIP());
        
        history.put(historyEntry);
        
        logger.info("新设备绑定成功: {}", HardwareFingerprint.getDeviceName());
    }
    
    /**
     * 获取公网IP地址
     */
    private String getPublicIP() {
        try {
            // 简单的本地IP获取，实际项目中可以调用外部API获取公网IP
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("获取IP地址失败", e);
            return "unknown";
        }
    }
    
    /**
     * 检查网络连接是否可用
     */
    private boolean isNetworkAvailable() {
        try {
            // 尝试连接GitHub API
            URL url = new URL("https://api.github.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
            
        } catch (IOException e) {
            logger.debug("网络连接检查失败", e);
            return false;
        }
    }
    
    /**
     * 验证GitHub配置是否正确
     */
    public boolean validateConfiguration() {
        return config.isConfigurationValid();
    }
}