package com.tobacco.weight.test;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * 模拟身份证读卡器HTTP服务
 * 在端口7846上提供与真实设备相同的API接口
 * 用于开发和测试环境
 */
public class MockIdCardReaderServer {

    private static final Logger logger = LoggerFactory.getLogger(MockIdCardReaderServer.class);
    private static final int PORT = 7846;
    
    private HttpServer server;
    private boolean isRunning = false;
    private Random random = new Random();
    
    // 模拟数据
    private static final String[] SAMPLE_NAMES = {
        "张三", "李四", "王五", "赵六", "陈七", "刘八", "杨九", "黄十",
        "周明", "吴亮", "郑强", "王丽", "李梅", "张华", "陈芳", "刘洋"
    };
    
    private static final String[] SAMPLE_ADDRESSES = {
        "广东省深圳市南山区科技园南区XX路XX号XX小区XX栋XX室",
        "北京市朝阳区建国门外大街XX号XX大厦XX层",
        "上海市浦东新区陆家嘴金融贸易区XX路XX号",
        "江苏省南京市鼓楼区中山路XX号XX广场XX座",
        "浙江省杭州市西湖区文三路XX号XX大厦XX楼",
        "四川省成都市武侯区天府大道XX号XX中心XX层"
    };
    
    private static final String[] SAMPLE_ISSUERS = {
        "深圳市公安局南山分局",
        "北京市公安局朝阳分局", 
        "上海市公安局浦东分局",
        "南京市公安局鼓楼分局",
        "杭州市公安局西湖分局",
        "成都市公安局武侯分局"
    };

    /**
     * 启动模拟服务器
     */
    public void start() throws IOException {
        if (isRunning) {
            logger.warn("模拟服务器已在运行");
            return;
        }

        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // 注册API端点
        server.createContext("/getReaderInfo", new GetReaderInfoHandler());
        server.createContext("/readSamID", new ReadSamIDHandler());
        server.createContext("/api/readCard", new ReadCardHandler());
        server.createContext("/readIDCard", new ReadIDCardHandler()); 
        server.createContext("/readCardNo", new ReadCardNoHandler());
        server.createContext("/readBankCard", new ReadBankCardHandler());
        server.createContext("/wltUnpack", new WltUnpackHandler());
        
        // 设置线程池
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        
        isRunning = true;
        logger.info("身份证读卡器模拟服务器已启动，监听端口: {}", PORT);
        logger.info("可用的API端点:");
        logger.info("  - GET http://127.0.0.1:{}/getReaderInfo", PORT);
        logger.info("  - GET http://127.0.0.1:{}/readSamID", PORT);
        logger.info("  - GET http://127.0.0.1:{}/api/readCard?utf8=1", PORT);
        logger.info("  - GET http://127.0.0.1:{}/readCardNo", PORT);
        logger.info("  - GET http://127.0.0.1:{}/readBankCard", PORT);
    }

    /**
     * 停止模拟服务器
     */
    public void stop() {
        if (server != null && isRunning) {
            server.stop(0);
            isRunning = false;
            logger.info("身份证读卡器模拟服务器已停止");
        }
    }

    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 获取读卡器信息处理器
     */
    private class GetReaderInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("收到请求: GET /getReaderInfo");
            
            JSONObject response = new JSONObject();
            response.put("readerParam", "PORT:HID");
            response.put("readerVersion", "SFZ101_V1.1_MOCK");
            response.put("samid", "05.13-20220315-0003607681-1474081524");
            response.put("sn", "MOCK-" + String.format("%08X", random.nextInt()));
            response.put("result", 0);

            sendJsonResponse(exchange, 200, response.toString());
        }
    }

    /**
     * 读取SAM ID处理器
     */
    private class ReadSamIDHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("收到请求: GET /readSamID");
            
            JSONObject response = new JSONObject();
            response.put("samid", "05.13-20220315-0003607681-1474081524");
            response.put("sn", "MOCK-" + String.format("%08X", random.nextInt()));
            response.put("result", 0);

            sendJsonResponse(exchange, 200, response.toString());
        }
    }

    /**
     * 读取身份证处理器 (HTTP API格式)
     */
    private class ReadCardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("收到请求: GET /api/readCard");
            
            // 模拟读卡延迟
            try {
                Thread.sleep(500 + random.nextInt(1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 随机生成身份证信息
            String name = SAMPLE_NAMES[random.nextInt(SAMPLE_NAMES.length)];
            String idNumber = generateMockIdNumber();
            String address = SAMPLE_ADDRESSES[random.nextInt(SAMPLE_ADDRESSES.length)];
            String issuer = SAMPLE_ISSUERS[random.nextInt(SAMPLE_ISSUERS.length)];
            
            JSONObject resultContent = new JSONObject();
            resultContent.put("name", name);
            resultContent.put("idNum", idNumber);
            resultContent.put("gender", random.nextBoolean() ? "1" : "2");
            resultContent.put("nation", String.format("%02d", 1 + random.nextInt(10))); // 01-10 民族代码
            resultContent.put("birthday", generateMockBirthday());
            resultContent.put("address", address);
            resultContent.put("issueOrg", issuer);
            resultContent.put("effectDate", generateMockEffectDate());
            resultContent.put("expireDate", generateMockExpireDate());
            resultContent.put("photo", generateMockPhoto());
            
            JSONObject response = new JSONObject();
            response.put("errorMsg", "OK");
            response.put("resultContent", resultContent);
            response.put("resultFlag", "0");
            response.put("status", "0");
            response.put("verderId", "1000");

            sendJsonResponse(exchange, 200, response.toString());
            logger.info("返回模拟身份证数据: {} ({})", name, 
                idNumber.substring(0, 6) + "****" + idNumber.substring(14));
        }
    }

    /**
     * 读取身份证处理器 (原始格式)
     */
    private class ReadIDCardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("收到请求: GET /readIDCard");
            
            JSONObject response = new JSONObject();
            response.put("wzInfo", "张三            101XXXXXXXX广东省深圳市宝安区XXXX路XXXX花园X栋X座XXX        XXXXXXXXXXXXXXXXXX深圳市公安局宝安分局     XXXXXXXXXXXXXXXX                  ");
            response.put("zpWlt", generateMockWltPhoto());
            response.put("result", 0);

            sendJsonResponse(exchange, 200, response.toString());
        }
    }

    /**
     * 读取身份证物理卡号处理器
     */
    private class ReadCardNoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("收到请求: GET /readCardNo");
            
            JSONObject response = new JSONObject();
            response.put("cardNo", String.format("%016X", random.nextLong()));
            response.put("result", 0);

            sendJsonResponse(exchange, 200, response.toString());
        }
    }

    /**
     * 读取银行卡处理器
     */
    private class ReadBankCardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("收到请求: GET /readBankCard");
            
            JSONObject response = new JSONObject();
            response.put("uid", String.format("%08X", random.nextInt()));
            response.put("number", "6214" + String.format("%015d", Math.abs(random.nextLong()) % 1000000000000000L));
            response.put("expire", "25-12-31");
            response.put("owner", "");
            response.put("owner_id", "");
            response.put("result", 0);

            sendJsonResponse(exchange, 200, response.toString());
        }
    }

    /**
     * WLT照片解包处理器
     */
    private class WltUnpackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("收到请求: GET /wltUnpack");
            
            JSONObject response = new JSONObject();
            response.put("image", generateMockPhoto());
            response.put("result", 0);

            sendJsonResponse(exchange, 200, response.toString());
        }
    }

    /**
     * 生成模拟身份证号码
     */
    private String generateMockIdNumber() {
        String[] areaCodes = {"110101", "310101", "440301", "320102", "330102", "510104"};
        String areaCode = areaCodes[random.nextInt(areaCodes.length)];
        
        int year = 1970 + random.nextInt(35); // 1970-2004
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        
        String birthDate = String.format("%04d%02d%02d", year, month, day);
        String sequence = String.format("%03d", random.nextInt(1000));
        
        // 计算校验码
        String id17 = areaCode + birthDate + sequence;
        char checkCode = calculateIdCheckCode(id17);
        
        return id17 + checkCode;
    }

    /**
     * 计算身份证校验码
     */
    private char calculateIdCheckCode(String id17) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] checkCodes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += Character.getNumericValue(id17.charAt(i)) * weights[i];
        }
        
        return checkCodes[sum % 11];
    }

    /**
     * 生成模拟出生日期
     */
    private String generateMockBirthday() {
        int year = 1970 + random.nextInt(35);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return String.format("%04d%02d%02d", year, month, day);
    }

    /**
     * 生成模拟有效期开始日期
     */
    private String generateMockEffectDate() {
        int year = 2015 + random.nextInt(8);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return String.format("%04d%02d%02d", year, month, day);
    }

    /**
     * 生成模拟有效期结束日期
     */
    private String generateMockExpireDate() {
        int year = 2025 + random.nextInt(15);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return String.format("%04d%02d%02d", year, month, day);
    }

    /**
     * 生成模拟照片数据 (Base64编码的小图片)
     */
    private String generateMockPhoto() {
        // 生成一个简单的1x1像素PNG图片的Base64编码
        byte[] pngData = {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte)0x90, 0x77, 0x53, (byte)0xDE,
            0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
            0x08, (byte)0xD7, 0x63, (byte)0xF8, (byte)0x0F, 0x00, 0x00, 0x01,
            0x00, 0x01, (byte)0x01, 0x2A, 0x5A, (byte)0xD3, 0x00, 0x00,
            0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82
        };
        return Base64.getEncoder().encodeToString(pngData);
    }

    /**
     * 生成模拟WLT照片数据
     */
    private String generateMockWltPhoto() {
        byte[] mockData = new byte[1024]; // 1KB模拟数据
        random.nextBytes(mockData);
        return Base64.getEncoder().encodeToString(mockData);
    }

    /**
     * 发送JSON响应
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        // 设置CORS头部
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
        
        logger.debug("响应: {} {}", statusCode, jsonResponse.length() > 200 ? 
            jsonResponse.substring(0, 200) + "..." : jsonResponse);
    }

    /**
     * 主方法 - 用于独立运行模拟服务器
     */
    public static void main(String[] args) {
        MockIdCardReaderServer server = new MockIdCardReaderServer();
        
        try {
            server.start();
            
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("收到关闭信号，正在停止模拟服务器...");
                server.stop();
            }));
            
            // 保持服务器运行
            logger.info("模拟服务器正在运行，按 Ctrl+C 停止...");
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("模拟服务器启动失败", e);
            System.exit(1);
        }
    }
}
