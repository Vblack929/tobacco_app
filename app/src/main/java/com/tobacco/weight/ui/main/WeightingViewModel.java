package com.tobacco.weight.ui.main;

import com.tobacco.weight.data.model.WeightRecord;
import com.tobacco.weight.data.repository.WeightRecordRepository;
import com.tobacco.weight.data.repository.FarmerInfoRepository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.data.FarmerStatistics;
import com.tobacco.weight.data.FarmerInfo;

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

    // 身份证相关数据存储
    private String currentIdCardNumber = ""; // 当前身份证号
    private String currentGender = ""; // 当前性别
    private String currentNationality = ""; // 当前民族
    private String currentBirthDate = ""; // 当前出生日期
    private String currentAddress = ""; // 当前地址
    private String currentDepartment = ""; // 当前签发机关
    private String currentStartDate = ""; // 当前有效期开始
    private String currentEndDate = ""; // 当前有效期结束
    private byte[] currentPhoto = null; // 当前照片

    // 数据库Repository
    private final WeightRecordRepository weightRecordRepository;
    private final FarmerInfoRepository farmerInfoRepository;

    // Print-related LiveData
    private final MutableLiveData<PrintEvent> printEvent = new MutableLiveData<>();
    private final MutableLiveData<String> printStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isTestMode = new MutableLiveData<>(true); // Track test mode state

    // Print event class for communication with Fragment
    public static class PrintEvent {
        public enum Type {
            PRINT_SUCCESS, PRINT_FAILURE, CONNECTION_SUCCESS, CONNECTION_FAILED, STATUS_UPDATE
        }
        
        private final Type type;
        private final String message;
        private final String details;
        private final PrintData printData;
        
        public PrintEvent(Type type, String message, String details, PrintData printData) {
            this.type = type;
            this.message = message;
            this.details = details;
            this.printData = printData;
        }
        
        public Type getType() { return type; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
        public PrintData getPrintData() { return printData; }
    }
    
    // Print data class
    public static class PrintData {
        private final String farmerName;
        private final String tobaccoLevel;
        private final String precheckId;
        private final String printDate;
        private final String contractNumber;
        
        public PrintData(String farmerName, String tobaccoLevel, String precheckId, String printDate, String contractNumber) {
            this.farmerName = farmerName;
            this.tobaccoLevel = tobaccoLevel;
            this.precheckId = precheckId;
            this.printDate = printDate;
            this.contractNumber = contractNumber;
        }
        
        public String getFarmerName() { return farmerName; }
        public String getTobaccoLevel() { return tobaccoLevel; }
        public String getPrecheckId() { return precheckId; }
        public String getPrintDate() { return printDate; }
        public String getContractNumber() { return contractNumber; }
    }

    @Inject
    public WeightingViewModel(HardwareSimulator hardwareSimulator, WeightRecordRepository weightRecordRepository, FarmerInfoRepository farmerInfoRepository) {
        this.hardwareSimulator = hardwareSimulator;
        this.weightRecordRepository = weightRecordRepository;
        this.farmerInfoRepository = farmerInfoRepository;
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
        String readName = idCardData.getName();
        String readIdNumber = idCardData.getIdNumber();
        
        // 验证烟农身份一致性（模拟器也需要验证）
        if (!validateFarmerIdentity(readName, readIdNumber)) {
            String validationMessage = getFarmerValidationMessage(readName, readIdNumber);
            statusMessage.setValue("身份验证失败：" + validationMessage);
            return;
        }
        
        // 存储完整身份证信息（从模拟器）
        farmerName.setValue(readName);
        currentIdCardNumber = readIdNumber != null ? readIdNumber : "";
        currentAddress = idCardData.getAddress() != null ? idCardData.getAddress() : "";
        currentGender = ""; // 模拟器数据中没有性别字段
        currentNationality = "";
        currentBirthDate = "";
        currentDepartment = "";
        currentStartDate = idCardData.getIssueDate() != null ? idCardData.getIssueDate() : "";
        currentEndDate = idCardData.getExpiryDate() != null ? idCardData.getExpiryDate() : "";
        currentPhoto = idCardData.getPhoto();
        
        // 显示验证结果
        String validationMessage = getFarmerValidationMessage(readName, readIdNumber);
        statusMessage.setValue("身份证读取成功：" + readName + " - " + validationMessage);

        // 为新烟农生成合同号，已存在的烟农保持原合同号
        FarmerStatistics existing = findFarmerByIdCard(currentIdCardNumber);
        if (existing != null) {
            contractNumber.setValue(existing.getFarmerInfo().getContractNumber());
        } else {
            String newContractNumber = generateContractNumber();
            contractNumber.setValue(newContractNumber);
        }

        // 更新预检比例显示
        updateGlobalPrecheckRatios();
        updateCurrentFarmerPrecheckRatio(readName);
    }
    
    /**
     * 处理真实身份证数据（从真实硬件）
     */
    public void onRealIdCardDataReceived(com.tobacco.weight.hardware.idcard.IdCardData realIdCardData) {
        if (realIdCardData == null || !realIdCardData.isValid()) {
            statusMessage.setValue("身份证读取失败：数据无效");
            return;
        }
        
        String readName = realIdCardData.getName();
        String readIdNumber = realIdCardData.getIdNumber();
        
        // 验证烟农身份一致性
        if (!validateFarmerIdentity(readName, readIdNumber)) {
            String validationMessage = getFarmerValidationMessage(readName, readIdNumber);
            statusMessage.setValue("身份验证失败：" + validationMessage);
            return;
        }
        
        // 存储完整身份证信息（从真实硬件）
        farmerName.setValue(readName);
        currentIdCardNumber = readIdNumber != null ? readIdNumber : "";
        currentGender = realIdCardData.getGender() != null ? realIdCardData.getGender() : "";
        currentNationality = realIdCardData.getNationality() != null ? realIdCardData.getNationality() : "";
        currentBirthDate = realIdCardData.getBirthDate() != null ? realIdCardData.getBirthDate() : "";
        currentAddress = realIdCardData.getAddress() != null ? realIdCardData.getAddress() : "";
        currentDepartment = realIdCardData.getDepartment() != null ? realIdCardData.getDepartment() : "";
        currentStartDate = realIdCardData.getStartDate() != null ? realIdCardData.getStartDate() : "";
        currentEndDate = realIdCardData.getEndDate() != null ? realIdCardData.getEndDate() : "";
        
        // 处理照片（Bitmap转byte[]）
        if (realIdCardData.getPhoto() != null) {
            try {
                java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
                realIdCardData.getPhoto().compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream);
                currentPhoto = stream.toByteArray();
                stream.close();
            } catch (Exception e) {
                currentPhoto = null;
            }
        } else {
            currentPhoto = null;
        }
        
        // 显示验证结果
        String validationMessage = getFarmerValidationMessage(readName, readIdNumber);
        statusMessage.setValue("身份证读取成功：" + readName + " (身份证号: " + maskIdCard(currentIdCardNumber) + ") - " + validationMessage);

        // 为新烟农生成合同号，已存在的烟农保持原合同号
        FarmerStatistics existing = findFarmerByIdCard(currentIdCardNumber);
        if (existing != null) {
            contractNumber.setValue(existing.getFarmerInfo().getContractNumber());
        } else {
            String newContractNumber = generateContractNumber();
            contractNumber.setValue(newContractNumber);
        }

        // 更新预检比例显示
        updateGlobalPrecheckRatios();
        updateCurrentFarmerPrecheckRatio(readName);
    }
    
    /**
     * 遮盖身份证号中间部分
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 14) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
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
     * 测试模拟重量
     */
    public void simulateWeight(double weight) {
        hardwareSimulator.simulateAddWeight(weight);
        statusMessage.setValue("模拟重量：" + weight + "kg");
    }

    /**
     * 保存称重记录到数据库
     */
    public void saveWeightRecord() {
        if (Boolean.TRUE.equals(isWeightStable.getValue())) {
            // 首先保存农户信息（如果是新农户）
            saveFarmerInfoIfNew(() -> {
                // 然后创建称重记录并保存到数据库
                WeightRecord dbRecord = createWeightRecordFromCurrentData();
                if (dbRecord != null) {
                    weightRecordRepository.insert(dbRecord, new WeightRecordRepository.OnResultListener<Long>() {
                        @Override
                        public void onSuccess(Long recordId) {
                            statusMessage.postValue("称重记录已保存到数据库，ID: " + recordId);
                            updateCurrentTime();
                        }

                        @Override
                        public void onError(Exception e) {
                            statusMessage.postValue("保存失败: " + e.getMessage());
                        }
                    });
                } else {
                    statusMessage.setValue("数据不完整，无法保存");
                }
            });
        } else {
            statusMessage.setValue("重量不稳定，无法保存");
        }
    }

    /**
     * 保存农户信息（仅在首次出现身份证号时保存）
     */
    private void saveFarmerInfoIfNew(Runnable onComplete) {
        String farmerNameValue = farmerName.getValue();
        
        // 如果没有身份证号，但有农户姓名，则用姓名作为标识符保存
        if (currentIdCardNumber == null || currentIdCardNumber.trim().isEmpty()) {
            if (farmerNameValue == null || farmerNameValue.trim().isEmpty() || farmerNameValue.equals("未读取")) {
                statusMessage.setValue("农户信息不完整，无法保存");
                if (onComplete != null) onComplete.run();
                return;
            }
            
            // 生成一个临时身份证号用于数据库存储 (使用农户姓名的hash)
            String tempIdCard = "TEMP_" + farmerNameValue.hashCode();
            
            // 创建FarmerInfo对象（没有真实身份证信息）
            FarmerInfo farmerInfo = FarmerInfo.createWithIdCard(
                farmerNameValue,
                contractNumber.getValue() != null ? contractNumber.getValue() : "",
                tempIdCard, // 使用临时ID
                "", // 空性别
                "", // 空民族
                "", // 空出生日期
                "", // 空地址
                "", // 空签发机关
                "", // 空起始日期
                "", // 空结束日期
                null // 空照片
            );

            // 保存农户信息
            farmerInfoRepository.insertIfIdCardNotExists(farmerInfo, new FarmerInfoRepository.OnResultListener<FarmerInfoRepository.InsertResult>() {
                @Override
                public void onSuccess(FarmerInfoRepository.InsertResult result) {
                    if (result.isNewRecord()) {
                        statusMessage.postValue("新农户信息已保存（手动输入）: " + result.getMessage());
                    } else {
                        statusMessage.postValue("农户信息已存在（手动输入）: " + result.getMessage());
                    }
                    
                    // 更新当前身份证号为临时ID，以便后续保存称重记录时使用
                    currentIdCardNumber = tempIdCard;
                    
                    if (onComplete != null) onComplete.run();
                }

                @Override
                public void onError(Exception e) {
                    statusMessage.postValue("保存农户信息失败: " + e.getMessage());
                    if (onComplete != null) onComplete.run();
                }
            });
            return;
        }

        // 创建FarmerInfo对象（有真实身份证信息）
        FarmerInfo farmerInfo = FarmerInfo.createWithIdCard(
            farmerNameValue != null ? farmerNameValue : "",
            contractNumber.getValue() != null ? contractNumber.getValue() : "",
            currentIdCardNumber,
            currentGender,
            currentNationality,
            currentBirthDate,
            currentAddress,
            currentDepartment,
            currentStartDate,
            currentEndDate,
            currentPhoto
        );

        // 使用insertIfIdCardNotExists确保每个身份证号只存储一次
        farmerInfoRepository.insertIfIdCardNotExists(farmerInfo, new FarmerInfoRepository.OnResultListener<FarmerInfoRepository.InsertResult>() {
            @Override
            public void onSuccess(FarmerInfoRepository.InsertResult result) {
                if (result.isNewRecord()) {
                    statusMessage.postValue("新农户信息已保存: " + result.getMessage());
                } else {
                    statusMessage.postValue("农户信息已存在: " + result.getMessage());
                }
                
                // 更新首次称重时间（如果是第一次）
                if (result.isNewRecord()) {
                    farmerInfoRepository.updateFirstRecordTime(currentIdCardNumber, new FarmerInfoRepository.OnResultListener<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            // 首次称重时间更新成功
                        }

                        @Override
                        public void onError(Exception e) {
                            // 更新失败，但不影响主流程
                        }
                    });
                }
                
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onError(Exception e) {
                statusMessage.postValue("保存农户信息失败: " + e.getMessage());
                if (onComplete != null) onComplete.run(); // 即使失败也继续后续操作
            }
        });
    }

    /**
     * 从当前数据创建WeightRecord对象
     */
    private WeightRecord createWeightRecordFromCurrentData() {
        String farmer = farmerName.getValue();
        String contract = contractNumber.getValue();
        String level = selectedLevel.getValue();
        String weight = currentWeight.getValue();
        String precheckId = currentPrecheckId.getValue();

        if (farmer == null || farmer.equals("未读取") ||
                level == null || level.equals("未选择") ||
                weight == null) {
            return null;
        }

        WeightRecord record = new WeightRecord();
        record.setFarmerName(farmer);
        record.setIdCardNumber(currentIdCardNumber); // 使用当前身份证号
        record.setFarmerAddress(currentAddress); // 设置农户地址
        record.setFarmerGender(currentGender); // 设置农户性别
        record.setTobaccoPart(level);
        record.setTobaccoBundles(1); // 默认1捆

        // 解析重量（移除"kg"后缀）
        try {
            double weightValue = Double.parseDouble(weight.replace(" kg", ""));
            record.setWeight(weightValue);
        } catch (NumberFormatException e) {
            record.setWeight(5.0); // 默认5kg
        }

        record.setPreCheckNumber(precheckId);
        record.setOperatorName("操作员"); // 可以从系统获取
        record.setWarehouseNumber("WH001"); // 可以从设置获取
        record.setTotalAmount(0.0); // 需要计算
        record.setPurchasePrice(0.0); // 需要从等级获取价格

        // 设置时间
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());

        // 设置状态
        record.setStatus("已确认");
        record.setPrinted(false);
        record.setPrintCount(0);
        record.setExported(false);

        return record;
    }

    /**
     * 将WeighingRecord保存到数据库
     */
    private void saveRecordToDatabase(WeighingRecord weighingRecord) {
        // 首先保存农户信息（如果是新农户）
        saveFarmerInfoIfNew(() -> {
            // 然后保存称重记录
            WeightRecord dbRecord = convertToWeightRecord(weighingRecord);
            weightRecordRepository.insert(dbRecord, new WeightRecordRepository.OnResultListener<Long>() {
                @Override
                public void onSuccess(Long recordId) {
                    // 成功保存到数据库，可以记录日志或更新UI
                    System.out.println("数据库保存成功，记录ID: " + recordId);
                }

                @Override
                public void onError(Exception e) {
                    // 保存失败，可以记录日志或提示用户
                    System.err.println("数据库保存失败: " + e.getMessage());
                }
            });
        });
    }

    /**
     * 将WeighingRecord转换为WeightRecord
     */
    private WeightRecord convertToWeightRecord(WeighingRecord weighingRecord) {
        WeightRecord record = new WeightRecord();

        // 基本信息
        record.setRecordNumber(weighingRecord.getPrecheckId());
        record.setFarmerName(weighingRecord.getFarmerName());
        record.setIdCardNumber(currentIdCardNumber); // 使用当前身份证号
        record.setFarmerAddress(currentAddress); // 设置农户地址
        record.setFarmerGender(currentGender); // 设置农户性别
        record.setTobaccoPart(weighingRecord.getLeafType());
        record.setTobaccoBundles(1); // 默认1捆
        record.setWeight(weighingRecord.getWeight());
        record.setPreCheckNumber(weighingRecord.getPrecheckId());

        // 时间信息
        record.setCreateTime(weighingRecord.getTimestamp());
        record.setUpdateTime(weighingRecord.getTimestamp());

        // 操作信息
        record.setOperatorName("系统操作员");
        record.setWarehouseNumber("WH001");
        record.setStatus("已确认");

        // 默认值
        record.setTotalAmount(0.0);
        record.setPurchasePrice(0.0);
        record.setMoistureContent(0.0);
        record.setImpurityRate(0.0);
        record.setPrinted(false);
        record.setPrintCount(0);
        record.setExported(false);

        return record;
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
        String previousName = farmerName.getValue();
        farmerName.setValue(name);
        
        // 如果是手动输入且姓名发生变化，重置身份证相关信息
        // 这样每个不同的农户都会获得独立的临时身份证号
        if (name != null && !name.trim().isEmpty() && 
            !name.equals(previousName) && 
            (currentIdCardNumber == null || currentIdCardNumber.startsWith("TEMP_"))) {
            
            // 重置身份证相关信息，强制为新农户生成新的临时ID
            currentIdCardNumber = "";
            currentGender = "";
            currentNationality = "";
            currentBirthDate = "";
            currentAddress = "";
            currentDepartment = "";
            currentStartDate = "";
            currentEndDate = "";
            currentPhoto = null;
        }
        
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

        // 同时保存到数据库
        saveRecordToDatabase(record);

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
            // 创建新的FarmerStatistics，包含完整身份证信息
            statistics = FarmerStatistics.createWithFullInfo(
                record.getFarmerName(), 
                record.getContractNumber(), 
                currentIdCardNumber, 
                currentGender, 
                currentNationality, 
                currentBirthDate, 
                currentAddress, 
                currentDepartment, 
                currentStartDate, 
                currentEndDate, 
                currentPhoto
            );
            farmerStatisticsMap.put(farmerKey, statistics);
        } else {
            // 验证身份证号是否一致（防止数据不一致）
            if (!statistics.matchesIdCard(currentIdCardNumber)) {
                statusMessage.setValue("警告：身份证号不匹配！烟农: " + record.getFarmerName() + 
                                     " 已存在: " + statistics.getFarmerInfo().getMaskedIdCardNumber() + 
                                     " 当前: " + maskIdCard(currentIdCardNumber));
                return; // 不添加记录，防止数据混乱
            }
        }

        // 验证记录是否属于当前农户
        if (!statistics.isRecordValid(record)) {
            statusMessage.setValue("警告：称重记录与农户信息不匹配！");
            return;
        }

        // 只更新称重统计数据（身份证信息不可变）
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
     * 获取数据库Repository（供Fragment观察数据库状态使用）
     */
    public WeightRecordRepository getRepository() {
        return weightRecordRepository;
    }

    /**
     * 获取农户信息仓库
     */
    public FarmerInfoRepository getFarmerInfoRepository() {
        return farmerInfoRepository;
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
     * 清空当前身份证信息
     */
    public void clearIdCardData() {
        farmerName.setValue("未读取");
        currentIdCardNumber = "";
        currentGender = "";
        currentNationality = "";
        currentBirthDate = "";
        currentAddress = "";
        currentDepartment = "";
        currentStartDate = "";
        currentEndDate = "";
        currentPhoto = null;
        statusMessage.setValue("身份证信息已清空");
    }
    
    /**
     * 获取当前身份证信息（用于查询和验证）
     */
    public String getCurrentIdCardNumber() {
        return currentIdCardNumber;
    }
    
    public String getCurrentGender() {
        return currentGender;
    }
    
    public String getCurrentAddress() {
        return currentAddress;
    }
    
    public String getCurrentNationality() {
        return currentNationality;
    }
    
    public String getCurrentBirthDate() {
        return currentBirthDate;
    }
    
    public String getCurrentDepartment() {
        return currentDepartment;
    }
    
    public String getCurrentStartDate() {
        return currentStartDate;
    }
    
    public String getCurrentEndDate() {
        return currentEndDate;
    }
    
    public byte[] getCurrentPhoto() {
        return currentPhoto;
    }
    
    /**
     * 检查当前是否有有效的身份证信息
     */
    public boolean hasValidIdCardInfo() {
        return currentIdCardNumber != null && !currentIdCardNumber.trim().isEmpty() &&
               farmerName.getValue() != null && !farmerName.getValue().equals("未读取");
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

        // 重置身份证信息
        clearIdCardData();

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
    
    /**
     * 根据身份证号查找烟农统计信息（用于数据库链接）
     */
    public FarmerStatistics findFarmerByIdCard(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            return null;
        }
        
        for (FarmerStatistics stats : farmerStatisticsMap.values()) {
            if (stats.getFarmerInfo().matchesIdCard(idCardNumber)) {
                return stats;
            }
        }
        return null;
    }
    
    /**
     * 根据身份证号获取烟农信息（用于数据库查询）
     */
    public FarmerInfo getFarmerInfoByIdCard(String idCardNumber) {
        FarmerStatistics stats = findFarmerByIdCard(idCardNumber);
        return stats != null ? stats.getFarmerInfo() : null;
    }
    
    /**
     * 根据身份证号获取烟农姓名（用于数据库查询）
     */
    public String getFarmerNameByIdCard(String idCardNumber) {
        FarmerInfo farmerInfo = getFarmerInfoByIdCard(idCardNumber);
        return farmerInfo != null ? farmerInfo.getFarmerName() : null;
    }
    
    /**
     * 检查身份证号是否已存在（防重复）
     */
    public boolean isIdCardExists(String idCardNumber) {
        return findFarmerByIdCard(idCardNumber) != null;
    }
    
    /**
     * 验证烟农身份信息一致性
     */
    public boolean validateFarmerIdentity(String farmerName, String idCardNumber) {
        if (farmerName == null || idCardNumber == null) {
            return false;
        }
        
        // 检查是否已存在相同姓名的烟农
        FarmerStatistics existingByName = farmerStatisticsMap.get(farmerName);
        if (existingByName != null) {
            return existingByName.getFarmerInfo().matchesIdCard(idCardNumber);
        }
        
        // 检查是否已存在相同身份证的烟农
        FarmerStatistics existingById = findFarmerByIdCard(idCardNumber);
        if (existingById != null) {
            return existingById.getFarmerInfo().matchesName(farmerName);
        }
        
        // 都不存在，可以创建新的
        return true;
    }
    
    /**
     * 获取烟农身份验证结果
     */
    public String getFarmerValidationMessage(String farmerName, String idCardNumber) {
        if (farmerName == null || idCardNumber == null || farmerName.trim().isEmpty() || idCardNumber.trim().isEmpty()) {
            return "姓名或身份证号不能为空";
        }
        
        FarmerStatistics existingByName = farmerStatisticsMap.get(farmerName);
        FarmerStatistics existingById = findFarmerByIdCard(idCardNumber);
        
        if (existingByName != null && existingById != null) {
            if (existingByName == existingById) {
                return "验证成功：烟农信息一致";
            } else {
                return "错误：姓名和身份证号分属不同烟农";
            }
        } else if (existingByName != null) {
            return "错误：烟农 " + farmerName + " 已存在，但身份证号不匹配 (现有: " + 
                   existingByName.getFarmerInfo().getMaskedIdCardNumber() + ")";
        } else if (existingById != null) {
            return "错误：身份证号已存在，但姓名为 " + existingById.getFarmerInfo().getFarmerName();
        } else {
            return "验证成功：新烟农信息";
        }
    }

    /**
     * 准备打印数据（从ViewModel状态获取）
     */
    public PrintData preparePrintData(String farmerNameFromUI) {
        // 获取当前数据（允许空白字段）
        String farmerName = farmerNameFromUI != null ? farmerNameFromUI.trim() : "";
        String selectedLevel = this.selectedLevel.getValue();
        String precheckId = this.currentPrecheckId.getValue();
        String contractNumber = this.contractNumber.getValue();

        // 处理空值，用默认值替换
        if (farmerName.isEmpty()) {
            farmerName = "未填写";
        }
        if (selectedLevel == null || selectedLevel.equals("未选择")) {
            selectedLevel = "未选择";
        }
        if (precheckId == null || precheckId.equals("未生成")) {
            precheckId = "未生成";
        }
        if (contractNumber == null || contractNumber.equals("未设置")) {
            contractNumber = "未设置";
        }

        // 创建当前日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());

        return new PrintData(farmerName, selectedLevel, precheckId, currentDate, contractNumber);
    }

    /**
     * 验证打印数据是否有效
     */
    public boolean validatePrintData(PrintData printData) {
        // 在这个版本中我们允许所有数据，即使是默认值
        // 但可以在这里添加任何业务规则
        return printData != null;
    }

    /**
     * 通知打印事件
     */
    public void notifyPrintSuccess(PrintData printData) {
        printEvent.setValue(new PrintEvent(PrintEvent.Type.PRINT_SUCCESS, "打印成功", null, printData));
        printStatus.setValue("标签打印完成");
    }

    public void notifyPrintFailure(String errorType, String errorMessage, String errorDetails) {
        printEvent.setValue(new PrintEvent(PrintEvent.Type.PRINT_FAILURE, errorType + ": " + errorMessage, errorDetails, null));
        printStatus.setValue("打印失败: " + errorMessage);
    }

    public void notifyConnectionSuccess(String devicePath) {
        printEvent.setValue(new PrintEvent(PrintEvent.Type.CONNECTION_SUCCESS, "打印机连接成功", devicePath, null));
        printStatus.setValue("打印机已连接: " + devicePath);
    }

    public void notifyConnectionFailed(String error) {
        printEvent.setValue(new PrintEvent(PrintEvent.Type.CONNECTION_FAILED, "打印机连接失败", error, null));
        printStatus.setValue("连接失败: " + error);
    }

    public void notifyPrintStatusUpdate(String status) {
        printEvent.setValue(new PrintEvent(PrintEvent.Type.STATUS_UPDATE, status, null, null));
        printStatus.setValue(status);
    }

    /**
     * 重置打印事件（避免重复处理）
     */
    public void clearPrintEvent() {
        printEvent.setValue(null);
    }

    // Getters for print-related LiveData
    public LiveData<PrintEvent> getPrintEvent() {
        return printEvent;
    }

    public LiveData<String> getPrintStatus() {
        return printStatus;
    }

    public LiveData<Boolean> getIsTestMode() {
        return isTestMode;
    }

    /**
     * 切换测试模式
     */
    public void toggleTestMode() {
        boolean currentMode = Boolean.TRUE.equals(isTestMode.getValue());
        setTestMode(!currentMode);
    }

    /**
     * 设置测试模式
     */
    public void setTestMode(boolean testMode) {
        isTestMode.setValue(testMode);
        statusMessage.setValue("打印模式: " + (testMode ? "测试模式" : "真实模式"));
    }

    /**
     * 强制启用测试模式（开发用）
     */
    public void enableTestMode() {
        setTestMode(true);
    }

    /**
     * 强制启用真实模式（生产用）
     */
    public void enableRealMode() {
        setTestMode(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.dispose();
        hardwareSimulator.cleanup();
    }
}