package com.tobacco.weight.test;

import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.WeighingRecordRepository;

import java.util.Date;

/**
 * 样本称重数据加载器
 * 用于向数据库中添加测试用的称重记录数据
 */
public class SampleWeighingDataLoader {

    public static void main(String[] args) {
        System.out.println("开始加载样本称重数据...");

        try {
            // 初始化数据库和仓库
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            WeighingRecordRepository repository = new WeighingRecordRepository(databaseManager);

            // 创建样本称重记录数据
            WeighingRecord[] sampleRecords = {
                createRecord("PK24080001", "张三", "2024001", "上部叶", 25.5, "操作员A", "110101199001011234"),
                createRecord("PK24080002", "张三", "2024001", "中部叶", 32.8, "操作员A", "110101199001011234"),
                createRecord("PK24080003", "李四", "2024002", "上部叶", 28.3, "操作员B", "110101199002021234"),
                createRecord("PK24080004", "李四", "2024002", "下部叶", 45.2, "操作员B", "110101199002021234"),
                createRecord("PK24080005", "王五", "2024003", "中部叶", 38.7, "操作员A", "110101199003031234"),
                createRecord("PK24080006", "王五", "2024003", "上部叶", 29.1, "操作员A", "110101199003031234"),
                createRecord("PK24080007", "赵六", "2024004", "下部叶", 42.6, "操作员C", "110101199004041234"),
                createRecord("PK24080008", "钱七", "2024005", "上部叶", 31.4, "操作员B", "110101199005051234"),
                createRecord("PK24080009", "钱七", "2024005", "中部叶", 27.9, "操作员B", "110101199005051234"),
                createRecord("PK24080010", "孙八", "2024006", "下部叶", 39.8, "操作员C", "110101199006061234"),
                createRecord("PK24080011", "孙八", "2024006", "上部叶", 26.3, "操作员C", "110101199006061234"),
                createRecord("PK24080012", "周九", "2024007", "中部叶", 33.7, "操作员A", "110101199007071234"),
                createRecord("PK24080013", "吴十", "2024008", "上部叶", 30.2, "操作员B", "110101199008081234"),
                createRecord("PK24080014", "吴十", "2024008", "下部叶", 41.5, "操作员B", "110101199008081234"),
                createRecord("PK24080015", "郑十一", "2024009", "中部叶", 35.6, "操作员A", "110101199009091234")
            };

            // 逐个插入数据
            for (int i = 0; i < sampleRecords.length; i++) {
                final int index = i;
                final WeighingRecord record = sampleRecords[i];

                repository.insert(record, new WeighingRecordRepository.OnResultListener<Long>() {
                    @Override
                    public void onSuccess(Long id) {
                        System.out.println("成功插入称重记录 " + (index + 1) + ": " + 
                                record.getFarmerName() + " - " + record.getLeafType() + 
                                " " + record.getWeight() + "kg (ID: " + id + ")");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.err.println("插入称重记录 " + (index + 1) + " 失败: " + e.getMessage());
                    }
                });
            }

            // 等待所有异步操作完成
            Thread.sleep(3000);

            // 关闭仓库
            repository.shutdown();
            databaseManager.closeConnection();

            System.out.println("样本称重数据加载完成！");
            System.out.println("现在农户注册管理界面应该能显示这些农户了。");

        } catch (Exception e) {
            System.err.println("加载样本数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建称重记录
     */
    private static WeighingRecord createRecord(String precheckId, String farmerName, 
                                             String contractNumber, String leafType, 
                                             double weight, String operator, String idCardNumber) {
        WeighingRecord record = new WeighingRecord();
        record.setPrecheckId(precheckId);
        record.setFarmerName(farmerName);
        record.setContractNumber(contractNumber);
        record.setLeafType(leafType);
        record.setWeight(weight);
        record.setOperator(operator);
        record.setIdCardNumber(idCardNumber);
        record.setWarehouseNumber("仓库A");
        record.setStatus("正常");
        record.setTimestamp(new Date());
        record.setBundleCount(1);
        return record;
    }
}