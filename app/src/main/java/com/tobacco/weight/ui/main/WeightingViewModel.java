package com.tobacco.weight.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.data.FarmerStatistics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

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

    // 合同号数据
    private final MutableLiveData<String> contractA = new MutableLiveData<>("");
    private final MutableLiveData<String> contractB = new MutableLiveData<>("");
    private final MutableLiveData<String> contractC = new MutableLiveData<>("");
    private final MutableLiveData<String> contractD = new MutableLiveData<>("");

    // 合同量数据
    private final MutableLiveData<String> contractAmountA = new MutableLiveData<>("");
    private final MutableLiveData<String> contractAmountB = new MutableLiveData<>("");
    private final MutableLiveData<String> contractAmountC = new MutableLiveData<>("");
    private final MutableLiveData<String> contractAmountD = new MutableLiveData<>("");

    // 新增：预检比例相关数据
    private final MutableLiveData<String> contractAmount = new MutableLiveData<>("");
    private final MutableLiveData<String> precheckRatio = new MutableLiveData<>("");
    private final MutableLiveData<String> upperRatio = new MutableLiveData<>("");
    private final MutableLiveData<String> middleRatio = new MutableLiveData<>("");
    private final MutableLiveData<String> lowerRatio = new MutableLiveData<>("");

    // 预检编号和日期
    private final MutableLiveData<String> currentPrecheckId = new MutableLiveData<>("未生成");
    private final MutableLiveData<String> currentPrecheckDate = new MutableLiveData<>("--");

    // 新增：称重记录管理
    private static int precheckCounter = 100000000; // 预检标号计数器，从YJ100000000开始
    private static int contractCounter = 10000000; // 合同号计数器，从HT10000000开始
    private final Map<String, FarmerStatistics> farmerStatisticsMap = new HashMap<>();
    private final List<WeighingRecord> allWeighingRecords = new ArrayList<>();

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

        // 预设默认烟农姓名（可以通过输入框修改）
        farmerName.setValue("张三");

        // 自动生成初始合同号
        String initialContractNumber = generateContractNumber();
        contractNumber.setValue(initialContractNumber);

        // 固定重量为5kg
        currentWeight.setValue("5.00 kg");
        weightValue.setValue(5.0);
        isWeightStable.setValue(true);

        // 设置设备状态为已连接
        deviceStatus.setValue("电子秤: ✓\n打印机: ✓\n身份证: ✓");
        statusMessage.setValue("设备就绪，重量稳定：5.00kg");

        // 初始化预检比例显示
        upperRatio.setValue("0.0%");
        middleRatio.setValue("0.0%");
        lowerRatio.setValue("0.0%");
        precheckRatio.setValue("0.0%");

        // 初始化预检编号和日期
        currentPrecheckId.setValue("未生成");
        currentPrecheckDate.setValue("--");
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
                        .subscribe(this::onWeightDataReceived));

        // 订阅身份证数据
        compositeDisposable.add(
                hardwareSimulator.getIdCardDataObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onIdCardDataReceived));

        // 订阅设备状态
        compositeDisposable.add(
                hardwareSimulator.getDeviceStatusObservable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onDeviceStatusReceived));
    }

    /**
     * 处理重量数据
     */
    private void onWeightDataReceived(HardwareSimulator.WeightData weightData) {
        // 使用固定重量5.00kg代替随机重量
        double weight = 5.00;
        boolean stable = weightData.isStable();

        weightValue.setValue(weight);
        isWeightStable.setValue(stable);
        currentWeight.setValue(String.format(Locale.getDefault(), "%.2f kg", weight));

        // 更新状态消息
        if (stable) {
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

        // 自动生成合同号
        String newContractNumber = generateContractNumber();
        contractNumber.setValue(newContractNumber);

        // 更新预检比例显示
        updateGlobalPrecheckRatios();
        updateCurrentFarmerPrecheckRatio(idCardData.getName());
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
    public LiveData<String> getFarmerName() {
        return farmerName;
    }

    public LiveData<String> getContractNumber() {
        return contractNumber;
    }

    public LiveData<String> getCurrentWeight() {
        return currentWeight;
    }

    public LiveData<String> getDeviceStatus() {
        return deviceStatus;
    }

    public LiveData<String> getPrecheckLevel() {
        return precheckLevel;
    }

    public LiveData<String> getCurrentTime() {
        return currentTime;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<Double> getWeightValue() {
        return weightValue;
    }

    public LiveData<Boolean> getIsWeightStable() {
        return isWeightStable;
    }

    public LiveData<String> getSelectedLevel() {
        return selectedLevel;
    }

    public LiveData<Boolean> getScaleConnected() {
        return scaleConnected;
    }

    public LiveData<Boolean> getPrinterConnected() {
        return printerConnected;
    }

    public LiveData<Boolean> getIdCardReaderConnected() {
        return idCardReaderConnected;
    }

    public LiveData<String> getPriceA() {
        return priceA;
    }

    public LiveData<String> getPriceB() {
        return priceB;
    }

    public LiveData<String> getPriceC() {
        return priceC;
    }

    public LiveData<String> getPriceD() {
        return priceD;
    }

    // Setters for prices
    public void setPriceA(String price) {
        priceA.setValue(price);
    }

    public void setPriceB(String price) {
        priceB.setValue(price);
    }

    public void setPriceC(String price) {
        priceC.setValue(price);
    }

    public void setPriceD(String price) {
        priceD.setValue(price);
    }

    // Getters for contract data
    public LiveData<String> getContractA() {
        return contractA;
    }

    public LiveData<String> getContractB() {
        return contractB;
    }

    public LiveData<String> getContractC() {
        return contractC;
    }

    public LiveData<String> getContractD() {
        return contractD;
    }

    public LiveData<String> getContractAmountA() {
        return contractAmountA;
    }

    public LiveData<String> getContractAmountB() {
        return contractAmountB;
    }

    public LiveData<String> getContractAmountC() {
        return contractAmountC;
    }

    public LiveData<String> getContractAmountD() {
        return contractAmountD;
    }

    // Setters for contract data
    public void setContractA(String contract) {
        contractA.setValue(contract);
    }

    public void setContractB(String contract) {
        contractB.setValue(contract);
    }

    public void setContractC(String contract) {
        contractC.setValue(contract);
    }

    public void setContractD(String contract) {
        contractD.setValue(contract);
    }

    public void setContractAmountA(String amount) {
        contractAmountA.setValue(amount);
    }

    public void setContractAmountB(String amount) {
        contractAmountB.setValue(amount);
    }

    public void setContractAmountC(String amount) {
        contractAmountC.setValue(amount);
    }

    public void setContractAmountD(String amount) {
        contractAmountD.setValue(amount);
    }

    // 新增：预检比例相关getter方法
    public LiveData<String> getContractAmount() {
        return contractAmount;
    }

    public LiveData<String> getPrecheckRatio() {
        return precheckRatio;
    }

    public LiveData<String> getUpperRatio() {
        return upperRatio;
    }

    public LiveData<String> getMiddleRatio() {
        return middleRatio;
    }

    public LiveData<String> getLowerRatio() {
        return lowerRatio;
    }

    public LiveData<String> getCurrentPrecheckId() {
        return currentPrecheckId;
    }

    public LiveData<String> getCurrentPrecheckDate() {
        return currentPrecheckDate;
    }

    // 新增：预检比例相关setter方法
    public void setContractAmount(String amount) {
        contractAmount.setValue(amount);
    }

    public void setPrecheckRatio(String ratio) {
        precheckRatio.setValue(ratio);
    }

    public void setUpperRatio(String ratio) {
        upperRatio.setValue(ratio);
    }

    public void setMiddleRatio(String ratio) {
        middleRatio.setValue(ratio);
    }

    public void setLowerRatio(String ratio) {
        lowerRatio.setValue(ratio);
    }

    /**
     * 设置烟农姓名（手动输入）
     */
    public void setFarmerName(String name) {
        farmerName.setValue(name);
        // 烟农姓名变更时，重新计算预检比例
        if (name != null && !name.trim().isEmpty()) {
            updateGlobalPrecheckRatios();
            updateCurrentFarmerPrecheckRatio(name.trim());
        }
    }

    /**
     * 确认称重操作
     * 记录当前称重数据并更新统计信息
     */
    public void confirmWeighing() {
        String currentFarmerName = farmerName.getValue();
        String currentContractNumber = contractNumber.getValue();
        String currentSelectedLevel = selectedLevel.getValue();

        if (currentFarmerName == null || currentFarmerName.trim().isEmpty()) {
            statusMessage.setValue("请输入烟农姓名");
            return;
        }

        if (currentSelectedLevel == null || currentSelectedLevel.equals("未选择")) {
            statusMessage.setValue("请选择部叶类型");
            return;
        }

        if (currentContractNumber == null || currentContractNumber.equals("未设置")) {
            // 自动生成合同号
            currentContractNumber = generateContractNumber();
            contractNumber.setValue(currentContractNumber);
        }

        // 生成预检标号 - YJ100000000格式
        String precheckId = "YJ" + String.format("%09d", ++precheckCounter);

        // 更新当前预检编号和日期
        currentPrecheckId.setValue(precheckId);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        currentPrecheckDate.setValue(dateFormat.format(new Date()));

        // 创建称重记录
        WeighingRecord record = new WeighingRecord(
                precheckId,
                currentFarmerName.trim(),
                currentContractNumber,
                currentSelectedLevel,
                5.0 // 固定重量5kg
        );

        // 保存到全局记录列表
        allWeighingRecords.add(record);

        // 更新烟农统计数据
        updateFarmerStatistics(record);

        // 重置选中状态
        selectedLevel.setValue("未选择");

        // 更新状态消息
        statusMessage.setValue("称重完成 - 预检号: " + precheckId + " | " + currentSelectedLevel + " 5.00kg");

        // 更新预检比例显示
        updatePrecheckRatios(currentFarmerName.trim());
    }

    /**
     * 更新烟农统计数据
     */
    private void updateFarmerStatistics(WeighingRecord record) {
        String farmerKey = record.getFarmerName();

        FarmerStatistics statistics = farmerStatisticsMap.get(farmerKey);
        if (statistics == null) {
            statistics = new FarmerStatistics(record.getFarmerName(), record.getContractNumber());
            farmerStatisticsMap.put(farmerKey, statistics);
        }

        statistics.addWeighingRecord(record);
    }

    /**
     * 更新预检比例显示 - 计算全局预检比例
     */
    private void updatePrecheckRatios(String farmerName) {
        // 计算全局各部叶预检比例
        updateGlobalPrecheckRatios();

        // 计算当前烟农的预检比例
        updateCurrentFarmerPrecheckRatio(farmerName);
    }

    /**
     * 计算全局各部叶预检比例
     */
    private void updateGlobalPrecheckRatios() {
        if (allWeighingRecords.isEmpty()) {
            upperRatio.setValue("0.0%");
            middleRatio.setValue("0.0%");
            lowerRatio.setValue("0.0%");
            return;
        }

        // 计算总重量和各部叶重量
        double totalWeight = 0.0;
        double upperWeight = 0.0;
        double middleWeight = 0.0;
        double lowerWeight = 0.0;

        for (WeighingRecord record : allWeighingRecords) {
            double weight = record.getWeight();
            totalWeight += weight;

            switch (record.getLeafType()) {
                case "上部叶":
                    upperWeight += weight;
                    break;
                case "中部叶":
                    middleWeight += weight;
                    break;
                case "下部叶":
                    lowerWeight += weight;
                    break;
            }
        }

        // 计算各部叶预检比例（占全部预检重量的比例）
        if (totalWeight > 0) {
            double upperPercentage = (upperWeight / totalWeight) * 100.0;
            double middlePercentage = (middleWeight / totalWeight) * 100.0;
            double lowerPercentage = (lowerWeight / totalWeight) * 100.0;

            upperRatio.setValue(String.format("%.1f%%", upperPercentage));
            middleRatio.setValue(String.format("%.1f%%", middlePercentage));
            lowerRatio.setValue(String.format("%.1f%%", lowerPercentage));
        } else {
            upperRatio.setValue("0.0%");
            middleRatio.setValue("0.0%");
            lowerRatio.setValue("0.0%");
        }
    }

    /**
     * 计算当前烟农的预检比例（当前预检重量占全部预检重量的比例）
     */
    private void updateCurrentFarmerPrecheckRatio(String farmerName) {
        if (allWeighingRecords.isEmpty()) {
            precheckRatio.setValue("0.0%");
            return;
        }

        // 计算当前烟农的总预检重量
        double farmerTotalWeight = 0.0;
        double globalTotalWeight = 0.0;

        for (WeighingRecord record : allWeighingRecords) {
            double weight = record.getWeight();
            globalTotalWeight += weight;

            if (record.getFarmerName().equals(farmerName)) {
                farmerTotalWeight += weight;
            }
        }

        // 计算当前烟农预检重量占全部预检重量的比例
        if (globalTotalWeight > 0) {
            double farmerPercentage = (farmerTotalWeight / globalTotalWeight) * 100.0;
            precheckRatio.setValue(String.format("%.1f%%", farmerPercentage));
        } else {
            precheckRatio.setValue("0.0%");
        }
    }

    /**
     * 获取烟农统计数据
     */
    public FarmerStatistics getFarmerStatistics(String farmerName) {
        return farmerStatisticsMap.get(farmerName);
    }

    /**
     * 获取所有烟农统计数据
     */
    public Map<String, FarmerStatistics> getAllFarmerStatistics() {
        return new HashMap<>(farmerStatisticsMap);
    }

    /**
     * 获取所有称重记录
     */
    public List<WeighingRecord> getAllWeighingRecords() {
        return new ArrayList<>(allWeighingRecords);
    }

    /**
     * 生成新的合同号
     */
    private String generateContractNumber() {
        return "HT" + String.format("%08d", ++contractCounter);
    }

    /**
     * 手动生成新合同号（供外部调用）
     */
    public void generateNewContractNumber() {
        String newContractNumber = generateContractNumber();
        contractNumber.setValue(newContractNumber);
        statusMessage.setValue("已生成新合同号：" + newContractNumber);
    }

    /**
     * 重置所有数据（测试用）
     */
    public void resetAllData() {
        farmerStatisticsMap.clear();
        allWeighingRecords.clear();
        precheckCounter = 100000000; // 重置预检编号
        contractCounter = 10000000; // 重置合同编号

        // 重置界面数据
        farmerName.setValue("张三");
        selectedLevel.setValue("未选择");
        String newContractNumber = generateContractNumber();
        contractNumber.setValue(newContractNumber);

        // 重置预检比例显示
        upperRatio.setValue("0.0%");
        middleRatio.setValue("0.0%");
        lowerRatio.setValue("0.0%");
        precheckRatio.setValue("0.0%");

        // 重置预检编号和日期
        currentPrecheckId.setValue("未生成");
        currentPrecheckDate.setValue("--");

        statusMessage.setValue("数据已重置");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.dispose();
        hardwareSimulator.cleanup();
    }
}