package com.tobacco.weight.hardware.simulator;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * 硬件设备模拟器
 * 用于在没有实际硬件时进行开发和测试
 */
public class HardwareSimulator {
    private static final String TAG = "HardwareSimulator";
    
    private final Random random = new Random();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    // 设备状态
    private boolean isScaleConnected = false;
    private boolean isIdCardReaderConnected = false;
    private boolean isPrinterConnected = false;
    
    // 数据流
    private final BehaviorSubject<WeightData> weightDataSubject = BehaviorSubject.create();
    private final BehaviorSubject<IdCardData> idCardDataSubject = BehaviorSubject.create();
    private final BehaviorSubject<DeviceStatus> deviceStatusSubject = BehaviorSubject.create();
    
    // 模拟数据
    private double currentWeight = 0.0;
    private boolean isWeightStable = true;
    
    /**
     * 重量数据模型
     */
    public static class WeightData {
        private double weight;
        private boolean isStable;
        private boolean isOverload;
        private boolean isUnderload;
        private long timestamp;
        
        public WeightData(double weight, boolean isStable) {
            this.weight = weight;
            this.isStable = isStable;
            this.isOverload = weight > 50.0; // 模拟超重
            this.isUnderload = weight < 0.1; // 模拟欠重
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and setters
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public boolean isStable() { return isStable; }
        public void setStable(boolean stable) { isStable = stable; }
        public boolean isOverload() { return isOverload; }
        public void setOverload(boolean overload) { isOverload = overload; }
        public boolean isUnderload() { return isUnderload; }
        public void setUnderload(boolean underload) { isUnderload = underload; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * 身份证数据模型
     */
    public static class IdCardData {
        private String name;
        private String idNumber;
        private String address;
        private String issueDate;
        private String expiryDate;
        private byte[] photo;
        
        public IdCardData(String name, String idNumber, String address) {
            this.name = name;
            this.idNumber = idNumber;
            this.address = address;
            this.issueDate = "20200101";
            this.expiryDate = "20300101";
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIdNumber() { return idNumber; }
        public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getIssueDate() { return issueDate; }
        public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
        public byte[] getPhoto() { return photo; }
        public void setPhoto(byte[] photo) { this.photo = photo; }
    }
    
    /**
     * 设备状态模型
     */
    public static class DeviceStatus {
        private boolean scaleConnected;
        private boolean idCardReaderConnected;
        private boolean printerConnected;
        
        public DeviceStatus(boolean scaleConnected, boolean idCardReaderConnected, boolean printerConnected) {
            this.scaleConnected = scaleConnected;
            this.idCardReaderConnected = idCardReaderConnected;
            this.printerConnected = printerConnected;
        }
        
        // Getters and setters
        public boolean isScaleConnected() { return scaleConnected; }
        public void setScaleConnected(boolean scaleConnected) { this.scaleConnected = scaleConnected; }
        public boolean isIdCardReaderConnected() { return idCardReaderConnected; }
        public void setIdCardReaderConnected(boolean idCardReaderConnected) { this.idCardReaderConnected = idCardReaderConnected; }
        public boolean isPrinterConnected() { return printerConnected; }
        public void setPrinterConnected(boolean printerConnected) { this.printerConnected = printerConnected; }
    }
    
    /**
     * 初始化模拟器
     */
    public void initialize() {
        Log.d(TAG, "正在初始化硬件模拟器...");
        
        // 模拟设备连接
        simulateDeviceConnection();
        
        // 开始模拟重量数据
        startWeightSimulation();
        
        Log.d(TAG, "硬件模拟器初始化完成");
    }
    
    /**
     * 模拟设备连接
     */
    private void simulateDeviceConnection() {
        mainHandler.postDelayed(() -> {
            isScaleConnected = true;
            isIdCardReaderConnected = true;
            isPrinterConnected = true;
            
            DeviceStatus status = new DeviceStatus(isScaleConnected, isIdCardReaderConnected, isPrinterConnected);
            deviceStatusSubject.onNext(status);
            
            Log.d(TAG, "模拟设备连接完成");
        }, 2000);
    }
    
    /**
     * 开始模拟重量数据
     */
    private void startWeightSimulation() {
        executorService.execute(() -> {
            while (true) {
                try {
                    // 模拟重量变化
                    if (random.nextBoolean()) {
                        // 50%概率产生重量变化
                        double weightChange = (random.nextDouble() - 0.5) * 0.5; // ±0.25kg变化
                        currentWeight = Math.max(0, currentWeight + weightChange);
                        
                        // 模拟重量稳定性
                        isWeightStable = Math.abs(weightChange) < 0.1;
                    } else {
                        // 50%概率保持稳定
                        isWeightStable = true;
                    }
                    
                    WeightData weightData = new WeightData(currentWeight, isWeightStable);
                    
                    mainHandler.post(() -> weightDataSubject.onNext(weightData));
                    
                    Thread.sleep(500); // 每500ms更新一次
                } catch (InterruptedException e) {
                    Log.e(TAG, "重量模拟线程被中断", e);
                    break;
                }
            }
        });
    }
    
    /**
     * 模拟身份证读取
     */
    public void simulateIdCardRead() {
        Log.d(TAG, "开始模拟身份证读取...");
        
        mainHandler.postDelayed(() -> {
            // 模拟身份证数据
            String[] names = {"张三", "李四", "王五", "赵六", "钱七"};
            String[] addresses = {"河南省许昌市襄城县", "河南省许昌市建安区", "河南省许昌市禹州市", "河南省许昌市长葛市", "河南省许昌市魏都区"};
            
            String name = names[random.nextInt(names.length)];
            String address = addresses[random.nextInt(addresses.length)];
            String idNumber = generateRandomIdNumber();
            
            IdCardData idCardData = new IdCardData(name, idNumber, address);
            idCardDataSubject.onNext(idCardData);
            
            Log.d(TAG, "模拟身份证读取完成: " + name);
        }, 1000);
    }
    
    /**
     * 生成随机身份证号
     */
    private String generateRandomIdNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 17; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(random.nextInt(10)); // 最后一位
        return sb.toString();
    }
    
    /**
     * 模拟打印操作
     */
    public boolean simulatePrint(String content) {
        Log.d(TAG, "开始模拟打印: " + content);
        
        // 模拟打印延迟
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "模拟打印完成");
        }, 2000);
        
        return true;
    }
    
    /**
     * 模拟去皮重/清零操作
     */
    public void simulateTare() {
        Log.d(TAG, "执行去皮重操作");
        currentWeight = 0.0;
        isWeightStable = true;
        
        WeightData weightData = new WeightData(currentWeight, isWeightStable);
        weightDataSubject.onNext(weightData);
    }
    
    /**
     * 模拟添加重量（用于测试）
     */
    public void simulateAddWeight(double weight) {
        Log.d(TAG, "添加模拟重量: " + weight + "kg");
        currentWeight = weight;
        isWeightStable = true;
        
        WeightData weightData = new WeightData(currentWeight, isWeightStable);
        weightDataSubject.onNext(weightData);
    }
    
    /**
     * 获取重量数据流
     */
    public Observable<WeightData> getWeightDataObservable() {
        return weightDataSubject.hide();
    }
    
    /**
     * 获取身份证数据流
     */
    public Observable<IdCardData> getIdCardDataObservable() {
        return idCardDataSubject.hide();
    }
    
    /**
     * 获取设备状态流
     */
    public Observable<DeviceStatus> getDeviceStatusObservable() {
        return deviceStatusSubject.hide();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        executorService.shutdown();
        weightDataSubject.onComplete();
        idCardDataSubject.onComplete();
        deviceStatusSubject.onComplete();
    }
} 