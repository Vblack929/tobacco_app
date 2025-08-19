package com.tobacco.weight.test;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * 简化版身份证读卡器模拟服务器
 * 不依赖复杂的日志系统，方便测试
 */
public class SimpleMockServer {

    private static final int PORT = 7846;
    private HttpServer server;
    private Random random = new Random();
    
    // 模拟数据
    private static final String[] SAMPLE_NAMES = {
        "伍丰虎", "伍伟亮", "伍伟林"
    };

    private static final String[] SAMPLE_ID_NUMBERS = {
        "430181197910160317",
        "430123196909033478",
        "430181198710260377"
    };
    
    private static final String[] SAMPLE_ADDRESSES = {
        "广东省深圳市南山区科技园南区XX路XX号XX小区XX栋XX室",
        "北京市朝阳区建国门外大街XX号XX大厦XX层",
        "上海市浦东新区陆家嘴金融贸易区XX路XX号"
    };
    
    private static final String[] SAMPLE_ISSUERS = {
        "深圳市公安局南山分局",
        "北京市公安局朝阳分局", 
        "上海市公安局浦东分局"
    };

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // 注册API端点
        server.createContext("/getReaderInfo", new GetReaderInfoHandler());
        server.createContext("/readSamID", new ReadSamIDHandler());
        server.createContext("/api/readCard", new ReadCardHandler());
        server.createContext("/readCardNo", new ReadCardNoHandler());
        
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
        
        System.out.println("🚀 身份证读卡器模拟服务器已启动！");
        System.out.println("📍 监听端口: " + PORT);
        System.out.println("🌐 可用的API端点:");
        System.out.println("   GET http://127.0.0.1:" + PORT + "/getReaderInfo");
        System.out.println("   GET http://127.0.0.1:" + PORT + "/api/readCard?utf8=1");
        System.out.println("   GET http://127.0.0.1:" + PORT + "/readSamID");
        System.out.println("   GET http://127.0.0.1:" + PORT + "/readCardNo");
        System.out.println();
        System.out.println("🧪 测试命令:");
        System.out.println("   curl http://127.0.0.1:" + PORT + "/getReaderInfo");
        System.out.println("   curl http://127.0.0.1:" + PORT + "/api/readCard?utf8=1");
        System.out.println();
        System.out.println("⏹️  按 Ctrl+C 停止服务器");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("✅ 模拟服务器已停止");
        }
    }

    /**
     * 获取读卡器信息
     */
    private class GetReaderInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("📥 GET /getReaderInfo");
            
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
     * 读取SAM ID
     */
    private class ReadSamIDHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("📥 GET /readSamID");
            
            JSONObject response = new JSONObject();
            response.put("samid", "05.13-20220315-0003607681-1474081524");
            response.put("sn", "MOCK-" + String.format("%08X", random.nextInt()));
            response.put("result", 0);

            sendJsonResponse(exchange, 200, response.toString());
        }
    }

    /**
     * 读取身份证
     */
    private class ReadCardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("📥 GET /api/readCard");
            
            // 模拟读卡延迟
            try {
                Thread.sleep(800 + random.nextInt(500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 使用真实的样本数据（姓名和身份证号对应）
            int sampleIndex = random.nextInt(SAMPLE_NAMES.length);
            String name = SAMPLE_NAMES[sampleIndex];
            String idNumber = SAMPLE_ID_NUMBERS[sampleIndex];
            String address = SAMPLE_ADDRESSES[random.nextInt(SAMPLE_ADDRESSES.length)];
            String issuer = SAMPLE_ISSUERS[random.nextInt(SAMPLE_ISSUERS.length)];
            
            JSONObject resultContent = new JSONObject();
            resultContent.put("name", name);
            resultContent.put("idNum", idNumber);
            resultContent.put("gender", extractGenderFromId(idNumber));
            resultContent.put("nation", String.format("%02d", 1 + random.nextInt(10)));
            resultContent.put("birthday", extractBirthdayFromId(idNumber));
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
            System.out.println("📤 返回模拟身份证: " + name + " (" + 
                idNumber.substring(0, 6) + "****" + idNumber.substring(14) + ")");
        }
    }

    /**
     * 读取卡号
     */
    private class ReadCardNoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("📥 GET /readCardNo");
            
            JSONObject response = new JSONObject();
            response.put("cardNo", String.format("%016X", random.nextLong()));
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
        
        int year = 1980 + random.nextInt(30);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        
        String birthDate = String.format("%04d%02d%02d", year, month, day);
        String sequence = String.format("%03d", random.nextInt(1000));
        
        String id17 = areaCode + birthDate + sequence;
        char checkCode = calculateIdCheckCode(id17);
        
        return id17 + checkCode;
    }

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
     * 从身份证号提取性别
     */
    private String extractGenderFromId(String idNumber) {
        if (idNumber != null && idNumber.length() >= 17) {
            // 身份证号第17位：奇数=男性(1)，偶数=女性(2)
            int genderDigit = Character.getNumericValue(idNumber.charAt(16));
            return (genderDigit % 2 == 1) ? "1" : "2";
        }
        // 备用：随机性别
        return random.nextBoolean() ? "1" : "2";
    }

    /**
     * 从身份证号提取出生日期
     */
    private String extractBirthdayFromId(String idNumber) {
        if (idNumber != null && idNumber.length() >= 14) {
            // 身份证号第7-14位是出生日期YYYYMMDD
            return idNumber.substring(6, 14);
        }
        // 备用：生成模拟出生日期
        int year = 1980 + random.nextInt(30);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return String.format("%04d%02d%02d", year, month, day);
    }

    private String generateMockEffectDate() {
        int year = 2018 + random.nextInt(5);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return String.format("%04d%02d%02d", year, month, day);
    }

    private String generateMockExpireDate() {
        int year = 2028 + random.nextInt(10);
        int month = 1 + random.nextInt(12);
        int day = 1 + random.nextInt(28);
        return String.format("%04d%02d%02d", year, month, day);
    }

    private String generateMockPhoto() {
        // 1x1像素PNG图片
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

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    public static void main(String[] args) {
        SimpleMockServer server = new SimpleMockServer();
        
        try {
            server.start();
            
            // 关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 收到关闭信号...");
                server.stop();
            }));
            
            // 保持运行
            Thread.currentThread().join();
            
        } catch (Exception e) {
            System.err.println("❌ 服务器启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
