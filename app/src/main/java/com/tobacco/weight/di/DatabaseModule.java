package com.tobacco.weight.di;

import android.app.Application;
import android.content.Context;

import com.tobacco.weight.data.dao.WeightRecordDao;
import com.tobacco.weight.data.dao.FarmerInfoDao;
import com.tobacco.weight.data.database.TobaccoDatabase;
import com.tobacco.weight.data.repository.WeightRecordRepository;
import com.tobacco.weight.data.repository.FarmerInfoRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * 数据库相关的依赖注入模块
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    /**
     * 提供数据库实例
     */
    @Provides
    @Singleton
    public TobaccoDatabase provideDatabase(@ApplicationContext Context context) {
        return TobaccoDatabase.getInstance(context);
    }

    /**
     * 提供WeightRecordDao
     */
    @Provides
    public WeightRecordDao provideWeightRecordDao(TobaccoDatabase database) {
        return database.weightRecordDao();
    }

    /**
     * 提供FarmerInfoDao
     */
    @Provides
    public FarmerInfoDao provideFarmerInfoDao(TobaccoDatabase database) {
        return database.farmerInfoDao();
    }

    /**
     * 提供WeightRecordRepository
     */
    @Provides
    @Singleton
    public WeightRecordRepository provideWeightRecordRepository(Application application) {
        return new WeightRecordRepository(application);
    }

    /**
     * 提供FarmerInfoRepository
     */
    @Provides
    @Singleton
    public FarmerInfoRepository provideFarmerInfoRepository(Application application) {
        return new FarmerInfoRepository(application);
    }
}