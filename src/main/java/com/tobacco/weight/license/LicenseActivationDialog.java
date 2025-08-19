package com.tobacco.weight.license;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 许可证激活对话框
 * 提供用户友好的许可证激活界面
 */
public class LicenseActivationDialog extends Dialog<String> {

    private static final Logger logger = LoggerFactory.getLogger(LicenseActivationDialog.class);

    private TextField licenseIdField;
    private Label statusLabel;
    private Label deviceInfoLabel;
    private Button activateButton;
    private Button cancelButton;
    private ProgressIndicator progressIndicator;

    public LicenseActivationDialog(Stage owner) {
        if (owner != null && owner.getScene() != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);

        setTitle("烟叶称重系统 - 许可证激活");
        setHeaderText("请输入您的许可证密钥进行激活");
        setResizable(false);

        createContent();
        setupEventHandlers();

        // 设置结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return licenseIdField.getText().trim();
            }
            return null;
        });

        // 显示设备信息
        updateDeviceInfo();
    }

    private void createContent() {
        // 创建主布局
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER_LEFT);

        // 设备信息区域
        VBox deviceInfoSection = createDeviceInfoSection();

        // 许可证输入区域
        VBox licenseInputSection = createLicenseInputSection();

        // 状态显示区域
        HBox statusSection = createStatusSection();

        // 按钮区域
        HBox buttonSection = createButtonSection();

        mainLayout.getChildren().addAll(
                deviceInfoSection,
                new Separator(),
                licenseInputSection,
                statusSection,
                buttonSection);

        getDialogPane().setContent(mainLayout);
        getDialogPane().getButtonTypes().clear(); // 清除默认按钮
    }

    private VBox createDeviceInfoSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("设备信息");
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));

        deviceInfoLabel = new Label();
        deviceInfoLabel.setStyle(
                "-fx-background-color: #f5f5f5; -fx-padding: 8; -fx-border-color: #ddd; -fx-border-radius: 4;");
        deviceInfoLabel.setWrapText(true);
        deviceInfoLabel.setPrefWidth(400);

        Label infoNote = new Label("请将上述设备信息提供给软件供应商以获取许可证密钥");
        infoNote.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");
        infoNote.setWrapText(true);

        section.getChildren().addAll(titleLabel, deviceInfoLabel, infoNote);
        return section;
    }

    private VBox createLicenseInputSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("许可证密钥");
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));

        licenseIdField = new TextField();
        licenseIdField.setPromptText("请输入许可证密钥，格式如：YC-TWW-2025-XXXX-XXXX-XXXX");
        licenseIdField.setPrefWidth(400);
        licenseIdField.setStyle("-fx-font-family: monospace;");

        // 自动格式化输入
        licenseIdField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null) {
                // 移除所有非字母数字字符，然后重新格式化
                String cleaned = newText.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                if (cleaned.length() > 21) {
                    cleaned = cleaned.substring(0, 21);
                }

                StringBuilder formatted = new StringBuilder();
                // 格式: YC-TWW-2025-XXXX-XXXX-XXXX
                // 位置: 0123456789012345678901
                for (int i = 0; i < cleaned.length(); i++) {
                    // 在位置 2, 5, 9, 13, 17 后添加连字符
                    if (i == 2 || i == 5 || i == 9 || i == 13 || i == 17) {
                        formatted.append("-");
                    }
                    formatted.append(cleaned.charAt(i));
                }

                if (!formatted.toString().equals(newText)) {
                    Platform.runLater(() -> {
                        licenseIdField.setText(formatted.toString());
                        licenseIdField.positionCaret(formatted.length());
                    });
                }
            }

            // 更新激活按钮状态
            updateActivateButtonState();
        });

        Label formatNote = new Label("许可证密钥格式：YC-TWW-2025-XXXX-XXXX-XXXX");
        formatNote.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        section.getChildren().addAll(titleLabel, licenseIdField, formatNote);
        return section;
    }

    private HBox createStatusSection() {
        HBox section = new HBox(8);
        section.setAlignment(Pos.CENTER_LEFT);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(20, 20);
        progressIndicator.setVisible(false);

        statusLabel = new Label();
        statusLabel.setWrapText(true);

        section.getChildren().addAll(progressIndicator, statusLabel);
        return section;
    }

    private HBox createButtonSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_RIGHT);

        cancelButton = new Button("取消");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(e -> {
            setResult(null);
            close();
        });

        activateButton = new Button("激活");
        activateButton.setPrefWidth(80);
        activateButton.setDefaultButton(true);
        activateButton.setDisable(true);

        section.getChildren().addAll(cancelButton, activateButton);
        return section;
    }

    private void setupEventHandlers() {
        activateButton.setOnAction(e -> performActivation());

        // 回车键激活
        licenseIdField.setOnAction(e -> {
            if (!activateButton.isDisabled()) {
                performActivation();
            }
        });
    }

    private void updateDeviceInfo() {
        try {
            String deviceName = HardwareFingerprint.getDeviceName();
            String fingerprint = HardwareFingerprint.getReadableFingerprint();

            String deviceInfo = String.format(
                    "设备名称: %s\n" +
                            "设备指纹: %s",
                    deviceName, fingerprint);

            deviceInfoLabel.setText(deviceInfo);

        } catch (Exception e) {
            logger.error("获取设备信息失败", e);
            deviceInfoLabel.setText("无法获取设备信息");
        }
    }

    private void updateActivateButtonState() {
        String licenseId = licenseIdField.getText().trim();
        boolean isValidFormat = licenseId.matches("^YC-TWW-2025-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");
        activateButton.setDisable(!isValidFormat);
    }

    private void performActivation() {
        String licenseId = licenseIdField.getText().trim();

        if (licenseId.isEmpty()) {
            showStatus("请输入许可证密钥", false);
            return;
        }

        // 显示进度指示器
        setActivationInProgress(true);
        showStatus("正在验证许可证...", true);

        // 在后台线程中执行激活
        Thread activationThread = new Thread(() -> {
            try {
                boolean success = LicenseService.getInstance().activateLicense(licenseId);

                Platform.runLater(() -> {
                    setActivationInProgress(false);

                    if (success) {
                        showStatus("许可证激活成功！", true);

                        // 延迟关闭对话框
                        Timeline timeline = new Timeline(
                                new KeyFrame(Duration.seconds(1), e -> {
                                    setResult(licenseId);
                                    close();
                                }));
                        timeline.play();

                    } else {
                        showStatus("许可证激活失败，请检查密钥是否正确或联系技术支持", false);
                    }
                });

            } catch (Exception e) {
                logger.error("许可证激活异常", e);

                Platform.runLater(() -> {
                    setActivationInProgress(false);
                    showStatus("激活过程中发生错误：" + e.getMessage(), false);
                });
            }
        });

        activationThread.setDaemon(true);
        activationThread.start();
    }

    private void setActivationInProgress(boolean inProgress) {
        progressIndicator.setVisible(inProgress);
        activateButton.setDisable(inProgress);
        licenseIdField.setDisable(inProgress);
    }

    private void showStatus(String message, boolean isSuccess) {
        statusLabel.setText(message);

        if (isSuccess) {
            statusLabel.setStyle("-fx-text-fill: #2e7d32;"); // 绿色
        } else {
            statusLabel.setStyle("-fx-text-fill: #d32f2f;"); // 红色
        }
    }

    /**
     * 显示许可证信息对话框
     */
    public static void showLicenseInfo(Stage owner) {
        LicenseService licenseService = LicenseService.getInstance();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("许可证信息");
        alert.setHeaderText("当前许可证状态");

        String licenseInfo = licenseService.getLicenseStatusInfo();
        String deviceInfo = licenseService.getDeviceBindingInfo();

        alert.setContentText(licenseInfo + "\n\n" + deviceInfo);
        alert.showAndWait();
    }

    /**
     * 显示设备指纹信息对话框
     */
    public static void showDeviceFingerprint(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("设备指纹信息");
        alert.setHeaderText("当前设备硬件指纹");

        String deviceName = HardwareFingerprint.getDeviceName();
        String fingerprint = HardwareFingerprint.getReadableFingerprint();

        String content = String.format(
                "设备名称: %s\n" +
                        "设备指纹: %s\n\n" +
                        "请将此信息提供给软件供应商以获取许可证密钥。",
                deviceName, fingerprint);

        alert.setContentText(content);
        alert.showAndWait();
    }
}