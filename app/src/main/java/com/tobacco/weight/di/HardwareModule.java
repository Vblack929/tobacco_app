package com.tobacco.weight.di;

import android.content.Context;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import com.tobacco.weight.hardware.scale.ScaleManager;
import com.tobacco.weight.hardware.printer.PrinterManager;
import com.tobacco.weight.hardware.idcard.IdCardManager;
import com.tobacco.weight.hardware.serial.SerialPortManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
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
    public PrinterManager providePrinterManager(@ApplicationContext Context context) {
        return new PrinterManager(context);
    }
    
    @Provides
    @Singleton
    public SerialPortManager provideSerialPortManager() {
        return new SerialPortManager();
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