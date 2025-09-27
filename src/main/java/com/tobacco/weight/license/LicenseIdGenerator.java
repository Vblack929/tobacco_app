package com.tobacco.weight.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * 动态绑定许可证ID生成器
 * 生成类似Typora的许可证密钥，支持不同授权类型
 */
public class LicenseIdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LicenseIdGenerator.class);

    private static final String LICENSE_PREFIX = "YC-TWW";
    private static final String CUSTOMER_TIER_CODE = LicenseType.CUSTOMER_LIMITED.getTierCode();
    private static final String DEVELOPER_TIER_CODE = LicenseType.DEVELOPER_UNLIMITED.getTierCode();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private LicenseIdGenerator() {
        // 工具类不需要实例化
    }

    /**
     * 根据最大设备数量推断许可证类型并生成ID
     */
    public static String generateLicenseId(String customerName, int maxDevices, int validDays) {
        LicenseType licenseType = (maxDevices <= 0 || maxDevices >= Integer.MAX_VALUE)
                ? LicenseType.DEVELOPER_UNLIMITED
                : LicenseType.CUSTOMER_LIMITED;
        return generateLicenseId(customerName, licenseType, validDays);
    }

    /**
     * 指定许可证类型生成ID
     */
    public static String generateLicenseId(String customerName, LicenseType licenseType, int validDays) {
        int resolvedMaxDevices = licenseType.isUnlimitedDevices()
                ? Integer.MAX_VALUE
                : licenseType.getDefaultMaxDevices();
        return generateLicenseId(customerName, licenseType, resolvedMaxDevices, validDays);
    }

    private static String generateLicenseId(String customerName, LicenseType licenseType, int maxDevices, int validDays) {
        try {
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

            String licenseData = String.format("%s|%s|%s|%d|%d|%s",
                    customerName != null ? customerName : "DEFAULT",
                    licenseType.name(),
                    timestamp,
                    maxDevices,
                    validDays,
                    uuid);

            String checksum = generateChecksum(licenseData);
            String tierCode = licenseType == LicenseType.DEVELOPER_UNLIMITED ? DEVELOPER_TIER_CODE : CUSTOMER_TIER_CODE;

            String licenseId = String.format("%s-%s-%s-%s-%s",
                    LICENSE_PREFIX,
                    tierCode,
                    uuid.substring(0, 4),
                    uuid.substring(4, 8),
                    checksum.substring(0, 4).toUpperCase());

            logger.info("生成许可证ID: {}, 客户: {}, 类型: {}, 最大设备数: {}, 有效期: {}",
                    licenseId,
                    customerName,
                    licenseType,
                    licenseType.isUnlimitedDevices() ? "不限制" : maxDevices,
                    validDays > 0 ? validDays + "天" : "永久");

            return licenseId;
        } catch (Exception e) {
            logger.error("生成许可证ID失败", e);
            throw new RuntimeException("生成许可证ID失败", e);
        }
    }

    public static boolean isValidFormat(String licenseId) {
        if (licenseId == null || licenseId.trim().isEmpty()) {
            return false;
        }
        return licenseId.toUpperCase().matches("^YC-TWW-(2025|DEV0)-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");
    }

    public static boolean validateLicenseId(String licenseId) {
        return isValidFormat(licenseId);
    }

    public static LicenseInfo parseLicenseId(String licenseId) {
        if (!isValidFormat(licenseId)) {
            return null;
        }

        try {
            String[] parts = licenseId.split("-");
            String tierCode = parts[2];
            String uuid = parts[3] + parts[4].substring(0, 4);

            LicenseType licenseType = LicenseType.fromTierCode(tierCode);
            int resolvedMaxDevices = licenseType.isUnlimitedDevices()
                    ? Integer.MAX_VALUE
                    : licenseType.getDefaultMaxDevices();

            LicenseInfo info = new LicenseInfo(licenseId,
                    licenseType == LicenseType.DEVELOPER_UNLIMITED ? "开发者授权" : "甲方授权",
                    resolvedMaxDevices,
                    0,
                    licenseType);
            info.setUuid(uuid);
            info.setCreatedAt(LocalDateTime.now());
            info.setActive(true);

            return info;
        } catch (Exception e) {
            logger.error("解析许可证ID失败: {}", licenseId, e);
            return null;
        }
    }

    private static String generateChecksum(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash).replaceAll("[^A-Z0-9]", "").substring(0, 8);
        } catch (Exception e) {
            throw new RuntimeException("生成校验码失败", e);
        }
    }

    public static String generateTestLicenseId() {
        return generateLicenseId("TEST_CUSTOMER", LicenseType.CUSTOMER_LIMITED, 0);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("动态绑定许可证ID生成器");
            System.out.println("用法:");
            System.out.println("  --generate <客户名称> <最大设备数> <有效天数>");
            System.out.println("  --generate-dev <客户名称> <有效天数>");
            System.out.println("  --test  生成测试许可证");
            System.out.println("  --validate <许可证ID>  验证许可证格式");
            System.out.println();
            System.out.println("示例:");
            System.out.println("  java LicenseIdGenerator --generate \"甲方公司\" 2 365");
            System.out.println("  java LicenseIdGenerator --generate-dev \"开发者团队\" 0");
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

            case "--generate-dev":
                if (args.length >= 3) {
                    String customerName = args[1];
                    int validDays = Integer.parseInt(args[2]);
                    String licenseId = generateLicenseId(customerName, LicenseType.DEVELOPER_UNLIMITED, validDays);
                    System.out.println("生成的开发者许可证ID: " + licenseId);
                } else {
                    System.out.println("参数不足，需要: 客户名称 有效天数");
                }
                break;

            case "--test":
                System.out.println("测试许可证ID: " + generateTestLicenseId());
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
                            System.out.println("类型: " + info.getLicenseType());
                            System.out.println("最大设备数: " + (info.isUnlimitedDevices() ? "不限制" : info.getMaxDevices()));
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