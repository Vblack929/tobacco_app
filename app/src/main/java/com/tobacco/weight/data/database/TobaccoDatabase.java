package com.tobacco.weight.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import com.tobacco.weight.data.dao.WeightRecordDao;
import com.tobacco.weight.data.dao.FarmerInfoDao;
import com.tobacco.weight.data.entity.FarmerInfoEntity;
import com.tobacco.weight.data.model.WeightRecord;

/**
 * 烟叶称重数据库
 * 
 * 包含两个主要表：
 * - farmer_info: 烟农身份信息表（每个身份证号只存储一次）
 * - weight_records: 称重记录表（通过身份证号关联烟农信息）
 * 
 * 版本历史：
 * v1: 初始版本
 * v2: 添加烟农信息表和身份证关联
 * v3: 重构WeightRecord以支持详细的烟叶部位分级存储（上部叶/中部叶/下部叶独立记录）
 */
@Database(
    entities = { 
        WeightRecord.class, 
        FarmerInfoEntity.class 
    }, 
    version = 3, 
    exportSchema = true
)
@TypeConverters({ DateConverter.class })
public abstract class TobaccoDatabase extends RoomDatabase {

    private static volatile TobaccoDatabase INSTANCE;

    /**
     * 获取称重记录DAO
     */
    public abstract WeightRecordDao weightRecordDao();

    /**
     * 获取烟农信息DAO
     */
    public abstract FarmerInfoDao farmerInfoDao();

    /**
     * 获取数据库实例（单例模式）
     */
    public static TobaccoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TobaccoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            TobaccoDatabase.class,
                            "tobacco_database")
                            .fallbackToDestructiveMigration() // 开发阶段可以使用
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 关闭数据库连接
     */
    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}