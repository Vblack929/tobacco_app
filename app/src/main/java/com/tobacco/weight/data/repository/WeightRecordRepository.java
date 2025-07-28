package com.tobacco.weight.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.tobacco.weight.data.dao.WeightRecordDao;
import com.tobacco.weight.data.database.TobaccoDatabase;
import com.tobacco.weight.data.model.WeightRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 称重记录仓库类
 * 管理数据访问逻辑，提供统一的数据接口
 */
@Singleton
public class WeightRecordRepository {

    private final WeightRecordDao weightRecordDao;
    private final ExecutorService executor;

    @Inject
    public WeightRecordRepository(Application application) {
        TobaccoDatabase database = TobaccoDatabase.getInstance(application);
        weightRecordDao = database.weightRecordDao();
        executor = Executors.newFixedThreadPool(4);
    }

    /**
     * 插入新的称重记录
     */
    public void insert(WeightRecord record, OnResultListener<Long> listener) {
        executor.execute(() -> {
            try {
                long id = weightRecordDao.insert(record);
                if (listener != null) {
                    listener.onSuccess(id);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 更新称重记录
     */
    public void update(WeightRecord record, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int count = weightRecordDao.update(record);
                if (listener != null) {
                    listener.onSuccess(count);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 删除称重记录
     */
    public void delete(WeightRecord record, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int count = weightRecordDao.delete(record);
                if (listener != null) {
                    listener.onSuccess(count);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 获取所有记录
     */
    public LiveData<List<WeightRecord>> getAllRecords() {
        return weightRecordDao.getAllRecords();
    }

    /**
     * 获取称重记录总数
     */
    public void getTotalRecordCount(CountCallback callback) {
        executor.execute(() -> {
            try {
                int count = weightRecordDao.getTotalRecordCount();
                if (callback != null) {
                    callback.onSuccess(count);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取记录总数失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取所有记录的总重量
     */
    public void getTotalWeight(WeightCallback callback) {
        executor.execute(() -> {
            try {
                double totalWeight = weightRecordDao.getTotalWeight();
                if (callback != null) {
                    callback.onSuccess(totalWeight);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取总重量失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取所有记录的总叶片数量（总捆数）
     */
    public void getTotalLeafCount(CountCallback callback) {
        executor.execute(() -> {
            try {
                int totalLeafCount = weightRecordDao.getTotalLeafCount();
                if (callback != null) {
                    callback.onSuccess(totalLeafCount);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取总叶片数量失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取指定农户的记录统计
     */
    public void getFarmerRecordStatistics(String idCardNumber, FarmerStatsCallback callback) {
        executor.execute(() -> {
            try {
                int totalLeafCount = weightRecordDao.getFarmerTotalLeafCount(idCardNumber);
                double totalWeight = weightRecordDao.getFarmerTotalWeight(idCardNumber);
                Long lastRecordTimestamp = weightRecordDao.getFarmerLastRecordTimestamp(idCardNumber);
                
                String lastRecordDate = null;
                if (lastRecordTimestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    lastRecordDate = sdf.format(new Date(lastRecordTimestamp));
                }
                
                if (callback != null) {
                    callback.onSuccess(totalLeafCount, totalWeight, lastRecordDate);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取农户统计失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取指定农户的完整统计信息（包含记录数和捆数）
     */
    public void getFarmerCompleteStatistics(String idCardNumber, CompleteStatsCallback callback) {
        executor.execute(() -> {
            try {
                int recordCount = weightRecordDao.getFarmerRecordCount(idCardNumber);
                int totalLeafCount = weightRecordDao.getFarmerTotalLeafCount(idCardNumber);
                double totalWeight = weightRecordDao.getFarmerTotalWeight(idCardNumber);
                Long lastRecordTimestamp = weightRecordDao.getFarmerLastRecordTimestamp(idCardNumber);
                
                String lastRecordDate = null;
                if (lastRecordTimestamp != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    lastRecordDate = sdf.format(new Date(lastRecordTimestamp));
                }
                
                if (callback != null) {
                    callback.onSuccess(recordCount, totalLeafCount, totalWeight, lastRecordDate);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取农户完整统计失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取指定农户的叶型统计详情（包含数量和重量）
     */
    public void getFarmerLeafTypeStatistics(String idCardNumber, LeafTypeStatsCallback callback) {
        executor.execute(() -> {
            try {
                // 获取各部叶的数量和重量
                int upperCount = weightRecordDao.getLeafTypeCountByIdCard(idCardNumber, "上部叶");
                int middleCount = weightRecordDao.getLeafTypeCountByIdCard(idCardNumber, "中部叶");
                int lowerCount = weightRecordDao.getLeafTypeCountByIdCard(idCardNumber, "下部叶");
                
                double upperWeight = weightRecordDao.getLeafTypeWeightByIdCard(idCardNumber, "上部叶");
                double middleWeight = weightRecordDao.getLeafTypeWeightByIdCard(idCardNumber, "中部叶");
                double lowerWeight = weightRecordDao.getLeafTypeWeightByIdCard(idCardNumber, "下部叶");
                
                // 计算总数量和总重量
                int totalCount = upperCount + middleCount + lowerCount;
                double totalWeight = upperWeight + middleWeight + lowerWeight;
                
                // 计算比例
                LeafTypeRatios ratios = new LeafTypeRatios();
                if (totalCount > 0) {
                    ratios.upperCountRatio = (upperCount * 100.0) / totalCount;
                    ratios.middleCountRatio = (middleCount * 100.0) / totalCount;
                    ratios.lowerCountRatio = (lowerCount * 100.0) / totalCount;
                }
                
                if (totalWeight > 0) {
                    ratios.upperWeightRatio = (upperWeight * 100.0) / totalWeight;
                    ratios.middleWeightRatio = (middleWeight * 100.0) / totalWeight;
                    ratios.lowerWeightRatio = (lowerWeight * 100.0) / totalWeight;
                }
                
                // 设置原始数据
                ratios.upperCount = upperCount;
                ratios.middleCount = middleCount;
                ratios.lowerCount = lowerCount;
                ratios.upperWeight = upperWeight;
                ratios.middleWeight = middleWeight;
                ratios.lowerWeight = lowerWeight;
                ratios.totalCount = totalCount;
                ratios.totalWeight = totalWeight;
                
                if (callback != null) {
                    callback.onSuccess(ratios);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取叶型统计失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 获取指定烟农姓名获取记录
     */
    public LiveData<List<WeightRecord>> getRecordsByFarmerName(String farmerName) {
        return weightRecordDao.getRecordsByFarmerName(farmerName);
    }

    /**
     * 获取指定身份证号的所有记录
     */
    public LiveData<List<WeightRecord>> getRecordsByIdCard(String idCardNumber) {
        return weightRecordDao.getRecordsByIdCard(idCardNumber);
    }

    /**
     * 根据预检编号获取记录
     */
    public LiveData<List<WeightRecord>> getRecordsByPrecheckNumber(String precheckNumber) {
        return weightRecordDao.getRecordsByPrecheckNumber(precheckNumber);
    }

    /**
     * 根据日期范围获取记录
     */
    public LiveData<List<WeightRecord>> getRecordsByDateRange(long startTime, long endTime) {
        return weightRecordDao.getRecordsByDateRange(startTime, endTime);
    }

    /**
     * 获取指定烟农的总重量
     */
    public LiveData<Double> getTotalWeightByFarmer(String farmerName) {
        return weightRecordDao.getTotalWeightByFarmer(farmerName);
    }

    /**
     * 获取指定烟农的记录数量
     */
    public LiveData<Integer> getRecordCountByFarmer(String farmerName) {
        return weightRecordDao.getRecordCountByFarmer(farmerName);
    }

    /**
     * 清空所有记录
     */
    public void deleteAll(OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int count = weightRecordDao.deleteAll();
                if (listener != null) {
                    listener.onSuccess(count);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * 结果回调接口
     */
    public interface OnResultListener<T> {
        void onSuccess(T result);

        void onError(Exception e);
    }

    // === 回调接口 ===

    /**
     * 计数回调接口
     */
    public interface CountCallback {
        void onSuccess(int count);
        void onFailure(String error);
    }

    /**
     * 重量回调接口
     */
    public interface WeightCallback {
        void onSuccess(double weight);
        void onFailure(String error);
    }

    /**
     * 农户统计回调接口
     */
    public interface FarmerStatsCallback {
        void onSuccess(int totalLeafCount, double totalWeight, String lastRecordDate);
        void onFailure(String error);
    }

    /**
     * 完整统计回调接口
     */
    public interface CompleteStatsCallback {
        void onSuccess(int recordCount, int totalLeafCount, double totalWeight, String lastRecordDate);
        void onFailure(String error);
    }

    /**
     * 叶型统计回调接口
     */
    public interface LeafTypeStatsCallback {
        void onSuccess(LeafTypeRatios ratios);
        void onFailure(String error);
    }

    /**
     * 叶型统计数据模型
     */
    public static class LeafTypeRatios {
        public int upperCount;
        public int middleCount;
        public int lowerCount;
        public double upperWeight;
        public double middleWeight;
        public double lowerWeight;
        public double upperCountRatio;
        public double middleCountRatio;
        public double lowerCountRatio;
        public double upperWeightRatio;
        public double middleWeightRatio;
        public double lowerWeightRatio;
        public int totalCount;
        public double totalWeight;
    }
}