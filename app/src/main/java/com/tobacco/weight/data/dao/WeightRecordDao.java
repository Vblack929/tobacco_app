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
    @Query("SELECT primary_tobacco_part as tobacco_part, SUM(total_weight) as total_weight, COUNT(*) as total_count FROM weight_records WHERE farmer_name = :farmerName GROUP BY primary_tobacco_part")
    LiveData<List<LeafTypeStatistics>> getLeafTypeStatisticsByFarmer(String farmerName);
    
    /**
     * 获取指定烟农的各部叶重量统计（按身份证号）
     */
    @Query("SELECT primary_tobacco_part as tobacco_part, SUM(total_weight) as total_weight, COUNT(*) as total_count FROM weight_records WHERE id_card_number = :idCardNumber GROUP BY primary_tobacco_part")
    LiveData<List<LeafTypeStatistics>> getLeafTypeStatisticsByIdCard(String idCardNumber);
    
    /**
     * 获取指定烟农的各部叶综合统计（按身份证号）- 包含数量和重量
     */
    @Query("SELECT primary_tobacco_part as tobacco_part, SUM(total_weight) as total_weight, COUNT(*) as total_count FROM weight_records WHERE id_card_number = :idCardNumber GROUP BY primary_tobacco_part")
    List<LeafTypeStatistics> getLeafTypeDetailsByIdCard(String idCardNumber);
    
    /**
     * 获取指定烟农各部叶的数量统计（按身份证号）
     */
    @Query("SELECT COUNT(*) FROM weight_records WHERE id_card_number = :idCardNumber AND primary_tobacco_part = :tobaccoPart")
    int getLeafTypeCountByIdCard(String idCardNumber, String tobaccoPart);
    
    /**
     * 获取指定烟农各部叶的重量统计（按身份证号）
     */
    @Query("SELECT COALESCE(SUM(total_weight), 0.0) FROM weight_records WHERE id_card_number = :idCardNumber AND primary_tobacco_part = :tobaccoPart")
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
    @Query("SELECT COALESCE(SUM(total_bundles), 0) FROM weight_records")
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
    @Query("SELECT COALESCE(SUM(total_bundles), 0) FROM weight_records WHERE id_card_number = :idCardNumber")
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

    // === 新增：详细烟叶分级数据查询方法 ===
    
    /**
     * 获取指定身份证号的详细烟叶分级统计
     */
    @Query("SELECT " +
           "SUM(upper_leaf_bundles) as upper_bundles, SUM(upper_leaf_weight) as upper_weight, " +
           "SUM(middle_leaf_bundles) as middle_bundles, SUM(middle_leaf_weight) as middle_weight, " +
           "SUM(lower_leaf_bundles) as lower_bundles, SUM(lower_leaf_weight) as lower_weight, " +
           "SUM(total_bundles) as total_bundles, SUM(total_weight) as total_weight, " +
           "COUNT(*) as record_count " +
           "FROM weight_records WHERE id_card_number = :idCardNumber")
    DetailedLeafStatistics getDetailedLeafStatistics(String idCardNumber);
    
    /**
     * 获取系统总的详细烟叶分级统计
     */
    @Query("SELECT " +
           "SUM(upper_leaf_bundles) as upper_bundles, SUM(upper_leaf_weight) as upper_weight, " +
           "SUM(middle_leaf_bundles) as middle_bundles, SUM(middle_leaf_weight) as middle_weight, " +
           "SUM(lower_leaf_bundles) as lower_bundles, SUM(lower_leaf_weight) as lower_weight, " +
           "SUM(total_bundles) as total_bundles, SUM(total_weight) as total_weight, " +
           "COUNT(*) as record_count " +
           "FROM weight_records")
    DetailedLeafStatistics getSystemDetailedLeafStatistics();
    
    /**
     * 获取指定身份证号农户的上部叶统计
     */
    @Query("SELECT SUM(upper_leaf_bundles) FROM weight_records WHERE id_card_number = :idCardNumber")
    int getFarmerUpperLeafBundles(String idCardNumber);
    
    @Query("SELECT SUM(upper_leaf_weight) FROM weight_records WHERE id_card_number = :idCardNumber")
    double getFarmerUpperLeafWeight(String idCardNumber);
    
    /**
     * 获取指定身份证号农户的中部叶统计
     */
    @Query("SELECT SUM(middle_leaf_bundles) FROM weight_records WHERE id_card_number = :idCardNumber")
    int getFarmerMiddleLeafBundles(String idCardNumber);
    
    @Query("SELECT SUM(middle_leaf_weight) FROM weight_records WHERE id_card_number = :idCardNumber")
    double getFarmerMiddleLeafWeight(String idCardNumber);
    
    /**
     * 获取指定身份证号农户的下部叶统计
     */
    @Query("SELECT SUM(lower_leaf_bundles) FROM weight_records WHERE id_card_number = :idCardNumber")
    int getFarmerLowerLeafBundles(String idCardNumber);
    
    @Query("SELECT SUM(lower_leaf_weight) FROM weight_records WHERE id_card_number = :idCardNumber")
    double getFarmerLowerLeafWeight(String idCardNumber);
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

/**
 * 详细烟叶分级统计数据（支持各部位独立统计）
 */
class DetailedLeafStatistics {
    public int upper_bundles;     // 上部叶捆数
    public double upper_weight;   // 上部叶重量
    public int middle_bundles;    // 中部叶捆数
    public double middle_weight;  // 中部叶重量
    public int lower_bundles;     // 下部叶捆数
    public double lower_weight;   // 下部叶重量
    public int total_bundles;     // 总捆数
    public double total_weight;   // 总重量
    public int record_count;      // 记录数
    
    public DetailedLeafStatistics() {}
    
    public DetailedLeafStatistics(int upper_bundles, double upper_weight,
                                  int middle_bundles, double middle_weight,
                                  int lower_bundles, double lower_weight,
                                  int total_bundles, double total_weight,
                                  int record_count) {
        this.upper_bundles = upper_bundles;
        this.upper_weight = upper_weight;
        this.middle_bundles = middle_bundles;
        this.middle_weight = middle_weight;
        this.lower_bundles = lower_bundles;
        this.lower_weight = lower_weight;
        this.total_bundles = total_bundles;
        this.total_weight = total_weight;
        this.record_count = record_count;
    }
    
    /**
     * 获取上部叶占比（按捆数）
     */
    public double getUpperLeafBundleRatio() {
        return total_bundles > 0 ? (upper_bundles * 100.0 / total_bundles) : 0.0;
    }
    
    /**
     * 获取中部叶占比（按捆数）
     */
    public double getMiddleLeafBundleRatio() {
        return total_bundles > 0 ? (middle_bundles * 100.0 / total_bundles) : 0.0;
    }
    
    /**
     * 获取下部叶占比（按捆数）
     */
    public double getLowerLeafBundleRatio() {
        return total_bundles > 0 ? (lower_bundles * 100.0 / total_bundles) : 0.0;
    }
    
    /**
     * 获取统计描述文本
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (upper_bundles > 0) {
            desc.append(String.format("上部叶:%d捆(%.1fkg/%.1f%%)", 
                upper_bundles, upper_weight, getUpperLeafBundleRatio()));
        }
        if (middle_bundles > 0) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append(String.format("中部叶:%d捆(%.1fkg/%.1f%%)", 
                middle_bundles, middle_weight, getMiddleLeafBundleRatio()));
        }
        if (lower_bundles > 0) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append(String.format("下部叶:%d捆(%.1fkg/%.1f%%)", 
                lower_bundles, lower_weight, getLowerLeafBundleRatio()));
        }
        
        if (desc.length() == 0) {
            return "无烟叶数据";
        }
        
        return desc.toString();
    }
}