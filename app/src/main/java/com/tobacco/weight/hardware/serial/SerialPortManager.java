package com.tobacco.weight.hardware.serial;

import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * 串口管理器
 * 负责串口设备的打开、关闭、读写操作
 */
public class SerialPortManager {
    
    private static final String TAG = "SerialPortManager";
    
    // 串口设备路径
    public static final String SCALE_SERIAL_PORT = "/dev/ttyUSB0";    // 电子秤串口
    public static final String PRINTER_SERIAL_PORT = "/dev/ttyUSB1";  // 打印机串口
    
    // 常用波特率
    public static final int BAUD_RATE_9600 = 9600;
    public static final int BAUD_RATE_115200 = 115200;
    
    private FileDescriptor fileDescriptor;
    private FileInputStream inputStream;
    private FileOutputStream outputStream;
    private boolean isOpen = false;
    private String portPath;
    private int baudRate;
    
    private ExecutorService readExecutor;
    private PublishSubject<byte[]> dataSubject;
    private volatile boolean isReading = false;
    
    /**
     * 构造函数
     */
    public SerialPortManager() {
        this.readExecutor = Executors.newSingleThreadExecutor();
        this.dataSubject = PublishSubject.create();
    }
    
    /**
     * 打开串口
     * @param path 串口设备路径
     * @param baudRate 波特率
     * @return 是否成功打开
     */
    public boolean openSerialPort(String path, int baudRate) {
        try {
            File device = new File(path);
            if (!device.exists()) {
                Log.e(TAG, "串口设备不存在: " + path);
                return false;
            }
            
            if (!device.canRead() || !device.canWrite()) {
                Log.e(TAG, "串口设备权限不足: " + path);
                return false;
            }
            
            // 调用native方法打开串口
            this.fileDescriptor = nativeOpen(path, baudRate, 0, 8, 1, 0);
            if (this.fileDescriptor == null) {
                Log.e(TAG, "串口打开失败: " + path);
                return false;
            }
            
            this.inputStream = new FileInputStream(fileDescriptor);
            this.outputStream = new FileOutputStream(fileDescriptor);
            this.portPath = path;
            this.baudRate = baudRate;
            this.isOpen = true;
            
            Log.i(TAG, "串口打开成功: " + path + ", 波特率: " + baudRate);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "串口打开异常: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        if (!isOpen) {
            return;
        }
        
        try {
            stopReading();
            
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            
            if (fileDescriptor != null) {
                nativeClose();
                fileDescriptor = null;
            }
            
            isOpen = false;
            Log.i(TAG, "串口关闭成功: " + portPath);
            
        } catch (IOException e) {
            Log.e(TAG, "串口关闭异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送数据
     * @param data 要发送的数据
     * @return 是否发送成功
     */
    public boolean sendData(byte[] data) {
        if (!isOpen || outputStream == null) {
            Log.e(TAG, "串口未打开，无法发送数据");
            return false;
        }
        
        try {
            outputStream.write(data);
            outputStream.flush();
            Log.d(TAG, "发送数据成功: " + bytesToHex(data));
            return true;
        } catch (IOException e) {
            Log.e(TAG, "发送数据异常: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送字符串数据
     * @param data 要发送的字符串
     * @return 是否发送成功
     */
    public boolean sendData(String data) {
        return sendData(data.getBytes());
    }
    
    /**
     * 开始读取数据
     * @return 数据流的Observable
     */
    public Observable<byte[]> startReading() {
        if (!isOpen || inputStream == null) {
            Log.e(TAG, "串口未打开，无法读取数据");
            return Observable.empty();
        }
        
        if (isReading) {
            Log.w(TAG, "已在读取数据中");
            return dataSubject;
        }
        
        isReading = true;
        readExecutor.execute(this::readDataLoop);
        Log.i(TAG, "开始读取串口数据");
        
        return dataSubject;
    }
    
    /**
     * 停止读取数据
     */
    public void stopReading() {
        isReading = false;
        Log.i(TAG, "停止读取串口数据");
    }
    
    /**
     * 数据读取循环
     */
    private void readDataLoop() {
        byte[] buffer = new byte[1024];
        
        while (isReading && isOpen && inputStream != null) {
            try {
                int length = inputStream.read(buffer);
                if (length > 0) {
                    byte[] data = new byte[length];
                    System.arraycopy(buffer, 0, data, 0, length);
                    dataSubject.onNext(data);
                    Log.d(TAG, "接收数据: " + bytesToHex(data));
                }
            } catch (IOException e) {
                if (isReading) {
                    Log.e(TAG, "读取数据异常: " + e.getMessage(), e);
                    dataSubject.onError(e);
                }
                break;
            }
        }
    }
    
    /**
     * 检查串口是否打开
     */
    public boolean isOpen() {
        return isOpen;
    }
    
    /**
     * 获取串口路径
     */
    public String getPortPath() {
        return portPath;
    }
    
    /**
     * 获取波特率
     */
    public int getBaudRate() {
        return baudRate;
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X ", b));
        }
        return result.toString().trim();
    }
    
    /**
     * 释放资源
     */
    public void release() {
        closeSerialPort();
        
        if (readExecutor != null && !readExecutor.isShutdown()) {
            readExecutor.shutdown();
        }
        
        if (dataSubject != null) {
            dataSubject.onComplete();
        }
    }
    
    // Native方法声明
    private native FileDescriptor nativeOpen(String path, int baudrate, int flags, int databits, int stopbits, int parity);
    private native void nativeClose();
    
    // 加载串口库
    static {
        System.loadLibrary("serialport");
    }
} 