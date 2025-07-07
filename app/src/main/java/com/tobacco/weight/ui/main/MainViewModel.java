package com.tobacco.weight.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.data.model.WeightRecord;
import com.tobacco.weight.data.repository.WeightRepository;
import com.tobacco.weight.hardware.scale.ScaleManager;
import com.tobacco.weight.hardware.scale.WeightData;
import com.tobacco.weight.hardware.printer.PrinterManager;
import com.tobacco.weight.hardware.idcard.IdCardManager;
import com.tobacco.weight.hardware.idcard.IdCardData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主界面ViewModel
 * 处理称重、身份证读取、数据录入、打印等核心业务逻辑
 */
@HiltViewModel
public class MainViewModel extends ViewModel {
    
    private static final String TAG = "MainViewModel";
    
    // 依赖注入
    private final WeightRepository weightRepository;
    private final ScaleManager scaleManager;
    private final PrinterManager printerManager;
    private final IdCardManager idCardManager;
    
    // 状态管理
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    // LiveData
    private final MutableLiveData<WeightData> currentWeight = new MutableLiveData<>();
    private final MutableLiveData<IdCardData> currentIdCard = new MutableLiveData<>();
    private final MutableLiveData<Boolean> scaleConnected = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> printerConnected = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> idCardConnected = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // 录入数据
    private final MutableLiveData<String> farmerName = new MutableLiveData<>();
    private final MutableLiveData<String> farmerAddress = new MutableLiveData<>();
    private final MutableLiveData<String> idCardNumber = new MutableLiveData<>();
    private final MutableLiveData<Integer> tobaccoBundles = new MutableLiveData<>(1);
    private final MutableLiveData<String> tobaccoPart = new MutableLiveData<>("中叶");
    private final MutableLiveData<String> preCheckNumber = new MutableLiveData<>();
    private final MutableLiveData<String> tobaccoGrade = new MutableLiveData<>("中等");
    private final MutableLiveData<Double> purchasePrice = new MutableLiveData<>(25.0);
    private final MutableLiveData<String> warehouseNumber = new MutableLiveData<>("001");
    private final MutableLiveData<String> operatorName = new MutableLiveData<>("操作员");
    private final MutableLiveData<String> remark = new MutableLiveData<>();
    
    // 统计数据
    private final MutableLiveData<Integer> todayRecordCount = new MutableLiveData<>(0);
    private final MutableLiveData<Double> todayTotalWeight = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> todayTotalAmount = new MutableLiveData<>(0.0);
    
    @Inject
    public MainViewModel(WeightRepository weightRepository, 
                        ScaleManager scaleManager, 
                        PrinterManager printerManager, 
                        IdCardManager idCardManager) {
        this.weightRepository = weightRepository;
        this.scaleManager = scaleManager;
        this.printerManager = printerManager;
        this.idCardManager = idCardManager;
        
        initializeDevices();
        loadTodayStatistics();
    }
    
    /**
     * 初始化设备连接
     */
    private void initializeDevices() {
        // 初始化电子秤
        initializeScale();
        
        // 初始化打印机
        initializePrinter();
        
        // 初始化身份证读卡器
        initializeIdCardReader();
    }
    
    /**
     * 初始化电子秤
     */
    private void initializeScale() {
        // 监听电子秤连接状态
        disposables.add(
            scaleManager.getConnectionObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connected -> {
                    scaleConnected.setValue(connected);
                    updateStatusMessage();
                })
        );
        
        // 监听重量数据
        disposables.add(
            scaleManager.getWeightObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(weightData -> {
                    currentWeight.setValue(weightData);
                })
        );
        
        // 尝试连接电子秤
        connectScale();
    }
    
    /**
     * 初始化打印机
     */
    private void initializePrinter() {
        // 监听打印结果
        disposables.add(
            printerManager.getPrintResultObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        statusMessage.setValue("打印成功: " + result.getRecordNumber());
                    } else {
                        statusMessage.setValue("打印失败: " + result.getMessage());
                    }
                })
        );
        
        // 尝试连接打印机
        connectPrinter();
    }
    
    /**
     * 初始化身份证读卡器
     */
    private void initializeIdCardReader() {
        // 初始化身份证读卡器
        idCardManager.initialize(TobaccoApplication.getAppContext());
        
        // 监听身份证数据
        disposables.add(
            idCardManager.getIdCardObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(idCardData -> {
                    currentIdCard.setValue(idCardData);
                    // 自动填充身份证信息
                    fillIdCardInfo(idCardData);
                })
        );
        
        // 监听连接状态
        disposables.add(
            idCardManager.getConnectionObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connected -> {
                    idCardConnected.setValue(connected);
                    updateStatusMessage();
                })
        );
        
        // 尝试连接身份证读卡器
        connectIdCardReader();
    }
    
    /**
     * 连接电子秤
     */
    public void connectScale() {
        disposables.add(
            Observable.fromCallable(() -> scaleManager.connect("/dev/ttyUSB0", 9600))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        statusMessage.setValue("电子秤连接成功");
                    } else {
                        statusMessage.setValue("电子秤连接失败");
                    }
                })
        );
    }
    
    /**
     * 连接打印机
     */
    public void connectPrinter() {
        disposables.add(
            Observable.fromCallable(() -> printerManager.connect("/dev/ttyUSB1", 9600))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    printerConnected.setValue(success);
                    if (success) {
                        statusMessage.setValue("打印机连接成功");
                    } else {
                        statusMessage.setValue("打印机连接失败");
                    }
                })
        );
    }
    
    /**
     * 连接身份证读卡器
     */
    public void connectIdCardReader() {
        disposables.add(
            Observable.fromCallable(() -> {
                idCardManager.connect(TobaccoApplication.getAppContext());
                return true;
            })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> {
                    if (success) {
                        statusMessage.setValue("身份证读卡器连接成功");
                    } else {
                        statusMessage.setValue("身份证读卡器连接失败");
                    }
                })
        );
    }
    
    /**
     * 自动填充身份证信息
     */
    private void fillIdCardInfo(IdCardData idCardData) {
        if (idCardData != null) {
            farmerName.setValue(idCardData.getName());
            farmerAddress.setValue(idCardData.getAddress());
            idCardNumber.setValue(idCardData.getIdNumber());
            statusMessage.setValue("身份证读取成功: " + idCardData.getName());
        }
    }
    
    /**
     * 保存称重记录
     */
    public void saveWeightRecord() {
        WeightData weight = currentWeight.getValue();
        if (weight == null || !weight.isRecordable()) {
            statusMessage.setValue("重量数据无效或不稳定，无法保存");
            return;
        }
        
        if (farmerName.getValue() == null || farmerName.getValue().trim().isEmpty()) {
            statusMessage.setValue("请输入农户姓名");
            return;
        }
        
        isLoading.setValue(true);
        
        // 创建称重记录
        WeightRecord record = createWeightRecord(weight);
        
        disposables.add(
            weightRepository.insertRecord(record)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    recordId -> {
                        isLoading.setValue(false);
                        record.setId(recordId);
                        statusMessage.setValue("记录保存成功: " + record.getRecordNumber());
                        
                        // 自动打印标签
                        if (printerConnected.getValue() == Boolean.TRUE) {
                            printLabel(record);
                        }
                        
                        // 更新统计数据
                        loadTodayStatistics();
                        
                        // 清空输入
                        clearInputs();
                    },
                    throwable -> {
                        isLoading.setValue(false);
                        statusMessage.setValue("保存失败: " + throwable.getMessage());
                    }
                )
        );
    }
    
    /**
     * 创建称重记录
     */
    private WeightRecord createWeightRecord(WeightData weightData) {
        WeightRecord record = new WeightRecord();
        
        // 生成记录编号
        String recordNumber = generateRecordNumber();
        record.setRecordNumber(recordNumber);
        
        // 设置基本信息
        record.setWeight(weightData.getWeight());
        record.setFarmerName(farmerName.getValue());
        record.setFarmerAddress(farmerAddress.getValue());
        record.setIdCardNumber(idCardNumber.getValue());
        record.setTobaccoBundles(tobaccoBundles.getValue() != null ? tobaccoBundles.getValue() : 1);
        record.setTobaccoPart(tobaccoPart.getValue());
        record.setPreCheckNumber(preCheckNumber.getValue());
        record.setTobaccoGrade(tobaccoGrade.getValue());
        record.setPurchasePrice(purchasePrice.getValue() != null ? purchasePrice.getValue() : 0.0);
        record.setWarehouseNumber(warehouseNumber.getValue());
        record.setOperatorName(operatorName.getValue());
        record.setRemark(remark.getValue());
        
        // 计算总金额
        record.calculateTotalAmount();
        
        // 生成二维码
        record.generateQrCode();
        
        return record;
    }
    
    /**
     * 生成记录编号
     */
    private String generateRecordNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        return "YY" + sdf.format(new Date()) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    /**
     * 打印标签
     */
    public void printLabel(WeightRecord record) {
        if (!printerConnected.getValue()) {
            statusMessage.setValue("打印机未连接");
            return;
        }
        
        disposables.add(
            printerManager.printWeightLabel(record)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess()) {
                            // 更新打印状态
                            weightRepository.markAsPrinted(record.getId())
                                .subscribeOn(Schedulers.io())
                                .subscribe();
                        }
                    }
                )
        );
    }
    
    /**
     * 电子秤去皮重
     */
    public void tareScale() {
        if (scaleManager.isConnected()) {
            scaleManager.tare();
            statusMessage.setValue("电子秤去皮重");
        } else {
            statusMessage.setValue("电子秤未连接");
        }
    }
    
    /**
     * 电子秤清零
     */
    public void zeroScale() {
        if (scaleManager.isConnected()) {
            scaleManager.zero();
            statusMessage.setValue("电子秤清零");
        } else {
            statusMessage.setValue("电子秤未连接");
        }
    }
    
    /**
     * 测试打印
     */
    public void testPrint() {
        if (!printerConnected.getValue()) {
            statusMessage.setValue("打印机未连接");
            return;
        }
        
        disposables.add(
            printerManager.testPrint()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
        );
    }
    
    /**
     * 加载今日统计数据
     */
    private void loadTodayStatistics() {
        // 加载今日记录数
        disposables.add(
            weightRepository.getTodayRecordCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(count -> todayRecordCount.setValue(count))
        );
        
        // 加载今日总重量
        disposables.add(
            weightRepository.getTodayTotalWeight()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(weight -> todayTotalWeight.setValue(weight))
        );
        
        // 加载今日总金额
        disposables.add(
            weightRepository.getTodayTotalAmount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(amount -> todayTotalAmount.setValue(amount))
        );
    }
    
    /**
     * 清空输入
     */
    private void clearInputs() {
        farmerName.setValue("");
        farmerAddress.setValue("");
        idCardNumber.setValue("");
        tobaccoBundles.setValue(1);
        preCheckNumber.setValue("");
        remark.setValue("");
    }
    
    /**
     * 更新状态消息
     */
    private void updateStatusMessage() {
        StringBuilder status = new StringBuilder();
        status.append("设备状态: ");
        status.append("电子秤").append(scaleConnected.getValue() ? "✓" : "✗").append(" ");
        status.append("打印机").append(printerConnected.getValue() ? "✓" : "✗").append(" ");
        status.append("身份证").append(idCardConnected.getValue() ? "✓" : "✗");
        
        statusMessage.setValue(status.toString());
    }
    
    // Getter 方法
    public LiveData<WeightData> getCurrentWeight() { return currentWeight; }
    public LiveData<IdCardData> getCurrentIdCard() { return currentIdCard; }
    public LiveData<Boolean> getScaleConnected() { return scaleConnected; }
    public LiveData<Boolean> getPrinterConnected() { return printerConnected; }
    public LiveData<Boolean> getIdCardConnected() { return idCardConnected; }
    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    
    public MutableLiveData<String> getFarmerName() { return farmerName; }
    public MutableLiveData<String> getFarmerAddress() { return farmerAddress; }
    public MutableLiveData<String> getIdCardNumber() { return idCardNumber; }
    public MutableLiveData<Integer> getTobaccoBundles() { return tobaccoBundles; }
    public MutableLiveData<String> getTobaccoPart() { return tobaccoPart; }
    public MutableLiveData<String> getPreCheckNumber() { return preCheckNumber; }
    public MutableLiveData<String> getTobaccoGrade() { return tobaccoGrade; }
    public MutableLiveData<Double> getPurchasePrice() { return purchasePrice; }
    public MutableLiveData<String> getWarehouseNumber() { return warehouseNumber; }
    public MutableLiveData<String> getOperatorName() { return operatorName; }
    public MutableLiveData<String> getRemark() { return remark; }
    
    public LiveData<Integer> getTodayRecordCount() { return todayRecordCount; }
    public LiveData<Double> getTodayTotalWeight() { return todayTotalWeight; }
    public LiveData<Double> getTodayTotalAmount() { return todayTotalAmount; }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.dispose();
        
        // 释放硬件资源
        if (scaleManager != null) {
            scaleManager.release();
        }
        if (printerManager != null) {
            printerManager.release();
        }
        if (idCardManager != null) {
            idCardManager.release();
        }
    }
} 