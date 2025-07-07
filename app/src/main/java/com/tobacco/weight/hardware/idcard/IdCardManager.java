package com.tobacco.weight.hardware.idcard;

import com.tobacco.weight.hardware.simulator.HardwareSimulator;
import javax.inject.Inject;

/**
 * 身份证读卡器管理器
 */
public class IdCardManager {
    
    private final HardwareSimulator simulator;
    
    @Inject
    public IdCardManager(HardwareSimulator simulator) {
        this.simulator = simulator;
    }
    
    public HardwareSimulator getSimulator() {
        return simulator;
    }
} 