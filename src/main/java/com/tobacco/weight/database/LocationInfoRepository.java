package com.tobacco.weight.database;

import com.tobacco.weight.data.LocationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 地区信息仓库
 * 提供异步的地区信息数据访问方法
 */
public class LocationInfoRepository {

    private static final Logger logger = LoggerFactory.getLogger(LocationInfoRepository.class);
    private final LocationInfoDao locationInfoDao;
    private final ExecutorService executorService;

    public LocationInfoRepository(DatabaseManager databaseManager) {
        this.locationInfoDao = new LocationInfoDao(databaseManager);
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("LocationInfoRepository-" + thread.getId());
            return thread;
        });
    }

    /**
     * 结果监听器接口
     */
    public interface OnResultListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * 查询所有乡镇
     */
    public void findAllTownships(OnResultListener<List<LocationInfo>> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.findAllTownships();
            } catch (Exception e) {
                logger.error("查询所有乡镇失败", e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 根据乡镇查询村庄
     */
    public void findVillagesByTownship(String townshipName, OnResultListener<List<LocationInfo>> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.findVillagesByTownship(townshipName);
            } catch (Exception e) {
                logger.error("根据乡镇查询村庄失败: {}", townshipName, e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 根据乡镇ID查询村庄
     */
    public void findVillagesByTownshipId(Long townshipId, OnResultListener<List<LocationInfo>> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.findVillagesByTownshipId(townshipId);
            } catch (Exception e) {
                logger.error("根据乡镇ID查询村庄失败: {}", townshipId, e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 查询所有地区信息
     */
    public void findAll(OnResultListener<List<LocationInfo>> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.findAll();
            } catch (Exception e) {
                logger.error("查询所有地区信息失败", e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 根据ID查询地区信息
     */
    public void findById(Long id, OnResultListener<LocationInfo> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.findById(id);
            } catch (Exception e) {
                logger.error("根据ID查询地区信息失败: {}", id, e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 根据乡镇和村庄名称查询
     */
    public void findByTownshipAndVillage(String townshipName, String villageName, OnResultListener<LocationInfo> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.findByTownshipAndVillage(townshipName, villageName);
            } catch (Exception e) {
                logger.error("根据乡镇和村庄查询失败: {} {}", townshipName, villageName, e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 插入地区信息
     */
    public void insert(LocationInfo location, OnResultListener<Long> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.insert(location);
            } catch (Exception e) {
                logger.error("插入地区信息失败", e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 批量插入地区信息
     */
    public void batchInsert(List<LocationInfo> locations, OnResultListener<Void> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                locationInfoDao.batchInsert(locations);
                return null;
            } catch (Exception e) {
                logger.error("批量插入地区信息失败", e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(null);
            }
        });
    }

    /**
     * 更新地区信息
     */
    public void update(LocationInfo location, OnResultListener<Integer> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.update(location);
            } catch (Exception e) {
                logger.error("更新地区信息失败", e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 软删除地区信息
     */
    public void softDelete(Long id, OnResultListener<Integer> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.softDelete(id);
            } catch (Exception e) {
                logger.error("软删除地区信息失败: {}", id, e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 检查地区信息是否存在
     */
    public void exists(String townshipName, String villageName, OnResultListener<Boolean> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.exists(townshipName, villageName);
            } catch (Exception e) {
                logger.error("检查地区信息是否存在失败: {} {}", townshipName, villageName, e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 获取记录总数
     */
    public void getCount(OnResultListener<Integer> listener) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return locationInfoDao.getCount();
            } catch (Exception e) {
                logger.error("获取地区信息总数失败", e);
                throw new RuntimeException(e);
            }
        }, executorService).whenComplete((result, throwable) -> {
            if (throwable != null) {
                listener.onError(new Exception(throwable));
            } else {
                listener.onSuccess(result);
            }
        });
    }

    /**
     * 同步方法：查询所有乡镇
     */
    public List<LocationInfo> getAllTownshipsSync() throws Exception {
        return locationInfoDao.findAllTownships();
    }

    /**
     * 同步方法：根据乡镇查询村庄
     */
    public List<LocationInfo> getVillagesByTownshipSync(String townshipName) throws Exception {
        return locationInfoDao.findVillagesByTownship(townshipName);
    }

    /**
     * 同步方法：批量插入
     */
    public void batchInsertSync(List<LocationInfo> locations) throws Exception {
        locationInfoDao.batchInsert(locations);
    }

    /**
     * 关闭仓库，释放资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            logger.info("LocationInfoRepository 已关闭");
        }
    }
}