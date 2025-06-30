# 烟叶称重管理系统技术规格说明

## 1. 核心技术架构

### 1.1 串口通信实现

#### 电子秤数据读取流程：
```java
// 1. 打开串口连接
SerialPortManager scalePort = new SerialPortManager();
boolean connected = scalePort.openSerialPort("/dev/ttyUSB0", 9600);

// 2. 启动数据监听
Observable<byte[]> dataStream = scalePort.startReading()
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread());

// 3. 解析重量数据
disposables.add(dataStream.subscribe(rawData -> {
    String dataString = new String(rawData).trim();
    WeightData weight = parseWeightData(dataString);
    if (weight != null && weight.isValid()) {
        currentWeightSubject.onNext(weight);
    }
}));
```

#### 重量稳定性检测算法：
```java
// 连续5次读数变化小于0.01kg视为稳定
private static final int STABLE_CHECK_COUNT = 5;
private static final double STABLE_THRESHOLD = 0.01;

private void checkWeightStability(double weight) {
    recentWeights[weightIndex] = weight;
    weightIndex = (weightIndex + 1) % STABLE_CHECK_COUNT;
    
    // 计算重量变化范围
    double min = Collections.min(Arrays.asList(recentWeights));
    double max = Collections.max(Arrays.asList(recentWeights));
    
    isWeightStable = (max - min) <= STABLE_THRESHOLD;
}
```

### 1.2 标签打印实现

#### 70×70mm标签生成：
```java
private Bitmap generateLabelBitmap(WeightRecord record) {
    // 标签尺寸：70×70mm，8点/mm = 560×560像素
    Bitmap bitmap = Bitmap.createBitmap(560, 560, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    
    // 绘制标题
    Paint titlePaint = new Paint();
    titlePaint.setTextSize(32);
    titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
    canvas.drawText("烟叶收购凭证", 160, 50, titlePaint);
    
    // 绘制基本信息
    Paint textPaint = new Paint();
    textPaint.setTextSize(24);
    
    int y = 100;
    canvas.drawText("编号: " + record.getRecordNumber(), 20, y += 35, textPaint);
    canvas.drawText("农户: " + record.getFarmerName(), 20, y += 35, textPaint);
    canvas.drawText("重量: " + String.format("%.3f kg", record.getWeight()), 20, y += 35, textPaint);
    canvas.drawText("金额: ¥" + String.format("%.2f", record.getTotalAmount()), 20, y += 35, textPaint);
    
    // 生成二维码
    Bitmap qrCode = generateQRCode(record.getQrCode(), 120);
    if (qrCode != null) {
        canvas.drawBitmap(qrCode, 440, 300, textPaint);
    }
    
    return bitmap;
}
```

#### ESC/POS打印指令转换：
```java
private byte[] convertBitmapToEscPos(Bitmap bitmap) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int bytesPerLine = (width + 7) / 8;
    
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    
    // ESC * 命令头
    output.write(0x1B); // ESC
    output.write(0x2A); // *
    output.write(33);   // 24点双密度
    output.write(bytesPerLine & 0xFF);
    output.write((bytesPerLine >> 8) & 0xFF);
    
    // 转换像素数据为打印数据
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x += 8) {
            byte pixelByte = 0;
            for (int bit = 0; bit < 8 && (x + bit) < width; bit++) {
                int pixel = bitmap.getPixel(x + bit, y);
                int gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3;
                if (gray < 128) { // 黑色像素
                    pixelByte |= (1 << (7 - bit));
                }
            }
            output.write(pixelByte);
        }
    }
    
    return output.toByteArray();
}
```

### 1.3 数据库操作

#### Room数据库查询优化：
```java
// 分页查询大量记录
@Query("SELECT * FROM weight_records ORDER BY create_time DESC LIMIT :limit OFFSET :offset")
Single<List<WeightRecord>> getRecordsPaged(int limit, int offset);

// 多条件搜索
@Query("SELECT * FROM weight_records WHERE " +
        "(:name IS NULL OR farmer_name LIKE '%' || :name || '%') AND " +
        "(:startDate IS NULL OR create_time >= :startDate) AND " +
        "(:endDate IS NULL OR create_time <= :endDate) " +
        "ORDER BY create_time DESC")
LiveData<List<WeightRecord>> searchRecords(String name, Date startDate, Date endDate);

// 今日统计查询
@Query("SELECT COUNT(*), SUM(weight), SUM(total_amount) FROM weight_records " +
        "WHERE DATE(create_time/1000, 'unixepoch') = DATE('now')")
Single<DailyStatistics> getTodayStatistics();
```

### 1.4 MVVM架构实现

#### ViewModel业务逻辑：
```java
@HiltViewModel
public class MainViewModel extends ViewModel {
    
    // 保存称重记录
    public void saveWeightRecord() {
        WeightData currentWeight = this.currentWeight.getValue();
        if (!isDataValid(currentWeight)) {
            statusMessage.setValue("数据无效");
            return;
        }
        
        WeightRecord record = createRecord(currentWeight);
        
        // 异步保存到数据库
        disposables.add(
            weightRepository.insertRecord(record)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> isLoading.setValue(true))
                .doFinally(() -> isLoading.setValue(false))
                .subscribe(
                    recordId -> {
                        statusMessage.setValue("保存成功");
                        // 自动打印
                        if (printerConnected.getValue()) {
                            printLabel(record);
                        }
                        clearInputs();
                    },
                    error -> statusMessage.setValue("保存失败: " + error.getMessage())
                )
        );
    }
    
    private boolean isDataValid(WeightData weight) {
        return weight != null && 
               weight.isValid() && 
               weight.isStable() && 
               weight.getWeight() > 0 &&
               !TextUtils.isEmpty(farmerName.getValue());
    }
}
```

## 2. 硬件通信协议

### 2.1 电子秤通信协议

#### 支持的数据格式：
```
格式1: "ST,GS,+0012.345kg" (标准协议)
格式2: "+12.345kg" (简单格式)
格式3: "12345" (纯数字，需转换单位)
状态码: "OL"(超载), "UL"(欠载), "ERR"(错误)
```

#### 控制命令：
```java
// 电子秤控制命令
public void sendTareCommand() {
    serialPort.sendData("T\r\n".getBytes()); // 去皮重
}

public void sendZeroCommand() {
    serialPort.sendData("Z\r\n".getBytes()); // 清零
}

public void requestWeight() {
    serialPort.sendData("W\r\n".getBytes()); // 请求重量
}
```

### 2.2 热敏打印机协议

#### ESC/POS指令集：
```java
// 基本控制指令
private static final byte[] CMD_INIT = {0x1B, 0x40};           // 初始化
private static final byte[] CMD_CUT_PAPER = {0x1D, 0x56, 0x42, 0x00}; // 切纸
private static final byte[] CMD_FEED_LINES = {0x1B, 0x64, 0x03};       // 走纸

// 字体设置
private static final byte[] CMD_FONT_NORMAL = {0x1B, 0x21, 0x00};      // 正常字体
private static final byte[] CMD_FONT_BOLD = {0x1B, 0x21, 0x08};        // 粗体
private static final byte[] CMD_FONT_LARGE = {0x1B, 0x21, 0x30};       // 大字体

// 对齐方式
private static final byte[] CMD_ALIGN_LEFT = {0x1B, 0x61, 0x00};       // 左对齐
private static final byte[] CMD_ALIGN_CENTER = {0x1B, 0x61, 0x01};     // 居中
private static final byte[] CMD_ALIGN_RIGHT = {0x1B, 0x61, 0x02};      // 右对齐
```

### 2.3 身份证读卡器

#### USB通信实现：
```java
public class IdCardManager {
    private UsbManager usbManager;
    private UsbDevice idCardDevice;
    private UsbDeviceConnection connection;
    
    public boolean connect() {
        // 查找身份证读卡器设备
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (isIdCardReader(device)) {
                idCardDevice = device;
                break;
            }
        }
        
        if (idCardDevice != null) {
            connection = usbManager.openDevice(idCardDevice);
            return connection != null;
        }
        
        return false;
    }
    
    private boolean isIdCardReader(UsbDevice device) {
        // 根据厂商ID和产品ID判断是否为身份证读卡器
        return device.getVendorId() == ID_CARD_VENDOR_ID && 
               device.getProductId() == ID_CARD_PRODUCT_ID;
    }
}
```

## 3. 数据导出功能

### 3.1 Excel导出实现：
```java
public class ExcelExporter {
    
    public File exportToExcel(List<WeightRecord> records, String fileName) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("称重记录");
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"记录编号", "农户姓名", "身份证号", "重量(kg)", 
                              "单价(元/kg)", "总金额(元)", "创建时间"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            // 填充数据
            for (int i = 0; i < records.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                WeightRecord record = records.get(i);
                
                dataRow.createCell(0).setCellValue(record.getRecordNumber());
                dataRow.createCell(1).setCellValue(record.getFarmerName());
                dataRow.createCell(2).setCellValue(record.getIdCardNumber());
                dataRow.createCell(3).setCellValue(record.getWeight());
                dataRow.createCell(4).setCellValue(record.getPurchasePrice());
                dataRow.createCell(5).setCellValue(record.getTotalAmount());
                dataRow.createCell(6).setCellValue(record.getCreateTime().toString());
            }
            
            // 保存文件
            File exportDir = new File(Environment.getExternalStorageDirectory(), "TobaccoExport");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            
            File excelFile = new File(exportDir, fileName + ".xlsx");
            FileOutputStream outputStream = new FileOutputStream(excelFile);
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
            
            return excelFile;
            
        } catch (IOException e) {
            Log.e("ExcelExporter", "导出Excel失败", e);
            return null;
        }
    }
}
```

## 4. 性能优化策略

### 4.1 内存管理：
```java
// 图片内存优化
private Bitmap loadOptimizedBitmap(String imagePath, int targetWidth, int targetHeight) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(imagePath, options);
    
    options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);
    options.inJustDecodeBounds = false;
    
    return BitmapFactory.decodeFile(imagePath, options);
}

// 数据库查询优化
@Query("SELECT * FROM weight_records WHERE create_time BETWEEN :start AND :end " +
        "ORDER BY create_time DESC LIMIT 100")
LiveData<List<WeightRecord>> getRecentRecords(long start, long end);
```

### 4.2 异步处理：
```java
// 使用RxJava处理复杂异步操作
public Observable<String> processWeightRecord(WeightRecord record) {
    return Observable.fromCallable(() -> {
        // 1. 保存记录
        long recordId = weightRepository.insertRecord(record).blockingGet();
        
        // 2. 生成二维码
        record.generateQrCode();
        
        // 3. 更新记录
        weightRepository.updateRecord(record).blockingAwait();
        
        return "处理完成: " + record.getRecordNumber();
    })
    .subscribeOn(Schedulers.computation())
    .observeOn(AndroidSchedulers.mainThread());
}
```

## 5. 错误处理和日志

### 5.1 全局异常处理：
```java
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 保存错误日志
        saveErrorLog(ex);
        
        // 发送错误报告
        sendErrorReport(ex);
        
        // 重启应用
        restartApplication();
    }
    
    private void saveErrorLog(Throwable ex) {
        try {
            File logDir = new File(context.getExternalFilesDir(null), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            File logFile = new File(logDir, "crash_" + System.currentTimeMillis() + ".log");
            FileWriter writer = new FileWriter(logFile);
            
            writer.write("Time: " + new Date().toString() + "\n");
            writer.write("Exception: " + ex.toString() + "\n");
            
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            writer.write("Stack Trace: " + sw.toString());
            
            writer.close();
        } catch (IOException e) {
            Log.e("CrashHandler", "保存错误日志失败", e);
        }
    }
}
```

### 5.2 设备连接重试：
```java
private void connectWithRetry(String devicePath, int maxRetries) {
    Observable.fromCallable(() -> connectDevice(devicePath))
        .retryWhen(errors -> 
            errors.zipWith(Observable.range(1, maxRetries + 1), (error, i) -> {
                if (i > maxRetries) {
                    throw new RuntimeException("连接失败", error);
                }
                return i;
            })
            .flatMap(retryCount -> {
                Log.i(TAG, "重试连接第" + retryCount + "次");
                return Observable.timer(retryCount * 1000, TimeUnit.MILLISECONDS);
            })
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            success -> Log.i(TAG, "设备连接成功"),
            error -> Log.e(TAG, "设备连接最终失败", error)
        );
}
```

这份技术规格说明详细描述了系统的核心实现逻辑，为开发团队提供了完整的技术参考。 