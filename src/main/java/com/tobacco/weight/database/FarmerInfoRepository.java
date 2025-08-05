package com.tobacco.weight.database;

import com.tobacco.weight.data.FarmerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 烟农信息仓库类
 * 管理烟农信息的数据访问逻辑，提供统一的数据接口
 */
public class FarmerInfoRepository {

    private static final Logger logger = LoggerFactory.getLogger(FarmerInfoRepository.class);

    private final FarmerInfoDao farmerInfoDao;
    private final ExecutorService executor;

    public FarmerInfoRepository(DatabaseManager databaseManager) {
        this.farmerInfoDao = new FarmerInfoDao(databaseManager);
        this.executor = Executors.newFixedThreadPool(2);
    }

    /**
     * 结果监听器接口
     */
    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * 插入新的烟农信息
     */
    public void insert(FarmerInfo farmerInfo, OnResultListener<Long> listener) {
        executor.execute(() -> {
            try {
                long id = farmerInfoDao.insert(farmerInfo);
                if (listener != null) {
                    listener.onSuccess(id);
                }
            } catch (Exception e) {
                logger.error("插入烟农信息失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 更新烟农信息
     */
    public void update(FarmerInfo farmerInfo, long id, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int affectedRows = farmerInfoDao.update(farmerInfo, id);
                if (listener != null) {
                    listener.onSuccess(affectedRows);
                }
            } catch (Exception e) {
                logger.error("更新烟农信息失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 删除烟农信息
     */
    public void delete(long id, OnResultListener<Integer> listener) {
        executor.execute(() -> {
            try {
                int affectedRows = farmerInfoDao.delete(id);
                if (listener != null) {
                    listener.onSuccess(affectedRows);
                }
            } catch (Exception e) {
                logger.error("删除烟农信息失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 获取所有烟农信息
     */
    public void findAll(OnResultListener<List<FarmerInfo>> listener) {
        executor.execute(() -> {
            try {
                List<FarmerInfo> farmers = farmerInfoDao.findAll();
                if (listener != null) {
                    listener.onSuccess(farmers);
                }
            } catch (Exception e) {
                logger.error("查询所有烟农信息失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据姓名或合同号搜索烟农
     */
    public void searchByNameOrContract(String searchTerm, OnResultListener<List<FarmerInfo>> listener) {
        executor.execute(() -> {
            try {
                List<FarmerInfo> farmers = farmerInfoDao.searchByNameOrContract(searchTerm);
                if (listener != null) {
                    listener.onSuccess(farmers);
                }
            } catch (Exception e) {
                logger.error("搜索烟农信息失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据地址筛选烟农
     */
    public void findByLocation(String location, OnResultListener<List<FarmerInfo>> listener) {
        executor.execute(() -> {
            try {
                List<FarmerInfo> farmers = farmerInfoDao.findByLocation(location);
                if (listener != null) {
                    listener.onSuccess(farmers);
                }
            } catch (Exception e) {
                logger.error("根据地址筛选烟农信息失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 获取所有不同的地址信息
     */
    public void getAllDistinctLocations(OnResultListener<List<String>> listener) {
        executor.execute(() -> {
            try {
                List<String> locations = farmerInfoDao.getAllDistinctLocations();
                if (listener != null) {
                    listener.onSuccess(locations);
                }
            } catch (Exception e) {
                logger.error("获取地址列表失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 根据身份证号查找烟农
     */
    public void findByIdCardNumber(String idCardNumber, OnResultListener<FarmerInfo> listener) {
        executor.execute(() -> {
            try {
                FarmerInfo farmer = farmerInfoDao.findByIdCardNumber(idCardNumber);
                if (listener != null) {
                    listener.onSuccess(farmer);
                }
            } catch (Exception e) {
                logger.error("根据身份证号查找烟农失败", e);
                if (listener != null) {
                    listener.onError(e);
                }
            }
        });
    }

    /**
     * 同步获取所有烟农信息（用于导出等场景）
     */
    public List<FarmerInfo> findAllSync() throws Exception {
        try {
            return farmerInfoDao.findAll();
        } catch (Exception e) {
            logger.error("同步获取所有烟农信息失败", e);
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
}