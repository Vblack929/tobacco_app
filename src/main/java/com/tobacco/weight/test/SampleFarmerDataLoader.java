package com.tobacco.weight.test;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.FarmerInfoRepository;

/**
 * 样本农户数据加载器
 * 用于向数据库中添加测试用的农户数据
 */
public class SampleFarmerDataLoader {

    public static void main(String[] args) {
        System.out.println("开始加载样本农户数据...");

        try {
            // 初始化数据库和仓库
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            FarmerInfoRepository repository = new FarmerInfoRepository(databaseManager);

            // 创建样本农户数据
            FarmerInfo[] sampleFarmers = {
                FarmerInfo.createWithIdCard(
                    "张三", "2024001", "110101199001011234",
                    "男", "汉族", "1990-01-01",
                    "北京市朝阳区某某乡某某村",
                    "北京市公安局朝阳分局", "2010-01-01", "2030-01-01", null
                ),
                FarmerInfo.createWithIdCard(
                    "李四", "2024002", "110101199002021234",
                    "女", "汉族", "1990-02-02",
                    "北京市海淀区某某乡某某村",
                    "北京市公安局海淀分局", "2010-02-02", "2030-02-02", null
                ),
                FarmerInfo.createWithIdCard(
                    "王五", "2024003", "110101199003031234",
                    "男", "蒙古族", "1990-03-03",
                    "内蒙古自治区某某盟某某旗某某乡某某村",
                    "内蒙古自治区公安厅", "2010-03-03", "2030-03-03", null
                ),
                FarmerInfo.createWithIdCard(
                    "赵六", "2024004", "110101199004041234",
                    "女", "回族", "1990-04-04",
                    "宁夏回族自治区某某市某某乡某某村",
                    "宁夏公安厅", "2010-04-04", "2030-04-04", null
                ),
                FarmerInfo.createWithIdCard(
                    "钱七", "2024005", "110101199005051234",
                    "男", "汉族", "1990-05-05",
                    "云南省某某州某某县某某乡某某村",
                    "云南省公安厅", "2010-05-05", "2030-05-05", null
                ),
                FarmerInfo.createWithIdCard(
                    "孙八", "2024006", "110101199006061234",
                    "女", "壮族", "1990-06-06",
                    "广西壮族自治区某某市某某乡某某村",
                    "广西公安厅", "2010-06-06", "2030-06-06", null
                ),
                FarmerInfo.createWithIdCard(
                    "周九", "2024007", "110101199007071234",
                    "男", "藏族", "1990-07-07",
                    "西藏自治区某某地区某某县某某乡某某村",
                    "西藏公安厅", "2010-07-07", "2030-07-07", null
                ),
                FarmerInfo.createWithIdCard(
                    "吴十", "2024008", "110101199008081234",
                    "女", "维吾尔族", "1990-08-08",
                    "新疆维吾尔自治区某某地区某某县某某乡某某村",
                    "新疆公安厅", "2010-08-08", "2030-08-08", null
                ),
                FarmerInfo.createWithIdCard(
                    "郑十一", "2024009", "110101199009091234",
                    "男", "朝鲜族", "1990-09-09",
                    "吉林省某某市某某县某某乡某某村",
                    "吉林省公安厅", "2010-09-09", "2030-09-09", null
                ),
                FarmerInfo.createWithIdCard(
                    "王十二", "2024010", "110101199010101234",
                    "女", "满族", "1990-10-10",
                    "辽宁省某某市某某县某某乡某某村",
                    "辽宁省公安厅", "2010-10-10", "2030-10-10", null
                )
            };

            // 逐个插入数据
            for (int i = 0; i < sampleFarmers.length; i++) {
                final int index = i;
                final FarmerInfo farmer = sampleFarmers[i];

                repository.insert(farmer, new FarmerInfoRepository.OnResultListener<Long>() {
                    @Override
                    public void onSuccess(Long id) {
                        System.out.println("成功插入农户 " + (index + 1) + ": " + 
                                farmer.getFarmerName() + " (ID: " + id + ")");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.err.println("插入农户 " + (index + 1) + " 失败: " + e.getMessage());
                    }
                });
            }

            // 等待所有异步操作完成
            Thread.sleep(3000);

            // 关闭仓库
            repository.shutdown();
            databaseManager.closeConnection();

            System.out.println("样本农户数据加载完成！");

        } catch (Exception e) {
            System.err.println("加载样本数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}