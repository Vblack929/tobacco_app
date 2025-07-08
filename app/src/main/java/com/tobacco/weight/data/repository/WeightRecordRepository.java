package com.tobacco.weight.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.tobacco.weight.data.dao.WeightRecordDao;
import com.tobacco.weight.data.database.TobaccoDatabase;
import com.tobacco.weight.data.model.WeightRecord;

import java.util.List;
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
     * 根据烟农姓名获取记录
     */
    public LiveData<List<WeightRecord>> getRecordsByFarmerName(String farmerName) {
        return weightRecordDao.getRecordsByFarmerName(farmerName);
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
     * 结果回调接口
     */
    public interface OnResultListener<T> {
        void onSuccess(T result);

        void onError(Exception e);
    }
}