package com.tobacco.weight.hardware.idcard;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import javax.inject.Inject;

/**
 * 身份证读卡器管理器
 * 使用硬件模拟器进行身份证读取功能
 */
public class IdCardManager {
    
    private final HardwareSimulator simulator;
    
    @Inject
    public IdCardManager(HardwareSimulator simulator) {
        this.simulator = simulator;
    }
    
    /**
     * 连接身份证读卡器
     */
    public void connect() {
        if (simulator != null) {
            simulator.connectIdCardReader();
        }
    }
    
    /**
     * 读取身份证
     */
    public void readCard() {
        if (simulator != null) {
            simulator.generateIdCardData();
        }
    }
    
    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return simulator != null && simulator.isIdCardReaderConnected();
    }
    
    /**
     * 获取身份证数据
     */
    public IdCardData getCurrentIdCardData() {
        if (simulator != null) {
            HardwareSimulator.IdCardData simData = simulator.getCurrentIdCardData();
            if (simData != null) {
                // 转换模拟器数据到我们的IdCardData格式
                IdCardData idCardData = new IdCardData();
                idCardData.setName(simData.name);
                idCardData.setIdNumber(simData.idNumber);
                idCardData.setAddress(simData.address);
                idCardData.setGender(simData.gender);
                return idCardData;
            }
        }
        return null;
    }
    
    /**
     * 获取硬件模拟器实例
     */
    public HardwareSimulator getSimulator() {
        return simulator;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        // 资源释放由模拟器处理
        if (simulator != null) {
            simulator.disconnectIdCardReader();
        }
    }
} 