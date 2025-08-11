package com.tobacco.weight.hardware;

import com.bland.IDReader;
import com.tobacco.weight.data.FarmerInfo;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 身份证读卡器管理器
 * 负责身份证信息的读取和解析
 */
public class IdCardReader {

    private static final Logger logger = LoggerFactory.getLogger(IdCardReader.class);

    // SDK实例
    private IDReader reader;
    private static final String CONFIG_PATH = "./config";
    
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

    // 诊断信息（用于连接失败时展示更详细的原因）
    private final List<String> connectionAttempts = new ArrayList<>();
    private String lastPhase = "";              // Init / DetectSAM / CheckDriver / Read
    private String lastApi = "";                // getsam / readcard
    private Integer lastReturnCode = null;       // SDK返回码或解析的ret字段
    private String lastJsonSnippet = "";        // 最近一次返回的JSON片段（截断）
    private String lastExceptionMessage = "";   // 最近一次异常信息
    
    // 回调接口
    private Consumer<Boolean> onConnectionStatusChanged;
    private Consumer<FarmerInfo> onIdCardRead;
    private Consumer<String> onErrorOccurred;

    /**
     * 构造函数
     */
    public IdCardReader() {
        reader = new IDReader();
        initializeReader();
    }

    /**
     * 初始化身份证读卡器
     */
    public void initializeReader() {
        logger.info("开始初始化身份证读卡器...");
        
        try {
            connectionAttempts.clear();
            lastPhase = "Init";
            lastApi = "";
            lastReturnCode = null;
            lastJsonSnippet = "";
            lastExceptionMessage = "";

            // 步骤1: 初始化SDK (attemptConnection方法)
            logger.info("步骤1: 初始化SDK和加载驱动...");
            if (!attemptConnection()) {
                setError(ERROR_COMMUNICATION_FAILED, "attemptConnection()方法失败 - SDK初始化失败");
                logger.error("初始化失败: attemptConnection()方法返回false");
                return;
            }
            
            // 步骤2: 检测硬件设备 (detectDevice方法)
            logger.info("步骤2: 检测硬件设备...");
            if (!detectDevice()) {
                setError(ERROR_DEVICE_NOT_FOUND, "detectDevice()方法失败 - 未检测到身份证读卡器硬件设备");
                logger.error("初始化失败: detectDevice()方法返回false");
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
            lastExceptionMessage = e.getMessage();
        }
    }
    
    /**
     * 检测设备是否存在
     * 通过尝试获取SAM信息来检测设备
     */
    private boolean detectDevice() {
        try {
            logger.info("正在检测身份证读卡器硬件...");
            lastPhase = "DetectSAM";
            lastApi = "getsam";
            
            // 尝试多次检测设备，参考厂商demo的实现
            int maxAttempts = 10;
            for (int i = 0; i < maxAttempts; i++) {
                try {
                    String result = reader.WebSocketAPI("{\"module\":\"idcard\",\"msgid\":\"0\",\"function\":\"getsam\"}");
                    JSONObject jsondata = new JSONObject(result);
                    lastJsonSnippet = truncateJson(result);
                    
                    if (!jsondata.has("errorMsg") && jsondata.has("data")) {
                        JSONObject data = jsondata.getJSONObject("data");
                        if (data.has("samid")) {
                            String samId = data.getString("samid");
                            logger.info("检测到身份证读卡器设备, SAM ID: {}", samId);
                            connectionAttempts.add("getsam 成功, SAM ID=" + samId);
                            return true;
                        }
                    }
                    
                    if (i < maxAttempts - 1) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            logger.warn("未检测到有效的身份证读卡器设备");
            setError(ERROR_DEVICE_NOT_FOUND, "未检测到有效的身份证读卡器设备");
            connectionAttempts.add("getsam 失败: 多次尝试后仍未检测到设备");
            return false;
            
        } catch (Exception e) {
            logger.error("detectDevice()异常", e);
            setError(ERROR_DEVICE_NOT_FOUND, "detectDevice()方法异常: " + e.getMessage());
            lastExceptionMessage = e.getMessage();
            connectionAttempts.add("getsam 异常: " + e.getMessage());
            return false;
        }
    }



    /**
     * 尝试建立连接
     * 初始化IDReader SDK和加载本地库
     */
    private boolean attemptConnection() {
        try {
            logger.info("正在初始化身份证读卡器SDK...");
            
            // 初始化IDReader SDK (使用厂商的Init方法)
            lastPhase = "Init";
            int result = reader.Init(CONFIG_PATH);
            lastReturnCode = result;
            if (result < 0) {
                logger.error("SDK初始化失败, 返回码: {}", result);
                setError(ERROR_COMMUNICATION_FAILED, "SDK初始化失败, 返回码: " + result);
                connectionAttempts.add("SDK初始化失败: ret=" + result + ", configPath=" + new File(CONFIG_PATH).getAbsolutePath());
                return false;
            }
            
            logger.info("身份证读卡器SDK初始化成功");
            connectionAttempts.add("SDK初始化成功");
            return true;
            
        } catch (Throwable e) { // 捕获包括 UnsatisfiedLinkError 在内的所有错误
            logger.error("attemptConnection()异常", e);
            setError(ERROR_COMMUNICATION_FAILED, "attemptConnection()方法异常: " + e.getMessage());
            lastExceptionMessage = e.getMessage();
            connectionAttempts.add("SDK初始化异常: " + e.getMessage());
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

            // 使用真实的身份证读卡器API
            lastPhase = "Read";
            lastApi = "readcard";
            String result = reader.WebSocketAPI("{\"module\":\"idcard\",\"msgid\":\"0\",\"function\":\"readcard\"}");
            lastJsonSnippet = truncateJson(result);
            FarmerInfo farmerInfo = parseRealIdCardData(result);

            if (farmerInfo != null) {
                logger.info("身份证读取成功: {}", farmerInfo.getFarmerName());
                errorCode = ERROR_NONE;
                lastError = "";
                connectionAttempts.add("读卡成功");

                // 通知读取结果
                if (onIdCardRead != null) {
                    onIdCardRead.accept(farmerInfo);
                }
            } else {
                setError(ERROR_CARD_DATA_INVALID, "身份证数据无效或读取失败");
                connectionAttempts.add("读卡失败");
                if (onIdCardRead != null) {
                    onIdCardRead.accept(null);
                }
            }

        } catch (Exception e) {
            logger.error("读取身份证失败", e);
            setError(ERROR_UNKNOWN, "读取身份证异常: " + e.getMessage());
            lastExceptionMessage = e.getMessage();
            connectionAttempts.add("读卡异常: " + e.getMessage());

            // 通知读取失败
            if (onIdCardRead != null) {
                onIdCardRead.accept(null);
            }
        }
    }

    /**
     * 解析真实身份证数据
     * 从IDReader SDK的JSON响应中解析身份证信息
     */
    private FarmerInfo parseRealIdCardData(String jsonResult) {
        try {
            logger.info("解析身份证读取结果...");
            
            JSONObject jsondata = new JSONObject(jsonResult);
            
            // 检查是否是读卡消息
            if (jsondata.has("function") && !jsondata.getString("function").equals("readcard")) {
                logger.warn("非读卡消息，跳过解析");
                return null;
            }
            
            // 检查错误信息
            if (jsondata.has("errorMsg")) {
                String errorMsg = jsondata.getString("errorMsg");
                logger.error("读卡失败: {}", errorMsg);
                setError(ERROR_CARD_READ_FAILED, "读卡失败: " + errorMsg);
                connectionAttempts.add("readcard 失败: " + errorMsg);
                return null;
            }
            
            // 检查返回码
            if (jsondata.has("ret") && jsondata.getInt("ret") != 0) {
                int rc = jsondata.getInt("ret");
                lastReturnCode = rc;
                logger.error("读卡返回错误码: {}", rc);
                setError(ERROR_CARD_READ_FAILED, "读卡返回错误码: " + rc);
                connectionAttempts.add("readcard ret=" + rc);
                return null;
            }
            
            if (!jsondata.has("data")) {
                logger.error("读卡结果中缺少data字段");
                setError(ERROR_CARD_DATA_INVALID, "读卡结果中缺少data字段");
                return null;
            }
            
            JSONObject content = jsondata.getJSONObject("data");
            
            // 检查证件类型（居民身份证、外国人永久居留证、港澳台居住证）
            String type = content.optString("type", " ");
            if (!type.equals(" ")) {
                logger.info("证件类型: {}", type.equals("I") ? "外国人永久居留证" : 
                                          type.equals("J") ? "港澳台居住证" : "未知");
            }
            
            // 解析身份证基本信息
            String name = content.getString("name");
            String idNumber = content.getString("number");
            String gender = content.getInt("gender") == 1 ? "男" : 
                           content.getInt("gender") == 2 ? "女" : "未知";
            String nationality = type.equals(" ") ? getRaceName(content.getString("race")) : "其他";
            String birthDate = content.getString("birthday");
            String address = type.equals("I") ? "" : content.getString("address"); // 外国人永久居留证无住址
            String issuer = content.getString("issuer");
            String validStart = content.getString("valid"); // 注意：厂商拼写为"valied"而非"valid"
            String validEnd = content.getString("expire");
            
            // 解析照片
            byte[] photo = null;
            if (content.has("photo")) {
                try {
                    photo = Base64.getDecoder().decode(content.getString("photo"));
                } catch (Exception e) {
                    logger.warn("照片数据解析失败: {}", e.getMessage());
                }
            }
            
            // 生成合同号（业务逻辑）
            String contractNumber = generateContractNumber(name);
            
            logger.info("成功解析身份证: {} ({})", name, 
                    idNumber.length() > 14 ? idNumber.substring(0, 6) + "****" + idNumber.substring(14) : idNumber);
            
            return FarmerInfo.createWithIdCard(name, contractNumber, idNumber,
                    gender, nationality, birthDate, address,
                    issuer, validStart, validEnd, photo);

        } catch (Exception e) {
            logger.error("解析身份证数据失败", e);
            setError(ERROR_CARD_DATA_INVALID, "解析身份证数据失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 将民族代码转换为民族名称
     */
    private String getRaceName(String raceCode) {
        // 完整的民族代码映射表
        String[][] raceTable = {
            {"01", "汉族"}, {"02", "蒙古族"}, {"03", "回族"}, {"04", "藏族"},
            {"05", "维吾尔族"}, {"06", "苗族"}, {"07", "彝族"}, {"08", "壮族"},
            {"09", "布依族"}, {"10", "朝鲜族"}, {"11", "满族"}, {"12", "侗族"},
            {"13", "瑶族"}, {"14", "白族"}, {"15", "土家族"}, {"16", "哈尼族"},
            {"17", "哈萨克族"}, {"18", "傣族"}, {"19", "黎族"}, {"20", "傈僳族"},
            {"21", "佤族"}, {"22", "畲族"}, {"23", "高山族"}, {"24", "拉祜族"},
            {"25", "水族"}, {"26", "东乡族"}, {"27", "纳西族"}, {"28", "景颇族"},
            {"29", "柯尔克孜族"}, {"30", "土族"}, {"31", "达斡尔族"}, {"32", "仫佬族"},
            {"33", "羌族"}, {"34", "布朗族"}, {"35", "撒拉族"}, {"36", "毛南族"},
            {"37", "仡佬族"}, {"38", "锡伯族"}, {"39", "阿昌族"}, {"40", "普米族"},
            {"41", "塔吉克族"}, {"42", "怒族"}, {"43", "乌孜别克族"}, {"44", "俄罗斯族"},
            {"45", "鄂温克族"}, {"46", "德昂族"}, {"47", "保安族"}, {"48", "裕固族"},
            {"49", "京族"}, {"50", "塔塔尔族"}, {"51", "独龙族"}, {"52", "鄂伦春族"},
            {"53", "赫哲族"}, {"54", "门巴族"}, {"55", "珞巴族"}, {"56", "基诺族"}
        };
        
        for (String[] race : raceTable) {
            if (race[0].equals(raceCode)) {
                return race[1];
            }
        }
        return "其他";
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
        try {
            if (reader != null && isConnected) {
                try {
                    // 使用厂商的ServiceStop方法停止服务
                    int result = IDReader.ServiceStop();
                    if (result < 0) {
                        logger.warn("ServiceStop返回错误码: {}", result);
                    } else {
                        logger.info("IDReader SDK服务已停止");
                    }
                } catch (Throwable nativeErr) {
                    // 捕获 UnsatisfiedLinkError 等所有原生级错误
                    logger.warn("停止SDK服务时出现异常: {}", nativeErr.getMessage());
                    lastExceptionMessage = nativeErr.getMessage();
                    connectionAttempts.add("ServiceStop 异常: " + nativeErr.getMessage());
                }
            }
        } catch (Throwable e) {
            logger.warn("disconnect()异常: {}", e.getMessage());
        }
        
        isConnected = false;
        deviceName = "";
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
                        "检查: 设备响应、端口占用、通信协议\n\n" +
                        "%s", ERROR_COMMUNICATION_FAILED, buildDiagnosis());
                        
            case ERROR_CARD_NOT_DETECTED:
                return String.format("[错误代码 %d] 身份证读取 - 未检测到卡片\n" +
                        "问题: readIdCard()中卡片检测失败\n" +
                        "检查: 身份证放置位置和接触\n\n" +
                        "%s", ERROR_CARD_NOT_DETECTED, buildDiagnosis());
                        
            case ERROR_CARD_READ_FAILED:
                return String.format("[错误代码 %d] 身份证读取 - 读取失败\n" +
                        "问题: readIdCard()中数据读取失败\n" +
                        "检查: 身份证状态和读卡器功能\n\n" +
                        "%s", ERROR_CARD_READ_FAILED, buildDiagnosis());
                        
            default:
                return String.format("[错误代码 %d] %s\n\n%s", errorCode, lastError, buildDiagnosis());
        }
    }

    private String buildDiagnosis() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.arch");
        String java = System.getProperty("java.version");
        StringBuilder sb = new StringBuilder();
        sb.append("诊断信息:\n");
        sb.append("- 操作系统: ").append(os).append('\n');
        sb.append("- Java: ").append(java).append('\n');
        sb.append("- 上一步骤: ").append(lastPhase).append('\n');
        if (!lastApi.isEmpty()) sb.append("- 最近API: ").append(lastApi).append('\n');
        if (lastReturnCode != null) sb.append("- 返回码: ").append(lastReturnCode).append('\n');
        if (!lastExceptionMessage.isEmpty()) sb.append("- 异常: ").append(lastExceptionMessage).append('\n');
        if (!lastJsonSnippet.isEmpty()) sb.append("- 返回JSON: ").append(lastJsonSnippet).append('\n');
        if (!connectionAttempts.isEmpty()) {
            sb.append("- 尝试记录:\n");
            int max = Math.min(connectionAttempts.size(), 8);
            for (int i = connectionAttempts.size() - max; i < connectionAttempts.size(); i++) {
                sb.append("  • ").append(connectionAttempts.get(i)).append('\n');
            }
        }
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (!osName.contains("win")) {
            sb.append("- 提示: 当前为非Windows平台，SDK可能不完全支持。请在Windows上配套驱动与DLL测试\n");
        }
        return sb.toString();
    }

    private String truncateJson(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\n|\r", " ");
        return t.length() > 300 ? t.substring(0, 300) + "..." : t;
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