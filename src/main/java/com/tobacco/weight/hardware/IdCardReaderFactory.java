package com.tobacco.weight.hardware;

import com.tobacco.weight.test.MockIdCardReaderServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 身份证读卡器工厂类
 * 提供真实设备和模拟设备的统一创建接口
 */
public class IdCardReaderFactory {

    private static final Logger logger = LoggerFactory.getLogger(IdCardReaderFactory.class);
    
    // 模拟服务器实例
    private static MockIdCardReaderServer mockServer;
    private static boolean mockMode = false;

    /**
     * 创建身份证读卡器实例
     * @param useMockDevice 是否使用模拟设备
     * @return IdCardReader实例
     */
    public static IdCardReader createIdCardReader(boolean useMockDevice) {
        if (useMockDevice) {
            return createMockIdCardReader();
        } else {
            return createRealIdCardReader();
        }
    }

    /**
     * 创建真实设备的读卡器
     */
    private static IdCardReader createRealIdCardReader() {
        logger.info("创建真实身份证读卡器实例");
        IdCardReader reader = new IdCardReader();
        reader.setEnableSignatureVerification(true); // 真实设备启用签名验证
        return reader;
    }

    /**
     * 创建模拟设备的读卡器
     */
    private static IdCardReader createMockIdCardReader() {
        logger.info("创建模拟身份证读卡器实例");
        
        // 启动模拟服务器
        if (mockServer == null || !mockServer.isRunning()) {
            try {
                mockServer = new MockIdCardReaderServer();
                mockServer.start();
                mockMode = true;
                logger.info("模拟服务器已启动");
                
                // 添加关闭钩子
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (mockServer != null && mockServer.isRunning()) {
                        logger.info("应用关闭，停止模拟服务器...");
                        mockServer.stop();
                    }
                }));
                
            } catch (Exception e) {
                logger.error("启动模拟服务器失败", e);
                throw new RuntimeException("无法启动模拟服务器", e);
            }
        }
        
        IdCardReader reader = new IdCardReader();
        reader.setEnableSignatureVerification(false); // 模拟设备禁用签名验证
        return reader;
    }

    /**
     * 停止模拟服务器
     */
    public static void stopMockServer() {
        if (mockServer != null && mockServer.isRunning()) {
            mockServer.stop();
            mockMode = false;
            logger.info("模拟服务器已停止");
        }
    }

    /**
     * 检查是否在模拟模式
     */
    public static boolean isMockMode() {
        return mockMode;
    }

    /**
     * 根据系统属性自动选择设备类型
     * 系统属性: tobacco.idcard.mock=true 使用模拟设备
     */
    public static IdCardReader createAutoIdCardReader() {
        boolean useMock = Boolean.parseBoolean(System.getProperty("tobacco.idcard.mock", "false"));
        
        if (useMock) {
            logger.info("根据系统属性 tobacco.idcard.mock=true，使用模拟设备");
        } else {
            logger.info("根据系统属性 tobacco.idcard.mock=false，使用真实设备");
        }
        
        return createIdCardReader(useMock);
    }

    /**
     * 检测真实设备是否可用
     * 如果不可用则自动切换到模拟模式
     */
    public static IdCardReader createSmartIdCardReader() {
        // 首先尝试真实设备
        IdCardReader realReader = createRealIdCardReader();
        
        try {
            // 快速测试连接
            boolean connected = realReader.connect();
            if (connected) {
                logger.info("检测到真实身份证读卡器，使用真实设备");
                return realReader;
            }
        } catch (Exception e) {
            logger.debug("真实设备连接失败: {}", e.getMessage());
        } finally {
            realReader.disconnect();
        }
        
        // 真实设备不可用，使用模拟设备
        logger.info("真实设备不可用，自动切换到模拟模式");
        return createMockIdCardReader();
    }
}
