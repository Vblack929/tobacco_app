package com.tobacco.weight.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 硬件指纹生成器
 * 为动态绑定许可证系统生成唯一的设备标识
 */
public class HardwareFingerprint {

    private static final Logger logger = LoggerFactory.getLogger(HardwareFingerprint.class);

    /**
     * 生成当前设备的硬件指纹
     * @return 设备指纹字符串（SHA-256哈希值）
     */
    public static String generateFingerprint() {
        try {
            List<String> hardwareInfo = new ArrayList<>();
            
            // 获取CPU信息
            String cpuInfo = getCpuInfo();
            if (cpuInfo != null && !cpuInfo.trim().isEmpty()) {
                hardwareInfo.add("CPU:" + cpuInfo.trim());
            }
            
            // 获取主板信息
            String motherboardInfo = getMotherboardInfo();
            if (motherboardInfo != null && !motherboardInfo.trim().isEmpty()) {
                hardwareInfo.add("MB:" + motherboardInfo.trim());
            }
            
            // 获取硬盘序列号
            String diskInfo = getDiskInfo();
            if (diskInfo != null && !diskInfo.trim().isEmpty()) {
                hardwareInfo.add("DISK:" + diskInfo.trim());
            }
            
            // 获取网卡MAC地址
            String macInfo = getMacAddress();
            if (macInfo != null && !macInfo.trim().isEmpty()) {
                hardwareInfo.add("MAC:" + macInfo.trim());
            }
            
            // 如果没有获取到任何硬件信息，使用系统属性作为备选
            if (hardwareInfo.isEmpty()) {
                logger.warn("无法获取硬件信息，使用系统属性作为备选");
                hardwareInfo.add("OS:" + System.getProperty("os.name"));
                hardwareInfo.add("USER:" + System.getProperty("user.name"));
                hardwareInfo.add("ARCH:" + System.getProperty("os.arch"));
                hardwareInfo.add("JAVA:" + System.getProperty("java.version"));
            }
            
            // 排序确保一致性
            Collections.sort(hardwareInfo);
            
            // 组合所有信息
            String combined = String.join("|", hardwareInfo);
            logger.debug("硬件信息组合: {}", combined);
            
            // 生成SHA-256哈希
            return sha256(combined);
            
        } catch (Exception e) {
            logger.error("生成硬件指纹失败", e);
            // 返回一个基于系统属性的备用指纹
            return generateFallbackFingerprint();
        }
    }

    /**
     * 获取设备名称（用于显示）
     */
    public static String getDeviceName() {
        try {
            String computerName = System.getenv("COMPUTERNAME");
            if (computerName != null && !computerName.trim().isEmpty()) {
                return computerName.trim();
            }
            
            String hostname = System.getenv("HOSTNAME");
            if (hostname != null && !hostname.trim().isEmpty()) {
                return hostname.trim();
            }
            
            // 尝试通过命令获取
            if (isWindows()) {
                String result = executeCommand("hostname");
                if (result != null && !result.trim().isEmpty()) {
                    return result.trim();
                }
            }
            
            // 默认使用用户名
            return System.getProperty("user.name", "Unknown");
            
        } catch (Exception e) {
            logger.debug("获取设备名称失败: {}", e.getMessage());
            return "Unknown Device";
        }
    }

    /**
     * 获取CPU信息
     */
    private static String getCpuInfo() {
        try {
            if (isWindows()) {
                return executeCommand("wmic cpu get ProcessorId /value");
            } else {
                return executeCommand("cat /proc/cpuinfo | grep 'processor' | head -1");
            }
        } catch (Exception e) {
            logger.debug("获取CPU信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取主板信息
     */
    private static String getMotherboardInfo() {
        try {
            if (isWindows()) {
                return executeCommand("wmic baseboard get SerialNumber /value");
            } else {
                return executeCommand("sudo dmidecode -s baseboard-serial-number");
            }
        } catch (Exception e) {
            logger.debug("获取主板信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取硬盘信息
     */
    private static String getDiskInfo() {
        try {
            if (isWindows()) {
                return executeCommand("wmic diskdrive get SerialNumber /value");
            } else {
                return executeCommand("sudo hdparm -i /dev/sda | grep SerialNo");
            }
        } catch (Exception e) {
            logger.debug("获取硬盘信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取MAC地址
     */
    private static String getMacAddress() {
        try {
            // 优先使用Java API获取MAC地址
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            List<String> macAddresses = new ArrayList<>();
            
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                
                if (mac != null && mac.length == 6 && !ni.isLoopback() && !ni.isVirtual()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X", mac[i]));
                        if (i < mac.length - 1) {
                            sb.append("-");
                        }
                    }
                    macAddresses.add(sb.toString());
                }
            }
            
            if (!macAddresses.isEmpty()) {
                Collections.sort(macAddresses);
                return macAddresses.get(0); // 返回第一个MAC地址
            }
            
            // 备选方案：使用命令行
            if (isWindows()) {
                return executeCommand("getmac /fo csv /nh");
            } else {
                return executeCommand("cat /sys/class/net/*/address");
            }
            
        } catch (Exception e) {
            logger.debug("获取MAC地址失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 执行系统命令
     */
    private static String executeCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        if (isWindows()) {
            pb.command("cmd.exe", "/c", command);
        } else {
            pb.command("sh", "-c", command);
        }
        
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.length() > 0 && !trimmed.contains("=") && 
                    !trimmed.contains("ProcessorId") && !trimmed.contains("SerialNumber") && 
                    !trimmed.contains("Physical Address")) {
                    output.append(trimmed).append("\n");
                }
            }
        }
        
        process.waitFor();
        return output.toString().trim();
    }

    /**
     * 判断是否为Windows系统
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
    
    /**
     * 生成备用指纹（当无法获取硬件信息时）
     */
    private static String generateFallbackFingerprint() {
        try {
            String fallback = System.getProperty("os.name") + "|" +
                            System.getProperty("user.name") + "|" +
                            System.getProperty("os.arch") + "|" +
                            System.getProperty("java.version") + "|" +
                            System.currentTimeMillis();
            return sha256(fallback);
        } catch (Exception e) {
            logger.error("生成备用指纹失败", e);
            return "FALLBACK_FINGERPRINT_" + System.currentTimeMillis();
        }
    }

    /**
     * 计算SHA-256哈希值
     */
    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256算法不可用", e);
        }
    }

    /**
     * 格式化硬件指纹为可读形式
     */
    public static String formatFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.length() < 16) {
            return fingerprint;
        }
        
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < fingerprint.length(); i += 4) {
            if (i > 0) {
                formatted.append("-");
            }
            int endIndex = Math.min(i + 4, fingerprint.length());
            formatted.append(fingerprint.substring(i, endIndex));
        }
        
        return formatted.toString();
    }
    
    /**
     * 获取硬件指纹的可读格式（用于显示给用户）
     */
    public static String getReadableFingerprint() {
        String fingerprint = generateFingerprint();
        return formatFingerprint(fingerprint);
    }
    
    /**
     * 命令行工具入口
     */
    public static void main(String[] args) {
        System.out.println("设备硬件指纹生成器");
        System.out.println("==================");
        
        String deviceName = getDeviceName();
        String fingerprint = generateFingerprint();
        String readableFingerprint = formatFingerprint(fingerprint);
        
        System.out.println("设备名称: " + deviceName);
        System.out.println("硬件指纹: " + fingerprint);
        System.out.println("可读格式: " + readableFingerprint);
        System.out.println();
        System.out.println("请将此硬件指纹信息提供给软件供应商进行设备绑定。");
    }
}