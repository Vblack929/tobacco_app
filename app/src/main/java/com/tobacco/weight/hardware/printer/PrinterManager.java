package com.tobacco.weight.hardware.printer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tobacco.weight.data.model.WeightRecord;
import com.tobacco.weight.hardware.serial.SerialPortManager;
import com.tobacco.weight.hardware.simulator.HardwareSimulator;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import javax.inject.Inject;

/**
 * 打印机管理器
 * 负责热敏打印机的控制和标签打印
 */
public class PrinterManager {
    
    private static final String TAG = "PrinterManager";
    
    // 标签尺寸常量 (70x70mm)
    private static final int LABEL_WIDTH_MM = 70;
    private static final int LABEL_HEIGHT_MM = 70;
    private static final int LABEL_WIDTH_DOTS = 560;  // 8点/mm分辨率
    private static final int LABEL_HEIGHT_DOTS = 560;
    
    // ESC/POS命令
    private static final byte[] ESC = {0x1B};
    private static final byte[] GS = {0x1D};
    private static final byte[] LF = {0x0A};
    private static final byte[] CR = {0x0D};
    private static final byte[] CRLF = {0x0D, 0x0A};
    
    // 打印机控制命令
    private static final byte[] CMD_INIT = {0x1B, 0x40};                    // 初始化
    private static final byte[] CMD_CUT_PAPER = {0x1D, 0x56, 0x42, 0x00};  // 切纸
    private static final byte[] CMD_FEED_LINES = {0x1B, 0x64, 0x03};       // 走纸3行
    
    private SerialPortManager serialPortManager;
    private PublishSubject<PrintResult> printResultSubject;
    private boolean isConnected = false;
    
    // 打印模板配置
    private PrintTemplate currentTemplate;
    
    private final HardwareSimulator simulator;
    
    @Inject
    public PrinterManager(HardwareSimulator simulator) {
        this.simulator = simulator;
        this.serialPortManager = new SerialPortManager();
        this.printResultSubject = PublishSubject.create();
        this.currentTemplate = createDefaultTemplate();
    }
    
    /**
     * 连接打印机
     * @param portPath 串口路径
     * @param baudRate 波特率
     * @return 是否连接成功
     */
    public boolean connect(String portPath, int baudRate) {
        try {
            if (serialPortManager.isOpen()) {
                disconnect();
            }
            
            boolean success = serialPortManager.openSerialPort(portPath, baudRate);
            if (success) {
                isConnected = true;
                initializePrinter();
                Log.i(TAG, "打印机连接成功");
            } else {
                isConnected = false;
                Log.e(TAG, "打印机连接失败");
            }
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "连接打印机异常: " + e.getMessage(), e);
            isConnected = false;
            return false;
        }
    }
    
    /**
     * 断开打印机连接
     */
    public void disconnect() {
        try {
            serialPortManager.closeSerialPort();
            isConnected = false;
            Log.i(TAG, "打印机连接已断开");
            
        } catch (Exception e) {
            Log.e(TAG, "断开打印机连接异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化打印机
     */
    private void initializePrinter() {
        try {
            // 发送初始化命令
            serialPortManager.sendData(CMD_INIT);
            Thread.sleep(100);
            
            // 设置页面大小 (70x70mm)
            setPageSize(LABEL_WIDTH_MM, LABEL_HEIGHT_MM);
            
            Log.i(TAG, "打印机初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "初始化打印机异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 设置页面大小
     */
    private void setPageSize(int widthMm, int heightMm) {
        // 这里根据具体打印机型号发送相应的页面设置命令
        // 例如：ESC "Q" n1 n2 (设置页面宽度)
        byte[] cmd = {0x1B, 0x51, (byte) widthMm, (byte) heightMm};
        serialPortManager.sendData(cmd);
    }
    
    /**
     * 打印称重记录标签
     * @param record 称重记录
     * @return 打印结果Observable
     */
    public Observable<PrintResult> printWeightLabel(WeightRecord record) {
        return Observable.fromCallable(() -> {
            try {
                if (!isConnected) {
                    throw new Exception("打印机未连接");
                }
                
                Log.i(TAG, "开始打印标签: " + record.getRecordNumber());
                
                // 生成标签内容
                Bitmap labelBitmap = generateLabelBitmap(record);
                
                // 发送位图数据到打印机
                sendBitmapToPrinter(labelBitmap);
                
                // 切纸
                serialPortManager.sendData(CMD_CUT_PAPER);
                
                // 走纸
                serialPortManager.sendData(CMD_FEED_LINES);
                
                Log.i(TAG, "标签打印完成: " + record.getRecordNumber());
                
                return new PrintResult(true, "打印成功", record.getRecordNumber());
                
            } catch (Exception e) {
                Log.e(TAG, "打印标签异常: " + e.getMessage(), e);
                return new PrintResult(false, "打印失败: " + e.getMessage(), record.getRecordNumber());
            }
        });
    }
    
    /**
     * 生成标签位图
     * @param record 称重记录
     * @return 标签位图
     */
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
        
        // 基本信息
        paint.setTextSize(24);
        paint.setTypeface(Typeface.DEFAULT);
        
        int startY = 100;
        int lineHeight = 35;
        
        canvas.drawText("记录编号: " + record.getRecordNumber(), 20, startY, paint);
        canvas.drawText("农户姓名: " + record.getFarmerName(), 20, startY + lineHeight, paint);
        canvas.drawText("性别: " + (record.getFarmerGender() != null ? record.getFarmerGender() : "未填"), 300, startY + lineHeight, paint);
        canvas.drawText("身份证号: " + formatIdCard(record.getIdCardNumber()), 20, startY + lineHeight * 2, paint);
        canvas.drawText("地址: " + (record.getFarmerAddress() != null ? record.getFarmerAddress() : "未填"), 20, startY + lineHeight * 3, paint);
        canvas.drawText("烟叶部位: " + record.getTobaccoPart(), 20, startY + lineHeight * 4, paint);
        canvas.drawText("捆数: " + record.getTobaccoBundles() + "捆", 300, startY + lineHeight * 4, paint);
        
        // 重量和金额（重点显示）
        paint.setTextSize(28);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        canvas.drawText("重量: " + String.format("%.3f", record.getWeight()) + " kg", 20, startY + lineHeight * 5 + 10, paint);
        canvas.drawText("金额: ¥" + String.format("%.2f", record.getTotalAmount()), 20, startY + lineHeight * 6 + 10, paint);
        
        // 时间
        paint.setTextSize(20);
        paint.setTypeface(Typeface.DEFAULT);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        canvas.drawText("时间: " + sdf.format(record.getCreateTime()), 20, startY + lineHeight * 6 + 20, paint);
        
        // 生成二维码
        if (record.getQrCode() != null && !record.getQrCode().isEmpty()) {
            Bitmap qrBitmap = generateQRCode(record.getQrCode(), 120);
            if (qrBitmap != null) {
                canvas.drawBitmap(qrBitmap, LABEL_WIDTH_DOTS - 140, startY + lineHeight * 4, paint);
            }
        }
        
        // 底部信息
        paint.setTextSize(18);
        canvas.drawText("操作员: " + record.getOperatorName(), 20, LABEL_HEIGHT_DOTS - 60, paint);
        canvas.drawText("仓库: " + record.getWarehouseNumber(), 20, LABEL_HEIGHT_DOTS - 35, paint);
        canvas.drawText("预检编号: " + record.getPreCheckNumber(), 20, LABEL_HEIGHT_DOTS - 10, paint);
        
        return bitmap;
    }
    
    /**
     * 生成二维码
     * @param content 二维码内容
     * @param size 二维码大小
     * @return 二维码位图
     */
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
    
    /**
     * 发送位图数据到打印机
     * @param bitmap 位图
     */
    private void sendBitmapToPrinter(Bitmap bitmap) throws Exception {
        // 将位图转换为打印机可识别的数据格式
        byte[] imageData = convertBitmapToEscPos(bitmap);
        
        // 发送图像打印命令
        serialPortManager.sendData(imageData);
    }
    
    /**
     * 将位图转换为ESC/POS格式
     * @param bitmap 位图
     * @return ESC/POS数据
     */
    private byte[] convertBitmapToEscPos(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // 计算每行需要的字节数（8个像素一个字节）
        int bytesPerLine = (width + 7) / 8;
        
        // 创建数据缓冲区
        byte[] imageData = new byte[height * bytesPerLine + 8];
        int index = 0;
        
        // ESC * 命令头 (位图模式)
        imageData[index++] = 0x1B;
        imageData[index++] = 0x2A;
        imageData[index++] = 33;  // 24点双密度模式
        imageData[index++] = (byte) (bytesPerLine & 0xFF);
        imageData[index++] = (byte) ((bytesPerLine >> 8) & 0xFF);
        
        // 转换像素数据
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x += 8) {
                byte pixelByte = 0;
                for (int bit = 0; bit < 8 && (x + bit) < width; bit++) {
                    int pixel = bitmap.getPixel(x + bit, y);
                    // 判断是否为黑色像素
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
    
    /**
     * 格式化身份证号（隐藏中间部分）
     */
    private String formatIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }
    
    /**
     * 创建默认打印模板
     */
    private PrintTemplate createDefaultTemplate() {
        PrintTemplate template = new PrintTemplate();
        template.setName("默认模板");
        template.setWidth(LABEL_WIDTH_MM);
        template.setHeight(LABEL_HEIGHT_MM);
        template.setShowQRCode(true);
        template.setShowLogo(false);
        return template;
    }
    
    /**
     * 设置打印模板
     */
    public void setPrintTemplate(PrintTemplate template) {
        this.currentTemplate = template;
    }
    
    /**
     * 获取当前打印模板
     */
    public PrintTemplate getCurrentTemplate() {
        return currentTemplate;
    }
    
    /**
     * 测试打印
     */
    public Observable<PrintResult> testPrint() {
        return Observable.fromCallable(() -> {
            try {
                if (!isConnected) {
                    throw new Exception("打印机未连接");
                }
                
                // 发送测试页面
                String testContent = "打印机测试页面\n\n";
                testContent += "时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(System.currentTimeMillis()) + "\n";
                testContent += "状态: 正常\n";
                testContent += "分辨率: 8点/mm\n";
                testContent += "标签尺寸: 70x70mm\n\n";
                testContent += "测试完成\n";
                
                serialPortManager.sendData(testContent.getBytes());
                serialPortManager.sendData(CMD_CUT_PAPER);
                serialPortManager.sendData(CMD_FEED_LINES);
                
                return new PrintResult(true, "测试打印成功", "TEST");
                
            } catch (Exception e) {
                Log.e(TAG, "测试打印异常: " + e.getMessage(), e);
                return new PrintResult(false, "测试打印失败: " + e.getMessage(), "TEST");
            }
        });
    }
    
    /**
     * 获取打印结果Observable
     */
    public Observable<PrintResult> getPrintResultObservable() {
        return printResultSubject;
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return isConnected && serialPortManager.isOpen();
    }
    
    /**
     * 释放资源
     */
    public void release() {
        disconnect();
        serialPortManager.release();
        
        if (printResultSubject != null) {
            printResultSubject.onComplete();
        }
    }
    
    /**
     * 打印结果类
     */
    public static class PrintResult {
        private boolean success;
        private String message;
        private String recordNumber;
        private long timestamp;
        
        public PrintResult(boolean success, String message, String recordNumber) {
            this.success = success;
            this.message = message;
            this.recordNumber = recordNumber;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getter 方法
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getRecordNumber() { return recordNumber; }
        public long getTimestamp() { return timestamp; }
    }
    
    public HardwareSimulator getSimulator() {
        return simulator;
    }
} 