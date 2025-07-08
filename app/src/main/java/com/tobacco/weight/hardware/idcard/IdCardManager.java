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
        try {
            // 加载本地库
            System.loadLibrary("idreader");
            Log.d(TAG, "本地库加载成功");
            
            // 使用演示代码的ServiceStart方法，传入配置文件路径
            String configPath = context.getFilesDir().getAbsolutePath();
            int serviceResult = ServiceStart(configPath);
            
            Log.d(TAG, "SDK初始化结果 - ServiceStart: " + serviceResult);
            
            if (serviceResult >= 0) {
                Log.d(TAG, "✅ 身份证读卡器SDK初始化成功");
            } else {
                Log.e(TAG, "❌ 身份证读卡器SDK初始化失败: " + serviceResult);
            }
            
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "加载本地库失败", e);
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
                if (CompareReaderID(device.getVendorId(), device.getProductId())) {
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
        checkConnection();
    }
    
    /**
     * 检查连接状态 - 使用演示代码的Connected方法
     */
    private void checkConnection() {
        try {
            // 使用演示代码的Connected方法
            boolean connected = Connected(usbManager);
            
            Log.d(TAG, "连接检查结果: " + connected);
            
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
        if (isReading) return;
        
        Log.d(TAG, "启动读卡循环");
        isReading = true;
        
        disposables.add(
            Flowable.interval(2, TimeUnit.SECONDS)  // 2秒间隔检查
                .subscribeOn(Schedulers.io())
                .takeWhile(tick -> isConnected && isReading)
                .subscribe(
                    tick -> performRead(),
                    throwable -> {
                        Log.e(TAG, "读卡循环异常", throwable);
                        isReading = false;
                    },
                    () -> Log.d(TAG, "读卡循环已停止")
                )
        );
    }
    
    /**
     * 停止读卡循环
     */
    private void stopReadingLoop() {
        Log.d(TAG, "停止读卡循环");
        isReading = false;
        disposables.clear();
    }
    
    /**
     * 执行读卡操作 - 使用演示代码的WebSocketAPI方法
     */
    private void performRead() {
        try {
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
        try {
            ServiceStop();
        } catch (Exception e) {
            Log.w(TAG, "停止服务失败", e);
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
} 