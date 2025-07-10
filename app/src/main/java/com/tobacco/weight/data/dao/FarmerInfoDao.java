package com.tobacco.weight.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.tobacco.weight.data.entity.FarmerInfoEntity;

import java.util.List;

/**
 * 烟农身份信息数据访问接口
 * 
 * 设计原则：
 * - 身份证号唯一性保证
 * - 支持条件查询
 * - 提供统计接口
 */
@Dao
public interface FarmerInfoDao {

    /**
     * 插入新的烟农信息（忽略冲突，防止重复插入）
     * 如果身份证号已存在，则忽略插入
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(FarmerInfoEntity farmerInfo);

    /**
     * 插入新的烟农信息（替换冲突，更新现有信息）
     * 如果身份证号已存在，则替换原有记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertReplace(FarmerInfoEntity farmerInfo);

    /**
     * 更新烟农信息
     */
    @Update
    int update(FarmerInfoEntity farmerInfo);

    /**
     * 删除烟农信息
     */
    @Delete
    int delete(FarmerInfoEntity farmerInfo);

    /**
     * 根据身份证号查询烟农信息（精确匹配）
     */
    @Query("SELECT * FROM farmer_info WHERE id_card_number = :idCardNumber LIMIT 1")
    LiveData<FarmerInfoEntity> getFarmerByIdCard(String idCardNumber);

    /**
     * 同步方式根据身份证号查询烟农信息
     */
    @Query("SELECT * FROM farmer_info WHERE id_card_number = :idCardNumber LIMIT 1")
    FarmerInfoEntity getFarmerByIdCardSync(String idCardNumber);

    /**
     * 根据烟农姓名查询烟农信息
     */
    @Query("SELECT * FROM farmer_info WHERE farmer_name = :farmerName")
    LiveData<List<FarmerInfoEntity>> getFarmersByName(String farmerName);

    /**
     * 根据合同号查询烟农信息
     */
    @Query("SELECT * FROM farmer_info WHERE contract_number = :contractNumber LIMIT 1")
    LiveData<FarmerInfoEntity> getFarmerByContractNumber(String contractNumber);

    /**
     * 获取所有烟农信息（按创建时间倒序）
     */
    @Query("SELECT * FROM farmer_info WHERE is_active = 1 ORDER BY create_time DESC")
    LiveData<List<FarmerInfoEntity>> getAllActiveFarmers();

    /**
     * 获取所有烟农信息（包括非激活）
     */
    @Query("SELECT * FROM farmer_info ORDER BY create_time DESC")
    LiveData<List<FarmerInfoEntity>> getAllFarmers();

    /**
     * 获取烟农总数（同步版本，用于统计）
     */
    @Query("SELECT COUNT(*) FROM farmer_info WHERE is_active = 1")
    int getTotalFarmerCount();

    /**
     * 获取所有激活烟农（同步版本）
     */
    @Query("SELECT * FROM farmer_info WHERE is_active = 1 ORDER BY create_time DESC")
    List<FarmerInfoEntity> getAllActiveFarmersSync();

    /**
     * 获取最近注册的烟农（最近7天）
     */
    @Query("SELECT * FROM farmer_info WHERE create_time >= datetime('now', '-7 days') AND is_active = 1 ORDER BY create_time DESC")
    LiveData<List<FarmerInfoEntity>> getRecentFarmers();

    /**
     * 按性别统计烟农数量
     */
    @Query("SELECT gender, COUNT(*) as count FROM farmer_info WHERE is_active = 1 GROUP BY gender")
    LiveData<List<GenderStatistics>> getFarmerGenderStatistics();

    /**
     * 按地区统计烟农数量（地址前6位）
     */
    @Query("SELECT SUBSTR(address, 1, 6) as region, COUNT(*) as count FROM farmer_info WHERE is_active = 1 AND address IS NOT NULL GROUP BY SUBSTR(address, 1, 6) ORDER BY count DESC")
    LiveData<List<RegionStatistics>> getFarmerRegionStatistics();

    /**
     * 获取有完整身份证信息的烟农
     */
    @Query("SELECT * FROM farmer_info WHERE id_card_number IS NOT NULL AND id_card_number != '' AND farmer_name IS NOT NULL AND farmer_name != '' AND address IS NOT NULL AND address != '' AND is_active = 1 ORDER BY farmer_name")
    LiveData<List<FarmerInfoEntity>> getFarmersWithCompleteInfo();

    /**
     * 更新烟农激活状态
     */
    @Query("UPDATE farmer_info SET is_active = :isActive, update_time = datetime('now') WHERE id_card_number = :idCardNumber")
    int updateFarmerActiveStatus(String idCardNumber, boolean isActive);

    /**
     * 更新首次称重时间
     */
    @Query("UPDATE farmer_info SET first_record_time = datetime('now'), update_time = datetime('now') WHERE id_card_number = :idCardNumber AND first_record_time IS NULL")
    int updateFirstRecordTime(String idCardNumber);

    /**
     * 删除所有烟农信息（危险操作，用于测试）
     */
    @Query("DELETE FROM farmer_info")
    int deleteAll();

    /**
     * 获取所有身份证号列表（用于统计）
     */
    @Query("SELECT id_card_number FROM farmer_info WHERE is_active = 1")
    LiveData<List<String>> getAllIdCardNumbers();

    /**
     * 检查身份证号是否存在（返回数量）
     */
    @Query("SELECT COUNT(*) FROM farmer_info WHERE id_card_number = :idCardNumber")
    int checkIdCardExists(String idCardNumber);

    /**
     * 检查身份证号是否存在（返回布尔值）
     */
    @Query("SELECT COUNT(*) > 0 FROM farmer_info WHERE id_card_number = :idCardNumber")
    LiveData<Boolean> isIdCardExists(String idCardNumber);

    /**
     * 验证农户身份信息匹配
     */
    @Query("SELECT COUNT(*) > 0 FROM farmer_info WHERE farmer_name = :farmerName AND id_card_number = :idCardNumber")
    boolean validateFarmerIdentity(String farmerName, String idCardNumber);

    /**
     * 根据姓名搜索农户（模糊查询）
     */
    @Query("SELECT * FROM farmer_info WHERE farmer_name LIKE '%' || :searchQuery || '%' AND is_active = 1 ORDER BY farmer_name")
    LiveData<List<FarmerInfoEntity>> searchFarmersByName(String searchQuery);

    /**
     * 获取激活农户数量
     */
    @Query("SELECT COUNT(*) FROM farmer_info WHERE is_active = 1")
    LiveData<Integer> getActiveFarmerCount();
}

/**
 * 性别统计数据
 */
class GenderStatistics {
    public String gender;
    public int count;
}

/**
 * 地区统计数据
 */
class RegionStatistics {
    public String region;
    public int count;
} 