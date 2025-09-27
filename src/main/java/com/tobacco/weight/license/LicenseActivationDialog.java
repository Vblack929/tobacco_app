package com.tobacco.weight.license;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 许可证激活对话框
 * 提供用户友好的激活界面
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
        setHeaderText("请输入您的许可证密钥以完成激活");
        setResizable(false);

        createContent();
        setupEventHandlers();

        setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return licenseIdField.getText().trim();
            }
            return null;
        });

        updateDeviceInfo();
    }

    private void createContent() {
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER_LEFT);

        VBox deviceInfoSection = createDeviceInfoSection();
        VBox licenseInputSection = createLicenseInputSection();
        HBox statusSection = createStatusSection();
        HBox buttonSection = createButtonSection();

        mainLayout.getChildren().addAll(
                deviceInfoSection,
                new Separator(),
                licenseInputSection,
                statusSection,
                buttonSection);

        getDialogPane().setContent(mainLayout);
        getDialogPane().getButtonTypes().clear();
    }

    private VBox createDeviceInfoSection() {
        VBox section = new VBox(8);

        Label titleLabel = new Label("设备信息");
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));

        deviceInfoLabel = new Label();
        deviceInfoLabel.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 8; -fx-border-color: #ddd; -fx-border-radius: 4;");
        deviceInfoLabel.setWrapText(true);
        deviceInfoLabel.setPrefWidth(400);

        Label infoNote = new Label("请将上述设备信息提供给供应商以便获取许可证密钥");
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
        licenseIdField.setPromptText("请输入许可证密钥，格式示例：YC-TWW-2025-XXXX-XXXX-XXXX 或 YC-TWW-DEV0-XXXX-XXXX-XXXX");
        licenseIdField.setPrefWidth(400);
        licenseIdField.setStyle("-fx-font-family: monospace;");

        licenseIdField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText == null) {
                return;
            }

            String upperText = newText.toUpperCase();
            String cleaned = upperText.replaceAll("[^A-Z0-9]", "");
            if (cleaned.length() > 21) {
                cleaned = cleaned.substring(0, 21);
            }

            int[] groupLengths = {2, 3, 4, 4, 4, 4};
            StringBuilder formatted = new StringBuilder();
            int index = 0;
            for (int i = 0; i < groupLengths.length && index < cleaned.length(); i++) {
                int len = groupLengths[i];
                int end = Math.min(index + len, cleaned.length());
                formatted.append(cleaned.substring(index, end));
                index = end;
                if (index < cleaned.length() && i < groupLengths.length - 1) {
                    formatted.append('-');
                }
            }

            String formattedText = formatted.toString();
            long requestedHyphenCount = upperText.chars().filter(ch -> ch == '-').count();
            long currentHyphenCount = formattedText.chars().filter(ch -> ch == '-').count();
            if (requestedHyphenCount > currentHyphenCount
                    && currentHyphenCount < 5
                    && cleaned.length() > 0
                    && !formattedText.endsWith("-")) {
                formattedText = formattedText + "-";
            }

            if (!formattedText.equals(newText)) {
                licenseIdField.setText(formattedText);
                licenseIdField.positionCaret(formattedText.length());
            }
        });

        section.getChildren().addAll(titleLabel, licenseIdField);
        return section;
    }

    private HBox createStatusSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_LEFT);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(24, 24);

        statusLabel = new Label("请输入许可证密钥并点击激活");
        statusLabel.setStyle("-fx-text-fill: #555;");

        section.getChildren().addAll(progressIndicator, statusLabel);
        return section;
    }

    private HBox createButtonSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_RIGHT);

        activateButton = new Button("激活");
        cancelButton = new Button("取消");
        activateButton.setDisable(true);

        activateButton.setOnAction(e -> performActivation());
        cancelButton.setOnAction(e -> cancelActivation());

        section.getChildren().addAll(activateButton, cancelButton);
        return section;
    }

    private void setupEventHandlers() {
        licenseIdField.textProperty().addListener((obs, oldValue, newValue) -> updateActivateButtonState());
        getDialogPane().setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    if (!activateButton.isDisable()) {
                        performActivation();
                    }
                    break;
                case ESCAPE:
                    cancelActivation();
                    break;
                default:
                    break;
            }
        });
    }

    private void cancelActivation() {
        setResult(null);
        close();
    }

    private void updateDeviceInfo() {
        try {
            String deviceName = HardwareFingerprint.getDeviceName();
            String fingerprint = HardwareFingerprint.getReadableFingerprint();

            String deviceInfo = String.format("设备名称: %s%n设备指纹: %s", deviceName, fingerprint);
            deviceInfoLabel.setText(deviceInfo);
        } catch (Exception e) {
            logger.error("获取设备信息失败", e);
            deviceInfoLabel.setText("无法获取设备信息");
        }
    }

    private void updateActivateButtonState() {
        String licenseId = licenseIdField.getText().trim();
        boolean isValidFormat = licenseId.matches("^YC-TWW-(2025|DEV0)-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$");
        activateButton.setDisable(!isValidFormat);
    }

    private void performActivation() {
        String licenseId = licenseIdField.getText().trim();

        if (licenseId.isEmpty()) {
            showStatus("请输入许可证密钥", false);
            return;
        }

        setActivationInProgress(true);
        showStatus("正在验证许可证...", true);

        Thread activationThread = new Thread(() -> {
            try {
                boolean success = HybridLicenseService.getInstance().activateLicense(licenseId);

                Platform.runLater(() -> {
                    setActivationInProgress(false);

                    if (success) {
                        showStatus("许可证激活成功！", true);

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
                    showStatus("激活过程中发生错误: " + e.getMessage(), false);
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
        statusLabel.setStyle(isSuccess ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #d32f2f;");
    }

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

    public static void showDeviceFingerprint(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("设备指纹信息");
        alert.setHeaderText("当前设备指纹");

        String deviceName = HardwareFingerprint.getDeviceName();
        String fingerprint = HardwareFingerprint.getReadableFingerprint();

        String content = String.format("设备名称: %s%n设备指纹: %s%n%n请将此信息提供给软件供应商以获取许可证密钥。",
                deviceName, fingerprint);

        alert.setContentText(content);
        alert.showAndWait();
    }
}


