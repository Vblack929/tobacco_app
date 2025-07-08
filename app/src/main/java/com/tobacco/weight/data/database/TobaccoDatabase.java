package com.tobacco.weight.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import com.tobacco.weight.data.dao.WeightRecordDao;
import com.tobacco.weight.data.model.WeightRecord;

/**
 * 烟叶称重数据库
 */
@Database(entities = { WeightRecord.class }, version = 1, exportSchema = true)
@TypeConverters({ DateConverter.class })
public abstract class TobaccoDatabase extends RoomDatabase {

    private static volatile TobaccoDatabase INSTANCE;

    /**
     * 获取称重记录DAO
     */
    public abstract WeightRecordDao weightRecordDao();

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