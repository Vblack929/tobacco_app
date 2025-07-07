package com.tobacco.weight.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.hardware.scale.ScaleManager;
import com.tobacco.weight.hardware.printer.PrinterManager;
import com.tobacco.weight.hardware.idcard.IdCardManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 主界面ViewModel
 */
@HiltViewModel
public class MainViewModel extends ViewModel {
    
    private final ScaleManager scaleManager;
    private final PrinterManager printerManager;
    private final IdCardManager idCardManager;
    
    private final MutableLiveData<String> currentWeight = new MutableLiveData<>("0.000");
    private final MutableLiveData<Boolean> isConnected = new MutableLiveData<>(false);
    
    @Inject
    public MainViewModel(
        ScaleManager scaleManager,
        PrinterManager printerManager,
        IdCardManager idCardManager
    ) {
        this.scaleManager = scaleManager;
        this.printerManager = printerManager;
        this.idCardManager = idCardManager;
    }
    
    public LiveData<String> getCurrentWeight() { return currentWeight; }
    public LiveData<Boolean> getIsConnected() { return isConnected; }
    
    public ScaleManager getScaleManager() { return scaleManager; }
    public PrinterManager getPrinterManager() { return printerManager; }
    public IdCardManager getIdCardManager() { return idCardManager; }
    
    public void updateWeight(String weight) {
        currentWeight.setValue(weight);
    }
    
    public void updateConnectionStatus(boolean connected) {
        isConnected.setValue(connected);
    }
} 