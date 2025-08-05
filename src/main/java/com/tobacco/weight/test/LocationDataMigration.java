package com.tobacco.weight.test;

import com.tobacco.weight.data.LocationInfo;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.LocationInfoRepository;

import java.util.*;

/**
 * 地区数据迁移工具
 * 将硬编码的地区数据迁移到数据库中
 */
public class LocationDataMigration {

    /**
     * 原始地区数据（从原LocationData类复制）
     */
    private static final Map<String, List<String>> TOWNSHIP_VILLAGES = new LinkedHashMap<>();

    static {
        // 初始化乡镇和村庄数据
        TOWNSHIP_VILLAGES.put("永安镇", Arrays.asList("永和村", "永和村2", "丰裕村", "丰裕村2", "西湖潭村", "督正村", "大安村"));
        TOWNSHIP_VILLAGES.put("枨冲镇", Arrays.asList("三元村", "平息村", "和平村"));
        TOWNSHIP_VILLAGES.put("普迹镇", Arrays.asList("金峰村", "新街村"));
        TOWNSHIP_VILLAGES.put("官桥镇", Arrays.asList("一江村", "石灰嘴村", "九龙村"));
        TOWNSHIP_VILLAGES.put("沙市", Arrays.asList("长春", "赤马", "友助", "白水", "秀山", "敦睦", "河背", "团农", "文光", "东门", "莲塘", "沙市", "秧田", "中洲"));
        TOWNSHIP_VILLAGES.put("龙伏", Arrays.asList("坪上", "新开", "焦桥", "达峰", "黄桥", "泮春", "相市", "龙伏"));
        TOWNSHIP_VILLAGES.put("社港", Arrays.asList("合盛", "淮洲", "清江", "源田", "社港", "新光", "永兴", "浏北"));
        TOWNSHIP_VILLAGES.put("淳口", Arrays.asList("高田", "鹤源", "黄荆坪", "羊古滩", "农大", "同辉", "谢家", "南冲", "鸭头", "狮岩"));
        TOWNSHIP_VILLAGES.put("北盛", Arrays.asList("拔茅", "百塘", "亚洲湖", "卓然", "燕舞洲", "窑金", "边洲", "乌龙", "泉水", "马战", "仓胜", "环园"));
        TOWNSHIP_VILLAGES.put("洞阳", Arrays.asList("洞阳"));
        TOWNSHIP_VILLAGES.put("金云", Arrays.asList("金云"));
        TOWNSHIP_VILLAGES.put("大围山", Arrays.asList("中岳村", "北麓园村"));
        TOWNSHIP_VILLAGES.put("达浒", Arrays.asList("麻洲社区", "金田村", "长丰村", "书香村", "象形村"));
        TOWNSHIP_VILLAGES.put("沿溪", Arrays.asList("大光村", "金桔村", "礼花村", "沙龙村"));
        TOWNSHIP_VILLAGES.put("官渡", Arrays.asList("观音塘村", "田郊村", "兵和村", "新云山", "竹山社区", "南岳社区", "竹联村"));
        TOWNSHIP_VILLAGES.put("张坊", Arrays.asList("白石村", "茶林村", "陈桥村", "江口村", "人溪村", "张家坊社区"));
        TOWNSHIP_VILLAGES.put("小河", Arrays.asList("皇碑村", "潭湾村", "田心村", "乌石村", "新河村"));
        TOWNSHIP_VILLAGES.put("高坪", Arrays.asList("船仓村"));
        TOWNSHIP_VILLAGES.put("古港", Arrays.asList("白露村", "宝盖寺村", "燕港村", "东盈村", "花城村", "华湘村", "金园村", "三口村"));
        TOWNSHIP_VILLAGES.put("关口", Arrays.asList("金湖村"));
    }

    public static void main(String[] args) {
        System.out.println("开始地区数据迁移...");
        
        try {
            // 初始化数据库和仓库
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            LocationInfoRepository repository = new LocationInfoRepository(databaseManager);
            
            // 检查是否已经有数据
            repository.getCount(new LocationInfoRepository.OnResultListener<Integer>() {
                @Override
                public void onSuccess(Integer count) {
                    if (count > 0) {
                        System.out.println("数据库中已存在 " + count + " 条地区记录，跳过迁移");
                        return;
                    }
                    
                    // 执行迁移
                    performMigration(repository);
                }

                @Override
                public void onError(Exception e) {
                    System.err.println("检查数据库记录数失败: " + e.getMessage());
                    // 继续执行迁移
                    performMigration(repository);
                }
            });
            
            // 等待异步操作完成
            Thread.sleep(10000);
            
            // 关闭仓库
            repository.shutdown();
            databaseManager.closeConnection();
            
            System.out.println("地区数据迁移完成！");
            
        } catch (Exception e) {
            System.err.println("地区数据迁移失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行数据迁移
     */
    private static void performMigration(LocationInfoRepository repository) {
        System.out.println("开始迁移地区数据到数据库...");
        
        List<LocationInfo> allLocations = new ArrayList<>();
        Map<String, Long> townshipIdMap = new HashMap<>();
        
        // 第一步：创建所有乡镇记录
        int townshipOrder = 1;
        for (String townshipName : TOWNSHIP_VILLAGES.keySet()) {
            LocationInfo township = LocationInfo.createTownship(townshipName, townshipOrder++);
            allLocations.add(township);
            System.out.println("准备插入乡镇: " + townshipName);
        }
        
        // 批量插入乡镇
        repository.batchInsert(allLocations, new LocationInfoRepository.OnResultListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                System.out.println("乡镇数据插入成功，开始插入村庄数据...");
                
                // 查询乡镇ID并插入村庄
                insertVillages(repository);
            }

            @Override
            public void onError(Exception e) {
                System.err.println("乡镇数据插入失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 插入村庄数据
     */
    private static void insertVillages(LocationInfoRepository repository) {
        // 查询所有乡镇以获取ID
        repository.findAllTownships(new LocationInfoRepository.OnResultListener<List<LocationInfo>>() {
            @Override
            public void onSuccess(List<LocationInfo> townships) {
                System.out.println("查询到 " + townships.size() + " 个乡镇，开始插入村庄...");
                
                // 创建乡镇名称到ID的映射
                Map<String, Long> townshipIdMap = new HashMap<>();
                for (LocationInfo township : townships) {
                    townshipIdMap.put(township.getTownshipName(), township.getId());
                }
                
                // 创建所有村庄记录
                List<LocationInfo> villages = new ArrayList<>();
                
                for (Map.Entry<String, List<String>> entry : TOWNSHIP_VILLAGES.entrySet()) {
                    String townshipName = entry.getKey();
                    List<String> villageNames = entry.getValue();
                    Long townshipId = townshipIdMap.get(townshipName);
                    
                    if (townshipId == null) {
                        System.err.println("找不到乡镇ID: " + townshipName);
                        continue;
                    }
                    
                    int villageOrder = 1;
                    for (String villageName : villageNames) {
                        LocationInfo village = LocationInfo.createVillage(
                            townshipName, villageName, townshipId, villageOrder++);
                        villages.add(village);
                        System.out.println("准备插入村庄: " + townshipName + " - " + villageName);
                    }
                }
                
                // 批量插入村庄
                repository.batchInsert(villages, new LocationInfoRepository.OnResultListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        System.out.println("村庄数据插入成功！");
                        
                        // 验证迁移结果
                        verifyMigration(repository);
                    }

                    @Override
                    public void onError(Exception e) {
                        System.err.println("村庄数据插入失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                System.err.println("查询乡镇失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 验证迁移结果
     */
    private static void verifyMigration(LocationInfoRepository repository) {
        repository.getCount(new LocationInfoRepository.OnResultListener<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                System.out.println("验证迁移结果：数据库中共有 " + count + " 条地区记录");
                
                // 计算预期的记录数
                int expectedTownships = TOWNSHIP_VILLAGES.size();
                int expectedVillages = TOWNSHIP_VILLAGES.values().stream()
                        .mapToInt(List::size)
                        .sum();
                int expectedTotal = expectedTownships + expectedVillages;
                
                System.out.println("预期记录数：" + expectedTotal + " (乡镇: " + expectedTownships + ", 村庄: " + expectedVillages + ")");
                
                if (count == expectedTotal) {
                    System.out.println("✅ 数据迁移验证成功！");
                } else {
                    System.out.println("⚠️  数据迁移可能不完整，请检查日志");
                }
                
                // 显示详细的乡镇村庄统计
                showDetailedStats(repository);
            }

            @Override
            public void onError(Exception e) {
                System.err.println("验证迁移结果失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 显示详细的统计信息
     */
    private static void showDetailedStats(LocationInfoRepository repository) {
        repository.findAllTownships(new LocationInfoRepository.OnResultListener<List<LocationInfo>>() {
            @Override
            public void onSuccess(List<LocationInfo> townships) {
                System.out.println("\n📊 详细统计信息：");
                System.out.println("================================");
                
                for (LocationInfo township : townships) {
                    repository.findVillagesByTownship(township.getTownshipName(), 
                        new LocationInfoRepository.OnResultListener<List<LocationInfo>>() {
                        @Override
                        public void onSuccess(List<LocationInfo> villages) {
                            System.out.println(String.format("📍 %s: %d 个村庄", 
                                township.getTownshipName(), villages.size()));
                            
                            for (LocationInfo village : villages) {
                                System.out.println("  └── " + village.getVillageName());
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            System.err.println("查询村庄失败: " + township.getTownshipName());
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                System.err.println("查询乡镇统计失败: " + e.getMessage());
            }
        });
    }
}