package com.tobacco.weight.di;

import com.tobacco.weight.hardware.idcard.IdCardManager;
import com.tobacco.weight.hardware.printer.PrinterManager;
import com.tobacco.weight.hardware.scale.ScaleManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * 硬件模块依赖注入
 * 提供硬件管理器的单例实例
 */
@Module
@InstallIn(SingletonComponent.class)
public class HardwareModule {
    
    /**
     * 提供电子秤管理器
     */
    @Provides
    @Singleton
    public ScaleManager provideScaleManager() {
        return new ScaleManager();
    }
    
    /**
     * 提供打印机管理器
     */
    @Provides
    @Singleton
    public PrinterManager providePrinterManager() {
        return new PrinterManager();
    }
    
    /**
     * 提供身份证读卡器管理器
     */
    @Provides
    @Singleton
    public IdCardManager provideIdCardManager() {
        return new IdCardManager();
    }
} 