package com.tobacco.weight.service;

import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.LocationInfoDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 地区映射服务
 * 负责解析行政区划字符串，映射到乡镇和村庄
 */
public class LocationMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationMappingService.class);
    
    private final LocationInfoDao locationInfoDao;
    private final Map<String, LocationData.LocationInfo> locationCache;
    
    // 行政区划解析正则表达式
    private static final Pattern ADMIN_DIVISION_PATTERN = Pattern.compile("(.+?)([镇乡])(.+?)([村组社区])");
    private static final Pattern SIMPLE_PATTERN = Pattern.compile("(.+?)([镇乡])(.+)");
    
    public LocationMappingService(DatabaseManager databaseManager) {
        this.locationInfoDao = new LocationInfoDao(databaseManager);
        this.locationCache = new HashMap<>();
        loadLocationCache();
    }
    
    /**
     * 加载地区缓存
     */
    private void loadLocationCache() {
        try {
            // 从数据库加载所有地区信息到缓存
            // 这里可以根据实际需要实现缓存逻辑
            logger.info("地区映射缓存已加载");
        } catch (Exception e) {
            logger.error("加载地区映射缓存失败", e);
        }
    }
    
    /**
     * 解析行政区划字符串
     * 
     * @param adminDivision 行政区划字符串，如"永安镇督正村"
     * @return LocationInfo对象，包含乡镇和村庄信息
     */
    public LocationData.LocationInfo parseAdminDivision(String adminDivision) {
        if (adminDivision == null || adminDivision.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = adminDivision.trim();
        
        // 首先检查缓存
        if (locationCache.containsKey(trimmed)) {
            return locationCache.get(trimmed);
        }
        
        // 尝试标准格式解析：XX镇XX村
        Matcher matcher = ADMIN_DIVISION_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String township = matcher.group(1) + matcher.group(2); // 永安镇
            String village = matcher.group(3) + matcher.group(4);   // 督正村
            
            LocationData.LocationInfo locationInfo = new LocationData.LocationInfo(township, village);
            
            // 验证地区是否在已知注册表中
            if (isValidLocation(township, village)) {
                locationCache.put(trimmed, locationInfo);
                return locationInfo;
            }
        }
        
        // 尝试简化格式解析：XX镇XX（没有村字）
        matcher = SIMPLE_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String township = matcher.group(1) + matcher.group(2); // 永安镇
            String village = matcher.group(3) + "村";               // 督正村
            
            LocationData.LocationInfo locationInfo = new LocationData.LocationInfo(township, village);
            
            if (isValidLocation(township, village)) {
                locationCache.put(trimmed, locationInfo);
                return locationInfo;
            }
        }
        
        // 尝试其他可能的格式
        LocationData.LocationInfo fallbackLocation = tryFallbackParsing(trimmed);
        if (fallbackLocation != null && isValidLocation(fallbackLocation.getTownship(), fallbackLocation.getVillage())) {
            locationCache.put(trimmed, fallbackLocation);
            return fallbackLocation;
        }
        
        logger.warn("无法解析行政区划: {}", trimmed);
        return null;
    }
    
    /**
     * 尝试备用解析方法
     */
    private LocationData.LocationInfo tryFallbackParsing(String adminDivision) {
        // 尝试按常见分隔符分割
        String[] parts = adminDivision.split("[\\s,，、]");
        if (parts.length >= 2) {
            String township = parts[0].trim();
            String village = parts[1].trim();
            
            // 补充常见后缀
            if (!township.endsWith("镇") && !township.endsWith("乡")) {
                township += "镇";
            }
            if (!village.endsWith("村") && !village.endsWith("组") && !village.endsWith("社区")) {
                village += "村";
            }
            
            return new LocationData.LocationInfo(township, village);
        }
        
        // 尝试查找已知地区的部分匹配
        return findPartialMatch(adminDivision);
    }
    
    /**
     * 查找部分匹配的地区
     */
    private LocationData.LocationInfo findPartialMatch(String adminDivision) {
        try {
            // 从数据库查询可能匹配的地区
            List<String> allLocations = locationInfoDao.getAllLocationNames();
            
            for (String location : allLocations) {
                if (adminDivision.contains(location) || location.contains(adminDivision)) {
                    // 找到部分匹配，尝试解析
                    LocationData.LocationInfo parsed = LocationData.parseAddress(location);
                    if (parsed != null) {
                        logger.info("通过部分匹配找到地区: {} -> {}", adminDivision, location);
                        return parsed;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("查询地区信息失败", e);
        }
        
        return null;
    }
    
    /**
     * 验证地区是否在已知注册表中
     */
    private boolean isValidLocation(String township, String village) {
        try {
            // 检查数据库中是否存在该地区
            return locationInfoDao.existsLocation(township, village);
        } catch (SQLException e) {
            logger.error("验证地区失败: {} {}", township, village, e);
            return false;
        }
    }
    
    /**
     * 获取所有已知地区列表
     */
    public List<String> getAllKnownLocations() {
        try {
            return locationInfoDao.getAllLocationNames();
        } catch (SQLException e) {
            logger.error("获取地区列表失败", e);
            return List.of();
        }
    }
    
    /**
     * 添加新地区到注册表
     */
    public boolean addLocation(String township, String village) {
        try {
            locationInfoDao.insertLocation(township, village);
            logger.info("添加新地区: {} {}", township, village);
            return true;
        } catch (SQLException e) {
            logger.error("添加地区失败: {} {}", township, village, e);
            return false;
        }
    }
    
    /**
     * 清空缓存，重新加载
     */
    public void refreshCache() {
        locationCache.clear();
        loadLocationCache();
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("地区映射缓存: %d 条记录", locationCache.size());
    }
}
