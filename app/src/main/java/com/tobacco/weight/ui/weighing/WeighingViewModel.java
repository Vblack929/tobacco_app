package com.tobacco.weight.ui.weighing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.data.model.WeightRecord;
import com.tobacco.weight.hardware.scale.ScaleManager;
import com.tobacco.weight.hardware.scale.WeightData;
import com.tobacco.weight.ui.admin.AdminViewModel;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 称重ViewModel
 * 管理称重逻辑和数据
 */
@HiltViewModel
public class WeighingViewModel extends ViewModel {

    public enum WeighingState {
        IDLE, WEIGHING, COMPLETED
    }

    private final ScaleManager scaleManager;

    private final MutableLiveData<Double> currentWeight = new MutableLiveData<>(0.0);
    private final MutableLiveData<WeighingState> weighingState = new MutableLiveData<>(WeighingState.IDLE);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");
    private final MutableLiveData<String> precheckRatio = new MutableLiveData<>("0/0");
    private final MutableLiveData<String> precheckWeight = new MutableLiveData<>("0.00 kg");

    // 预检数据
    private int totalPrecheckCount = 0;
    private int currentPrecheckCount = 0;
    private double totalPrecheckWeight = 0.0;

    // 静态数据共享接口
    public interface DataSyncCallback {
        void onDataUpdated(int count, double weight);
    }

    private static DataSyncCallback dataSyncCallback;

    @Inject
    public WeighingViewModel(ScaleManager scaleManager) {
        this.scaleManager = scaleManager;
        initializeScale();
        initializePrecheckData();
    }

    public static void setDataSyncCallback(DataSyncCallback callback) {
        dataSyncCallback = callback;
    }

    private void initializeScale() {
        // 初始化电子秤连接
        scaleManager.setOnWeightDataReceived(this::onWeightDataReceived);
        scaleManager.connect();
    }

    private void initializePrecheckData() {
        // 初始化预检数据
        totalPrecheckCount = 50; // 假设总预检数量为50
        currentPrecheckCount = 0;
        totalPrecheckWeight = 0.0;
        updatePrecheckDisplay();
    }

    private void onWeightDataReceived(WeightData weightData) {
        currentWeight.postValue(weightData.getWeight());

        if (weighingState.getValue() == WeighingState.WEIGHING) {
            // 检查稳定性
            if (weightData.isStable()) {
                weighingState.postValue(WeighingState.COMPLETED);
                statusMessage.postValue("称重完成，重量稳定");
            }
        }
    }

    public void startWeighing() {
        weighingState.setValue(WeighingState.WEIGHING);
        statusMessage.setValue("正在称重，请保持稳定...");
        scaleManager.startMeasurement();
    }

    public void stopWeighing() {
        weighingState.setValue(WeighingState.IDLE);
        statusMessage.setValue("称重已停止");
        scaleManager.stopMeasurement();
    }

    public void saveRecord() {
        Double weight = currentWeight.getValue();
        if (weight != null && weight > 0) {
            WeightRecord record = new WeightRecord();
            record.setWeight(weight);
            record.setTimestamp(System.currentTimeMillis());
            record.setStatus("已保存");

            // TODO: 保存到数据库
            statusMessage.setValue("记录已保存");
            weighingState.setValue(WeighingState.IDLE);
        }
    }

    public void clearWeight() {
        scaleManager.clearWeight();
        currentWeight.setValue(0.0);
        weighingState.setValue(WeighingState.IDLE);
        statusMessage.setValue("重量已清零");
    }

    // Getters
    public LiveData<Double> getCurrentWeight() {
        return currentWeight;
    }

    public LiveData<WeighingState> getWeighingState() {
        return weighingState;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<String> getPrecheckRatio() {
        return precheckRatio;
    }

    public LiveData<String> getPrecheckWeight() {
        return precheckWeight;
    }

    /**
     * 模拟放置轻重量物品（1-5kg）
     */
    public void simulateLightWeight() {
        android.util.Log.d("WeighingViewModel", "simulateLightWeight() 被调用");
        double weight = 1.0 + Math.random() * 4.0; // 1-5kg随机重量
        android.util.Log.d("WeighingViewModel", "生成轻重量: " + weight + " kg");
        simulateWeightPlacement(weight);
        statusMessage.setValue("模拟放置轻重量物品: " + String.format("%.2f kg", weight));
    }

    /**
     * 模拟放置重重量物品（5-15kg）
     */
    public void simulateHeavyWeight() {
        android.util.Log.d("WeighingViewModel", "simulateHeavyWeight() 被调用");
        double weight = 5.0 + Math.random() * 10.0; // 5-15kg随机重量
        android.util.Log.d("WeighingViewModel", "生成重重量: " + weight + " kg");
        simulateWeightPlacement(weight);
        statusMessage.setValue("模拟放置重重量物品: " + String.format("%.2f kg", weight));
    }

    /**
     * 模拟重量放置
     */
    private void simulateWeightPlacement(double weight) {
        scaleManager.getSimulator().simulateAddWeight(weight);
        currentWeight.setValue(weight);

        // 更新预检数据
        updatePrecheckData(weight);
    }

    /**
     * 更新预检数据
     */
    private void updatePrecheckData(double weight) {
        currentPrecheckCount++;
        totalPrecheckWeight += weight;
        updatePrecheckDisplay();

        // 通知AdminViewModel数据更新
        if (dataSyncCallback != null) {
            dataSyncCallback.onDataUpdated(currentPrecheckCount, totalPrecheckWeight);
        }
    }

    /**
     * 更新预检显示
     */
    private void updatePrecheckDisplay() {
        precheckRatio.setValue(currentPrecheckCount + "/" + totalPrecheckCount);
        precheckWeight.setValue(String.format("%.2f kg", totalPrecheckWeight));
    }

    /**
     * 重置预检数据
     */
    public void resetPrecheckData() {
        android.util.Log.d("WeighingViewModel", "resetPrecheckData() 被调用");
        currentPrecheckCount = 0;
        totalPrecheckWeight = 0.0;
        updatePrecheckDisplay();
        statusMessage.setValue("预检数据已重置");
        android.util.Log.d("WeighingViewModel", "预检数据已重置完成");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        scaleManager.disconnect();
    }
}