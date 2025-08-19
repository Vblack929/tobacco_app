package com.tobacco.weight.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * 动态绑定许可证ID生成器
 * 生成类似Typora的许可证密钥，支持动态设备绑定
 */
public class LicenseIdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LicenseIdGenerator.class);

    // 许可证前缀
    private static final String LICENSE_PREFIX = "YC-TWW";

    // 许可证版本
    private static final String LICENSE_VERSION = "2025";

    /**
     * 生成新的许可证ID
     * 格式: YC-TWW-2025-XXXX-XXXX-XXXX
     * 
     * @param customerName 客户名称（可选）
     * @param maxDevices   最大设备数量
     * @param validDays    有效天数（0表示永久有效）
     * @return 生成的许可证ID
     */
    public static String generateLicenseId(String customerName, int maxDevices, int validDays) {
        try {
            // 生成唯一标识符
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

            // 生成时间戳
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 构建许可证数据
            String licenseData = String.format("%s|%s|%d|%d|%s",
                    customerName != null ? customerName : "DEFAULT",
                    timestamp,
                    maxDevices,
                    validDays,
                    uuid);

            // 生成校验码
            String checksum = generateChecksum(licenseData);

            // 格式化许可证ID
            String licenseId = String.format("%s-%s-%s-%s-%s",
                    LICENSE_PREFIX,
                    LICENSE_VERSION,
                    uuid.substring(0, 4),
                    uuid.substring(4, 8),
                    checksum.substring(0, 4).toUpperCase());

            logger.info("生成许可证ID: {}, 客户: {}, 最大设备数: {}, 有效期: {}天",
                    licenseId, customerName, maxDevices, validDays > 0 ? validDays : "永久");

            return licenseId;

        } catch (Exception e) {
            logger.error("生成许可证ID失败", e);
            throw new RuntimeException("生成许可证ID失败", e);
        }
    }

    /**
     * 验证许可证ID格式
     * 
     * @param licenseId 许可证ID
     * @return 是否为有效格式
     */
    public static boolean isValidFormat(String licenseId) {
        if (licenseId == null || licenseId.trim().isEmpty()) {
            return false;
        }

        // 检查格式: YC-TWW-2025-XXXX-XXXX-XXXX
        String pattern = "^YC-TWW-2025-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$";
        return licenseId.matches(pattern);
    }

    /**
     * 验证许可证ID
     * 
     * @param licenseId 许可证ID
     * @return 是否有效
     */
    public static boolean validateLicenseId(String licenseId) {
        return isValidFormat(licenseId);
    }

    /**
     * 解析许可证ID中的信息
     * 
     * @param licenseId 许可证ID
     * @return 许可证信息，如果解析失败返回null
     */
    public static LicenseInfo parseLicenseId(String licenseId) {
        if (!isValidFormat(licenseId)) {
            return null;
        }

        try {
            // 提取UUID部分
            String[] parts = licenseId.split("-");
            String uuid = parts[3] + parts[4].substring(0, 4); // 前8位是UUID，后4位是校验码

            // 为测试许可证设置默认值
            LicenseInfo info = new LicenseInfo();
            info.setLicenseId(licenseId);
            info.setUuid(uuid);
            info.setCustomerName("测试客户");
            info.setMaxDevices(2); // 设置最大设备数为2
            info.setValidDays(0); // 永久有效
            info.setCreatedAt(LocalDateTime.now());
            info.setActive(true);

            return info;

        } catch (Exception e) {
            logger.error("解析许可证ID失败: {}", licenseId, e);
            return null;
        }
    }

    /**
     * 生成校验码
     */
    private static String generateChecksum(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash).replaceAll("[^A-Z0-9]", "").substring(0, 8);
        } catch (Exception e) {
            throw new RuntimeException("生成校验码失败", e);
        }
    }

    /**
     * 生成用于测试的许可证ID
     */
    public static String generateTestLicenseId() {
        return generateLicenseId("TEST_CUSTOMER", 2, 0);
    }

    /**
     * 命令行工具入口
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("动态绑定许可证ID生成器");
            System.out.println("用法:");
            System.out.println("  --generate <客户名称> <最大设备数> <有效天数>");
            System.out.println("  --test  生成测试许可证");
            System.out.println("  --validate <许可证ID>  验证许可证格式");
            System.out.println();
            System.out.println("示例:");
            System.out.println("  java LicenseIdGenerator --generate \"甲方公司\" 2 365");
            System.out.println("  java LicenseIdGenerator --test");
            return;
        }

        String command = args[0];

        switch (command) {
            case "--generate":
                if (args.length >= 4) {
                    String customerName = args[1];
                    int maxDevices = Integer.parseInt(args[2]);
                    int validDays = Integer.parseInt(args[3]);
                    String licenseId = generateLicenseId(customerName, maxDevices, validDays);
                    System.out.println("生成的许可证ID: " + licenseId);
                } else {
                    System.out.println("参数不足，需要: 客户名称 最大设备数 有效天数");
                }
                break;

            case "--test":
                String testLicenseId = generateTestLicenseId();
                System.out.println("测试许可证ID: " + testLicenseId);
                break;

            case "--validate":
                if (args.length >= 2) {
                    String licenseId = args[1];
                    boolean valid = isValidFormat(licenseId);
                    System.out.println("许可证ID: " + licenseId);
                    System.out.println("格式验证: " + (valid ? "有效" : "无效"));

                    if (valid) {
                        LicenseInfo info = parseLicenseId(licenseId);
                        if (info != null) {
                            System.out.println("UUID: " + info.getUuid());
                        }
                    }
                } else {
                    System.out.println("请提供要验证的许可证ID");
                }
                break;

            default:
                System.out.println("未知命令: " + command);
                break;
        }
    }
}