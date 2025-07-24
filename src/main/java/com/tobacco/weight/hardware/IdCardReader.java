package com.tobacco.weight.hardware;

import com.tobacco.weight.data.FarmerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 身份证读卡器管理器
 * 负责身份证信息的读取和解析
 */
public class IdCardReader {

    private static final Logger logger = LoggerFactory.getLogger(IdCardReader.class);

    private boolean isConnected = false;
    private String deviceName;

    // 回调函数
    private Consumer<FarmerInfo> onIdCardRead;
    private Consumer<Boolean> onConnectionStatusChanged;

    /**
     * 构造函数
     */
    public IdCardReader() {
        initializeReader();
    }

    /**
     * 初始化读卡器
     */
    private void initializeReader() {
        try {
            // 模拟读卡器连接
            // 实际项目中需要根据具体的身份证读卡器SDK进行集成
            deviceName = "身份证读卡器";
            isConnected = true;

            logger.info("身份证读卡器初始化成功: {}", deviceName);

            // 通知连接状态变化
            if (onConnectionStatusChanged != null) {
                onConnectionStatusChanged.accept(true);
            }

        } catch (Exception e) {
            logger.error("初始化身份证读卡器失败", e);
        }
    }

    /**
     * 读取身份证信息
     */
    public void readIdCard() {
        try {
            logger.info("开始读取身份证信息...");

            // 模拟读取过程
            // 实际项目中需要调用身份证读卡器的SDK
            Thread.sleep(2000); // 模拟读取时间

            // 模拟身份证信息
            FarmerInfo farmerInfo = simulateIdCardData();

            if (farmerInfo != null) {
                logger.info("身份证读取成功: {}", farmerInfo.getFarmerName());

                // 通知读取结果
                if (onIdCardRead != null) {
                    onIdCardRead.accept(farmerInfo);
                }
            } else {
                logger.warn("身份证读取失败");

                // 通知读取失败
                if (onIdCardRead != null) {
                    onIdCardRead.accept(null);
                }
            }

        } catch (Exception e) {
            logger.error("读取身份证失败", e);

            // 通知读取失败
            if (onIdCardRead != null) {
                onIdCardRead.accept(null);
            }
        }
    }

    /**
     * 模拟身份证数据
     * 实际项目中需要解析真实的身份证数据
     */
    private FarmerInfo simulateIdCardData() {
        try {
            // 模拟身份证信息
            String farmerName = "张三";
            String contractNumber = "HT" + System.currentTimeMillis();
            String idCardNumber = "110101199001011234";
            String gender = "男";
            String nationality = "汉";
            String birthDate = "1990-01-01";
            String address = "北京市朝阳区";
            String department = "北京市公安局朝阳分局";
            String startDate = "2020-01-01";
            String endDate = "2030-01-01";
            byte[] photo = null; // 实际项目中需要读取照片数据

            return FarmerInfo.createWithIdCard(farmerName, contractNumber, idCardNumber,
                    gender, nationality, birthDate, address,
                    department, startDate, endDate, photo);

        } catch (Exception e) {
            logger.error("模拟身份证数据失败", e);
            return null;
        }
    }

    /**
     * 连接读卡器
     */
    public boolean connect() {
        try {
            isConnected = true;
            logger.info("身份证读卡器连接成功");

            // 通知连接状态变化
            if (onConnectionStatusChanged != null) {
                onConnectionStatusChanged.accept(true);
            }

            return true;

        } catch (Exception e) {
            logger.error("连接身份证读卡器失败", e);
            return false;
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        isConnected = false;
        logger.info("身份证读卡器连接已断开");

        // 通知连接状态变化
        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(false);
        }
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 获取设备名称
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * 设置身份证读取回调
     */
    public void setOnIdCardRead(Consumer<FarmerInfo> callback) {
        this.onIdCardRead = callback;
    }

    /**
     * 设置连接状态变化回调
     */
    public void setOnConnectionStatusChanged(Consumer<Boolean> callback) {
        this.onConnectionStatusChanged = callback;
    }

    /**
     * 获取设备信息
     */
    public String getDeviceInfo() {
        if (isConnected) {
            return String.format("设备: %s, 状态: %s",
                    deviceName, isConnected ? "已连接" : "未连接");
        }
        return "未连接";
    }

    /**
     * 测试读卡器
     */
    public boolean testReader() {
        try {
            logger.info("测试身份证读卡器...");

            // 模拟测试过程
            Thread.sleep(1000);

            logger.info("身份证读卡器测试成功");
            return true;

        } catch (Exception e) {
            logger.error("身份证读卡器测试失败", e);
            return false;
        }
    }
}