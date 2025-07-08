package com.tobacco.weight.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.tobacco.weight.data.model.WeightRecord;

import java.util.List;

/**
 * 称重记录数据访问接口
 */
@Dao
public interface WeightRecordDao {

    /**
     * 插入新的称重记录
     */
    @Insert
    long insert(WeightRecord record);

    /**
     * 更新称重记录
     */
    @Update
    int update(WeightRecord record);

    /**
     * 删除称重记录
     */
    @Delete
    int delete(WeightRecord record);

    /**
     * 根据ID查询记录
     */
    @Query("SELECT * FROM weight_records WHERE id = :id")
    LiveData<WeightRecord> getRecordById(long id);

    /**
     * 获取所有记录（按时间倒序）
     */
    @Query("SELECT * FROM weight_records ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getAllRecords();

    /**
     * 根据烟农姓名查询记录
     */
    @Query("SELECT * FROM weight_records WHERE farmer_name = :farmerName ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getRecordsByFarmerName(String farmerName);

    /**
     * 根据预检编号查询记录
     */
    @Query("SELECT * FROM weight_records WHERE pre_check_number = :precheckNumber ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getRecordsByPrecheckNumber(String precheckNumber);

    /**
     * 根据日期范围查询记录
     */
    @Query("SELECT * FROM weight_records WHERE create_time BETWEEN :startTime AND :endTime ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getRecordsByDateRange(long startTime, long endTime);

    /**
     * 获取指定烟农的总重量
     */
    @Query("SELECT SUM(weight) FROM weight_records WHERE farmer_name = :farmerName")
    LiveData<Double> getTotalWeightByFarmer(String farmerName);

    /**
     * 获取指定烟农的记录数量
     */
    @Query("SELECT COUNT(*) FROM weight_records WHERE farmer_name = :farmerName")
    LiveData<Integer> getRecordCountByFarmer(String farmerName);

    /**
     * 获取指定烟农的各部叶重量统计
     */
    @Query("SELECT tobacco_part, SUM(weight) as total_weight FROM weight_records WHERE farmer_name = :farmerName GROUP BY tobacco_part")
    LiveData<List<LeafTypeStatistics>> getLeafTypeStatisticsByFarmer(String farmerName);

    /**
     * 清空所有记录
     */
    @Query("DELETE FROM weight_records")
    int deleteAll();
}

/**
 * 部叶类型统计数据
 */
class LeafTypeStatistics {
    public String tobacco_part;
    public double total_weight;
}