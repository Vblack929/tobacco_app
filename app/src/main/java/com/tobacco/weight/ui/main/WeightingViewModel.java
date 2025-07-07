package com.tobacco.weight.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 称重界面ViewModel
 * 管理称重相关的数据和业务逻辑
 */
@HiltViewModel
public class WeightingViewModel extends ViewModel {
    
    private final HardwareSimulator hardwareSimulator;
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    
    // 界面数据
    private final MutableLiveData<String> farmerName = new MutableLiveData<>("未读取");
    private final MutableLiveData<String> contractNumber = new MutableLiveData<>("未设置");
    private final MutableLiveData<String> currentWeight = new MutableLiveData<>("0.00 kg");
    private final MutableLiveData<String> deviceStatus = new MutableLiveData<>("设备连接中...");
    private final MutableLiveData<String> precheckLevel = new MutableLiveData<>("未检测");
    private final MutableLiveData<String> currentTime = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("设备就绪，请放置烟叶");
    
    // 称重数据
    private final MutableLiveData<Double> weightValue = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isWeightStable = new MutableLiveData<>(false);
    private final MutableLiveData<String> selectedLevel = new MutableLiveData<>("未选择");
    
    // 设备状态
    private final MutableLiveData<Boolean> scaleConnected = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> printerConnected = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> idCardReaderConnected = new MutableLiveData<>(false);
    
    // 价格数据
    private final MutableLiveData<String> priceA = new MutableLiveData<>("0.00");
    private final MutableLiveData<String> priceB = new MutableLiveData<>("0.00");
    private final MutableLiveData<String> priceC = new MutableLiveData<>("0.00");
    private final MutableLiveData<String> priceD = new MutableLiveData<>("0.00");
    
    @Inject
    public WeightingViewModel(HardwareSimulator hardwareSimulator) {
        this.hardwareSimulator = hardwareSimulator;
        initializeData();
        subscribeToHardwareData();
    }
    
    /**
     * 初始化数据
     */
    private void initializeData() {
        // 设置当前时间
        updateCurrentTime();
        
        // 初始化硬件模拟器
        hardwareSimulator.initialize();
        
        // 设置默认价格
        priceA.setValue("25.00");
        priceB.setValue("20.00");
        priceC.setValue("15.00");
        priceD.setValue("10.00");
    }
    
    /**
     * 订阅硬件数据
     */
    private void subscribeToHardwareData() {
        // 订阅重量数据
        compositeDisposable.add(
            hardwareSimulator.getWeightDataObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onWeightDataReceived)
        );
        
        // 订阅身份证数据
        compositeDisposable.add(
            hardwareSimulator.getIdCardDataObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onIdCardDataReceived)
        );
        
        // 订阅设备状态
        compositeDisposable.add(
            hardwareSimulator.getDeviceStatusObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onDeviceStatusReceived)
        );
    }
    
    /**
     * 处理重量数据
     */
    private void onWeightDataReceived(HardwareSimulator.WeightData weightData) {
        double weight = weightData.getWeight();
        boolean stable = weightData.isStable();
        
        weightValue.setValue(weight);
        isWeightStable.setValue(stable);
        currentWeight.setValue(String.format(Locale.getDefault(), "%.2f kg", weight));
        
        // 更新状态消息
        if (weightData.isOverload()) {
            statusMessage.setValue("警告：重量超载！");
        } else if (weightData.isUnderload()) {
            statusMessage.setValue("请放置烟叶");
        } else if (stable) {
            statusMessage.setValue("重量稳定，可以操作");
        } else {
            statusMessage.setValue("重量不稳定，请等待...");
        }
    }
    
    /**
     * 处理身份证数据
     */
    private void onIdCardDataReceived(HardwareSimulator.IdCardData idCardData) {
        farmerName.setValue(idCardData.getName());
        statusMessage.setValue("身份证读取成功：" + idCardData.getName());
        
        // 可以根据身份证信息查询合同号
        contractNumber.setValue("HT" + System.currentTimeMillis() % 1000000);
    }
    
    /**
     * 处理设备状态
     */
    private void onDeviceStatusReceived(HardwareSimulator.DeviceStatus deviceStatus) {
        scaleConnected.setValue(deviceStatus.isScaleConnected());
        printerConnected.setValue(deviceStatus.isPrinterConnected());
        idCardReaderConnected.setValue(deviceStatus.isIdCardReaderConnected());
        
        // 更新设备状态显示
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("电子秤: ").append(deviceStatus.isScaleConnected() ? "✓" : "✗").append("\n");
        statusBuilder.append("打印机: ").append(deviceStatus.isPrinterConnected() ? "✓" : "✗").append("\n");
        statusBuilder.append("身份证: ").append(deviceStatus.isIdCardReaderConnected() ? "✓" : "✗");
        this.deviceStatus.setValue(statusBuilder.toString());
    }
    
    /**
     * 更新当前时间
     */
    private void updateCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        currentTime.setValue(sdf.format(new Date()));
    }
    
    /**
     * 读取身份证
     */
    public void readIdCard() {
        statusMessage.setValue("正在读取身份证...");
        hardwareSimulator.simulateIdCardRead();
    }
    
    /**
     * 去皮重操作
     */
    public void performTare() {
        statusMessage.setValue("执行去皮重操作");
        hardwareSimulator.simulateTare();
    }
    
    /**
     * 选择等级
     */
    public void selectLevel(String level) {
        selectedLevel.setValue(level);
        statusMessage.setValue("已选择等级：" + level);
    }
    
    /**
     * 打印标签
     */
    public void printLabel() {
        if (Boolean.TRUE.equals(printerConnected.getValue())) {
            String content = generateLabelContent();
            hardwareSimulator.simulatePrint(content);
            statusMessage.setValue("正在打印标签...");
        } else {
            statusMessage.setValue("打印机未连接");
        }
    }
    
    /**
     * 生成标签内容
     */
    private String generateLabelContent() {
        StringBuilder content = new StringBuilder();
        content.append("农户：").append(farmerName.getValue()).append("\n");
        content.append("合同号：").append(contractNumber.getValue()).append("\n");
        content.append("重量：").append(currentWeight.getValue()).append("\n");
        content.append("等级：").append(selectedLevel.getValue()).append("\n");
        content.append("时间：").append(currentTime.getValue()).append("\n");
        return content.toString();
    }
    
    /**
     * 测试模拟重量
     */
    public void simulateWeight(double weight) {
        hardwareSimulator.simulateAddWeight(weight);
        statusMessage.setValue("模拟重量：" + weight + "kg");
    }
    
    /**
     * 保存称重记录
     */
    public void saveWeightRecord() {
        if (Boolean.TRUE.equals(isWeightStable.getValue())) {
            // 这里应该保存到数据库
            statusMessage.setValue("称重记录已保存");
            updateCurrentTime();
        } else {
            statusMessage.setValue("重量不稳定，无法保存");
        }
    }
    
    // Getters for LiveData
    public LiveData<String> getFarmerName() { return farmerName; }
    public LiveData<String> getContractNumber() { return contractNumber; }
    public LiveData<String> getCurrentWeight() { return currentWeight; }
    public LiveData<String> getDeviceStatus() { return deviceStatus; }
    public LiveData<String> getPrecheckLevel() { return precheckLevel; }
    public LiveData<String> getCurrentTime() { return currentTime; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Double> getWeightValue() { return weightValue; }
    public LiveData<Boolean> getIsWeightStable() { return isWeightStable; }
    public LiveData<String> getSelectedLevel() { return selectedLevel; }
    public LiveData<Boolean> getScaleConnected() { return scaleConnected; }
    public LiveData<Boolean> getPrinterConnected() { return printerConnected; }
    public LiveData<Boolean> getIdCardReaderConnected() { return idCardReaderConnected; }
    public LiveData<String> getPriceA() { return priceA; }
    public LiveData<String> getPriceB() { return priceB; }
    public LiveData<String> getPriceC() { return priceC; }
    public LiveData<String> getPriceD() { return priceD; }
    
    // Setters for prices
    public void setPriceA(String price) { priceA.setValue(price); }
    public void setPriceB(String price) { priceB.setValue(price); }
    public void setPriceC(String price) { priceC.setValue(price); }
    public void setPriceD(String price) { priceD.setValue(price); }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.dispose();
        hardwareSimulator.cleanup();
    }
} 