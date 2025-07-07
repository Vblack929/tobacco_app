package com.tobacco.weight.hardware.idcard;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * 身份证读卡器管理器
 * 负责身份证读卡器的连接、读取和数据处理
 */
public class IdCardManager {
    
    private static final String TAG = "IdCardManager";
    private static final String ACTION_USB_PERMISSION = "com.bland.usb.USB_PERMISSION";
    
    // USB设备标识符
    private static final int[] VENDOR_IDS = {102, 1024};
    private static final int[] PRODUCT_IDS = {1, 2, 257, 50010};
    
    private Context context;
    private UsbManager usbManager;
    private UsbDevice targetDevice;
    private CompositeDisposable disposables;
    private ExecutorService executor;
    
    // 状态管理
    private BehaviorSubject<Boolean> connectionSubject;
    private BehaviorSubject<IdCardData> idCardDataSubject;
    private boolean isInitialized = false;
    private boolean isConnected = false;
    private boolean isReading = false;
    
    // 广播接收器
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "USB广播接收: " + action);
            
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && isIdCardReader(device)) {
                    Log.i(TAG, "检测到身份证读卡器设备");
                    handleDeviceAttached(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && isIdCardReader(device)) {
                    Log.i(TAG, "身份证读卡器设备断开");
                    handleDeviceDetached(device);
                }
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Log.i(TAG, "USB权限已授予");
                            targetDevice = device;
                            connectToDevice();
                        }
                    } else {
                        Log.e(TAG, "USB权限被拒绝");
                        connectionSubject.onNext(false);
                    }
                }
            }
        }
    };
    
    /**
     * 构造函数
     */
    public IdCardManager() {
        this.disposables = new CompositeDisposable();
        this.executor = Executors.newSingleThreadExecutor();
        this.connectionSubject = BehaviorSubject.createDefault(false);
        this.idCardDataSubject = BehaviorSubject.create();
        
        // 加载本地库
        loadNativeLibrary();
    }
    
    /**
     * 加载本地库
     */
    private void loadNativeLibrary() {
        try {
            System.loadLibrary("idreader");
            Log.i(TAG, "本地库加载成功");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "本地库加载失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化身份证读卡器
     */
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        
        if (usbManager == null) {
            Log.e(TAG, "无法获取USB管理器");
            return;
        }
        
        // 注册USB广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(usbReceiver, filter);
        
        // 初始化SDK
        initializeSDK();
        
        isInitialized = true;
        Log.i(TAG, "身份证读卡器初始化完成");
    }
    
    /**
     * 初始化SDK
     */
    private void initializeSDK() {
        executor.execute(() -> {
            try {
                // 获取配置文件路径
                String configPath = context.getFilesDir().getAbsolutePath() + "/config";
                
                // 初始化SDK
                boolean serviceStarted = IDReader.ServiceStart(configPath);
                boolean nfcInitialized = TpNfc.Init(configPath);
                
                if (serviceStarted && nfcInitialized) {
                    Log.i(TAG, "SDK初始化成功");
                } else {
                    Log.e(TAG, "SDK初始化失败");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "SDK初始化异常: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 连接身份证读卡器
     */
    public void connect(Context context) {
        if (!isInitialized) {
            initialize(context);
        }
        
        // 查找身份证读卡器设备
        UsbDevice device = findIdCardReader();
        if (device != null) {
            requestPermission(device);
        } else {
            Log.w(TAG, "未找到身份证读卡器设备");
            connectionSubject.onNext(false);
        }
    }
    
    /**
     * 查找身份证读卡器设备
     */
    private UsbDevice findIdCardReader() {
        if (usbManager == null) return null;
        
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (isIdCardReader(device)) {
                Log.i(TAG, "找到身份证读卡器设备: " + device.getDeviceName());
                return device;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为身份证读卡器设备
     */
    private boolean isIdCardReader(UsbDevice device) {
        int vendorId = device.getVendorId();
        int productId = device.getProductId();
        
        for (int vId : VENDOR_IDS) {
            if (vendorId == vId) {
                for (int pId : PRODUCT_IDS) {
                    if (productId == pId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * 请求USB权限
     */
    private void requestPermission(UsbDevice device) {
        if (usbManager.hasPermission(device)) {
            targetDevice = device;
            connectToDevice();
        } else {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_PERMISSION), 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
            );
            usbManager.requestPermission(device, permissionIntent);
        }
    }
    
    /**
     * 连接到设备
     */
    private void connectToDevice() {
        executor.execute(() -> {
            try {
                if (targetDevice != null) {
                    // 这里可以添加具体的设备连接逻辑
                    isConnected = true;
                    connectionSubject.onNext(true);
                    Log.i(TAG, "设备连接成功");
                    
                    // 开始读取
                    startReading();
                }
            } catch (Exception e) {
                Log.e(TAG, "设备连接失败: " + e.getMessage(), e);
                connectionSubject.onNext(false);
            }
        });
    }
    
    /**
     * 开始读取身份证
     */
    public void startReading() {
        if (!isConnected || isReading) {
            return;
        }
        
        isReading = true;
        Log.i(TAG, "开始读取身份证");
        
        disposables.add(
            Observable.interval(1, java.util.concurrent.TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tick -> {
                    if (isReading && isConnected) {
                        readIdCard();
                    }
                })
        );
    }
    
    /**
     * 停止读取
     */
    public void stopReading() {
        isReading = false;
        Log.i(TAG, "停止读取身份证");
    }
    
    /**
     * 读取身份证数据
     */
    private void readIdCard() {
        executor.execute(() -> {
            try {
                // 调用SDK读取身份证
                String jsonRequest = "{\"function\":\"readcard\"}";
                String response = IDReader.WebSocketAPI(jsonRequest);
                
                if (response != null && !response.isEmpty()) {
                    IdCardData idCardData = parseIdCardData(response);
                    if (idCardData != null && idCardData.isValid()) {
                        Log.i(TAG, "读取身份证成功: " + idCardData.getName());
                        idCardDataSubject.onNext(idCardData);
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "读取身份证异常: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 解析身份证数据
     */
    private IdCardData parseIdCardData(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            
            // 检查响应状态
            if (json.has("status") && !"success".equals(json.getString("status"))) {
                return null;
            }
            
            // 解析身份证信息
            JSONObject data = json.optJSONObject("data");
            if (data == null) {
                return null;
            }
            
            IdCardData idCardData = new IdCardData();
            idCardData.setName(data.optString("name", ""));
            idCardData.setIdNumber(data.optString("idNumber", ""));
            idCardData.setAddress(data.optString("address", ""));
            idCardData.setGender(data.optString("gender", ""));
            idCardData.setBirthDate(data.optString("birthDate", ""));
            idCardData.setNationality(data.optString("nationality", ""));
            
            return idCardData;
            
        } catch (JSONException e) {
            Log.e(TAG, "解析身份证数据失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 处理设备接入
     */
    private void handleDeviceAttached(UsbDevice device) {
        targetDevice = device;
        requestPermission(device);
    }
    
    /**
     * 处理设备断开
     */
    private void handleDeviceDetached(UsbDevice device) {
        if (targetDevice != null && targetDevice.equals(device)) {
            isConnected = false;
            isReading = false;
            targetDevice = null;
            connectionSubject.onNext(false);
        }
    }
    
    /**
     * 获取连接状态Observable
     */
    public Observable<Boolean> getConnectionObservable() {
        return connectionSubject.distinctUntilChanged();
    }
    
    /**
     * 获取身份证数据Observable
     */
    public Observable<IdCardData> getIdCardObservable() {
        return idCardDataSubject.distinctUntilChanged();
    }
    
    /**
     * 获取当前连接状态
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        try {
            isReading = false;
            disposables.clear();
            
            if (context != null) {
                context.unregisterReceiver(usbReceiver);
            }
            
            if (executor != null) {
                executor.shutdown();
            }
            
            Log.i(TAG, "身份证读卡器资源已释放");
            
        } catch (Exception e) {
            Log.e(TAG, "释放资源异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * SDK接口类 - 需要通过JNI实现
     */
    private static class IDReader {
        public static native boolean ServiceStart(String configPath);
        public static native String WebSocketAPI(String jsonRequest);
    }
    
    /**
     * NFC接口类 - 需要通过JNI实现
     */
    private static class TpNfc {
        public static native boolean Init(String configPath);
    }
} 