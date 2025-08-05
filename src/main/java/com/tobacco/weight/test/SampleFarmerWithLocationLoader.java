package com.tobacco.weight.test;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.FarmerInfoRepository;

/**
 * 带地区信息的农户样本数据加载器
 * 用于测试乡镇村庄筛选功能
 */
public class SampleFarmerWithLocationLoader {

    public static void main(String[] args) {
        System.out.println("开始加载带地区信息的样本农户数据...");

        try {
            // 初始化数据库和仓库
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            FarmerInfoRepository repository = new FarmerInfoRepository(databaseManager);

            // 创建带地区信息的样本农户数据
            FarmerInfo[] sampleFarmers = {
                // 永安镇的农户
                createFarmerWithLocation("张三", "2024001", "110101199001011234", "男", "永安镇", "永和村"),
                createFarmerWithLocation("李四", "2024002", "110101199002021234", "女", "永安镇", "丰裕村"),
                createFarmerWithLocation("王五", "2024003", "110101199003031234", "男", "永安镇", "西湖潭村"),
                
                // 枨冲镇的农户
                createFarmerWithLocation("赵六", "2024004", "110101199004041234", "女", "枨冲镇", "三元村"),
                createFarmerWithLocation("钱七", "2024005", "110101199005051234", "男", "枨冲镇", "平息村"),
                
                // 沙市的农户
                createFarmerWithLocation("孙八", "2024006", "110101199006061234", "女", "沙市", "长春"),
                createFarmerWithLocation("周九", "2024007", "110101199007071234", "男", "沙市", "赤马"),
                createFarmerWithLocation("吴十", "2024008", "110101199008081234", "女", "沙市", "友助"),
                createFarmerWithLocation("郑十一", "2024009", "110101199009091234", "男", "沙市", "白水"),
                
                // 龙伏的农户
                createFarmerWithLocation("王十二", "2024010", "110101199010101234", "女", "龙伏", "坪上"),
                createFarmerWithLocation("李十三", "2024011", "110101199011111234", "男", "龙伏", "新开"),
                
                // 社港的农户
                createFarmerWithLocation("张十四", "2024012", "110101199012121234", "女", "社港", "合盛"),
                createFarmerWithLocation("陈十五", "2024013", "110101199001131234", "男", "社港", "淮洲"),
                
                // 淳口的农户
                createFarmerWithLocation("刘十六", "2024014", "110101199002141234", "女", "淳口", "高田"),
                createFarmerWithLocation("黄十七", "2024015", "110101199003151234", "男", "淳口", "鹤源"),
                
                // 北盛的农户
                createFarmerWithLocation("杨十八", "2024016", "110101199004161234", "女", "北盛", "拔茅"),
                createFarmerWithLocation("林十九", "2024017", "110101199005171234", "男", "北盛", "百塘"),
                
                // 大围山的农户
                createFarmerWithLocation("何二十", "2024018", "110101199006181234", "女", "大围山", "中岳村"),
                createFarmerWithLocation("马二一", "2024019", "110101199007191234", "男", "大围山", "北麓园村"),
                
                // 官渡的农户
                createFarmerWithLocation("徐二二", "2024020", "110101199008201234", "女", "官渡", "观音塘村"),
                createFarmerWithLocation("罗二三", "2024021", "110101199009211234", "男", "官渡", "田郊村")
            };

            // 逐个插入数据
            for (int i = 0; i < sampleFarmers.length; i++) {
                final int index = i;
                final FarmerInfo farmer = sampleFarmers[i];

                repository.insert(farmer, new FarmerInfoRepository.OnResultListener<Long>() {
                    @Override
                    public void onSuccess(Long id) {
                        LocationData.LocationInfo locationInfo = LocationData.parseAddress(farmer.getAddress());
                        System.out.println("成功插入农户 " + (index + 1) + ": " + 
                                farmer.getFarmerName() + " - " + 
                                locationInfo.getTownship() + " " + locationInfo.getVillage() + 
                                " (ID: " + id + ")");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.err.println("插入农户 " + (index + 1) + " 失败: " + e.getMessage());
                    }
                });
            }

            // 等待所有异步操作完成
            Thread.sleep(5000);

            // 关闭仓库
            repository.shutdown();
            databaseManager.closeConnection();

            System.out.println("带地区信息的样本农户数据加载完成！");
            System.out.println("现在可以测试按乡镇和村庄筛选功能了。");

        } catch (Exception e) {
            System.err.println("加载样本数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建带地区信息的农户
     */
    private static FarmerInfo createFarmerWithLocation(String name, String contractNumber, 
                                                     String idCardNumber, String gender,
                                                     String township, String village) {
        String address = LocationData.formatAddress(township, village);
        return FarmerInfo.createWithIdCard(
                name, contractNumber, idCardNumber,
                gender, "汉族", "1990-01-01",
                address,
                "某某市公安局", "2010-01-01", "2030-01-01", null
        );
    }
}