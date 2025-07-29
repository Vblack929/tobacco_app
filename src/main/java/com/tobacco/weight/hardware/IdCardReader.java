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

    // 连接状态
    private boolean isConnected = false;
    private String deviceName = "";
    
    // 错误代码常量
    public static final int ERROR_NONE = 0;
    public static final int ERROR_DEVICE_NOT_FOUND = 1001;
    public static final int ERROR_DRIVER_NOT_INSTALLED = 1002;
    public static final int ERROR_PORT_OCCUPIED = 1003;
    public static final int ERROR_COMMUNICATION_FAILED = 1004;
    public static final int ERROR_DEVICE_TIMEOUT = 1005;
    public static final int ERROR_CARD_NOT_DETECTED = 2001;
    public static final int ERROR_CARD_READ_FAILED = 2002;
    public static final int ERROR_CARD_DATA_INVALID = 2003;
    public static final int ERROR_PERMISSION_DENIED = 3001;
    public static final int ERROR_UNKNOWN = 9999;
    
    // 错误状态
    private int errorCode = ERROR_NONE;
    private String lastError = "";
    
    // 回调接口
    private Consumer<Boolean> onConnectionStatusChanged;
    private Consumer<FarmerInfo> onIdCardRead;
    private Consumer<String> onErrorOccurred;

    /**
     * 构造函数
     */
    public IdCardReader() {
        initializeReader();
    }

    /**
     * 初始化身份证读卡器
     */
    public void initializeReader() {
        logger.info("开始初始化身份证读卡器...");
        
        try {
            // 步骤1: 检测硬件设备 (detectDevice方法)
            logger.info("步骤1: 检测硬件设备...");
            if (!detectDevice()) {
                setError(ERROR_DEVICE_NOT_FOUND, "detectDevice()方法失败 - 未检测到身份证读卡器硬件设备");
                logger.error("初始化失败: detectDevice()方法返回false");
                return;
            }
            
            // 步骤2: 检查驱动程序 (checkDriver方法)
            logger.info("步骤2: 检查驱动程序...");
            if (!checkDriver()) {
                setError(ERROR_DRIVER_NOT_INSTALLED, "checkDriver()方法失败 - 身份证读卡器驱动程序未安装或版本不兼容");
                logger.error("初始化失败: checkDriver()方法返回false");
                return;
            }
            
            // 步骤3: 尝试连接设备 (attemptConnection方法)
            logger.info("步骤3: 建立设备连接...");
            if (!attemptConnection()) {
                setError(ERROR_COMMUNICATION_FAILED, "attemptConnection()方法失败 - 无法与身份证读卡器建立通信连接");
                logger.error("初始化失败: attemptConnection()方法返回false");
                return;
            }
            
            // 初始化成功
            deviceName = "身份证读卡器 v2.1";
            isConnected = true;
            errorCode = ERROR_NONE;
            lastError = "";
            
            logger.info("身份证读卡器初始化成功: {}", deviceName);
            
        } catch (Exception e) {
            setError(ERROR_UNKNOWN, "initializeReader()方法异常: " + e.getMessage());
            logger.error("身份证读卡器初始化异常", e);
        }
    }
    
    /**
     * 检测设备是否存在
     * 实际项目中需要调用硬件检测API: return HardwareSDK.detectDevice();
     */
    private boolean detectDevice() {
        try {
            logger.info("正在检测身份证读卡器硬件...");
            Thread.sleep(500); // 模拟检测时间
            
            // 实际项目中替换为: return HardwareSDK.detectDevice();
            // 当前返回false因为没有真实硬件连接
            logger.warn("未检测到身份证读卡器硬件 - 需要集成真实硬件SDK");
            setError(ERROR_DEVICE_NOT_FOUND, "detectDevice()需要集成真实硬件SDK");
            return false;
            
        } catch (Exception e) {
            logger.error("detectDevice()异常", e);
            setError(ERROR_DEVICE_NOT_FOUND, "detectDevice()方法异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查驱动程序
     * 实际项目中需要检查驱动程序: return HardwareSDK.checkDriverVersion();
     */
    private boolean checkDriver() {
        try {
            logger.info("正在检查身份证读卡器驱动程序...");
            Thread.sleep(300);
            
            // 实际项目中替换为: return HardwareSDK.checkDriverVersion();
            // 当前返回false因为没有真实驱动程序
            logger.warn("未找到身份证读卡器驱动程序 - 需要集成真实硬件SDK");
            setError(ERROR_DRIVER_NOT_INSTALLED, "checkDriver()需要集成真实硬件SDK");
            return false;
            
        } catch (Exception e) {
            logger.error("checkDriver()异常", e);
            setError(ERROR_DRIVER_NOT_INSTALLED, "checkDriver()方法异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 尝试建立连接
     * 实际项目中需要调用连接API: return HardwareSDK.connectDevice();
     */
    private boolean attemptConnection() {
        try {
            logger.info("正在连接身份证读卡器...");
            Thread.sleep(800);
            
            // 实际项目中替换为: return HardwareSDK.connectDevice();
            // 当前返回false因为没有真实硬件连接
            logger.warn("无法连接身份证读卡器 - 需要集成真实硬件SDK");
            setError(ERROR_COMMUNICATION_FAILED, "attemptConnection()需要集成真实硬件SDK");
            return false;
            
        } catch (Exception e) {
            logger.error("attemptConnection()异常", e);
            setError(ERROR_COMMUNICATION_FAILED, "attemptConnection()方法异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 设置错误信息
     */
    private void setError(int code, String message) {
        this.errorCode = code;
        this.lastError = message;
        logger.error("ID卡读卡器错误 [{}]: {}", code, message);
        
        if (onErrorOccurred != null) {
            onErrorOccurred.accept(getDetailedErrorMessage());
        }
    }

    /**
     * 读取身份证信息
     */
    public void readIdCard() {
        try {
            logger.info("开始读取身份证信息...");
            
            if (!isConnected) {
                setError(ERROR_COMMUNICATION_FAILED, "设备未连接");
                if (onIdCardRead != null) {
                    onIdCardRead.accept(null);
                }
                return;
            }

            // 模拟读取过程
            // 实际项目中需要调用身份证读卡器的SDK
            Thread.sleep(2000); // 模拟读取时间
            
            // 检查是否有特定的读取失败模式
            // 实际项目中需要调用厂商提供的读卡器API (如: IDCardSDK.readCard())
            // 解析返回的二进制数据为FarmerInfo对象
            FarmerInfo farmerInfo = simulateIdCardData();

            if (farmerInfo != null) {
                logger.info("身份证读取成功: {}", farmerInfo.getFarmerName());
                errorCode = ERROR_NONE;
                lastError = "";

                // 通知读取结果
                if (onIdCardRead != null) {
                    onIdCardRead.accept(farmerInfo);
                }
            } else {
                setError(ERROR_CARD_DATA_INVALID, "身份证数据无效");
                if (onIdCardRead != null) {
                    onIdCardRead.accept(null);
                }
            }

        } catch (Exception e) {
            logger.error("读取身份证失败", e);
            setError(ERROR_UNKNOWN, "读取身份证异常: " + e.getMessage());

            // 通知读取失败
            if (onIdCardRead != null) {
                onIdCardRead.accept(null);
            }
        }
    }

    /**
     * 模拟身份证数据
     * 实际项目中需要解析真实的身份证数据
     * 
     * === 真实硬件集成步骤 ===
     * 1. 替换 simulateIdCardData() 为 parseRealIdCardData()
     * 2. 在 readIdCard() 中调用真实的硬件SDK:
     *    - 替换 Thread.sleep(2000) 为实际读取操作
     *    - 调用厂商提供的读卡器API (如: IDCardSDK.readCard())
     *    - 解析返回的二进制数据为FarmerInfo对象
     * 3. 在 detectDevice() 中调用硬件检测API
     * 4. 在 checkDriver() 中验证驱动程序版本
     * 5. 在 attemptConnection() 中建立真实的硬件连接
     * 
     * 例如:
     * // byte[] cardData = IDCardSDK.readCardData();
     * // return parseRealIdCardData(cardData);
     */
    private FarmerInfo simulateIdCardData() {
        try {
            // 模拟真实的身份证读取 - 提供多样化的测试数据
            // 实际项目中需要替换为: return parseRealIdCardData(rawCardData);
            
            // 测试用的农户信息池
            String[][] testFarmers = {
                {"张三", "110101199001011234", "男", "汉", "1990-01-01", "北京市朝阳区建国路100号"},
                {"李四", "130102198505152345", "女", "汉", "1985-05-15", "河北省石家庄市桥西区新华街88号"},
                {"王五", "230103197812256789", "男", "汉", "1978-12-25", "黑龙江省哈尔滨市道里区中央大街66号"},
                {"赵六", "320104198903183456", "女", "汉", "1989-03-18", "江苏省南京市秦淮区夫子庙路12号"},
                {"钱七", "510105199206227890", "男", "汉", "1992-06-22", "四川省成都市武侯区科华北路200号"},
                {"孙八", "610106198711091234", "女", "汉", "1987-11-09", "陕西省西安市雁塔区小寨路300号"},
                {"周九", "440107199404134567", "男", "汉", "1994-04-13", "广东省广州市天河区珠江新城88号"},
                {"吴十", "520108198608278901", "女", "汉", "1986-08-27", "贵州省贵阳市云岩区中华北路150号"}
            };
            
            // 随机选择一个农户信息
            int randomIndex = (int) (Math.random() * testFarmers.length);
            String[] selectedFarmer = testFarmers[randomIndex];
            
            String farmerName = selectedFarmer[0];
            String idCardNumber = selectedFarmer[1];
            String gender = selectedFarmer[2];
            String nationality = selectedFarmer[3];
            String birthDate = selectedFarmer[4];
            String address = selectedFarmer[5];
            
            // 生成动态信息
            String contractNumber = generateContractNumber(farmerName);
            String department = generateDepartment(idCardNumber);
            String startDate = generateStartDate();
            String endDate = generateEndDate(startDate);
            byte[] photo = null; // 实际项目中需要读取照片数据
            
            logger.info("模拟读取身份证: {} ({})", farmerName, idCardNumber.substring(0, 6) + "****" + idCardNumber.substring(14));

            return FarmerInfo.createWithIdCard(farmerName, contractNumber, idCardNumber,
                    gender, nationality, birthDate, address,
                    department, startDate, endDate, photo);

        } catch (Exception e) {
            logger.error("模拟身份证数据失败", e);
            return null;
        }
    }
    
    /**
     * 生成合同号
     */
    private String generateContractNumber(String farmerName) {
        // 根据农户姓名和时间戳生成合同号
        String nameCode = String.valueOf(farmerName.hashCode()).replace("-", "");
        String timeCode = String.valueOf(System.currentTimeMillis()).substring(8);
        return "HT" + nameCode.substring(0, Math.min(4, nameCode.length())) + timeCode;
    }
    
    /**
     * 根据身份证号生成签发机关
     */
    private String generateDepartment(String idCardNumber) {
        String prefix = idCardNumber.substring(0, 2);
        switch (prefix) {
            case "11": return "北京市公安局朝阳分局";
            case "13": return "河北省公安厅石家庄市公安局";
            case "23": return "黑龙江省公安厅哈尔滨市公安局";
            case "32": return "江苏省公安厅南京市公安局";
            case "51": return "四川省公安厅成都市公安局";
            case "61": return "陕西省公安厅西安市公安局";
            case "44": return "广东省公安厅广州市公安局";
            case "52": return "贵州省公安厅贵阳市公安局";
            default: return "公安局";
        }
    }
    
    /**
     * 生成有效期开始日期
     */
    private String generateStartDate() {
        // 模拟最近几年内的签发日期
        int year = 2020 + (int)(Math.random() * 4); // 2020-2023
        int month = 1 + (int)(Math.random() * 12);
        int day = 1 + (int)(Math.random() * 28);
        return String.format("%04d-%02d-%02d", year, month, day);
    }
    
    /**
     * 生成有效期结束日期
     */
    private String generateEndDate(String startDate) {
        try {
            // 身份证有效期通常是10年或20年
            int validYears = Math.random() > 0.5 ? 10 : 20;
            String[] parts = startDate.split("-");
            int year = Integer.parseInt(parts[0]) + validYears;
            return String.format("%04d-%s-%s", year, parts[1], parts[2]);
        } catch (Exception e) {
            return "2030-12-31"; // 默认值
        }
    }

    /**
     * 连接读卡器
     */
    public boolean connect() {
        try {
            logger.info("尝试连接身份证读卡器...");
            
            // 重新初始化设备
            logger.info("执行初始化流程...");
            initializeReader();
            
            if (isConnected) {
                logger.info("身份证读卡器连接成功");
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(true);
                }
                return true;
            } else {
                logger.error("身份证读卡器连接失败 - 错误: {}", getDetailedErrorMessage());
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(false);
                }
                return false;
            }
            
        } catch (Exception e) {
            logger.error("连接身份证读卡器异常", e);
            setError(ERROR_UNKNOWN, "connect()方法异常: " + e.getMessage());
            isConnected = false;
            
            if (onConnectionStatusChanged != null) {
                onConnectionStatusChanged.accept(false);
            }
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
     * 获取错误代码
     */
    public int getErrorCode() {
        return errorCode;
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
     * 设置错误回调
     */
    public void setOnErrorOccurred(Consumer<String> callback) {
        this.onErrorOccurred = callback;
    }

    /**
     * 获取最后的错误信息
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * 获取详细的错误信息
     */
    public String getDetailedErrorMessage() {
        switch (errorCode) {
            case ERROR_NONE:
                return "无错误";
                
            case ERROR_DEVICE_NOT_FOUND:
                return String.format("[错误代码 %d] detectDevice()方法失败\n" +
                        "问题: 硬件设备检测失败\n" +
                        "检查: USB连接、设备电源、设备管理器状态", ERROR_DEVICE_NOT_FOUND);
                        
            case ERROR_DRIVER_NOT_INSTALLED:
                return String.format("[错误代码 %d] checkDriver()方法失败\n" +
                        "问题: 驱动程序检查失败\n" +
                        "检查: 驱动安装、版本兼容性、权限", ERROR_DRIVER_NOT_INSTALLED);
                        
            case ERROR_COMMUNICATION_FAILED:
                return String.format("[错误代码 %d] attemptConnection()方法失败\n" +
                        "问题: 设备通信连接失败\n" +
                        "检查: 设备响应、端口占用、通信协议", ERROR_COMMUNICATION_FAILED);
                        
            case ERROR_CARD_NOT_DETECTED:
                return String.format("[错误代码 %d] 身份证读取 - 未检测到卡片\n" +
                        "问题: readIdCard()中卡片检测失败\n" +
                        "检查: 身份证放置位置和接触", ERROR_CARD_NOT_DETECTED);
                        
            case ERROR_CARD_READ_FAILED:
                return String.format("[错误代码 %d] 身份证读取 - 读取失败\n" +
                        "问题: readIdCard()中数据读取失败\n" +
                        "检查: 身份证状态和读卡器功能", ERROR_CARD_READ_FAILED);
                        
            default:
                return String.format("[错误代码 %d] %s", errorCode, lastError);
        }
    }

    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return errorCode != ERROR_NONE;
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

    /**
     * 检查并通知连接状态
     * 在UI注册回调后调用此方法
     */
    public void checkConnectionStatus() {
        if (onConnectionStatusChanged != null) {
            onConnectionStatusChanged.accept(isConnected);
        }
    }

    /**
     * 从外部添加错误日志（用于主界面错误通知）
     */
    public static void logError(String message) {
        // 静态方法记录错误日志
        LoggerFactory.getLogger(IdCardReader.class).info("外部错误: {}", message);
    }
}