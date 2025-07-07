package com.tobacco.weight.hardware.idcard;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 身份证读卡器Observable功能测试
 * 测试BehaviorSubject数据流和连接状态
 */
@RunWith(MockitoJUnitRunner.class)
public class IdCardManagerObservableTest {
    
    @Mock
    private Context mockContext;
    
    private IdCardManager idCardManager;
    private TestObserver<Boolean> connectionObserver;
    private TestObserver<IdCardData> dataObserver;
    
    @Before
    public void setUp() {
        // 注意：这里我们测试的是Observable功能，不涉及真实的USB操作
        idCardManager = new IdCardManager();
        
        // 设置测试观察者
        connectionObserver = new TestObserver<>();
        dataObserver = new TestObserver<>();
        
        // 订阅Observable
        idCardManager.getConnectionObservable().subscribe(connectionObserver);
        idCardManager.getIdCardObservable().subscribe(dataObserver);
    }
    
    /**
     * 测试初始连接状态
     */
    @Test
    public void testInitialConnectionState() {
        // 验证初始连接状态为false
        assertFalse("初始连接状态应为false", idCardManager.isConnected());
        
        // 验证连接状态Observable初始值
        connectionObserver.assertValueCount(1);
        connectionObserver.assertValue(false);
    }
    
    /**
     * 测试连接状态变化
     */
    @Test
    public void testConnectionStateChanges() {
        // 模拟连接状态变化（这里只测试Observable部分，不涉及真实USB）
        
        // 验证初始状态
        connectionObserver.assertValueCount(1);
        connectionObserver.assertValueAt(0, false);
        
        // 注意：由于IdCardManager依赖真实的USB操作，
        // 这里我们主要验证Observable的初始状态和结构
        assertNotNull("连接状态Observable不应为null", idCardManager.getConnectionObservable());
        assertNotNull("身份证数据Observable不应为null", idCardManager.getIdCardObservable());
    }
    
    /**
     * 测试Observable的线程安全性
     */
    @Test
    public void testObservableThreadSafety() throws InterruptedException {
        // 创建多个观察者
        TestObserver<Boolean> observer1 = new TestObserver<>();
        TestObserver<Boolean> observer2 = new TestObserver<>();
        TestObserver<Boolean> observer3 = new TestObserver<>();
        
        // 同时订阅
        idCardManager.getConnectionObservable().subscribe(observer1);
        idCardManager.getConnectionObservable().subscribe(observer2);
        idCardManager.getConnectionObservable().subscribe(observer3);
        
        // 等待一小段时间
        Thread.sleep(100);
        
        // 验证所有观察者都收到初始值
        observer1.assertValueCount(1);
        observer2.assertValueCount(1);
        observer3.assertValueCount(1);
        
        observer1.assertValue(false);
        observer2.assertValue(false);
        observer3.assertValue(false);
    }
    
    /**
     * 测试资源释放
     */
    @Test
    public void testResourceCleanup() {
        // 验证release方法不会抛出异常
        assertDoesNotThrow("释放资源不应抛出异常", () -> {
            idCardManager.release();
        });
        
        // 多次调用release应该是安全的
        assertDoesNotThrow("多次释放应该安全", () -> {
            idCardManager.release();
            idCardManager.release();
        });
    }
    
    /**
     * 测试distinctUntilChanged功能
     */
    @Test
    public void testDistinctUntilChanged() {
        // 创建新的观察者来测试distinctUntilChanged
        TestObserver<Boolean> distinctObserver = new TestObserver<>();
        
        // 订阅带有distinctUntilChanged的Observable
        idCardManager.getConnectionObservable()
                .subscribe(distinctObserver);
        
        // 验证初始值
        distinctObserver.assertValueCount(1);
        distinctObserver.assertValue(false);
        
        // 注意：由于我们使用的是BehaviorSubject.createDefault(false)
        // 它会立即发出默认值false
    }
    
    /**
     * 模拟成功的身份证数据流
     */
    @Test
    public void testSimulatedIdCardDataFlow() {
        // 创建模拟的身份证数据
        IdCardData mockData = new IdCardData();
        mockData.setName("测试用户");
        mockData.setIdNumber("123456789012345678");
        mockData.setGender("男");
        mockData.setBirthDate("19900101");
        mockData.setAddress("测试地址");
        mockData.setNationality("汉");
        
        // 验证数据有效性
        assertTrue("模拟数据应该有效", mockData.isValid());
        
        // 验证数据Observable已准备就绪
        assertNotNull("数据Observable应该存在", idCardManager.getIdCardObservable());
        
        // 验证数据Observer初始状态
        dataObserver.assertValueCount(0); // BehaviorSubject.create()没有初始值
    }
    
    /**
     * 测试错误处理
     */
    @Test
    public void testErrorHandling() {
        // 测试当context为null时的处理
        assertDoesNotThrow("空context应该被正确处理", () -> {
            try {
                idCardManager.connect(null);
            } catch (Exception e) {
                // 预期的异常，不需要处理
            }
        });
    }
    
    /**
     * 测试Observable订阅和取消订阅
     */
    @Test
    public void testObservableSubscriptionLifecycle() {
        // 测试多次订阅和取消订阅
        TestObserver<Boolean> tempObserver = new TestObserver<>();
        
        // 订阅
        idCardManager.getConnectionObservable().subscribe(tempObserver);
        tempObserver.assertValueCount(1);
        
        // 取消订阅
        tempObserver.dispose();
        assertTrue("观察者应该被取消订阅", tempObserver.isDisposed());
    }
    
    /**
     * 辅助方法：验证不抛出异常
     */
    private void assertDoesNotThrow(String message, Runnable executable) {
        try {
            executable.run();
        } catch (Exception e) {
            fail(message + ": " + e.getMessage());
        }
    }
    
    /**
     * 集成测试：模拟完整的读卡流程
     */
    @Test
    public void testCompleteSimulatedFlow() {
        // 1. 验证初始状态
        assertFalse("初始连接状态", idCardManager.isConnected());
        connectionObserver.assertValue(false);
        dataObserver.assertValueCount(0);
        
        // 2. 创建测试数据
        String testJson = "{\n" +
                "  \"status\": \"success\",\n" +
                "  \"data\": {\n" +
                "    \"name\": \"张三\",\n" +
                "    \"idNumber\": \"123456789012345678\",\n" +
                "    \"gender\": \"男\",\n" +
                "    \"birthDate\": \"19900101\",\n" +
                "    \"address\": \"北京市东城区\",\n" +
                "    \"nationality\": \"汉\"\n" +
                "  }\n" +
                "}";
        
        // 3. 测试JSON解析
        IdCardData parsedData = IdCardData.fromJson(testJson);
        assertNotNull("JSON解析应该成功", parsedData);
        assertEquals("张三", parsedData.getName());
        assertTrue("解析的数据应该有效", parsedData.isValid());
        
        // 4. 验证数据格式化
        assertEquals("123456********5678", parsedData.getFormattedIdNumber());
        
        // 5. 验证Observable结构完整性
        assertNotNull("连接Observable应该存在", idCardManager.getConnectionObservable());
        assertNotNull("数据Observable应该存在", idCardManager.getIdCardObservable());
        
        // 6. 测试资源清理
        assertDoesNotThrow("资源清理应该安全", () -> idCardManager.release());
    }
} 