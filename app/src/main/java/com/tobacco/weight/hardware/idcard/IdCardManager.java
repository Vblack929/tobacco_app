package com.tobacco.weight.hardware.idcard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

import java.util.concurrent.TimeUnit;

/**
 * 身份证读卡器管理器
 * 基于演示项目实现，使用正确的VID/PID和连接方法
 */
public class IdCardManager {
    private static final String TAG = "IdCardManager";
    
    // 是否支持原生身份证读卡器功能
    private static boolean nativeLibraryAvailable = false;
    
    // 开发模式控制 - 只有在明确启用时才进行模拟
    private boolean developmentSimulationEnabled = false;
    
    private Context context;
    private UsbManager usbManager;
    private UsbBroadcastReceiver usbReceiver;
    
    // RxJava流
    private final BehaviorProcessor<Boolean> connectionProcessor = BehaviorProcessor.createDefault(false);
    private final BehaviorProcessor<IdCardData> cardDataProcessor = BehaviorProcessor.create();
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    // 状态管理
    private boolean isInitialized = false;
    private boolean isConnected = false;
    private boolean isReading = false;
    
    /**
     * 初始化读卡器
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        
        Log.d(TAG, "初始化身份证读卡器");
        
        // 加载本地库并初始化SDK
        initializeNativeLibrary();
        
        // 注册USB广播接收器
        registerUsbReceiver();
        
        // 检查当前已连接的设备
        checkExistingDevices();
        
        isInitialized = true;
        Log.d(TAG, "身份证读卡器初始化完成");
    }
    
    /**
     * 连接读卡器
     */
    public void connectReader() {
        Log.d(TAG, "尝试连接读卡器");
        if (!isInitialized) {
            Log.w(TAG, "读卡器未初始化");
            return;
        }
        
        // 使用演示代码的Connected方法检查连接
        checkConnection();
    }
    
    /**
     * 启用开发模拟模式（仅用于开发测试）
     */
    public void enableDevelopmentSimulation() {
        developmentSimulationEnabled = true;
        Log.d(TAG, "✅ 已启用开发模拟模式");
        // 重新检查连接状态
        if (isInitialized) {
            checkConnection();
        }
    }
    
    /**
     * 禁用开发模拟模式（生产模式）
     */
    public void disableDevelopmentSimulation() {
        developmentSimulationEnabled = false;
        Log.d(TAG, "❌ 已禁用开发模拟模式");
        
        // 立即停止读卡循环，防止继续生成模拟数据
        if (isReading) {
            stopReadingLoop();
            Log.d(TAG, "🛑 强制停止读卡循环");
        }
        
        // 重新检查连接状态
        if (isInitialized) {
            checkConnection();
        }
    }
    
    /**
     * 检查是否启用了开发模拟模式
     */
    public boolean isDevelopmentSimulationEnabled() {
        return developmentSimulationEnabled;
    }
    
    /**
     * 获取连接状态流
     */
    public Flowable<Boolean> connectionStream() {
        return connectionProcessor.distinctUntilChanged();
    }
    
    /**
     * 获取身份证数据流
     */
    public Flowable<IdCardData> cardDataStream() {
        return cardDataProcessor.distinctUntilChanged();
    }
    
    /**
     * 初始化本地库 - 使用演示代码的方法
     */
    private void initializeNativeLibrary() {
        // 检查原生库是否可用
        if (!nativeLibraryAvailable) {
            Log.w(TAG, "❌ 原生身份证读卡器库不可用");
            if (developmentSimulationEnabled) {
                Log.d(TAG, "🧪 开发模拟模式已启用");
            } else {
                Log.d(TAG, "🏭 生产模式 - 需要真实硬件才能工作");
            }
            return;
        }
        
        try {
            Log.d(TAG, "本地库已加载，开始SDK初始化");
            
            // 使用演示代码的ServiceStart方法，传入配置文件路径
            String configPath = context.getFilesDir().getAbsolutePath();
            int serviceResult = ServiceStart(configPath);
            
            Log.d(TAG, "SDK初始化结果 - ServiceStart: " + serviceResult);
            
            if (serviceResult >= 0) {
                Log.d(TAG, "✅ 身份证读卡器SDK初始化成功");
            } else {
                Log.e(TAG, "❌ 身份证读卡器SDK初始化失败: " + serviceResult);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "SDK初始化异常", e);
        }
    }
    
    /**
     * 注册USB广播接收器
     */
    private void registerUsbReceiver() {
        usbReceiver = new UsbBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        
        context.registerReceiver(usbReceiver, filter);
        Log.d(TAG, "USB广播接收器已注册");
    }
    
    /**
     * USB广播接收器
     */
    private class UsbBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            
            Log.d(TAG, "收到USB广播: " + action);
            
            if (device != null) {
                Log.d(TAG, "USB设备 - VID: " + String.format("0x%04X", device.getVendorId()) + 
                           ", PID: " + String.format("0x%04X", device.getProductId()));
                
                // 使用演示代码的CompareReaderID检查是否为支持的设备
                boolean isSupported;
                if (!nativeLibraryAvailable) {
                    if (developmentSimulationEnabled) {
                        // 仅在开发模拟模式下假装设备受支持
                        isSupported = true;
                        Log.d(TAG, "🧪 开发模拟模式 - 假装设备受支持");
                    } else {
                        // 生产模式下无原生库：任何设备都不受支持
                        isSupported = false;
                        Log.d(TAG, "❌ 生产模式无原生库 - 设备不受支持");
                    }
                } else {
                    isSupported = CompareReaderID(device.getVendorId(), device.getProductId());
                }
                
                if (isSupported) {
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        Log.d(TAG, "✅ 支持的读卡器设备已连接");
                        checkConnection();
                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        Log.d(TAG, "❌ 读卡器设备已断开");
                        handleDeviceDisconnected();
                    }
                } else {
                    Log.d(TAG, "不是支持的身份证读卡器设备");
                }
            }
        }
    }
    
    /**
     * 检查当前已连接的设备
     */
    private void checkExistingDevices() {
        Log.d(TAG, "检查当前已连接的设备");
        
        // 确保初始状态正确
        if (!nativeLibraryAvailable && !developmentSimulationEnabled) {
            Log.d(TAG, "🏭 生产模式初始化 - 设置为未连接状态");
            isConnected = false;
            connectionProcessor.onNext(false);
        }
        
        checkConnection();
    }
    
    /**
     * 检查连接状态 - 使用演示代码的Connected方法
     */
    private void checkConnection() {
        try {
            boolean connected;
            
            if (!nativeLibraryAvailable) {
                // 原生库不可用时的处理
                if (developmentSimulationEnabled) {
                    // 仅在明确启用开发模拟时才假装连接
                    connected = true;
                    Log.d(TAG, "🧪 开发模拟模式 - 假装连接成功");
                } else {
                    // 生产模式：原生库不可用 = 无硬件连接
                    connected = false;
                    Log.d(TAG, "❌ 原生库不可用且未启用开发模拟 - 无硬件连接");
                }
            } else {
                // 使用演示代码的Connected方法检查真实硬件
                connected = Connected(usbManager);
                Log.d(TAG, "🔧 真实硬件连接检查结果: " + connected);
            }
            
            if (connected && !isConnected) {
                // 连接建立
                isConnected = true;
                connectionProcessor.onNext(true);
                startReadingLoop();
                Log.d(TAG, "📤 身份证读卡器连接成功，发出连接状态: true");
                
            } else if (!connected && isConnected) {
                // 连接断开
                handleDeviceDisconnected();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "检查连接异常", e);
            if (isConnected) {
                handleDeviceDisconnected();
            }
        }
    }
    
    /**
     * 处理设备断开
     */
    private void handleDeviceDisconnected() {
        Log.d(TAG, "处理设备断开");
        stopReadingLoop();
        isConnected = false;
        connectionProcessor.onNext(false);
        Log.d(TAG, "📤 发出连接状态: false");
    }
    
    /**
     * 启动读卡循环
     */
    private void startReadingLoop() {
        if (isReading) {
            Log.d(TAG, "读卡循环已在运行，跳过启动");
            return;
        }
        
        Log.d(TAG, "启动读卡循环");
        
        // 确保清理之前的订阅
        disposables.clear();
        isReading = true;
        
        disposables.add(
            Flowable.interval(2, TimeUnit.SECONDS)  // 2秒间隔检查
                .subscribeOn(Schedulers.io())
                .takeWhile(tick -> {
                    boolean shouldContinue = isConnected && isReading;
                    if (!shouldContinue) {
                        Log.d(TAG, "🛑 读卡循环条件不满足，准备停止 - isConnected: " + isConnected + ", isReading: " + isReading);
                    }
                    return shouldContinue;
                })
                .subscribe(
                    tick -> performRead(),
                    throwable -> {
                        Log.e(TAG, "读卡循环异常", throwable);
                        isReading = false;
                    },
                    () -> {
                        Log.d(TAG, "✅ 读卡循环已正常停止");
                        isReading = false;
                    }
                )
        );
        
        Log.d(TAG, "✅ 读卡循环已启动");
    }
    
    /**
     * 停止读卡循环
     */
    private void stopReadingLoop() {
        Log.d(TAG, "停止读卡循环");
        isReading = false;
        
        // 清理所有订阅，确保没有未完成的任务
        disposables.clear();
        
        // 记录状态以便调试
        Log.d(TAG, "✅ 读卡循环已完全停止，所有订阅已清理");
    }
    
    /**
     * 执行读卡操作 - 使用演示代码的WebSocketAPI方法
     */
    private void performRead() {
        try {
            // 首先检查读卡循环是否应该继续运行
            if (!isReading || !isConnected) {
                Log.d(TAG, "🛑 读卡循环已停止，跳过此次读取");
                return;
            }
            
            // 处理无原生库的情况
            if (!nativeLibraryAvailable) {
                if (developmentSimulationEnabled) {
                    // 仅在开发模拟模式下生成模拟数据
                    if (Math.random() < 0.05) { // 5%概率生成数据
                        Log.d(TAG, "🧪 开发模拟模式 - 生成测试身份证数据");
                        IdCardData simulatedData = createSimulatedCardData();
                        cardDataProcessor.onNext(simulatedData);
                        Log.d(TAG, "📤 发出模拟身份证数据流");
                    }
                } else {
                    // 生产模式下无原生库：不应该进入这里，立即停止读卡循环
                    Log.w(TAG, "❌ 生产模式下读卡循环仍在运行，立即停止");
                    stopReadingLoop();
                    handleDeviceDisconnected();
                }
                return;
            }
            
            // 先检查连接状态
            if (!Connected(usbManager)) {
                Log.w(TAG, "读卡器连接已断开");
                handleDeviceDisconnected();
                return;
            }
            
            // 使用演示代码的WebSocketAPI读取身份证
            String jsonResult = WebSocketAPI("{\"function\":\"readcard\"}");
            
            if (jsonResult != null && !jsonResult.trim().isEmpty() && !jsonResult.equals("{}")) {
                Log.d(TAG, "📥 收到读卡JSON: " + jsonResult);
                
                // 解析JSON数据
                IdCardData cardData = IdCardData.fromSdkJson(jsonResult);
                
                if (cardData != null && cardData.isValid()) {
                    Log.d(TAG, "✅ 身份证数据解析成功: " + cardData.getName());
                    cardDataProcessor.onNext(cardData);
                    Log.d(TAG, "📤 发出身份证数据流");
                } else {
                    Log.w(TAG, "身份证数据解析失败或无效");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "执行读卡操作异常", e);
        }
    }
    
    /**
     * 创建模拟身份证数据
     */
    private IdCardData createSimulatedCardData() {
        IdCardData data = new IdCardData();
        data.setName("张三");
        data.setIdNumber("110101199001011234");
        data.setAddress("北京市东城区测试街道123号");
        data.setGender("男");
        Log.d(TAG, "创建模拟身份证数据: " + data.getName());
        return data;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "释放身份证读卡器资源");
        
        stopReadingLoop();
        
        if (usbReceiver != null && context != null) {
            try {
                context.unregisterReceiver(usbReceiver);
            } catch (Exception e) {
                Log.w(TAG, "注销USB接收器失败", e);
            }
        }
        
        // 停止服务
        if (nativeLibraryAvailable) {
            try {
                ServiceStop();
            } catch (Exception e) {
                Log.w(TAG, "停止服务失败", e);
            }
        } else {
            Log.d(TAG, "无原生库 - 跳过服务停止");
        }
        
        isInitialized = false;
        isConnected = false;
    }
    
    // ====== 原生方法声明 (基于演示项目) ======
    
    /**
     * 启动 WebAPI 服务以及阅读器联网服务
     */
    public static native int ServiceStart(String CfgPath);
    
    /**
     * 停止 WebAPI 服务以及阅读器联网服务
     */
    public static native int ServiceStop();
    
    /**
     * 检查阅读器是否连接,并创建设置好USB设备文件描述符
     */
    public static native boolean Connected(UsbManager usbManager);
    
    /**
     * 根据 Vendor ID 和 Product ID 判断USB设备是否为阅读器
     */
    public static native boolean CompareReaderID(int VID, int PID);
    
    /**
     * 直接调用 WebAPI 服务提供的 websocket 接口
     */
    public static native String WebSocketAPI(String cmd);
    
    // 安全加载身份证读卡器库
    static {
        try {
            System.loadLibrary("idreader");
            nativeLibraryAvailable = true;
            Log.i(TAG, "身份证读卡器原生库加载成功");
        } catch (UnsatisfiedLinkError e) {
            nativeLibraryAvailable = false;
            Log.w(TAG, "身份证读卡器原生库未找到，将使用模拟模式: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否支持原生身份证读卡器功能
     */
    public static boolean isNativeLibraryAvailable() {
        return nativeLibraryAvailable;
    }
} 