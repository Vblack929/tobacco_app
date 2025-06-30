# 烟叶称重管理系统技术规格说明

## 1. 系统概述

### 1.1 应用场景
本系统专为烟叶收购站设计，运行于Android 13平板设备，通过多种硬件设备实现自动化称重、数据记录、标签打印的完整业务流程。

### 1.2 核心功能
- **实时称重**: 通过RS232串口连接电子秤，实时读取并解析重量数据
- **身份证识别**: USB连接身份证读卡器，自动获取农户身份信息
- **数据记录**: 本地SQLite数据库存储，支持离线操作和数据备份
- **标签打印**: 串口连接热敏打印机，生成70×70mm标准标签
- **历史查询**: 多维度搜索、统计分析和数据导出功能

## 2. 核心技术实现

### 2.1 串口通信架构

#### 2.1.1 底层通信实现

**SerialPortManager.java** - 串口管理器核心逻辑：

```java
// 串口设备路径配置
public static final String SCALE_SERIAL_PORT = "/dev/ttyUSB0";    // 电子秤
public static final String PRINTER_SERIAL_PORT = "/dev/ttyUSB1";  // 打印机

// 串口打开核心逻辑
public boolean openSerialPort(String path, int baudRate) {
    try {
        File device = new File(path);
        if (!device.exists() || !device.canRead() || !device.canWrite()) {
            return false;
        }
        
        // 调用native方法打开串口
        this.fileDescriptor = nativeOpen(path, baudRate, 0, 8, 1, 0);
        if (this.fileDescriptor == null) {
            return false;
        }
        
        this.inputStream = new FileInputStream(fileDescriptor);
        this.outputStream = new FileOutputStream(fileDescriptor);
        this.isOpen = true;
        
        return true;
    } catch (Exception e) {
        Log.e(TAG, "串口打开异常: " + e.getMessage(), e);
        return false;
    }
}

// 数据发送实现
public boolean sendData(byte[] data) {
    if (!isOpen || outputStream == null) {
        return false;
    }
    
    try {
        outputStream.write(data);
        outputStream.flush();
        return true;
    } catch (IOException e) {
        Log.e(TAG, "发送数据异常: " + e.getMessage(), e);
        return false;
    }
}

// 异步数据读取
public Observable<byte[]> startReading() {
    if (!isOpen || inputStream == null) {
        return Observable.empty();
    }
    
    isReading = true;
    readExecutor.execute(this::readDataLoop);
    return dataSubject;
}

private void readDataLoop() {
    byte[] buffer = new byte[1024];
    
    while (isReading && isOpen && inputStream != null) {
        try {
            int length = inputStream.read(buffer);
            if (length > 0) {
                byte[] data = new byte[length];
                System.arraycopy(buffer, 0, data, 0, length);
                dataSubject.onNext(data);
            }
        } catch (IOException e) {
            if (isReading) {
                dataSubject.onError(e);
            }
            break;
        }
    }
}
```

#### 2.1.2 Native层实现

**serialport.cpp** - JNI串口实现：

```cpp
#include <jni.h>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

JNIEXPORT jobject JNICALL
Java_com_tobacco_weight_hardware_serial_SerialPortManager_nativeOpen(
    JNIEnv *env, jobject thiz, jstring path, jint baudrate, jint flags, 
    jint databits, jint stopbits, jint parity) {
    
    const char *path_utf = env->GetStringUTFChars(path, 0);
    
    // 打开串口设备
    int fd = open(path_utf, O_RDWR | O_NOCTTY | O_NDELAY);
    if (fd == -1) {
        env->ReleaseStringUTFChars(path, path_utf);
        return NULL;
    }
    
    // 配置串口参数
    struct termios options;
    tcgetattr(fd, &options);
    
    // 设置波特率
    speed_t speed;
    switch (baudrate) {
        case 9600: speed = B9600; break;
        case 115200: speed = B115200; break;
        default: speed = B9600; break;
    }
    cfsetispeed(&options, speed);
    cfsetospeed(&options, speed);
    
    // 设置数据位、停止位、校验位
    options.c_cflag &= ~CSIZE;
    options.c_cflag |= CS8;  // 8数据位
    options.c_cflag &= ~PARENB; // 无校验
    options.c_cflag &= ~CSTOPB; // 1停止位
    
    tcsetattr(fd, TCSANOW, &options);
    
    env->ReleaseStringUTFChars(path, path_utf);
    
    // 返回FileDescriptor对象
    jclass cFileDescriptor = env->FindClass("java/io/FileDescriptor");
    jmethodID iFileDescriptor = env->GetMethodID(cFileDescriptor, "<init>", "()V");
    jobject fileDescriptor = env->NewObject(cFileDescriptor, iFileDescriptor);
    
    jfieldID descriptorID = env->GetFieldID(cFileDescriptor, "descriptor", "I");
    env->SetIntField(fileDescriptor, descriptorID, (jint)fd);
    
    return fileDescriptor;
}
```

### 2.2 电子秤数据解析

#### 2.2.1 多协议支持

**ScaleManager.java** - 电子秤数据解析逻辑：

```java
// 支持多种电子秤协议的正则表达式
private static final Pattern WEIGHT_PATTERN_1 = Pattern.compile("([+-]?\\d+\\.?\\d*)\\s*kg", Pattern.CASE_INSENSITIVE);
private static final Pattern WEIGHT_PATTERN_2 = Pattern.compile("ST,GS,([+-]?\\d+\\.?\\d*)");
private static final Pattern WEIGHT_PATTERN_3 = Pattern.compile("([+-]?\\d+\\.?\\d*)");

// 智能解析重量数据
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

// 重量稳定性检测算法
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
    
    // 判断是否稳定（变化范围小于阈值）
    isWeightStable = (max - min) <= STABLE_THRESHOLD;
}
```

#### 2.2.2 重量数据模型

**WeightData.java** - 重量数据封装：

```java
public class WeightData {
    private double weight;           // 重量值（kg）
    private String unit = "kg";      // 重量单位
    private boolean isValid = false; // 数据是否有效
    private boolean isStable = false;// 重量是否稳定
    private boolean isOverload = false;   // 是否超载
    private boolean isUnderload = false;  // 是否欠载
    private boolean isError = false;      // 是否有错误
    private String rawData;          // 原始数据
    private long timestamp;          // 时间戳
    
    // 检查重量是否可用于记录
    public boolean isRecordable() {
        return isValid && !isError && !isOverload && !isUnderload && isStable && weight > 0;
    }
    
    // 获取格式化的重量显示
    public String getFormattedWeight() {
        if (!isValid) return "---";
        if (isError) return "ERR";
        if (isOverload) return "OVER";
        if (isUnderload) return "UNDER";
        
        return String.format("%.3f %s", weight, unit);
    }
}
```

### 2.3 标签打印实现

#### 2.3.1 标签生成算法

**PrinterManager.java** - 70×70mm标签生成：

```java
// 标签尺寸常量 (70x70mm, 8点/mm分辨率)
private static final int LABEL_WIDTH_DOTS = 560;
private static final int LABEL_HEIGHT_DOTS = 560;

// 生成标签位图
private Bitmap generateLabelBitmap(WeightRecord record) {
    Bitmap bitmap = Bitmap.createBitmap(LABEL_WIDTH_DOTS, LABEL_HEIGHT_DOTS, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.WHITE);
    
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.setAntiAlias(true);
    
    // 标题
    paint.setTextSize(32);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    canvas.drawText("烟叶收购凭证", 160, 50, paint);
    
    // 分割线
    paint.setStrokeWidth(2);
    canvas.drawLine(20, 70, LABEL_WIDTH_DOTS - 20, 70, paint);
    
    // 基本信息布局
    paint.setTextSize(24);
    paint.setTypeface(Typeface.DEFAULT);
    
    int startY = 100;
    int lineHeight = 35;
    
    canvas.drawText("记录编号: " + record.getRecordNumber(), 20, startY, paint);
    canvas.drawText("农户姓名: " + record.getFarmerName(), 20, startY + lineHeight, paint);
    canvas.drawText("身份证号: " + formatIdCard(record.getIdCardNumber()), 20, startY + lineHeight * 2, paint);
    
    // 重量和金额（重点显示）
    paint.setTextSize(28);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    canvas.drawText("重量: " + String.format("%.3f", record.getWeight()) + " kg", 20, startY + lineHeight * 4 + 10, paint);
    canvas.drawText("金额: ¥" + String.format("%.2f", record.getTotalAmount()), 20, startY + lineHeight * 5 + 10, paint);
    
    // 二维码生成和绘制
    if (record.getQrCode() != null && !record.getQrCode().isEmpty()) {
        Bitmap qrBitmap = generateQRCode(record.getQrCode(), 120);
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, LABEL_WIDTH_DOTS - 140, startY + lineHeight * 4, paint);
        }
    }
    
    return bitmap;
}

// 二维码生成
private Bitmap generateQRCode(String content, int size) {
    try {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        
        return bitmap;
    } catch (WriterException e) {
        Log.e(TAG, "生成二维码失败: " + e.getMessage(), e);
        return null;
    }
}
```

#### 2.3.2 ESC/POS打印指令

```java
// 将位图转换为ESC/POS格式
private byte[] convertBitmapToEscPos(Bitmap bitmap) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    
    // 计算每行需要的字节数（8个像素一个字节）
    int bytesPerLine = (width + 7) / 8;
    
    // 创建数据缓冲区
    byte[] imageData = new byte[height * bytesPerLine + 8];
    int index = 0;
    
    // ESC * 命令头 (位图模式)
    imageData[index++] = 0x1B; // ESC
    imageData[index++] = 0x2A; // *
    imageData[index++] = 33;   // 24点双密度模式
    imageData[index++] = (byte) (bytesPerLine & 0xFF);
    imageData[index++] = (byte) ((bytesPerLine >> 8) & 0xFF);
    
    // 转换像素数据
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x += 8) {
            byte pixelByte = 0;
            for (int bit = 0; bit < 8 && (x + bit) < width; bit++) {
                int pixel = bitmap.getPixel(x + bit, y);
                // 判断是否为黑色像素（RGB阈值判断）
                if (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel) < 384) {
                    pixelByte |= (1 << (7 - bit));
                }
            }
            imageData[index++] = pixelByte;
        }
    }
    
    // 添加换行
    imageData[index++] = 0x0A;
    
    return imageData;
}
```

### 2.4 数据库设计

#### 2.4.1 Room数据库配置

**TobaccoDatabase.java**：

```java
@Database(
    entities = {WeightRecord.class},
    version = 1,
    exportSchema = true
)
public abstract class TobaccoDatabase extends RoomDatabase {
    
    public static TobaccoDatabase getInstance() {
        if (INSTANCE == null) {
            synchronized (TobaccoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            TobaccoApplication.getAppContext(),
                            TobaccoDatabase.class,
                            DATABASE_NAME
                    )
                    .addCallback(DATABASE_CALLBACK)
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
```

#### 2.4.2 复杂查询实现

**WeightRecordDao.java** - 高级查询示例：

```java
// 多条件搜索
@Query("SELECT * FROM weight_records WHERE " +
        "(:farmerName IS NULL OR farmer_name LIKE '%' || :farmerName || '%') AND " +
        "(:idCardNumber IS NULL OR id_card_number = :idCardNumber) AND " +
        "(:tobaccoPart IS NULL OR tobacco_part = :tobaccoPart) AND " +
        "(:startDate IS NULL OR create_time >= :startDate) AND " +
        "(:endDate IS NULL OR create_time <= :endDate) " +
        "ORDER BY create_time DESC")
LiveData<List<WeightRecord>> advancedSearch(String farmerName, String idCardNumber, 
                                           String tobaccoPart, Date startDate, Date endDate);

// 统计查询
@Query("SELECT COALESCE(SUM(weight), 0) FROM weight_records WHERE DATE(create_time/1000, 'unixepoch') = DATE('now')")
Single<Double> getTodayTotalWeight();

@Query("SELECT COALESCE(SUM(total_amount), 0) FROM weight_records WHERE DATE(create_time/1000, 'unixepoch') = DATE('now')")
Single<Double> getTodayTotalAmount();
```

### 2.5 MVVM架构实现

#### 2.5.1 ViewModel核心逻辑

**MainViewModel.java** - 业务逻辑处理：

```java
@HiltViewModel
public class MainViewModel extends ViewModel {
    
    // 保存称重记录的完整流程
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
}
```

#### 2.5.2 依赖注入配置

**HardwareModule.java** - Hilt模块：

```java
@Module
@InstallIn(SingletonComponent.class)
public class HardwareModule {
    
    @Provides
    @Singleton
    public ScaleManager provideScaleManager() {
        return new ScaleManager();
    }
    
    @Provides
    @Singleton
    public PrinterManager providePrinterManager() {
        return new PrinterManager();
    }
    
    @Provides
    @Singleton
    public IdCardManager provideIdCardManager() {
        return new IdCardManager();
    }
}
```

## 3. 性能优化

### 3.1 串口数据处理优化
- 使用RxJava实现异步数据流处理
- 缓冲区管理避免数据丢失
- 背压处理防止内存溢出

### 3.2 数据库性能优化
- 索引优化：在常用查询字段上建立索引
- 分页查询：大数据量场景下的分页加载
- 事务处理：批量操作使用事务提高性能

### 3.3 UI响应优化
- LiveData数据绑定减少UI更新开销
- RecyclerView优化：ViewHolder复用、DiffUtil增量更新
- 图片处理：位图压缩和内存回收

## 4. 错误处理和日志

### 4.1 全局异常处理
```java
private static class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        // 记录错误日志
        Log.e("GlobalException", "未捕获异常", ex);
        
        // 保存错误信息到文件
        saveErrorToFile(ex);
        
        // 重启应用或优雅退出
        System.exit(1);
    }
}
```

### 4.2 设备连接重试机制
```java
// 带重试的设备连接
private void connectWithRetry(int maxRetries) {
    Observable.fromCallable(() -> scaleManager.connect(portPath, baudRate))
        .retryWhen(errors -> errors
            .zipWith(Observable.range(1, maxRetries), (error, i) -> i)
            .flatMap(retryCount -> Observable.timer(retryCount * 1000, TimeUnit.MILLISECONDS))
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            success -> Log.i(TAG, "设备连接成功"),
            error -> Log.e(TAG, "设备连接失败", error)
        );
}
```

## 5. 安全考虑

### 5.1 数据加密
- 身份证号码脱敏显示
- 敏感数据本地加密存储
- 传输数据校验和验证

### 5.2 权限管理
- 串口设备访问权限检查
- 文件系统权限管理
- USB设备权限申请

## 6. 测试策略

### 6.1 单元测试
- 数据解析逻辑测试
- 业务逻辑验证
- 工具类功能测试

### 6.2 集成测试
- 数据库操作测试
- 硬件通信模拟测试
- 端到端业务流程测试

### 6.3 性能测试
- 大数据量下的响应时间
- 内存使用情况监控
- 电池消耗评估

## 7. 部署和维护

### 7.1 系统要求
- Android 13 (API 33) 或更高版本
- 4GB RAM，32GB存储空间
- 支持USB Host模式
- 串口转USB适配器支持

### 7.2 设备配置
- 电子秤：RS232接口，波特率9600
- 打印机：热敏打印机，支持ESC/POS指令
- 身份证读卡器：符合国标的USB接口设备

### 7.3 维护建议
- 定期数据备份和清理
- 硬件设备定期校准
- 系统日志监控和分析
- 定期软件更新和安全补丁

这份技术规格说明为开发团队提供了详细的实现指导，确保系统的稳定性、可靠性和可维护性。 