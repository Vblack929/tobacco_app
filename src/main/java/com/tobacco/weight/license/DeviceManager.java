package com.tobacco.weight.license;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备管理类
 * 负责设备绑定、解绑和管理功能
 */
public class DeviceManager {
    private static final Logger logger = LoggerFactory.getLogger(DeviceManager.class);
    
    private final OnlineLicenseService onlineService;
    private final ObjectMapper mapper;
    
    public DeviceManager() {
        this.onlineService = new OnlineLicenseService();
        this.mapper = new ObjectMapper();
    }
    
    /**
     * 设备信息类
     */
    public static class DeviceInfo {
        private String deviceFingerprint;
        private String deviceName;
        private String bindTime;
        private String lastUsedTime;
        private String ipAddress;
        private boolean active;
        
        // 构造函数
        public DeviceInfo() {}
        
        public DeviceInfo(String deviceFingerprint, String deviceName, String bindTime, 
                         String lastUsedTime, String ipAddress, boolean active) {
            this.deviceFingerprint = deviceFingerprint;
            this.deviceName = deviceName;
            this.bindTime = bindTime;
            this.lastUsedTime = lastUsedTime;
            this.ipAddress = ipAddress;
            this.active = active;
        }
        
        // Getter 和 Setter 方法
        public String getDeviceFingerprint() { return deviceFingerprint; }
        public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
        
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        
        public String getBindTime() { return bindTime; }
        public void setBindTime(String bindTime) { this.bindTime = bindTime; }
        
        public String getLastUsedTime() { return lastUsedTime; }
        public void setLastUsedTime(String lastUsedTime) { this.lastUsedTime = lastUsedTime; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        @Override
        public String toString() {
            return String.format("DeviceInfo{fingerprint='%s', name='%s', bindTime='%s', lastUsed='%s', ip='%s', active=%s}",
                    deviceFingerprint, deviceName, bindTime, lastUsedTime, ipAddress, active);
        }
    }
    
    /**
     * 获取许可证的所有绑定设备
     */
    public List<DeviceInfo> getBoundDevices(String licenseId) {
        List<DeviceInfo> devices = new ArrayList<>();
        
        try {
            String gistContent = onlineService.fetchGistContent();
            if (gistContent == null) {
                logger.error("无法获取许可证数据");
                return devices;
            }
            
            JsonNode rootNode = mapper.readTree(gistContent);
            JsonNode licensesNode = rootNode.get("licenses");
            
            if (licensesNode == null || !licensesNode.has(licenseId)) {
                logger.warn("许可证不存在: {}", licenseId);
                return devices;
            }
            
            JsonNode licenseNode = licensesNode.get(licenseId);
            JsonNode boundDevicesNode = licenseNode.get("boundDevices");
            
            if (boundDevicesNode != null && boundDevicesNode.isArray()) {
                for (JsonNode deviceNode : boundDevicesNode) {
                    DeviceInfo device = new DeviceInfo(
                        deviceNode.get("deviceFingerprint").asText(),
                        deviceNode.get("deviceName").asText(),
                        deviceNode.get("bindTime").asText(),
                        deviceNode.get("lastUsedTime").asText(),
                        deviceNode.get("ipAddress").asText(),
                        deviceNode.get("active").asBoolean(true)
                    );
                    devices.add(device);
                }
            }
            
            logger.info("获取到 {} 个绑定设备，许可证: {}", devices.size(), licenseId);
            
        } catch (Exception e) {
            logger.error("获取绑定设备失败", e);
        }
        
        return devices;
    }
    
    /**
     * 解绑指定设备
     */
    public boolean unbindDevice(String licenseId, String deviceFingerprint) {
        try {
            String gistContent = onlineService.fetchGistContent();
            if (gistContent == null) {
                logger.error("无法获取许可证数据");
                return false;
            }
            
            ObjectNode rootNode = (ObjectNode) mapper.readTree(gistContent);
            ObjectNode licensesNode = (ObjectNode) rootNode.get("licenses");
            
            if (!licensesNode.has(licenseId)) {
                logger.error("许可证不存在: {}", licenseId);
                return false;
            }
            
            ObjectNode licenseNode = (ObjectNode) licensesNode.get(licenseId);
            ArrayNode boundDevicesNode = (ArrayNode) licenseNode.get("boundDevices");
            
            if (boundDevicesNode == null) {
                logger.warn("许可证没有绑定设备: {}", licenseId);
                return false;
            }
            
            // 查找并移除指定设备
            boolean deviceFound = false;
            for (int i = 0; i < boundDevicesNode.size(); i++) {
                JsonNode deviceNode = boundDevicesNode.get(i);
                if (deviceFingerprint.equals(deviceNode.get("deviceFingerprint").asText())) {
                    boundDevicesNode.remove(i);
                    deviceFound = true;
                    logger.info("设备已从许可证中移除: {} from {}", deviceFingerprint, licenseId);
                    break;
                }
            }
            
            if (!deviceFound) {
                logger.warn("设备未找到: {} in {}", deviceFingerprint, licenseId);
                return false;
            }
            
            // 添加解绑记录到激活历史
            addActivationHistory(licenseNode, "device_unbind", deviceFingerprint, "手动解绑", "");
            
            // 更新元数据
            updateMetadata(rootNode);
            
            // 保存更新后的数据
            String updatedContent = mapper.writeValueAsString(rootNode);
            boolean success = onlineService.updateGistContent(updatedContent);
            
            if (success) {
                logger.info("设备解绑成功: {} from {}", deviceFingerprint, licenseId);
            } else {
                logger.error("设备解绑失败: 无法更新云端数据");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("设备解绑过程中发生错误", e);
            return false;
        }
    }
    
    /**
     * 停用设备（不删除，只标记为非活跃）
     */
    public boolean deactivateDevice(String licenseId, String deviceFingerprint) {
        try {
            String gistContent = onlineService.fetchGistContent();
            if (gistContent == null) {
                logger.error("无法获取许可证数据");
                return false;
            }
            
            ObjectNode rootNode = (ObjectNode) mapper.readTree(gistContent);
            ObjectNode licensesNode = (ObjectNode) rootNode.get("licenses");
            
            if (!licensesNode.has(licenseId)) {
                logger.error("许可证不存在: {}", licenseId);
                return false;
            }
            
            ObjectNode licenseNode = (ObjectNode) licensesNode.get(licenseId);
            ArrayNode boundDevicesNode = (ArrayNode) licenseNode.get("boundDevices");
            
            if (boundDevicesNode == null) {
                logger.warn("许可证没有绑定设备: {}", licenseId);
                return false;
            }
            
            // 查找并停用指定设备
            boolean deviceFound = false;
            for (JsonNode deviceNode : boundDevicesNode) {
                if (deviceFingerprint.equals(deviceNode.get("deviceFingerprint").asText())) {
                    ((ObjectNode) deviceNode).put("active", false);
                    deviceFound = true;
                    logger.info("设备已停用: {} in {}", deviceFingerprint, licenseId);
                    break;
                }
            }
            
            if (!deviceFound) {
                logger.warn("设备未找到: {} in {}", deviceFingerprint, licenseId);
                return false;
            }
            
            // 添加停用记录到激活历史
            addActivationHistory(licenseNode, "device_deactivate", deviceFingerprint, "设备停用", "");
            
            // 更新元数据
            updateMetadata(rootNode);
            
            // 保存更新后的数据
            String updatedContent = mapper.writeValueAsString(rootNode);
            boolean success = onlineService.updateGistContent(updatedContent);
            
            if (success) {
                logger.info("设备停用成功: {} in {}", deviceFingerprint, licenseId);
            } else {
                logger.error("设备停用失败: 无法更新云端数据");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("设备停用过程中发生错误", e);
            return false;
        }
    }
    
    /**
     * 重新激活设备
     */
    public boolean reactivateDevice(String licenseId, String deviceFingerprint) {
        try {
            String gistContent = onlineService.fetchGistContent();
            if (gistContent == null) {
                logger.error("无法获取许可证数据");
                return false;
            }
            
            ObjectNode rootNode = (ObjectNode) mapper.readTree(gistContent);
            ObjectNode licensesNode = (ObjectNode) rootNode.get("licenses");
            
            if (!licensesNode.has(licenseId)) {
                logger.error("许可证不存在: {}", licenseId);
                return false;
            }
            
            ObjectNode licenseNode = (ObjectNode) licensesNode.get(licenseId);
            ArrayNode boundDevicesNode = (ArrayNode) licenseNode.get("boundDevices");
            
            if (boundDevicesNode == null) {
                logger.warn("许可证没有绑定设备: {}", licenseId);
                return false;
            }
            
            // 检查活跃设备数量
            int activeDeviceCount = 0;
            for (JsonNode deviceNode : boundDevicesNode) {
                if (deviceNode.get("active").asBoolean(true)) {
                    activeDeviceCount++;
                }
            }
            
            int maxDevices = licenseNode.get("maxDevices").asInt(2);
            if (activeDeviceCount >= maxDevices) {
                logger.warn("无法重新激活设备: 已达到最大设备数量限制 ({}/{})", activeDeviceCount, maxDevices);
                return false;
            }
            
            // 查找并重新激活指定设备
            boolean deviceFound = false;
            for (JsonNode deviceNode : boundDevicesNode) {
                if (deviceFingerprint.equals(deviceNode.get("deviceFingerprint").asText())) {
                    ((ObjectNode) deviceNode).put("active", true);
                    ((ObjectNode) deviceNode).put("lastUsedTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
                    deviceFound = true;
                    logger.info("设备已重新激活: {} in {}", deviceFingerprint, licenseId);
                    break;
                }
            }
            
            if (!deviceFound) {
                logger.warn("设备未找到: {} in {}", deviceFingerprint, licenseId);
                return false;
            }
            
            // 添加重新激活记录到激活历史
            addActivationHistory(licenseNode, "device_reactivate", deviceFingerprint, "设备重新激活", "");
            
            // 更新元数据
            updateMetadata(rootNode);
            
            // 保存更新后的数据
            String updatedContent = mapper.writeValueAsString(rootNode);
            boolean success = onlineService.updateGistContent(updatedContent);
            
            if (success) {
                logger.info("设备重新激活成功: {} in {}", deviceFingerprint, licenseId);
            } else {
                logger.error("设备重新激活失败: 无法更新云端数据");
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("设备重新激活过程中发生错误", e);
            return false;
        }
    }
    
    /**
     * 获取许可证的设备使用统计
     */
    public DeviceUsageStats getDeviceUsageStats(String licenseId) {
        try {
            String gistContent = onlineService.fetchGistContent();
            if (gistContent == null) {
                return new DeviceUsageStats(0, 0, 0, 2);
            }
            
            JsonNode rootNode = mapper.readTree(gistContent);
            JsonNode licensesNode = rootNode.get("licenses");
            
            if (licensesNode == null || !licensesNode.has(licenseId)) {
                return new DeviceUsageStats(0, 0, 0, 2);
            }
            
            JsonNode licenseNode = licensesNode.get(licenseId);
            JsonNode boundDevicesNode = licenseNode.get("boundDevices");
            
            int totalDevices = 0;
            int activeDevices = 0;
            int inactiveDevices = 0;
            int maxDevices = licenseNode.get("maxDevices").asInt(2);
            
            if (boundDevicesNode != null && boundDevicesNode.isArray()) {
                totalDevices = boundDevicesNode.size();
                
                for (JsonNode deviceNode : boundDevicesNode) {
                    if (deviceNode.get("active").asBoolean(true)) {
                        activeDevices++;
                    } else {
                        inactiveDevices++;
                    }
                }
            }
            
            return new DeviceUsageStats(totalDevices, activeDevices, inactiveDevices, maxDevices);
            
        } catch (Exception e) {
            logger.error("获取设备使用统计失败", e);
            return new DeviceUsageStats(0, 0, 0, 2);
        }
    }
    
    /**
     * 设备使用统计类
     */
    public static class DeviceUsageStats {
        private final int totalDevices;
        private final int activeDevices;
        private final int inactiveDevices;
        private final int maxDevices;
        
        public DeviceUsageStats(int totalDevices, int activeDevices, int inactiveDevices, int maxDevices) {
            this.totalDevices = totalDevices;
            this.activeDevices = activeDevices;
            this.inactiveDevices = inactiveDevices;
            this.maxDevices = maxDevices;
        }
        
        public int getTotalDevices() { return totalDevices; }
        public int getActiveDevices() { return activeDevices; }
        public int getInactiveDevices() { return inactiveDevices; }
        public int getMaxDevices() { return maxDevices; }
        public int getAvailableSlots() { return maxDevices - activeDevices; }
        
        public boolean canBindNewDevice() {
            return activeDevices < maxDevices;
        }
        
        @Override
        public String toString() {
            return String.format("DeviceUsageStats{total=%d, active=%d, inactive=%d, max=%d, available=%d}",
                    totalDevices, activeDevices, inactiveDevices, maxDevices, getAvailableSlots());
        }
    }
    
    /**
     * 添加激活历史记录
     */
    private void addActivationHistory(ObjectNode licenseNode, String action, String deviceFingerprint, 
                                    String deviceName, String ipAddress) {
        try {
            ArrayNode historyNode = (ArrayNode) licenseNode.get("activationHistory");
            if (historyNode == null) {
                historyNode = mapper.createArrayNode();
                licenseNode.set("activationHistory", historyNode);
            }
            
            ObjectNode historyEntry = mapper.createObjectNode();
            historyEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
            historyEntry.put("action", action);
            historyEntry.put("deviceFingerprint", deviceFingerprint);
            historyEntry.put("deviceName", deviceName);
            historyEntry.put("ipAddress", ipAddress);
            
            historyNode.add(historyEntry);
            
        } catch (Exception e) {
            logger.warn("添加激活历史记录失败", e);
        }
    }
    
    /**
     * 更新元数据
     */
    private void updateMetadata(ObjectNode rootNode) {
        try {
            ObjectNode metadataNode = (ObjectNode) rootNode.get("metadata");
            if (metadataNode == null) {
                metadataNode = mapper.createObjectNode();
                rootNode.set("metadata", metadataNode);
            }
            
            metadataNode.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
            
        } catch (Exception e) {
            logger.warn("更新元数据失败", e);
        }
    }
}