package com.tobacco.weight.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.tobacco.weight.data.model.WeightRecord;

import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * 称重记录数据访问对象
 * 定义对weight_records表的数据库操作
 */
@Dao
public interface WeightRecordDao {
    
    /**
     * 插入新的称重记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Single<Long> insert(WeightRecord record);
    
    /**
     * 批量插入称重记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertAll(List<WeightRecord> records);
    
    /**
     * 更新称重记录
     */
    @Update
    Completable update(WeightRecord record);
    
    /**
     * 删除称重记录
     */
    @Delete
    Completable delete(WeightRecord record);
    
    /**
     * 根据ID删除记录
     */
    @Query("DELETE FROM weight_records WHERE id = :id")
    Completable deleteById(long id);
    
    /**
     * 清空所有记录
     */
    @Query("DELETE FROM weight_records")
    Completable deleteAll();
    
    /**
     * 根据ID查询记录
     */
    @Query("SELECT * FROM weight_records WHERE id = :id")
    Single<WeightRecord> getById(long id);
    
    /**
     * 获取所有记录（LiveData）
     */
    @Query("SELECT * FROM weight_records ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getAllRecords();
    
    /**
     * 获取所有记录（Flowable）
     */
    @Query("SELECT * FROM weight_records ORDER BY create_time DESC")
    Flowable<List<WeightRecord>> getAllRecordsFlowable();
    
    /**
     * 分页查询记录
     */
    @Query("SELECT * FROM weight_records ORDER BY create_time DESC LIMIT :limit OFFSET :offset")
    Single<List<WeightRecord>> getRecordsPaged(int limit, int offset);
    
    /**
     * 根据农户姓名搜索
     */
    @Query("SELECT * FROM weight_records WHERE farmer_name LIKE '%' || :farmerName || '%' ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> searchByFarmerName(String farmerName);
    
    /**
     * 根据身份证号搜索
     */
    @Query("SELECT * FROM weight_records WHERE id_card_number = :idCardNumber ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> searchByIdCard(String idCardNumber);
    
    /**
     * 根据记录编号搜索
     */
    @Query("SELECT * FROM weight_records WHERE record_number = :recordNumber")
    Single<WeightRecord> getByRecordNumber(String recordNumber);
    
    /**
     * 根据时间范围查询
     */
    @Query("SELECT * FROM weight_records WHERE create_time BETWEEN :startTime AND :endTime ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getRecordsByDateRange(Date startTime, Date endTime);
    
    /**
     * 获取今日记录
     */
    @Query("SELECT * FROM weight_records WHERE DATE(create_time/1000, 'unixepoch') = DATE('now') ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getTodayRecords();
    
    /**
     * 获取未打印的记录
     */
    @Query("SELECT * FROM weight_records WHERE is_printed = 0 ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> getUnprintedRecords();
    
    /**
     * 获取未导出的记录
     */
    @Query("SELECT * FROM weight_records WHERE is_exported = 0 ORDER BY create_time DESC")
    Single<List<WeightRecord>> getUnexportedRecords();
    
    /**
     * 标记记录为已打印
     */
    @Query("UPDATE weight_records SET is_printed = 1, print_count = print_count + 1 WHERE id = :id")
    Completable markAsPrinted(long id);
    
    /**
     * 标记记录为已导出
     */
    @Query("UPDATE weight_records SET is_exported = 1 WHERE id IN (:ids)")
    Completable markAsExported(List<Long> ids);
    
    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(*) FROM weight_records")
    Single<Integer> getRecordCount();
    
    /**
     * 获取今日记录总数
     */
    @Query("SELECT COUNT(*) FROM weight_records WHERE DATE(create_time/1000, 'unixepoch') = DATE('now')")
    Single<Integer> getTodayRecordCount();
    
    /**
     * 获取今日总重量
     */
    @Query("SELECT COALESCE(SUM(weight), 0) FROM weight_records WHERE DATE(create_time/1000, 'unixepoch') = DATE('now')")
    Single<Double> getTodayTotalWeight();
    
    /**
     * 获取今日总金额
     */
    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM weight_records WHERE DATE(create_time/1000, 'unixepoch') = DATE('now')")
    Single<Double> getTodayTotalAmount();
    
    /**
     * 根据条件进行高级搜索
     */
    @Query("SELECT * FROM weight_records WHERE " +
            "(:farmerName IS NULL OR farmer_name LIKE '%' || :farmerName || '%') AND " +
            "(:idCardNumber IS NULL OR id_card_number = :idCardNumber) AND " +
            "(:tobaccoPart IS NULL OR tobacco_part = :tobaccoPart) AND " +
            "(:startDate IS NULL OR create_time >= :startDate) AND " +
            "(:endDate IS NULL OR create_time <= :endDate) " +
            "ORDER BY create_time DESC")
    LiveData<List<WeightRecord>> advancedSearch(String farmerName, String idCardNumber, 
                                               String tobaccoPart, Date startDate, Date endDate);
} 