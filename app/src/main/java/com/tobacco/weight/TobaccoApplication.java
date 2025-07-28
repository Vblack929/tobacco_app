package com.tobacco.weight;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 烟叶称重应用主类
 * 负责应用初始化和全局配置
 */
@HiltAndroidApp
public class TobaccoApplication extends Application {

    private static final String TAG = "TobaccoApplication";
    private static Context applicationContext;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;

        Log.i(TAG, "TobaccoApplication 启动");

        // 初始化应用配置
        initializeAppConfig();

        Log.i(TAG, "TobaccoApplication 初始化完成");
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

        Log.i(TAG, "应用配置初始化完成");
    }

    /**
     * 全局异常处理器
     */
    private static class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
            // 记录错误日志
            Log.e(TAG, "未捕获的异常", ex);
            ex.printStackTrace();

            // 重启应用或执行其他错误处理逻辑
            System.exit(1);
        }
    }
}