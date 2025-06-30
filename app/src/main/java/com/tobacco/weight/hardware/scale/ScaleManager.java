package com.tobacco.weight.hardware.scale;

import android.util.Log;

import com.tobacco.weight.hardware.serial.SerialPortManager;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * 电子秤管理器
 * 负责电子秤连接、数据解析、重量稳定性检测
 */
public class ScaleManager {
    
    private static final String TAG = "ScaleManager";
    
    // 重量数据正则表达式（适配不同厂商的电子秤协议）
    private static final Pattern WEIGHT_PATTERN_1 = Pattern.compile("([+-]?\\d+\\.?\\d*)\\s*kg", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEIGHT_PATTERN_2 = Pattern.compile("ST,GS,([+-]?\\d+\\.?\\d*)");
    private static final Pattern WEIGHT_PATTERN_3 = Pattern.compile("([+-]?\\d+\\.?\\d*)");
    
    // 重量稳定性检测参数
    private static final int STABLE_CHECK_COUNT = 5;      // 连续稳定次数
    private static final double STABLE_THRESHOLD = 0.01;   // 稳定阈值（kg）
    private static final long STABLE_CHECK_INTERVAL = 200; // 检测间隔（ms）
    
    private SerialPortManager serialPortManager;
    private CompositeDisposable disposables;
    
    // 重量数据流
    private BehaviorSubject<WeightData> weightSubject;
    private BehaviorSubject<Boolean> connectionSubject;
    
    // 重量稳定性检测
    private double[] recentWeights;
    private int weightIndex = 0;
    private boolean isWeightStable = false;
    
    private WeightData currentWeight;
    
    /**
     * 构造函数
     */
    public ScaleManager() {
        this.serialPortManager = new SerialPortManager();
        this.disposables = new CompositeDisposable();
        this.weightSubject = BehaviorSubject.create();
        this.connectionSubject = BehaviorSubject.createDefault(false);
        this.recentWeights = new double[STABLE_CHECK_COUNT];
        this.currentWeight = new WeightData();
    }
    
    /**
     * 连接电子秤
     * @param portPath 串口路径
     * @param baudRate 波特率
     * @return 是否连接成功
     */
    public boolean connect(String portPath, int baudRate) {
        try {
            if (serialPortManager.isOpen()) {
                disconnect();
            }
            
            boolean success = serialPortManager.openSerialPort(portPath, baudRate);
            if (success) {
                startDataReading();
                connectionSubject.onNext(true);
                Log.i(TAG, "电子秤连接成功");
            } else {
                connectionSubject.onNext(false);
                Log.e(TAG, "电子秤连接失败");
            }
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "连接电子秤异常: " + e.getMessage(), e);
            connectionSubject.onNext(false);
            return false;
        }
    }
    
    /**
     * 断开电子秤连接
     */
    public void disconnect() {
        try {
            disposables.clear();
            serialPortManager.closeSerialPort();
            connectionSubject.onNext(false);
            resetWeightStability();
            Log.i(TAG, "电子秤连接已断开");
            
        } catch (Exception e) {
            Log.e(TAG, "断开电子秤连接异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 开始读取数据
     */
    private void startDataReading() {
        Observable<byte[]> dataObservable = serialPortManager.startReading()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        
        disposables.add(
            dataObservable.subscribe(
                this::processRawData,
                throwable -> {
                    Log.e(TAG, "读取电子秤数据异常: " + throwable.getMessage(), throwable);
                    connectionSubject.onNext(false);
                }
            )
        );
    }
    
    /**
     * 处理原始数据
     * @param rawData 原始字节数据
     */
    private void processRawData(byte[] rawData) {
        try {
            String dataString = new String(rawData).trim();
            Log.d(TAG, "接收电子秤数据: " + dataString);
            
            WeightData weightData = parseWeightData(dataString);
            if (weightData != null) {
                currentWeight = weightData;
                checkWeightStability(weightData.getWeight());
                weightData.setStable(isWeightStable);
                weightSubject.onNext(weightData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理电子秤数据异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析重量数据
     * @param dataString 数据字符串
     * @return 解析后的重量数据
     */
    private WeightData parseWeightData(String dataString) {
        try {
            WeightData weightData = new WeightData();
            weightData.setRawData(dataString);
            weightData.setTimestamp(System.currentTimeMillis());
            
            // 尝试不同的解析模式
            Double weight = tryParseWeight(dataString);
            if (weight != null) {
                weightData.setWeight(weight);
                weightData.setValid(true);
                
                // 检测状态信息
                weightData.setOverload(dataString.contains("OL") || dataString.contains("OVER"));
                weightData.setUnderload(dataString.contains("UL") || dataString.contains("UNDER"));
                weightData.setError(dataString.contains("ERR") || dataString.contains("ERROR"));
                
                return weightData;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "解析重量数据异常: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 尝试解析重量值
     * @param dataString 数据字符串
     * @return 重量值（kg）
     */
    private Double tryParseWeight(String dataString) {
        // 模式1: 包含kg单位
        Matcher matcher1 = WEIGHT_PATTERN_1.matcher(dataString);
        if (matcher1.find()) {
            try {
                return Double.parseDouble(matcher1.group(1));
            } catch (NumberFormatException e) {
                Log.w(TAG, "解析重量数据格式错误: " + matcher1.group(1));
            }
        }
        
        // 模式2: 标准协议格式
        Matcher matcher2 = WEIGHT_PATTERN_2.matcher(dataString);
        if (matcher2.find()) {
            try {
                return Double.parseDouble(matcher2.group(1));
            } catch (NumberFormatException e) {
                Log.w(TAG, "解析重量数据格式错误: " + matcher2.group(1));
            }
        }
        
        // 模式3: 纯数字格式
        Matcher matcher3 = WEIGHT_PATTERN_3.matcher(dataString.replaceAll("[^\\d.-]", ""));
        if (matcher3.find()) {
            try {
                String weightStr = matcher3.group(1);
                if (!weightStr.isEmpty()) {
                    return Double.parseDouble(weightStr);
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "解析重量数据格式错误: " + matcher3.group(1));
            }
        }
        
        return null;
    }
    
    /**
     * 检测重量稳定性
     * @param weight 当前重量
     */
    private void checkWeightStability(double weight) {
        recentWeights[weightIndex] = weight;
        weightIndex = (weightIndex + 1) % STABLE_CHECK_COUNT;
        
        // 检查是否有足够的数据点
        boolean hasEnoughData = true;
        for (double w : recentWeights) {
            if (w == 0.0) {
                hasEnoughData = false;
                break;
            }
        }
        
        if (!hasEnoughData) {
            isWeightStable = false;
            return;
        }
        
        // 计算重量变化范围
        double min = recentWeights[0];
        double max = recentWeights[0];
        for (double w : recentWeights) {
            if (w < min) min = w;
            if (w > max) max = w;
        }
        
        // 判断是否稳定
        isWeightStable = (max - min) <= STABLE_THRESHOLD;
    }
    
    /**
     * 重置重量稳定性检测
     */
    private void resetWeightStability() {
        for (int i = 0; i < recentWeights.length; i++) {
            recentWeights[i] = 0.0;
        }
        weightIndex = 0;
        isWeightStable = false;
    }
    
    /**
     * 发送控制命令
     * @param command 命令字符串
     * @return 是否发送成功
     */
    public boolean sendCommand(String command) {
        if (!serialPortManager.isOpen()) {
            Log.e(TAG, "电子秤未连接，无法发送命令");
            return false;
        }
        
        return serialPortManager.sendData(command + "\r\n");
    }
    
    /**
     * 去皮重
     */
    public boolean tare() {
        return sendCommand("T");
    }
    
    /**
     * 清零
     */
    public boolean zero() {
        return sendCommand("Z");
    }
    
    /**
     * 获取重量数据流
     */
    public Observable<WeightData> getWeightObservable() {
        return weightSubject.distinctUntilChanged();
    }
    
    /**
     * 获取连接状态流
     */
    public Observable<Boolean> getConnectionObservable() {
        return connectionSubject.distinctUntilChanged();
    }
    
    /**
     * 获取当前重量
     */
    public WeightData getCurrentWeight() {
        return currentWeight;
    }
    
    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return serialPortManager.isOpen();
    }
    
    /**
     * 释放资源
     */
    public void release() {
        disconnect();
        disposables.dispose();
        serialPortManager.release();
        
        if (weightSubject != null) {
            weightSubject.onComplete();
        }
        
        if (connectionSubject != null) {
            connectionSubject.onComplete();
        }
    }
} 