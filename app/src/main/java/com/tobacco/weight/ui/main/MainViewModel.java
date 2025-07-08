package com.tobacco.weight.ui.main;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import com.tobacco.weight.hardware.idcard.IdCardData;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * 主界面ViewModel
 */
@HiltViewModel
public class MainViewModel extends ViewModel {
    private static final String TAG = "MainViewModel";

    private final HardwareSimulator hardwareSimulator;
    private final Context context;

    private final MutableLiveData<String> currentWeight = new MutableLiveData<>("0.000");
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);

    // 身份证相关LiveData
    private final MutableLiveData<Boolean> idCardConnected = new MutableLiveData<>(false);
    private final MutableLiveData<IdCardData> idCardData = new MutableLiveData<>();
    private final MutableLiveData<String> farmerName = new MutableLiveData<>("");
    private final MutableLiveData<String> farmerIdNumber = new MutableLiveData<>("");
    private final MutableLiveData<String> farmerAddress = new MutableLiveData<>("");
    private final MutableLiveData<String> farmerGender = new MutableLiveData<>("");

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public MainViewModel(
            HardwareSimulator hardwareSimulator,
            @ApplicationContext Context context) {
        this.hardwareSimulator = hardwareSimulator;
        this.context = context;

        Log.d(TAG, "MainViewModel创建，使用硬件模拟器");

        // 初始化模拟器数据
        initializeSimulatorData();
    }

    /**
     * 初始化模拟器数据
     */
    private void initializeSimulatorData() {
        // 模拟连接状态
        idCardConnected.setValue(true);
        isConnected.setValue(true);

        // 设置默认测试数据
        farmerName.setValue("张三");
        farmerIdNumber.setValue("110101199001011234");
        farmerAddress.setValue("北京市朝阳区");
        farmerGender.setValue("男");

        Log.d(TAG, "模拟器数据初始化完成");
    }

    /**
     * 处理连接状态变化
     */
    private void handleConnectionStatusChange(boolean connected) {
        idCardConnected.setValue(connected);

        if (connected) {
            Log.d(TAG, "身份证读卡器已连接");
        } else {
            Log.d(TAG, "身份证读卡器已断开");
            // 清空表单数据
            clearIdCardInfo();
        }
    }

    /**
     * 处理接收到的身份证数据
     */
    private void handleCardDataReceived(IdCardData cardData) {
        if (cardData != null && cardData.isValid()) {
            idCardData.setValue(cardData);
            fillIdCardInfo(cardData);
            Log.d(TAG, "身份证信息已填充到UI");
        }
    }

    /**
     * 填充身份证信息到UI
     */
    private void fillIdCardInfo(IdCardData cardData) {
        farmerName.setValue(cardData.getName());
        farmerIdNumber.setValue(cardData.getIdNumber());
        farmerAddress.setValue(cardData.getAddress());
        farmerGender.setValue(cardData.getGender());

        Log.d(TAG, "身份证信息填充完成 - 姓名: " + cardData.getName() +
                ", 身份证: " + cardData.getIdNumber() +
                ", 地址: " + cardData.getAddress());
    }

    /**
     * 清空身份证信息
     */
    private void clearIdCardInfo() {
        farmerName.setValue("");
        farmerIdNumber.setValue("");
        farmerAddress.setValue("");
        farmerGender.setValue("");
        idCardData.setValue(null);
    }

    // Getters for LiveData
    public LiveData<String> getCurrentWeight() {
        return currentWeight;
    }

    public LiveData<Boolean> getIsConnected() {
        return isConnected;
    }

    public LiveData<Boolean> getIdCardConnected() {
        return idCardConnected;
    }

    public LiveData<IdCardData> getIdCardData() {
        return idCardData;
    }

    public LiveData<String> getFarmerName() {
        return farmerName;
    }

    public LiveData<String> getFarmerIdNumber() {
        return farmerIdNumber;
    }

    public LiveData<String> getFarmerAddress() {
        return farmerAddress;
    }

    public LiveData<String> getFarmerGender() {
        return farmerGender;
    }

    public HardwareSimulator getHardwareSimulator() {
        return hardwareSimulator;
    }

    public void updateWeight(String weight) {
        currentWeight.setValue(weight);
    }

    public void updateConnectionStatus(boolean connected) {
        isConnected.setValue(connected);
    }

    /**
     * 手动连接读卡器
     */
    public void connectIdCardReader() {
        Log.d(TAG, "模拟连接身份证读卡器");
        idCardConnected.setValue(true);
    }

    /**
     * 模拟读取身份证
     */
    public void simulateReadIdCard() {
        Log.d(TAG, "模拟读取身份证");

        // 创建模拟身份证数据
        IdCardData mockData = new IdCardData();
        mockData.setName("李四");
        mockData.setIdNumber("110101199002021234");
        mockData.setAddress("北京市海淀区");
        mockData.setGender("女");

        handleCardDataReceived(mockData);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "MainViewModel清理");

        disposables.dispose();
    }
}