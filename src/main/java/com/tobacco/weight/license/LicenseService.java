package com.tobacco.weight.license;

import com.tobacco.weight.database.DatabaseManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 许可服务
 * - 负责激活码校验与持久化
 * - 激活状态保存在表 system_config 中
 */
public class LicenseService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

    private static final String CONFIG_KEY_LICENSE_STATUS = "license_status";       // activated | none
    private static final String CONFIG_KEY_LICENSE_KEY_HASH = "license_key_hash";   // SHA-256
    private static final String CONFIG_KEY_LICENSE_ACTIVATED_AT = "license_activated_at";

    // 当无法从资源文件读取到激活码列表时，使用该默认激活码进行校验
    private static final String DEFAULT_UNIFIED_CODE = "YC-TWW-2025-ACTIVE";

    private static LicenseService instance;

    private final DatabaseManager databaseManager;
    private final Set<String> validUnifiedCodes;

    private LicenseService() {
        this.databaseManager = DatabaseManager.getInstance();
        this.validUnifiedCodes = loadUnifiedCodes();
        logger.info("许可服务初始化完成，有效激活码数量: {}", validUnifiedCodes.size());
    }

    public static synchronized LicenseService getInstance() {
        if (instance == null) {
            instance = new LicenseService();
        }
        return instance;
    }

    /**
     * 确保系统已激活。若未激活则弹出对话框要求输入激活码。
     * 返回值表示是否允许继续启动。
     */
    public boolean ensureLicensed(Window ownerWindow) {
        try {
            if (isLicensed()) {
                return true;
            }

            logger.warn("系统未激活，弹出激活对话框");
            LicenseActivationDialog dialog = new LicenseActivationDialog(ownerWindow);

            for (int attempt = 1; attempt <= 3; attempt++) {
                String code = dialog.showAndWaitForCode();
                if (code == null) {
                    // 用户取消
                    logger.info("用户取消激活，阻止启动");
                    return false;
                }

                if (validateAndSaveLicense(code)) {
                    showInfo("激活成功", "激活码验证通过，感谢使用。");
                    return true;
                } else {
                    showError("激活失败", "激活码无效，请核对后再试。（剩余尝试次数: " + (3 - attempt) + ")");
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("许可校验发生异常", e);
            showError("激活异常", "激活过程出现错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 是否已经激活
     */
    public boolean isLicensed() {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT config_value FROM system_config WHERE config_key = ?")) {
            ps.setString(1, CONFIG_KEY_LICENSE_STATUS);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString(1);
                    return Objects.equals("activated", status);
                }
            }
        } catch (SQLException e) {
            logger.error("读取激活状态失败", e);
        }
        return false;
    }

    /**
     * 校验激活码并持久化
     */
    public boolean validateAndSaveLicense(String licenseCode) {
        if (licenseCode == null || licenseCode.trim().isEmpty()) {
            return false;
        }

        String code = licenseCode.trim();
        boolean valid = isUnifiedCodeValid(code);

        if (!valid) {
            return false;
        }

        String hash = sha256(code);
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);

            upsertConfig(conn, CONFIG_KEY_LICENSE_KEY_HASH, hash, "激活码哈希");
            upsertConfig(conn, CONFIG_KEY_LICENSE_STATUS, "activated", "许可证状态");
            upsertConfig(conn, CONFIG_KEY_LICENSE_ACTIVATED_AT,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "激活时间");

            conn.commit();
            logger.info("激活信息已保存");
            return true;
        } catch (SQLException e) {
            logger.error("保存激活信息失败", e);
            return false;
        }
    }

    private void upsertConfig(Connection conn, String key, String value, String desc) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO system_config (config_key, config_value, description) VALUES (?, ?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, desc);
            ps.executeUpdate();
        }
    }

    /**
     * 统一分发激活码校验：
     * - 优先从资源文件 /licenses/activation_codes.txt 加载白名单
     * - 若未找到资源或为空，使用 DEFAULT_UNIFIED_CODE
     */
    private boolean isUnifiedCodeValid(String code) {
        return validUnifiedCodes.contains(code);
    }

    private Set<String> loadUnifiedCodes() {
        Set<String> codes = new HashSet<>();
        // 资源文件支持 # 作为注释行
        try (InputStream in = LicenseService.class.getResourceAsStream("/licenses/activation_codes.txt")) {
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        codes.add(line);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("读取激活码资源文件失败: {}", e.getMessage());
        }

        if (codes.isEmpty()) {
            codes.add(DEFAULT_UNIFIED_CODE);
        }
        return codes;
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}


