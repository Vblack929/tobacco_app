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
     * 根据身份证号查询记录（用于数据库链接）
     */
    @Query("SELECT * FROM weight_records WHERE id_card_number = :idCardNumber ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getRecordsByIdCardNumber(String idCardNumber);

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
     * 获取指定烟农的各部叶重量统计（按姓名）
     */
    @Query("SELECT tobacco_part, SUM(weight) as total_weight, COUNT(*) as total_count FROM weight_records WHERE farmer_name = :farmerName GROUP BY tobacco_part")
    LiveData<List<LeafTypeStatistics>> getLeafTypeStatisticsByFarmer(String farmerName);
    
    /**
     * 获取指定烟农的各部叶重量统计（按身份证号）
     */
    @Query("SELECT tobacco_part, SUM(weight) as total_weight, COUNT(*) as total_count FROM weight_records WHERE id_card_number = :idCardNumber GROUP BY tobacco_part")
    LiveData<List<LeafTypeStatistics>> getLeafTypeStatisticsByIdCard(String idCardNumber);
    
    /**
     * 获取指定烟农的各部叶综合统计（按身份证号）- 包含数量和重量
     */
    @Query("SELECT tobacco_part, SUM(weight) as total_weight, COUNT(*) as total_count FROM weight_records WHERE id_card_number = :idCardNumber GROUP BY tobacco_part")
    List<LeafTypeStatistics> getLeafTypeDetailsByIdCard(String idCardNumber);
    
    /**
     * 获取指定烟农各部叶的数量统计（按身份证号）
     */
    @Query("SELECT COUNT(*) FROM weight_records WHERE id_card_number = :idCardNumber AND tobacco_part = :tobaccoPart")
    int getLeafTypeCountByIdCard(String idCardNumber, String tobaccoPart);
    
    /**
     * 获取指定烟农各部叶的重量统计（按身份证号）
     */
    @Query("SELECT COALESCE(SUM(weight), 0.0) FROM weight_records WHERE id_card_number = :idCardNumber AND tobacco_part = :tobaccoPart")
    double getLeafTypeWeightByIdCard(String idCardNumber, String tobaccoPart);
    
    /**
     * 根据身份证号获取烟农的总重量
     */
    @Query("SELECT SUM(weight) FROM weight_records WHERE id_card_number = :idCardNumber")
    LiveData<Double> getTotalWeightByIdCard(String idCardNumber);
    
    /**
     * 根据身份证号获取烟农的记录数量
     */
    @Query("SELECT COUNT(*) FROM weight_records WHERE id_card_number = :idCardNumber")
    LiveData<Integer> getRecordCountByIdCard(String idCardNumber);
    
    /**
     * 获取所有不同的烟农身份证号（用于统计）
     */
    @Query("SELECT DISTINCT id_card_number FROM weight_records WHERE id_card_number IS NOT NULL AND id_card_number != ''")
    LiveData<List<String>> getAllFarmerIdCards();

    /**
     * 获取称重记录总数
     */
    @Query("SELECT COUNT(*) FROM weight_records")
    int getTotalRecordCount();

    /**
     * 获取所有记录的总重量
     */
    @Query("SELECT COALESCE(SUM(weight), 0.0) FROM weight_records")
    double getTotalWeight();

    /**
     * 获取所有记录的总叶片数量（总捆数）
     */
    @Query("SELECT COALESCE(SUM(tobacco_bundles), 0) FROM weight_records")
    int getTotalLeafCount();

    /**
     * 获取指定身份证号的记录数量
     */
    @Query("SELECT COUNT(*) FROM weight_records WHERE id_card_number = :idCardNumber")
    int getFarmerRecordCount(String idCardNumber);

    /**
     * 获取指定身份证号的总重量
     */
    @Query("SELECT COALESCE(SUM(weight), 0.0) FROM weight_records WHERE id_card_number = :idCardNumber")
    double getFarmerTotalWeight(String idCardNumber);

    /**
     * 获取指定身份证号的总叶片数量（总捆数）
     */
    @Query("SELECT COALESCE(SUM(tobacco_bundles), 0) FROM weight_records WHERE id_card_number = :idCardNumber")
    int getFarmerTotalLeafCount(String idCardNumber);

    /**
     * 获取指定身份证号的最近记录时间戳
     */
    @Query("SELECT timestamp FROM weight_records WHERE id_card_number = :idCardNumber ORDER BY timestamp DESC LIMIT 1")
    Long getFarmerLastRecordTimestamp(String idCardNumber);

    /**
     * 根据身份证号获取记录
     */
    @Query("SELECT * FROM weight_records WHERE id_card_number = :idCardNumber ORDER BY timestamp DESC")
    LiveData<List<WeightRecord>> getRecordsByIdCard(String idCardNumber);

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
    public int total_count;
    
    public LeafTypeStatistics() {}
    
    public LeafTypeStatistics(String tobacco_part, double total_weight, int total_count) {
        this.tobacco_part = tobacco_part;
        this.total_weight = total_weight;
        this.total_count = total_count;
    }
}