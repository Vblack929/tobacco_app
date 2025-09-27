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

/**
 * 在线许可证验证服务
 * 使用 GitHub Gist 作为云端许可证数据仓库
 */
public class OnlineLicenseService {
    private static final Logger logger = LoggerFactory.getLogger(OnlineLicenseService.class);
    
    private final LicenseConfig config = LicenseConfig.getInstance();
    
        public OnlineLicenseResponse verifyLicenseOnline(String licenseId, String deviceFingerprint) {
        logger.info("开始在线验证许可证: {}", licenseId);

        try {
            if (!isNetworkAvailable()) {
                logger.warn("网络不可用，无法执行在线验证");
                return OnlineLicenseResponse.failure(LicenseVerificationResult.NETWORK_ERROR);
            }

            String gistContent = fetchGistContent();
            if (gistContent == null) {
                logger.error("无法获取云端许可证数据");
                return OnlineLicenseResponse.failure(LicenseVerificationResult.SERVER_ERROR);
            }

            JSONObject rootData = new JSONObject(gistContent);
            JSONObject licenses = rootData.optJSONObject("licenses");
            if (licenses == null) {
                logger.error("云端数据格式错误：缺少 licenses 字段");
                return OnlineLicenseResponse.failure(LicenseVerificationResult.SERVER_ERROR);
            }

            if (!licenses.has(licenseId)) {
                logger.error("许可证不存在: {}", licenseId);
                return OnlineLicenseResponse.failure(LicenseVerificationResult.LICENSE_NOT_FOUND);
            }

            JSONObject license = licenses.getJSONObject(licenseId);
            LicenseType remoteType = LicenseType.fromTierCode(extractTierCode(licenseId));
            if (license.has("licenseType")) {
                remoteType = LicenseType.fromTierCode(license.optString("licenseType", remoteType.getTierCode()));
            }
            license.put("licenseType", remoteType.name());

            boolean unlimited = remoteType.isUnlimitedDevices() || license.optBoolean("unlimited", false);
            license.put("unlimited", unlimited);

            String status = license.optString("status", "inactive");
            if (!"active".equalsIgnoreCase(status)) {
                logger.error("许可证状态无效: {}", status);
                return OnlineLicenseResponse.failure(LicenseVerificationResult.LICENSE_INACTIVE);
            }

            if (isLicenseExpired(license)) {
                logger.error("许可证已过期");
                return OnlineLicenseResponse.failure(LicenseVerificationResult.LICENSE_EXPIRED);
            }

            JSONArray boundDevices = license.optJSONArray("boundDevices");
            if (boundDevices == null) {
                boundDevices = new JSONArray();
                license.put("boundDevices", boundDevices);
            }

            boolean deviceFound = false;
            int deviceIndex = -1;
            for (int i = 0; i < boundDevices.length(); i++) {
                JSONObject device = boundDevices.getJSONObject(i);
                if (deviceFingerprint.equals(device.optString("deviceFingerprint"))) {
                    deviceFound = true;
                    deviceIndex = i;
                    break;
                }
            }

            boolean modified = false;
            if (deviceFound) {
                JSONObject device = boundDevices.getJSONObject(deviceIndex);
                device.put("lastUsedTime", Instant.now().toString());
                device.put("active", true);
                modified = true;
                logger.info("设备已绑定，刷新最后使用时间");
            } else {
                int maxDevices = license.optInt("maxDevices", remoteType.getDefaultMaxDevices());
                if (unlimited) {
                    maxDevices = Integer.MAX_VALUE;
                }
                int activeDevices = countActiveDevices(boundDevices);

                if (activeDevices >= maxDevices) {
                    String limitLabel = maxDevices == Integer.MAX_VALUE ? "不限制" : String.valueOf(maxDevices);
                    logger.error("已达到最大设备绑定数量 {}/{}", activeDevices, limitLabel);
                    return OnlineLicenseResponse.failure(LicenseVerificationResult.MAX_DEVICES_EXCEEDED);
                }

                bindNewDevice(license, deviceFingerprint);
                if (!unlimited) {
                    license.put("maxDevices", maxDevices);
                }
                modified = true;
                logger.info("成功绑定新设备");
            }

            if (modified) {
                boolean updateSuccess = updateGistContent(rootData.toString());
                if (!updateSuccess) {
                    logger.error("更新 Gist 失败");
                    return OnlineLicenseResponse.failure(LicenseVerificationResult.SERVER_ERROR);
                }
                logger.info("成功更新云端许可证数据");
            }

            return OnlineLicenseResponse.success(license);
        } catch (Exception e) {
            logger.error("在线验证失败", e);
            return OnlineLicenseResponse.failure(LicenseVerificationResult.SERVER_ERROR);
        }
    }
    public String fetchGistContent() {
        try {
            URL url = new URL(config.getGithubApiUrl() + config.getGistId());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + config.getGithubToken());
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "TobaccoWeight-LicenseSystem/1.0");

            connection.setConnectTimeout(config.getConnectionTimeout());
            connection.setReadTimeout(config.getReadTimeout());

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.error("GitHub API请求失败，响应码: {}", responseCode);
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

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

    public boolean updateGistContent(String content) {
        try {
            URL url = new URL(config.getGithubApiUrl() + config.getGistId());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            connection.setRequestProperty("Authorization", "token " + config.getGithubToken());
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "TobaccoWeight-LicenseSystem/1.0");

            connection.setConnectTimeout(config.getConnectionTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            connection.setDoOutput(true);

            JSONObject requestBody = new JSONObject();
            JSONObject files = new JSONObject();
            JSONObject licenseFile = new JSONObject();
            licenseFile.put("content", content);
            files.put(config.getGistFilename(), licenseFile);
            requestBody.put("files", files);

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

    private boolean isLicenseExpired(JSONObject license) {
        try {
            String expiryDateStr = license.optString("expiryDate");
            if (expiryDateStr == null || expiryDateStr.isEmpty() || "null".equalsIgnoreCase(expiryDateStr)) {
                return false;
            }
            Instant expiryDate = Instant.parse(expiryDateStr);
            return Instant.now().isAfter(expiryDate);
        } catch (DateTimeParseException e) {
            logger.error("解析过期时间失败: {}", license.optString("expiryDate"), e);
            return true;
        }
    }
    
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
    
    private String getPublicIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("获取 IP 地址失败", e);
            return "unknown";
        }
    }
    
    private boolean isNetworkAvailable() {
        try {
            URL url = new URL("https://api.github.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            logger.debug("网络连接检测失败", e);
            return false;
        }
    }
    
    public boolean validateConfiguration() {
        return config.isConfigurationValid();
    }
    
    private String extractTierCode(String licenseId) {
        String[] parts = licenseId.split("-");
        return parts.length >= 3 ? parts[2] : LicenseType.CUSTOMER_LIMITED.getTierCode();
    }
}






