package com.tobacco.weight.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.tobacco.weight.TobaccoApplication;
import com.tobacco.weight.data.model.WeightRecord;

/**
 * 烟叶称重应用数据库
 * 使用Room数据库框架
 */
@Database(
    entities = {WeightRecord.class},
    version = 1,
    exportSchema = true
)
public abstract class TobaccoDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "tobacco_weight.db";
    private static volatile TobaccoDatabase INSTANCE;
    
    /**
     * 获取数据库实例（单例模式）
     */
    public static TobaccoDatabase getInstance() {
        if (INSTANCE == null) {
            synchronized (TobaccoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            TobaccoApplication.getAppContext(),
                            TobaccoDatabase.class,
                            DATABASE_NAME
                    )
                    .addCallback(DATABASE_CALLBACK)
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // 仅开发时使用
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 获取称重记录DAO
     */
    public abstract WeightRecordDao weightRecordDao();
    
    /**
     * 数据库回调
     */
    private static final RoomDatabase.Callback DATABASE_CALLBACK = new RoomDatabase.Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            // 数据库创建时的初始化操作
            // 可以在这里插入初始数据
        }
        
        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            // 数据库打开时的操作
        }
    };
    
    /**
     * 数据库迁移（示例）
     * 从版本1到版本2的迁移
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // 执行数据库结构变更的SQL语句
            // 例如：添加新列
            // database.execSQL("ALTER TABLE weight_records ADD COLUMN new_column TEXT");
        }
    };
    
    /**
     * 关闭数据库连接
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
} 