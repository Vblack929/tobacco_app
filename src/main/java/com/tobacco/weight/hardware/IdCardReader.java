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
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

/**
 * 身份证读卡器管理器
 * 负责身份证信息的读取和解析
 */
public class IdCardReader {

    private static final Logger logger = LoggerFactory.getLogger(IdCardReader.class);

    // HTTP客户端实例
    private HttpClient httpClient;
    private static final String READER_BASE_URL = "http://127.0.0.1:7846";
    private static final int CONNECTION_TIMEOUT = 10; // 秒
    
    // RSA公钥 - 用于验证身份证数据签名（来自SDK文档示例）
    private static final String RSA_PUBLIC_KEY = 
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA8KVhMYEpLg8PiGS8rv4h" +
        "EKRX86FBJFKLvEL/o4Rz2O2BceWV4+mwzmyrbsI4Re8GP8RpqgwOFG/Zn3pMlCfo" + 
        "vDzGT5dHu329NQJBYCjvThsITdv7Km979PZ1tgAQnXd3DTy9rxfmPxIaqFzIuj/g" + 
        "+Z2hLJB3GZJXYlq1RSDROSgf6zry97SKBQpD/Og5odxw9QoNhu6k+DP9X8El/D1S" + 
        "NzHyl5LGXkifMEFy3bngC5fSQVtrgIqTW6NGPa10e/JWNsWO9DZXOyOjlnKJ3uj3" + 
        "gg3hjEfEAoe5r+5BSQKixqFwaRYC+NQcPoELTTqLh0JyRPI6QH6H0JsxjDzv1vij" + 
        "xwIDAQAB";
    
    // 连接状态
    private boolean isConnected = false;
    private String deviceName = "";
    
    // 配置选项
    private boolean enableSignatureVerification = true; // 是否启用签名验证
    
    // 错误代码常量 - 根据SDK文档第7章错误码定义
    public static final int ERROR_NONE = 0;                    // 成功
    public static final int ERROR_INVALID_PARAM = 1;           // 输入参数错误
    public static final int ERROR_DEVICE_NOT_FOUND = 2;        // 设备不存在（比如阅读器未连接）
    public static final int ERROR_CARD_NOT_DETECTED = 3;       // 找卡失败
    public static final int ERROR_COMMUNICATION_FAILED = 100;  // 通信连接失败
    public static final int ERROR_FUNCTION_NOT_SUPPORTED = 4;  // 该功能不支持
    public static final int ERROR_CARD_SELECT_FAILED = 5;      // 选卡失败
    public static final int ERROR_CARD_READ_FAILED = 6;        // 读卡失败
    public static final int ERROR_CARD_WRITE_FAILED = 7;       // 写卡失败
    public static final int ERROR_CARD_DATA_FORMAT = 8;        // 卡片数据格式错误
    public static final int ERROR_PHOTO_BASE64 = 9;            // 照片Base64编码错误
    public static final int ERROR_TEXT_FORMAT = 10;            // 卡片文本格式错误
    public static final int ERROR_IMAGE_LIBRARY = 11;          // 图片解码库加载失败
    public static final int ERROR_BMP_TO_JPG = 12;             // BMP转JPG函数缺失
    public static final int ERROR_IMAGE_FORMAT = 13;           // 图片格式不支持
    public static final int ERROR_TEMP_WRITE = 14;             // 临时目录写失败
    public static final int ERROR_WLT_DECODE = 15;             // WLT解码BMP失败
    public static final int ERROR_TEMP_READ = 16;              // 临时目录读失败
    public static final int ERROR_BMP_ENCODE = 17;             // BMP编码JPG失败
    public static final int ERROR_JPG_BASE64 = 18;             // JPG图片BASE64编码失败
    public static final int ERROR_BMP_BASE64 = 19;             // BMP图片BASE64编码失败
    public static final int ERROR_URL_DECODE = 20;             // URL解码失败
    public static final int ERROR_IMAGE_FORMAT_ERROR = 21;     // 图片格式错误
    public static final int ERROR_DATA_EXCEED_LIMIT = 22;      // 写入数据超过限制
    public static final int ERROR_UNKNOWN = 9999;              // 未知错误
    
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
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECTION_TIMEOUT))
            .build();
        initializeReader();
    }

    /**
     * 初始化身份证读卡器
     */
    public void initializeReader() {
        logger.info("开始初始化身份证读卡器...");
        
        try {
            connectionAttempts.clear();
            lastPhase = "初始化";
            lastApi = "";
            lastReturnCode = null;
            lastJsonSnippet = "";
            lastExceptionMessage = "";

            // 测试HTTP服务连接
            logger.info("测试HTTP服务连接...");
            if (testConnection()) {
                deviceName = "身份证读卡器 HTTP服务";
                isConnected = true;
                errorCode = ERROR_NONE;
                lastError = "";
                logger.info("身份证读卡器HTTP服务连接成功: {}", deviceName);
                
                // 通知UI连接状态变化
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(true);
                }
            } else {
                setError(ERROR_COMMUNICATION_FAILED, "HTTP服务连接失败");
                logger.error("初始化失败: HTTP服务连接失败");
                
                // 通知UI连接失败
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(false);
                }
            }
            
        } catch (Exception e) {
            setError(ERROR_UNKNOWN, "initializeReader()方法异常: " + e.getMessage());
            logger.error("身份证读卡器初始化异常", e);
            lastExceptionMessage = e.getMessage();
            
            // 通知UI连接失败
            if (onConnectionStatusChanged != null) {
                onConnectionStatusChanged.accept(false);
            }
        }
    }
    
    /**
     * 测试HTTP服务连接
     * 简化的连接测试，基于HTTP通信
     */
    private boolean testConnection() {
        try {
            logger.info("正在测试身份证读卡器HTTP服务连接...");
            lastPhase = "HTTP连接测试";
            lastApi = "getReaderInfo";
            
            // 测试HTTP服务是否可用并获取设备信息
            String result = makeHttpRequest("/getReaderInfo");
            JSONObject jsondata = new JSONObject(result);
            lastJsonSnippet = truncateJson(result);
            
            // 检查是否成功返回设备信息
            if (jsondata.has("result") && jsondata.getInt("result") == 0) {
                // 解析设备信息
                String samId = jsondata.optString("samid", "未知SAM ID");
                String version = jsondata.optString("readerVersion", "未知版本");
                String sn = jsondata.optString("sn", "未知序列号");
                
                logger.info("身份证读卡器HTTP服务连接成功:");
                logger.info("- SAM ID: {}", samId);
                logger.info("- 版本: {}", version);
                logger.info("- 序列号: {}", sn);
                
                connectionAttempts.add("HTTP服务连接成功: " + version + ", SAM=" + samId);
                return true;
            } else {
                logger.warn("设备响应错误: result={}", jsondata.optInt("result", -1));
                connectionAttempts.add("设备响应错误: result=" + jsondata.optInt("result", -1));
                return false;
            }
            
        } catch (Exception e) {
            logger.error("testConnection()异常", e);
            lastExceptionMessage = e.getMessage();
            connectionAttempts.add("HTTP连接异常: " + e.getMessage());
            return false;
        }
    }



    /**
     * 尝试建立连接
     * 初始化HTTP客户端连接
     */
    private boolean attemptConnection() {
        try {
            logger.info("正在初始化身份证读卡器HTTP连接...");
            
            lastPhase = "Init";
            
            // 测试HTTP服务是否可用
            String testUrl = "/getReaderInfo";
            String result = makeHttpRequest(testUrl);
            
            if (result != null && !result.isEmpty()) {
                logger.info("身份证读卡器HTTP服务连接成功");
                connectionAttempts.add("HTTP服务连接成功, URL=" + READER_BASE_URL);
                return true;
            } else {
                logger.error("HTTP服务连接失败: 空响应");
                setError(ERROR_COMMUNICATION_FAILED, "HTTP服务连接失败: 空响应");
                connectionAttempts.add("HTTP服务连接失败: 空响应");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("attemptConnection()异常", e);
            setError(ERROR_COMMUNICATION_FAILED, "attemptConnection()方法异常: " + e.getMessage());
            lastExceptionMessage = e.getMessage();
            connectionAttempts.add("HTTP连接异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 发送HTTP请求到身份证读卡器服务
     */
    private String makeHttpRequest(String endpoint) throws Exception {
        String url = READER_BASE_URL + endpoint;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(CONNECTION_TIMEOUT))
            .GET()
            .build();
            
        logger.debug("发送HTTP请求: {}", url);
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
            
        if (response.statusCode() == 200) {
            String body = response.body();
            logger.debug("HTTP响应: {}", truncateJson(body));
            return body;
        } else {
            throw new Exception("HTTP请求失败, 状态码: " + response.statusCode());
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
            
            // 检查HTTP服务连接状态
            if (!ensureConnected()) {
                setError(ERROR_COMMUNICATION_FAILED, "HTTP服务未连接");
                if (onIdCardRead != null) {
                    onIdCardRead.accept(null);
                }
                return;
            }

            // 使用HTTP API读取身份证
            lastPhase = "读取身份证";
            lastApi = "readCard";
            String result = makeHttpRequest("/api/readCard?utf8=1");
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
                setError(ERROR_CARD_DATA_FORMAT, "身份证数据无效或读取失败");
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
            
            // 检查HTTP API错误信息（不同于WebSocket API格式）
            if (jsondata.has("errorMsg")) {
                String errorMsg = jsondata.getString("errorMsg");
                if (!"OK".equals(errorMsg)) {
                    logger.error("读卡失败: {}", errorMsg);
                    setError(ERROR_CARD_READ_FAILED, "读卡失败: " + errorMsg);
                    connectionAttempts.add("readCard 失败: " + errorMsg);
                    return null;
                }
            }
            
            // 检查返回标志（HTTP API使用 resultFlag）
            if (jsondata.has("resultFlag")) {
                String resultFlag = jsondata.getString("resultFlag");
                if (!"0".equals(resultFlag)) {
                    logger.error("读卡返回错误标志: {}", resultFlag);
                    int errorCode = Integer.parseInt(resultFlag);
                    lastReturnCode = errorCode;
                    
                    // 根据错误码映射
                    int mappedError = mapSdkErrorCode(errorCode);
                    setError(mappedError, "读卡返回错误标志: " + resultFlag + " (" + getErrorDescription(errorCode) + ")");
                    connectionAttempts.add("readCard resultFlag=" + resultFlag + " - " + getErrorDescription(errorCode));
                    return null;
                }
            }
            
            // HTTP API 的数据在 resultContent 字段中
            if (!jsondata.has("resultContent")) {
                logger.error("读卡结果中缺少resultContent字段");
                setError(ERROR_CARD_DATA_FORMAT, "读卡结果中缺少resultContent字段");
                return null;
            }
            
            JSONObject content = jsondata.getJSONObject("resultContent");
            
            // 验证数字签名（如果启用且存在签名）
            if (enableSignatureVerification && (content.has("sign1") || content.has("sign2"))) {
                boolean signatureValid = verifySignature(content);
                if (!signatureValid) {
                    logger.warn("身份证数据签名验证失败");
                    setError(ERROR_CARD_DATA_FORMAT, "身份证数据签名验证失败");
                    return null;
                } else {
                    logger.info("身份证数据签名验证成功");
                }
            } else if (!enableSignatureVerification) {
                logger.debug("签名验证已禁用");
            }
            
            // HTTP API 只返回基本的身份证信息，没有复杂的证件类型区分
            logger.info("证件类型: 居民身份证 (HTTP API)");
            
            // 解析HTTP API返回的字段
            String name = content.getString("name");
            String idNumber = content.getString("idNum");  // HTTP API使用 idNum
            String gender = "1".equals(content.getString("gender")) ? "男" : 
                           "2".equals(content.getString("gender")) ? "女" : "未知";
            String birthDate = content.getString("birthday");
            String address = content.getString("address");
            String issueOrg = content.getString("issueOrg");  // HTTP API使用 issueOrg
            String effectDate = content.getString("effectDate");  // HTTP API使用 effectDate
            String expireDate = content.getString("expireDate");  // HTTP API使用 expireDate
            
            // 民族信息处理
            String nationality = getRaceName(content.getString("nation"));  // HTTP API使用 nation
            
            // 解析照片
            byte[] photo = null;
            if (content.has("photo")) {
                try {
                    photo = Base64.getDecoder().decode(content.getString("photo"));
                } catch (Exception e) {
                    logger.warn("照片数据解析失败: {}", e.getMessage());
                }
            }
            
            logger.info("成功解析身份证: {} ({})", name, 
                    idNumber.length() > 14 ? idNumber.substring(0, 6) + "****" + idNumber.substring(14) : idNumber);
            
            // 不生成合同号，让MainController从数据库查询
            return FarmerInfo.createWithIdCard(name, "", idNumber,
                    gender, nationality, birthDate, address,
                    issueOrg, effectDate, expireDate, photo);

        } catch (Exception e) {
            logger.error("解析身份证数据失败", e);
            setError(ERROR_CARD_DATA_FORMAT, "解析身份证数据失败: " + e.getMessage());
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
            logger.info("尝试连接身份证读卡器HTTP服务...");
            
            // 重新初始化设备（基于HTTP连接）
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
            if (httpClient != null && isConnected) {
                logger.info("关闭HTTP客户端连接");
                // HttpClient会自动管理连接，无需手动关闭
            }
        } catch (Exception e) {
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
     * 基于HTTP服务可用性实时检测
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * 检查当前连接状态
     * 通过快速HTTP请求验证连接是否有效
     */
    public void checkConnectionStatus() {
        try {
            // 快速测试连接（无日志干扰）
            boolean wasConnected = isConnected;
            String result = makeHttpRequest("/getReaderInfo");
            
            if (result != null && !result.isEmpty()) {
                JSONObject jsondata = new JSONObject(result);
                boolean isNowConnected = jsondata.has("result") && jsondata.getInt("result") == 0;
                
                if (isNowConnected != wasConnected) {
                    isConnected = isNowConnected;
                    logger.info("身份证读卡器连接状态变化: {}", isConnected ? "已连接" : "已断开");
                    
                    if (onConnectionStatusChanged != null) {
                        onConnectionStatusChanged.accept(isConnected);
                    }
                }
            } else if (wasConnected) {
                // 之前连接正常，现在无响应
                isConnected = false;
                logger.warn("身份证读卡器连接丢失");
                
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(false);
                }
            }
            
        } catch (Exception e) {
            if (isConnected) {
                isConnected = false;
                logger.warn("身份证读卡器连接检查失败: {}", e.getMessage());
                
                if (onConnectionStatusChanged != null) {
                    onConnectionStatusChanged.accept(false);
                }
            }
        }
    }
    
    /**
     * 确保连接可用
     * 在执行操作前检查并尝试恢复连接
     */
    private boolean ensureConnected() {
        if (isConnected) {
            // 快速验证当前连接
            try {
                String result = makeHttpRequest("/getReaderInfo");
                if (result != null && !result.isEmpty()) {
                    JSONObject jsondata = new JSONObject(result);
                    return jsondata.has("result") && jsondata.getInt("result") == 0;
                }
            } catch (Exception e) {
                logger.debug("连接验证失败: {}", e.getMessage());
            }
        }
        
        // 连接无效，尝试重新连接
        isConnected = false;
        return testConnection();
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
     * 设置是否启用签名验证
     */
    public void setEnableSignatureVerification(boolean enable) {
        this.enableSignatureVerification = enable;
        logger.info("签名验证已{}", enable ? "启用" : "禁用");
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
                return String.format("[错误代码 %d] HTTP服务不可用\n" +
                        "问题: 无法连接到身份证读卡器HTTP服务\n" +
                        "检查: 端口7846是否开放、读卡器服务是否启动", ERROR_DEVICE_NOT_FOUND);
                        
            case ERROR_FUNCTION_NOT_SUPPORTED:
                return String.format("[错误代码 %d] 功能不支持\n" +
                        "问题: 请求的功能不被设备支持\n" +
                        "检查: 设备型号和固件版本", ERROR_FUNCTION_NOT_SUPPORTED);
                        
            case ERROR_COMMUNICATION_FAILED:
                return String.format("[错误代码 %d] HTTP通信失败\n" +
                        "问题: 无法与身份证读卡器HTTP服务通信\n" +
                        "检查: 网络连接、端口7846状态、防火墙设置\n\n" +
                        "%s", ERROR_COMMUNICATION_FAILED, buildDiagnosis());
                        
            case ERROR_CARD_NOT_DETECTED:
                return String.format("[错误代码 %d] 未检测到身份证\n" +
                        "问题: 读卡器未检测到身份证卡片\n" +
                        "检查: 身份证放置位置和接触", ERROR_CARD_NOT_DETECTED);
                        
            case ERROR_CARD_READ_FAILED:
                return String.format("[错误代码 %d] 身份证读取失败\n" +
                        "问题: 身份证数据读取失败\n" +
                        "检查: 身份证状态和读卡器功能", ERROR_CARD_READ_FAILED);
                        
            default:
                return String.format("[错误代码 %d] %s\n\n%s", errorCode, lastError, buildDiagnosis());
        }
    }

    private String buildDiagnosis() {
        StringBuilder sb = new StringBuilder();
        sb.append("诊断信息:\n");
        sb.append("- HTTP服务地址: ").append(READER_BASE_URL).append('\n');
        sb.append("- 当前状态: ").append(lastPhase).append('\n');
        
        if (!lastApi.isEmpty()) {
            sb.append("- 最近API调用: ").append(lastApi).append('\n');
        }
        
        if (!lastExceptionMessage.isEmpty()) {
            sb.append("- 连接异常: ").append(lastExceptionMessage).append('\n');
        }
        
        if (!lastJsonSnippet.isEmpty()) {
            sb.append("- 服务响应: ").append(lastJsonSnippet).append('\n');
        }
        
        if (!connectionAttempts.isEmpty()) {
            sb.append("- 连接尝试:\n");
            int max = Math.min(connectionAttempts.size(), 3); // 只显示最近3次
            for (int i = connectionAttempts.size() - max; i < connectionAttempts.size(); i++) {
                sb.append("  • ").append(connectionAttempts.get(i)).append('\n');
            }
        }
        
        sb.append("- 建议:\n");
        sb.append("  • 确认身份证读卡器服务已启动\n");
        sb.append("  • 检查端口7846是否被占用\n");
        sb.append("  • 或使用模拟模式进行测试\n");
        
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
        logger.info("测试身份证读卡器HTTP服务...");
        return ensureConnected();
    }
    
    /**
     * 读取SAM ID
     */
    public String readSamId() {
        try {
            if (!ensureConnected()) {
                logger.error("读取SAM ID失败: HTTP服务未连接");
                return null;
            }
            
            String result = makeHttpRequest("/readSamID");
            JSONObject jsondata = new JSONObject(result);
            
            if (jsondata.has("result") && jsondata.getInt("result") == 0) {
                return jsondata.getString("samid");
            }
            return null;
        } catch (Exception e) {
            logger.error("读取SAM ID失败", e);
            return null;
        }
    }
    
    /**
     * 读取身份证物理卡号
     */
    public String readCardNumber() {
        try {
            if (!ensureConnected()) {
                logger.error("读取身份证物理卡号失败: HTTP服务未连接");
                return null;
            }
            
            String result = makeHttpRequest("/readCardNo");
            JSONObject jsondata = new JSONObject(result);
            
            if (jsondata.has("result") && jsondata.getInt("result") == 0) {
                return jsondata.getString("cardNo");
            }
            return null;
        } catch (Exception e) {
            logger.error("读取身份证物理卡号失败", e);
            return null;
        }
    }
    


    /**
     * 验证身份证数据的RSA数字签名
     * 根据SDK文档中的签名验证示例实现
     */
    private boolean verifySignature(JSONObject content) {
        try {
            // 获取RSA公钥
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(RSA_PUBLIC_KEY)));
            
            boolean sign1Valid = true;
            boolean sign2Valid = true;
            
            // 验证签名1 - SHA1withRSA签名验证
            if (content.has("sign1")) {
                try {
                    Signature signature = Signature.getInstance("SHA1withRSA");
                    signature.initVerify(publicKey);
                    
                    // 签名1使用UTF-16LE编码的文本数据和原始照片数据
                    signature.update(content.getString("text").getBytes(StandardCharsets.UTF_16LE));
                    signature.update(Base64.getDecoder().decode(content.getString("wlt")));
                    
                    byte[] signatureBytes = Base64.getDecoder().decode(content.getString("sign1"));
                    sign1Valid = signature.verify(signatureBytes);
                    
                    if (!sign1Valid) {
                        logger.warn("签名1验证失败");
                    } else {
                        logger.debug("签名1验证成功");
                    }
                } catch (Exception e) {
                    logger.warn("签名1验证异常: {}", e.getMessage());
                    sign1Valid = false;
                }
            }
            
            // 验证签名2 - RSA解密验证
            if (content.has("sign2")) {
                try {
                    Cipher cipher = Cipher.getInstance("RSA");
                    cipher.init(Cipher.DECRYPT_MODE, publicKey);
                    byte[] encryptedBytes = Base64.getDecoder().decode(content.getString("sign2"));
                    byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
                    
                    String decryptedInfo = new String(decryptedBytes, StandardCharsets.UTF_8);
                    logger.debug("签名2解密结果: {}", decryptedInfo);
                    
                    // 签名2包含用'|'分隔的信息，可以进一步验证内容一致性
                    String[] signData = decryptedInfo.split("\\|");
                    if (signData.length >= 3) {
                        String signIdNumber = signData[2];
                        String actualIdNumber = content.getString("number");
                        sign2Valid = signIdNumber.equals(actualIdNumber);
                        
                        if (!sign2Valid) {
                            logger.warn("签名2身份证号不匹配: 签名中={}, 实际={}", signIdNumber, actualIdNumber);
                        } else {
                            logger.debug("签名2验证成功");
                        }
                    } else {
                        logger.warn("签名2解密数据格式不正确");
                        sign2Valid = false;
                    }
                } catch (Exception e) {
                    logger.warn("签名2验证异常: {}", e.getMessage());
                    sign2Valid = false;
                }
            }
            
            return sign1Valid && sign2Valid;
            
        } catch (Exception e) {
            logger.error("签名验证失败", e);
            return false;
        }
    }

    /**
     * 映射SDK错误码到本地错误码
     */
    private int mapSdkErrorCode(int sdkErrorCode) {
        switch (sdkErrorCode) {
            case 0: return ERROR_NONE;
            case 1: return ERROR_INVALID_PARAM;
            case 2: return ERROR_DEVICE_NOT_FOUND;
            case 3: return ERROR_CARD_NOT_DETECTED;
            case 4: return ERROR_FUNCTION_NOT_SUPPORTED;
            case 100: return ERROR_COMMUNICATION_FAILED;
            case 5: return ERROR_CARD_SELECT_FAILED;
            case 6: return ERROR_CARD_READ_FAILED;
            case 7: return ERROR_CARD_WRITE_FAILED;
            case 8: return ERROR_CARD_DATA_FORMAT;
            case 9: return ERROR_PHOTO_BASE64;
            case 10: return ERROR_TEXT_FORMAT;
            case 11: return ERROR_IMAGE_LIBRARY;
            case 12: return ERROR_BMP_TO_JPG;
            case 13: return ERROR_IMAGE_FORMAT;
            case 14: return ERROR_TEMP_WRITE;
            case 15: return ERROR_WLT_DECODE;
            case 16: return ERROR_TEMP_READ;
            case 17: return ERROR_BMP_ENCODE;
            case 18: return ERROR_JPG_BASE64;
            case 19: return ERROR_BMP_BASE64;
            case 20: return ERROR_URL_DECODE;
            case 21: return ERROR_IMAGE_FORMAT_ERROR;
            case 22: return ERROR_DATA_EXCEED_LIMIT;
            default: return ERROR_UNKNOWN;
        }
    }
    
    /**
     * 获取错误码描述
     */
    private String getErrorDescription(int errorCode) {
        switch (errorCode) {
            case 0: return "成功";
            case 1: return "输入参数错误";
            case 2: return "设备不存在（比如阅读器未连接）";
            case 3: return "找卡失败";
            case 4: return "该功能不支持";
            case 100: return "通信连接失败";
            case 5: return "选卡失败";
            case 6: return "读卡失败";
            case 7: return "写卡失败";
            case 8: return "卡片数据格式错误";
            case 9: return "照片Base64编码错误";
            case 10: return "卡片文本格式错误";
            case 11: return "图片解码库加载失败";
            case 12: return "BMP转JPG函数缺失";
            case 13: return "图片格式不支持";
            case 14: return "临时目录写失败";
            case 15: return "WLT解码BMP失败";
            case 16: return "临时目录读失败";
            case 17: return "BMP编码JPG失败";
            case 18: return "JPG图片BASE64编码失败";
            case 19: return "BMP图片BASE64编码失败";
            case 20: return "URL解码失败";
            case 21: return "图片格式错误";
            case 22: return "写入数据超过限制";
            default: return "未知错误";
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