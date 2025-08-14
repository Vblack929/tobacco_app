package com.tobacco.weight.database;

import com.tobacco.weight.data.WeighingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 称重记录仓库类
 * 管理数据访问逻辑，提供统一的数据接口
 */
public class WeighingRecordRepository {

    private static final Logger logger = LoggerFactory.getLogger(WeighingRecordRepository.class);

    private final WeighingRecordDao weighingRecordDao;
    private final ExecutorService executor;

    public WeighingRecordRepository(DatabaseManager databaseManager) {
        this.weighingRecordDao = new WeighingRecordDao(databaseManager);
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * 插入新的称重记录
     */
    public void insert(WeighingRecord record, OnResultListener<Long> listener) {
        executor.execute(() -> {
            try {
                long id = weighingRecordDao.insert(record);
                if (listener != null) {
                    listener.onSuccess(id);
                }
            } catch (Exception e) {
                logger.error("插入称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 更新称重记录
     */
    public void update(WeighingRecord record, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int affectedRows = weighingRecordDao.update(record);
                if (listener != null) {
                    listener.onSuccess(affectedRows);
                }
            } catch (Exception e) {
                logger.error("更新称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 删除称重记录
     */
    public void delete(long id, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int affectedRows = weighingRecordDao.delete(id);
                if (listener != null) {
                    listener.onSuccess(affectedRows);
                }
            } catch (Exception e) {
                logger.error("删除称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据ID查询记录
     */
    public void findById(long id, OnResultListener<WeighingRecord> listener) {
        executor.execute(() -> {
            try {
                WeighingRecord record = weighingRecordDao.findById(id);
                if (listener != null) {
                    listener.onSuccess(record);
                }
            } catch (Exception e) {
                logger.error("查询称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 获取所有记录
     */
    public void findAll(OnResultListener<List<WeighingRecord>> listener) {
        executor.execute(() -> {
            try {
                List<WeighingRecord> records = weighingRecordDao.findAll();
                if (listener != null) {
                    listener.onSuccess(records);
                }
            } catch (Exception e) {
                logger.error("查询所有称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据烟农姓名查询记录
     */
    public void findByFarmerName(String farmerName, OnResultListener<List<WeighingRecord>> listener) {
        executor.execute(() -> {
            try {
                List<WeighingRecord> records = weighingRecordDao.findByFarmerName(farmerName);
                if (listener != null) {
                    listener.onSuccess(records);
                }
            } catch (Exception e) {
                logger.error("根据烟农姓名查询称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据身份证号查询记录
     */
    public void findByIdCardNumber(String idCardNumber, OnResultListener<List<WeighingRecord>> listener) {
        executor.execute(() -> {
            try {
                List<WeighingRecord> records = weighingRecordDao.findByIdCardNumber(idCardNumber);
                if (listener != null) {
                    listener.onSuccess(records);
                }
            } catch (Exception e) {
                logger.error("根据身份证号查询称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据预检编号查询记录
     */
    public void findByPrecheckId(String precheckId, OnResultListener<List<WeighingRecord>> listener) {
        executor.execute(() -> {
            try {
                List<WeighingRecord> records = weighingRecordDao.findByPrecheckId(precheckId);
                if (listener != null) {
                    listener.onSuccess(records);
                }
            } catch (Exception e) {
                logger.error("根据预检编号查询称重记录失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 获取记录总数
     */
    public void getCount(OnResultListener<Long> listener) {
        executor.execute(() -> {
            try {
                long count = weighingRecordDao.getCount();
                if (listener != null) {
                    listener.onSuccess(count);
                }
            } catch (Exception e) {
                logger.error("获取称重记录总数失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 同步获取所有记录（用于导出等场景）
     */
    public List<WeighingRecord> findAllSync() throws Exception {
        try {
            return weighingRecordDao.findAll();
        } catch (Exception e) {
            logger.error("同步获取所有记录失败", e);
            throw e;
        }
    }

    /**
     * 获取按合同号聚合的累计重量（作为"合同量"展示）
     */
    public java.util.Map<String, Double> getTotalWeightByContractSync() throws Exception {
        try {
            return weighingRecordDao.getTotalWeightByContract();
        } catch (Exception e) {
            logger.error("获取合同累计重量失败", e);
            throw e;
        }
    }

    /**
     * 获取合同表中的合同量信息
     */
    public java.util.Map<String, Double> getContractAmountsSync() throws Exception {
        try {
            return weighingRecordDao.getContractAmountsByContract();
        } catch (Exception e) {
            logger.error("获取合同量信息失败", e);
            throw e;
        }
    }

    /**
     * 关闭仓库
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * 结果监听器接口
     */
    public interface OnResultListener<T> {
        void onSuccess(T result);

        void onError(Exception e);
    }
}