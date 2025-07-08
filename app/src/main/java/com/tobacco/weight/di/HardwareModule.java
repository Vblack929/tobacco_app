package com.tobacco.weight.di;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import com.tobacco.weight.hardware.scale.ScaleManager;
import com.tobacco.weight.hardware.printer.PrinterManager;
import com.tobacco.weight.hardware.idcard.IdCardManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * 硬件模块依赖注入配置
 */
@Module
@InstallIn(SingletonComponent.class)
public class HardwareModule {
    
    /**
     * 提供硬件模拟器单例
     */
    @Provides
    @Singleton
    public HardwareSimulator provideHardwareSimulator() {
        return new HardwareSimulator();
    }
    
    /**
     * 提供电子秤管理器
     */
    @Provides
    @Singleton
    public ScaleManager provideScaleManager(HardwareSimulator simulator) {
        return new ScaleManager(simulator);
    }
    
    /**
     * 提供打印机管理器
     */
    @Provides
    @Singleton
    public PrinterManager providePrinterManager(HardwareSimulator simulator) {
        return new PrinterManager(simulator);
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