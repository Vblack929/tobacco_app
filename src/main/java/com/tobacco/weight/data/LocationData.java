package com.tobacco.weight.data;

import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.LocationInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 地区数据管理类（数据库版本）
 * 管理乡镇和村庄的层级关系，从数据库加载数据
 */
public class LocationData {

    private static final Logger logger = LoggerFactory.getLogger(LocationData.class);
    private static LocationInfoRepository repository;
    private static volatile boolean initialized = false;
    
    // 缓存数据以提高性能
    private static List<String> cachedTownships = new ArrayList<>();
    private static Map<String, List<String>> cachedTownshipVillages = new HashMap<>();

    /**
     * 初始化数据库连接
     */
    private static synchronized void initializeIfNeeded() {
        if (!initialized) {
            try {
                DatabaseManager databaseManager = DatabaseManager.getInstance();
                repository = new LocationInfoRepository(databaseManager);
                loadDataFromDatabase();
                initialized = true;
                logger.info("LocationData 数据库初始化完成");
            } catch (Exception e) {
                logger.error("LocationData 数据库初始化失败，使用备用数据", e);
                loadFallbackData();
                initialized = true;
            }
        }
    }

    /**
     * 从数据库加载数据
     */
    private static void loadDataFromDatabase() {
        try {
            // 同步加载乡镇数据
            List<com.tobacco.weight.data.LocationInfo> townships = repository.getAllTownshipsSync();
            cachedTownships.clear();
            cachedTownshipVillages.clear();
            
            for (com.tobacco.weight.data.LocationInfo township : townships) {
                String townshipName = township.getTownshipName();
                cachedTownships.add(townshipName);
                
                // 加载该乡镇下的村庄
                List<com.tobacco.weight.data.LocationInfo> villages = repository.getVillagesByTownshipSync(townshipName);
                List<String> villageNames = new ArrayList<>();
                for (com.tobacco.weight.data.LocationInfo village : villages) {
                    villageNames.add(village.getVillageName());
                }
                cachedTownshipVillages.put(townshipName, villageNames);
            }
            
            logger.info("从数据库加载了 {} 个乡镇，{} 个村庄映射", 
                       cachedTownships.size(), cachedTownshipVillages.size());
                       
        } catch (Exception e) {
            logger.error("从数据库加载地区数据失败", e);
            loadFallbackData();
        }
    }

    /**
     * 加载备用数据（硬编码数据）
     */
    private static void loadFallbackData() {
        logger.warn("使用备用的硬编码地区数据");
        
        Map<String, List<String>> fallbackData = new LinkedHashMap<>();
        fallbackData.put("永安镇", Arrays.asList("永和村", "永和村2", "丰裕村", "丰裕村2", "西湖潭村", "督正村", "大安村"));
        fallbackData.put("枨冲镇", Arrays.asList("三元村", "平息村", "和平村"));
        fallbackData.put("普迹镇", Arrays.asList("金峰村", "新街村"));
        fallbackData.put("官桥镇", Arrays.asList("一江村", "石灰嘴村", "九龙村"));
        fallbackData.put("沙市", Arrays.asList("长春", "赤马", "友助", "白水", "秀山", "敦睦", "河背", "团农", "文光", "东门", "莲塘", "沙市", "秧田", "中洲"));
        fallbackData.put("龙伏", Arrays.asList("坪上", "新开", "焦桥", "达峰", "黄桥", "泮春", "相市", "龙伏"));
        fallbackData.put("社港", Arrays.asList("合盛", "淮洲", "清江", "源田", "社港", "新光", "永兴", "浏北"));
        fallbackData.put("淳口", Arrays.asList("高田", "鹤源", "黄荆坪", "羊古滩", "农大", "同辉", "谢家", "南冲", "鸭头", "狮岩"));
        fallbackData.put("北盛", Arrays.asList("拔茅", "百塘", "亚洲湖", "卓然", "燕舞洲", "窑金", "边洲", "乌龙", "泉水", "马战", "仓胜", "环园"));
        fallbackData.put("洞阳", Arrays.asList("洞阳"));
        fallbackData.put("金云", Arrays.asList("金云"));
        fallbackData.put("大围山", Arrays.asList("中岳村", "北麓园村"));
        fallbackData.put("达浒", Arrays.asList("麻洲社区", "金田村", "长丰村", "书香村", "象形村"));
        fallbackData.put("沿溪", Arrays.asList("大光村", "金桔村", "礼花村", "沙龙村"));
        fallbackData.put("官渡", Arrays.asList("观音塘村", "田郊村", "兵和村", "新云山", "竹山社区", "南岳社区", "竹联村"));
        fallbackData.put("张坊", Arrays.asList("白石村", "茶林村", "陈桥村", "江口村", "人溪村", "张家坊社区"));
        fallbackData.put("小河", Arrays.asList("皇碑村", "潭湾村", "田心村", "乌石村", "新河村"));
        fallbackData.put("高坪", Arrays.asList("船仓村"));
        fallbackData.put("古港", Arrays.asList("白露村", "宝盖寺村", "燕港村", "东盈村", "花城村", "华湘村", "金园村", "三口村"));
        fallbackData.put("关口", Arrays.asList("金湖村"));
        
        cachedTownships.clear();
        cachedTownships.addAll(fallbackData.keySet());
        cachedTownshipVillages.clear();
        cachedTownshipVillages.putAll(fallbackData);
    }

    /**
     * 获取所有乡镇列表
     */
    public static List<String> getAllTownships() {
        initializeIfNeeded();
        return new ArrayList<>(cachedTownships);
    }

    /**
     * 根据乡镇获取村庄列表
     */
    public static List<String> getVillagesByTownship(String township) {
        initializeIfNeeded();
        List<String> villages = cachedTownshipVillages.get(township);
        return villages != null ? new ArrayList<>(villages) : new ArrayList<>();
    }

    /**
     * 检查乡镇是否存在
     */
    public static boolean isTownshipValid(String township) {
        initializeIfNeeded();
        return cachedTownships.contains(township);
    }

    /**
     * 检查村庄是否在指定乡镇下
     */
    public static boolean isVillageInTownship(String village, String township) {
        initializeIfNeeded();
        List<String> villages = cachedTownshipVillages.get(township);
        return villages != null && villages.contains(village);
    }

    /**
     * 根据村庄名称查找所属乡镇
     */
    public static String findTownshipByVillage(String village) {
        initializeIfNeeded();
        for (Map.Entry<String, List<String>> entry : cachedTownshipVillages.entrySet()) {
            if (entry.getValue().contains(village)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 刷新缓存数据
     */
    public static synchronized void refreshCache() {
        if (repository != null) {
            try {
                loadDataFromDatabase();
                logger.info("地区数据缓存已刷新");
            } catch (Exception e) {
                logger.error("刷新地区数据缓存失败", e);
            }
        }
    }

    /**
     * 获取数据库仓库实例（供其他类使用）
     */
    public static LocationInfoRepository getRepository() {
        initializeIfNeeded();
        return repository;
    }

    /**
     * 格式化地址字符串（乡镇 + 村庄）
     */
    public static String formatAddress(String township, String village) {
        if (township == null || township.trim().isEmpty()) {
            return village != null ? village.trim() : "";
        }
        if (village == null || village.trim().isEmpty()) {
            return township.trim();
        }
        return township.trim() + " " + village.trim();
    }

    /**
     * 从地址字符串中解析乡镇和村庄
     */
    public static LocationInfo parseAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new LocationInfo("", "");
        }

        String cleanAddress = address.trim();
        initializeIfNeeded();
        
        // 尝试匹配完整的乡镇名
        for (String township : cachedTownships) {
            if (cleanAddress.contains(township)) {
                // 找到乡镇，尝试提取村庄
                List<String> villages = cachedTownshipVillages.get(township);
                if (villages != null) {
                    for (String village : villages) {
                        if (cleanAddress.contains(village)) {
                            return new LocationInfo(township, village);
                        }
                    }
                }
                // 只找到乡镇，没有具体村庄
                return new LocationInfo(township, "");
            }
        }

        // 如果没有找到乡镇，尝试匹配村庄
        for (Map.Entry<String, List<String>> entry : cachedTownshipVillages.entrySet()) {
            for (String village : entry.getValue()) {
                if (cleanAddress.contains(village)) {
                    return new LocationInfo(entry.getKey(), village);
                }
            }
        }

        // 都没找到，返回原始地址作为村庄
        return new LocationInfo("", cleanAddress);
    }

    /**
     * 地址信息类
     */
    public static class LocationInfo {
        private final String township;
        private final String village;

        public LocationInfo(String township, String village) {
            this.township = township != null ? township : "";
            this.village = village != null ? village : "";
        }

        public String getTownship() {
            return township;
        }

        public String getVillage() {
            return village;
        }

        public boolean hasValidTownship() {
            return !township.isEmpty() && isTownshipValid(township);
        }

        public boolean hasValidVillage() {
            return !village.isEmpty();
        }

        @Override
        public String toString() {
            return formatAddress(township, village);
        }
    }
}