package com.tobacco.weight.ui.main;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.hardware.scale.ScaleManager;
import com.tobacco.weight.hardware.printer.PrinterManager;
import com.tobacco.weight.hardware.idcard.IdCardManager;
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
    
    private final ScaleManager scaleManager;
    private final PrinterManager printerManager;
    private final IdCardManager idCardManager;
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
        ScaleManager scaleManager,
        PrinterManager printerManager,
        IdCardManager idCardManager,
        @ApplicationContext Context context
    ) {
        this.scaleManager = scaleManager;
        this.printerManager = printerManager;
        this.idCardManager = idCardManager;
        this.context = context;
        
        Log.d(TAG, "MainViewModel创建，初始化身份证读卡器");
        
        // 初始化身份证读卡器并订阅流
        initializeIdCardReader();
    }
    
    /**
     * 初始化身份证读卡器并订阅流
     */
    private void initializeIdCardReader() {
        // 初始化读卡器
        idCardManager.initialize(context);
        
        // 订阅连接状态流
        disposables.add(
            idCardManager.connectionStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    connected -> {
                        Log.d(TAG, "收到连接状态更新: " + connected);
                        handleConnectionStatusChange(connected);
                    },
                    throwable -> {
                        Log.e(TAG, "连接状态流异常", throwable);
                        idCardConnected.setValue(false);
                    }
                )
        );
        
        // 订阅身份证数据流
        disposables.add(
            idCardManager.cardDataStream()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    cardData -> {
                        Log.d(TAG, "收到身份证数据: " + cardData.getName());
                        handleCardDataReceived(cardData);
                    },
                    throwable -> {
                        Log.e(TAG, "身份证数据流异常", throwable);
                    }
                )
        );
        
        Log.d(TAG, "身份证读卡器流订阅完成");
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
    public LiveData<String> getCurrentWeight() { return currentWeight; }
    public LiveData<Boolean> getIsConnected() { return isConnected; }
    public LiveData<Boolean> getIdCardConnected() { return idCardConnected; }
    public LiveData<IdCardData> getIdCardData() { return idCardData; }
    public LiveData<String> getFarmerName() { return farmerName; }
    public LiveData<String> getFarmerIdNumber() { return farmerIdNumber; }
    public LiveData<String> getFarmerAddress() { return farmerAddress; }
    public LiveData<String> getFarmerGender() { return farmerGender; }
    
    public ScaleManager getScaleManager() { return scaleManager; }
    public PrinterManager getPrinterManager() { return printerManager; }
    public IdCardManager getIdCardManager() { return idCardManager; }
    
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
        Log.d(TAG, "手动连接身份证读卡器");
        idCardManager.connectReader();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "MainViewModel清理");
        
        disposables.dispose();
        
        if (idCardManager != null) {
            idCardManager.release();
        }
    }
} 