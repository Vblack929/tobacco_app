package com.tobacco.weight.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.tobacco.weight.data.dao.FarmerInfoDao;
import com.tobacco.weight.data.database.TobaccoDatabase;
import com.tobacco.weight.data.entity.FarmerInfoEntity;
import com.tobacco.weight.data.FarmerInfo;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * 烟农身份信息仓库类
 * 
 * 职责：
 * - 确保每个身份证号只存储一次
 * - 提供农户信息的增删改查
 * - 管理农户状态和统计
 */
@Singleton
public class FarmerInfoRepository {

    private final FarmerInfoDao farmerInfoDao;
    private final ExecutorService executor;

    @Inject
    public FarmerInfoRepository(Application application) {
        TobaccoDatabase database = TobaccoDatabase.getInstance(application);
        farmerInfoDao = database.farmerInfoDao();
        executor = Executors.newFixedThreadPool(4);
    }

    /**
     * 插入新的烟农信息（如果身份证号已存在则忽略）
     * 这是推荐的插入方式，防止重复存储
     */
    public void insertIfNotExists(FarmerInfo farmerInfo, OnResultListener<Long> listener) {
        if (farmerInfo == null) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("FarmerInfo cannot be null"));
            }
            return;
        }
        
        FarmerInfoEntity entity = FarmerInfoEntity.fromFarmerInfo(farmerInfo);
        
        executor.execute(() -> {
            try {
                // 使用 IGNORE 策略，如果身份证号已存在则忽略插入
                long id = farmerInfoDao.insertIgnore(entity);
                if (listener != null) {
                    if (id > 0) {
                        listener.onSuccess(id);
                    } else {
                        // id = 0 表示记录已存在，被忽略
                        listener.onSuccess(0L); // 可以根据需要修改返回值
                    }
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 检查身份证号是否已存在，如果不存在则插入
     * 这是更安全的方式，先检查再插入
     */
    public void insertIfIdCardNotExists(FarmerInfo farmerInfo, OnResultListener<InsertResult> listener) {
        if (farmerInfo == null || farmerInfo.getIdCardNumber() == null || farmerInfo.getIdCardNumber().trim().isEmpty()) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("FarmerInfo or ID card number cannot be null or empty"));
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                // 先检查是否已存在
                int existingCount = farmerInfoDao.checkIdCardExists(farmerInfo.getIdCardNumber());
                
                if (existingCount > 0) {
                    // 已存在，返回现有记录
                    FarmerInfoEntity existingEntity = farmerInfoDao.getFarmerByIdCardSync(farmerInfo.getIdCardNumber());
                    if (listener != null) {
                        listener.onSuccess(new InsertResult(existingEntity.getId(), false, "身份证号已存在"));
                    }
                } else {
                    // 不存在，插入新记录
                    FarmerInfoEntity entity = FarmerInfoEntity.fromFarmerInfo(farmerInfo);
                    long newId = farmerInfoDao.insertIgnore(entity);
                    if (listener != null) {
                        listener.onSuccess(new InsertResult(newId, true, "新农户信息已保存"));
                    }
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 强制插入或替换烟农信息
     */
    public void insertOrReplace(FarmerInfo farmerInfo, OnResultListener<Long> listener) {
        if (farmerInfo == null) {
            if (listener != null) {
                listener.onError(new IllegalArgumentException("FarmerInfo cannot be null"));
            }
            return;
        }
        
        FarmerInfoEntity entity = FarmerInfoEntity.fromFarmerInfo(farmerInfo);
        
        executor.execute(() -> {
            try {
                long id = farmerInfoDao.insertReplace(entity);
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
     * 更新烟农信息
     */
    public void update(FarmerInfoEntity farmerInfo, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int count = farmerInfoDao.update(farmerInfo);
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
     * 更新首次称重时间
     */
    public void updateFirstRecordTime(String idCardNumber, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int count = farmerInfoDao.updateFirstRecordTime(idCardNumber);
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
     * 删除烟农信息
     */
    public void delete(FarmerInfoEntity farmerInfo, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int count = farmerInfoDao.delete(farmerInfo);
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

    // === 查询方法 ===

    /**
     * 根据身份证号查询烟农信息
     */
    public LiveData<FarmerInfoEntity> getFarmerByIdCard(String idCardNumber) {
        return farmerInfoDao.getFarmerByIdCard(idCardNumber);
    }

    /**
     * 同步方式根据身份证号查询烟农信息
     */
    public void getFarmerByIdCardSync(String idCardNumber, OnResultListener<FarmerInfoEntity> listener) {
        executor.execute(() -> {
            try {
                FarmerInfoEntity farmer = farmerInfoDao.getFarmerByIdCardSync(idCardNumber);
                if (listener != null) {
                    listener.onSuccess(farmer);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据烟农姓名查询烟农信息
     */
    public LiveData<List<FarmerInfoEntity>> getFarmersByName(String farmerName) {
        return farmerInfoDao.getFarmersByName(farmerName);
    }

    /**
     * 获取所有激活的烟农
     */
    public LiveData<List<FarmerInfoEntity>> getAllActiveFarmers() {
        return farmerInfoDao.getAllActiveFarmers();
    }

    /**
     * 获取所有烟农
     */
    public LiveData<List<FarmerInfoEntity>> getAllFarmers() {
        return farmerInfoDao.getAllFarmers();
    }

    /**
     * 获取所有活跃的农户
     */
    public void getAllActiveFarmers(FarmerListCallback callback) {
        executor.execute(() -> {
            try {
                List<FarmerInfoEntity> farmers = farmerInfoDao.getAllActiveFarmersSync();
                if (callback != null) {
                    callback.onSuccess(farmers);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取农户列表失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 检查身份证号是否已存在
     */
    public LiveData<Boolean> isIdCardExists(String idCardNumber) {
        return farmerInfoDao.isIdCardExists(idCardNumber);
    }

    /**
     * 同步检查身份证号是否已存在
     */
    public void checkIdCardExistsSync(String idCardNumber, OnResultListener<Boolean> listener) {
        executor.execute(() -> {
            try {
                int count = farmerInfoDao.checkIdCardExists(idCardNumber);
                if (listener != null) {
                    listener.onSuccess(count > 0);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 验证烟农身份信息
     */
    public void validateFarmerIdentity(String farmerName, String idCardNumber, OnResultListener<Boolean> listener) {
        executor.execute(() -> {
            try {
                boolean isValid = farmerInfoDao.validateFarmerIdentity(farmerName, idCardNumber);
                if (listener != null) {
                    listener.onSuccess(isValid);
                }
            } catch (Exception e) {
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 搜索烟农
     */
    public LiveData<List<FarmerInfoEntity>> searchFarmersByName(String searchQuery) {
        return farmerInfoDao.searchFarmersByName(searchQuery);
    }

    /**
     * 获取烟农总数
     */
    public LiveData<Integer> getActiveFarmerCount() {
        return farmerInfoDao.getActiveFarmerCount();
    }

    /**
     * 获取有完整身份证信息的烟农
     */
    public LiveData<List<FarmerInfoEntity>> getFarmersWithCompleteInfo() {
        return farmerInfoDao.getFarmersWithCompleteInfo();
    }

    /**
     * 获取农户总数（异步回调）
     */
    public void getTotalFarmerCount(CountCallback callback) {
        executor.execute(() -> {
            try {
                int count = farmerInfoDao.getTotalFarmerCount();
                if (callback != null) {
                    callback.onSuccess(count);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onFailure("获取农户总数失败: " + e.getMessage());
                }
            }
        });
    }

    // === 工具方法 ===

    /**
     * 关闭线程池
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    // === 回调接口 ===

    /**
     * 结果回调接口
     */
    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * 插入结果数据类
     */
    public static class InsertResult {
        private final long id;
        private final boolean isNewRecord;
        private final String message;

        public InsertResult(long id, boolean isNewRecord, String message) {
            this.id = id;
            this.isNewRecord = isNewRecord;
            this.message = message;
        }

        public long getId() { return id; }
        public boolean isNewRecord() { return isNewRecord; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "InsertResult{" +
                    "id=" + id +
                    ", isNewRecord=" + isNewRecord +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    /**
     * 农户列表回调接口
     */
    public interface FarmerListCallback {
        void onSuccess(List<FarmerInfoEntity> farmers);
        void onFailure(String error);
    }

    /**
     * 统计数量回调接口
     */
    public interface CountCallback {
        void onSuccess(int count);
        void onFailure(String error);
    }
} 