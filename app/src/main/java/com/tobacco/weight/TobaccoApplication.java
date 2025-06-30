package com.tobacco.weight;

import android.app.Application;
import android.content.Context;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 烟叶称重应用主类
 * 负责应用初始化和全局配置
 */
@HiltAndroidApp
public class TobaccoApplication extends Application {
    
    private static Context applicationContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        
        // 初始化应用配置
        initializeAppConfig();
    }
    
    /**
     * 获取应用上下文
     */
    public static Context getAppContext() {
        return applicationContext;
    }
    
    /**
     * 初始化应用配置
     */
    private void initializeAppConfig() {
        // 设置未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());
        
        // 初始化其他配置...
    }
    
    /**
     * 全局异常处理器
     */
    private static class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // 记录错误日志
            ex.printStackTrace();
            
            // 重启应用或执行其他错误处理逻辑
            System.exit(1);
        }
    }
} 