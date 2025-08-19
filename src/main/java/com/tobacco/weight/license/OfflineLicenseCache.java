package com.tobacco.weight.license;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 离线许可证缓存服务
 * 提供本地缓存机制，支持离线验证
 */
public class OfflineLicenseCache {
    private static final Logger logger = LoggerFactory.getLogger(OfflineLicenseCache.class);
    
    private static final String CACHE_DIR = "license_cache";
    private static final String CACHE_FILE = "offline_cache.json";
    private static final String BACKUP_FILE = "offline_cache.backup.json";
    
    // 缓存有效期配置
    private static final int CACHE_VALIDITY_HOURS = 72; // 72小时
    private static final int GRACE_PERIOD_HOURS = 24; // 宽限期24小时
    
    private final Path cacheFilePath;
    private final Path backupFilePath;
    
    public OfflineLicenseCache() {
        // 初始化缓存目录
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".tobacco_weight", CACHE_DIR);
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            logger.error("创建缓存目录失败", e);
        }
        
        this.cacheFilePath = cacheDir.resolve(CACHE_FILE);
        this.backupFilePath = cacheDir.resolve(BACKUP_FILE);
    }
    
    /**
     * 缓存许可证验证结果
     * @param licenseId 许可证ID
     * @param deviceFingerprint 设备指纹
     * @param verificationResult 验证结果
     * @param licenseData 许可证数据
     */
    public void cacheLicenseVerification(String licenseId, String deviceFingerprint, 
                                       LicenseVerificationResult verificationResult, 
                                       JSONObject licenseData) {
        try {
            JSONObject cache = loadCacheData();
            
            // 创建缓存条目
            JSONObject cacheEntry = new JSONObject();
            cacheEntry.put("licenseId", licenseId);
            cacheEntry.put("deviceFingerprint", deviceFingerprint);
            cacheEntry.put("verificationResult", verificationResult.name());
            cacheEntry.put("cacheTime", Instant.now().toString());
            cacheEntry.put("licenseData", licenseData);
            
            // 添加到缓存
            JSONArray entries = cache.optJSONArray("entries");
            if (entries == null) {
                entries = new JSONArray();
                cache.put("entries", entries);
            }
            
            // 移除旧的相同许可证缓存
            removeOldCacheEntry(entries, licenseId, deviceFingerprint);
            
            // 添加新缓存
            entries.put(cacheEntry);
            
            // 清理过期缓存
            cleanExpiredCache(entries);
            
            // 保存缓存
            saveCacheData(cache);
            
            logger.info("许可证验证结果已缓存: {} - {}", licenseId, verificationResult);
            
        } catch (Exception e) {
            logger.error("缓存许可证验证结果失败", e);
        }
    }
    
    /**
     * 从缓存中验证许可证
     * @param licenseId 许可证ID
     * @param deviceFingerprint 设备指纹
     * @return 验证结果，如果缓存无效或不存在返回null
     */
    public LicenseVerificationResult verifyFromCache(String licenseId, String deviceFingerprint) {
        try {
            JSONObject cache = loadCacheData();
            JSONArray entries = cache.optJSONArray("entries");
            
            if (entries == null) {
                logger.debug("缓存为空");
                return null;
            }
            
            // 查找匹配的缓存条目
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                
                if (licenseId.equals(entry.optString("licenseId")) && 
                    deviceFingerprint.equals(entry.optString("deviceFingerprint"))) {
                    
                    // 检查缓存是否有效
                    if (isCacheValid(entry)) {
                        String resultName = entry.optString("verificationResult");
                        LicenseVerificationResult result = LicenseVerificationResult.valueOf(resultName);
                        
                        logger.info("从缓存验证许可证成功: {} - {}", licenseId, result);
                        return result;
                    } else {
                        logger.debug("缓存已过期: {}", licenseId);
                        // 移除过期缓存
                        entries.remove(i);
                        saveCacheData(cache);
                        return null;
                    }
                }
            }
            
            logger.debug("未找到匹配的缓存条目: {}", licenseId);
            return null;
            
        } catch (Exception e) {
            logger.error("从缓存验证许可证失败", e);
            return null;
        }
    }
    
    /**
     * 检查是否有有效的离线缓存
     * @param licenseId 许可证ID
     * @param deviceFingerprint 设备指纹
     * @return 是否有有效缓存
     */
    public boolean hasValidCache(String licenseId, String deviceFingerprint) {
        return verifyFromCache(licenseId, deviceFingerprint) != null;
    }
    
    /**
     * 获取缓存的许可证数据
     * @param licenseId 许可证ID
     * @param deviceFingerprint 设备指纹
     * @return 许可证数据，如果不存在返回null
     */
    public JSONObject getCachedLicenseData(String licenseId, String deviceFingerprint) {
        try {
            JSONObject cache = loadCacheData();
            JSONArray entries = cache.optJSONArray("entries");
            
            if (entries == null) {
                return null;
            }
            
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                
                if (licenseId.equals(entry.optString("licenseId")) && 
                    deviceFingerprint.equals(entry.optString("deviceFingerprint")) &&
                    isCacheValid(entry)) {
                    
                    return entry.optJSONObject("licenseData");
                }
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("获取缓存许可证数据失败", e);
            return null;
        }
    }
    
    /**
     * 清除所有缓存
     */
    public void clearCache() {
        try {
            // 创建备份
            if (Files.exists(cacheFilePath)) {
                Files.copy(cacheFilePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 删除缓存文件
            Files.deleteIfExists(cacheFilePath);
            
            logger.info("缓存已清除");
            
        } catch (IOException e) {
            logger.error("清除缓存失败", e);
        }
    }
    
    /**
     * 清除特定许可证的缓存
     * @param licenseId 许可证ID
     */
    public void clearLicenseCache(String licenseId) {
        try {
            JSONObject cache = loadCacheData();
            JSONArray entries = cache.optJSONArray("entries");
            
            if (entries == null) {
                return;
            }
            
            // 移除匹配的缓存条目
            for (int i = entries.length() - 1; i >= 0; i--) {
                JSONObject entry = entries.getJSONObject(i);
                if (licenseId.equals(entry.optString("licenseId"))) {
                    entries.remove(i);
                }
            }
            
            saveCacheData(cache);
            logger.info("已清除许可证缓存: {}", licenseId);
            
        } catch (Exception e) {
            logger.error("清除许可证缓存失败", e);
        }
    }
    
    /**
     * 获取缓存统计信息
     * @return 缓存统计信息
     */
    public JSONObject getCacheStats() {
        JSONObject stats = new JSONObject();
        
        try {
            JSONObject cache = loadCacheData();
            JSONArray entries = cache.optJSONArray("entries");
            
            if (entries == null) {
                stats.put("totalEntries", 0);
                stats.put("validEntries", 0);
                stats.put("expiredEntries", 0);
                return stats;
            }
            
            int totalEntries = entries.length();
            int validEntries = 0;
            int expiredEntries = 0;
            
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                if (isCacheValid(entry)) {
                    validEntries++;
                } else {
                    expiredEntries++;
                }
            }
            
            stats.put("totalEntries", totalEntries);
            stats.put("validEntries", validEntries);
            stats.put("expiredEntries", expiredEntries);
            stats.put("cacheFile", cacheFilePath.toString());
            
        } catch (Exception e) {
            logger.error("获取缓存统计信息失败", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 加载缓存数据
     */
    private JSONObject loadCacheData() {
        try {
            if (!Files.exists(cacheFilePath)) {
                return new JSONObject();
            }
            
            String content = Files.readString(cacheFilePath, StandardCharsets.UTF_8);
            return new JSONObject(content);
            
        } catch (Exception e) {
            logger.error("加载缓存数据失败", e);
            
            // 尝试从备份恢复
            try {
                if (Files.exists(backupFilePath)) {
                    String backupContent = Files.readString(backupFilePath, StandardCharsets.UTF_8);
                    logger.info("从备份文件恢复缓存数据");
                    return new JSONObject(backupContent);
                }
            } catch (Exception backupError) {
                logger.error("从备份恢复缓存数据也失败", backupError);
            }
            
            return new JSONObject();
        }
    }
    
    /**
     * 保存缓存数据
     */
    private void saveCacheData(JSONObject cache) {
        try {
            // 创建备份
            if (Files.exists(cacheFilePath)) {
                Files.copy(cacheFilePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 保存新数据
            String content = cache.toString(2); // 格式化JSON
            Files.writeString(cacheFilePath, content, StandardCharsets.UTF_8);
            
        } catch (IOException e) {
            logger.error("保存缓存数据失败", e);
        }
    }
    
    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(JSONObject entry) {
        try {
            String cacheTimeStr = entry.optString("cacheTime");
            if (cacheTimeStr.isEmpty()) {
                return false;
            }
            
            Instant cacheTime = Instant.parse(cacheTimeStr);
            Instant now = Instant.now();
            
            // 检查是否在有效期内
            long hoursSinceCached = ChronoUnit.HOURS.between(cacheTime, now);
            
            // 如果验证结果是成功的，使用标准有效期
            String resultName = entry.optString("verificationResult");
            if ("SUCCESS".equals(resultName)) {
                return hoursSinceCached <= CACHE_VALIDITY_HOURS;
            }
            
            // 如果验证结果是失败的，使用较短的有效期
            return hoursSinceCached <= GRACE_PERIOD_HOURS;
            
        } catch (Exception e) {
            logger.error("检查缓存有效性失败", e);
            return false;
        }
    }
    
    /**
     * 移除旧的缓存条目
     */
    private void removeOldCacheEntry(JSONArray entries, String licenseId, String deviceFingerprint) {
        for (int i = entries.length() - 1; i >= 0; i--) {
            JSONObject entry = entries.getJSONObject(i);
            if (licenseId.equals(entry.optString("licenseId")) && 
                deviceFingerprint.equals(entry.optString("deviceFingerprint"))) {
                entries.remove(i);
            }
        }
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache(JSONArray entries) {
        for (int i = entries.length() - 1; i >= 0; i--) {
            JSONObject entry = entries.getJSONObject(i);
            if (!isCacheValid(entry)) {
                entries.remove(i);
            }
        }
    }
}