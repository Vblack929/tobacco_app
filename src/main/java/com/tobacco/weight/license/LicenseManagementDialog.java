package com.tobacco.weight.license;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 许可证管理对话框
 * 提供设备绑定管理、许可证信息查看等功能
 */
public class LicenseManagementDialog extends Dialog<Void> {

    private static final Logger logger = LoggerFactory.getLogger(LicenseManagementDialog.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private TableView<DeviceBindingRow> deviceTable;
    private ObservableList<DeviceBindingRow> deviceData;
    private Label licenseInfoLabel;
    private Label statusLabel;
    private Button refreshButton;
    private Button unbindButton;
    private Button closeButton;
    
    public LicenseManagementDialog(Stage owner) {
        if (owner != null && owner.getScene() != null) {
            initOwner(owner);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        
        setTitle("许可证管理");
        setHeaderText("许可证信息和设备管理");
        setResizable(true);
        
        createContent();
        setupEventHandlers();
        refreshData();
    }
    
    private void createContent() {
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setPrefWidth(700);
        mainLayout.setPrefHeight(500);
        
        // 许可证信息区域
        VBox licenseInfoSection = createLicenseInfoSection();
        
        // 设备管理区域
        VBox deviceManagementSection = createDeviceManagementSection();
        
        // 状态和按钮区域
        HBox bottomSection = createBottomSection();
        
        mainLayout.getChildren().addAll(
                licenseInfoSection,
                new Separator(),
                deviceManagementSection,
                bottomSection
        );
        
        getDialogPane().setContent(mainLayout);
        getDialogPane().getButtonTypes().clear();
    }
    
    private VBox createLicenseInfoSection() {
        VBox section = new VBox(8);
        
        Label titleLabel = new Label("许可证信息");
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
        
        licenseInfoLabel = new Label();
        licenseInfoLabel.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 10; -fx-border-color: #ddd; -fx-border-radius: 4;");
        licenseInfoLabel.setWrapText(true);
        
        section.getChildren().addAll(titleLabel, licenseInfoLabel);
        return section;
    }
    
    private VBox createDeviceManagementSection() {
        VBox section = new VBox(8);
        
        Label titleLabel = new Label("已绑定设备");
        titleLabel.setFont(Font.font(null, FontWeight.BOLD, 14));
        
        // 创建设备表格
        deviceTable = createDeviceTable();
        
        // 设备操作按钮
        HBox deviceButtonsBox = new HBox(10);
        deviceButtonsBox.setAlignment(Pos.CENTER_LEFT);
        
        refreshButton = new Button("刷新");
        refreshButton.setPrefWidth(80);
        
        unbindButton = new Button("解绑设备");
        unbindButton.setPrefWidth(80);
        unbindButton.setDisable(true);
        
        Button showFingerprintButton = new Button("查看当前设备指纹");
        showFingerprintButton.setPrefWidth(140);
        showFingerprintButton.setOnAction(e -> showCurrentDeviceFingerprint());
        
        deviceButtonsBox.getChildren().addAll(refreshButton, unbindButton, showFingerprintButton);
        
        section.getChildren().addAll(titleLabel, deviceTable, deviceButtonsBox);
        VBox.setVgrow(deviceTable, Priority.ALWAYS);
        
        return section;
    }
    
    private TableView<DeviceBindingRow> createDeviceTable() {
        TableView<DeviceBindingRow> table = new TableView<>();
        table.setPrefHeight(200);
        
        // 设备名称列
        TableColumn<DeviceBindingRow, String> nameColumn = new TableColumn<>("设备名称");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        nameColumn.setPrefWidth(120);
        
        // 设备指纹列
        TableColumn<DeviceBindingRow, String> fingerprintColumn = new TableColumn<>("设备指纹");
        fingerprintColumn.setCellValueFactory(new PropertyValueFactory<>("displayFingerprint"));
        fingerprintColumn.setPrefWidth(200);
        
        // 绑定时间列
        TableColumn<DeviceBindingRow, String> bindTimeColumn = new TableColumn<>("绑定时间");
        bindTimeColumn.setCellValueFactory(new PropertyValueFactory<>("bindTime"));
        bindTimeColumn.setPrefWidth(150);
        
        // 最后使用时间列
        TableColumn<DeviceBindingRow, String> lastUsedColumn = new TableColumn<>("最后使用");
        lastUsedColumn.setCellValueFactory(new PropertyValueFactory<>("lastUsedTime"));
        lastUsedColumn.setPrefWidth(150);
        
        // 状态列
        TableColumn<DeviceBindingRow, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(80);
        
        table.getColumns().addAll(nameColumn, fingerprintColumn, bindTimeColumn, lastUsedColumn, statusColumn);
        
        // 选择监听器
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            unbindButton.setDisable(newSelection == null || "当前设备".equals(newSelection.getStatus()));
        });
        
        deviceData = FXCollections.observableArrayList();
        table.setItems(deviceData);
        
        return table;
    }
    
    private HBox createBottomSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER_RIGHT);
        
        statusLabel = new Label();
        statusLabel.setWrapText(true);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        closeButton = new Button("关闭");
        closeButton.setPrefWidth(80);
        closeButton.setOnAction(e -> close());
        
        section.getChildren().addAll(statusLabel, spacer, closeButton);
        return section;
    }
    
    private void setupEventHandlers() {
        refreshButton.setOnAction(e -> refreshData());
        unbindButton.setOnAction(e -> unbindSelectedDevice());
    }
    
    private void refreshData() {
        try {
            LicenseService licenseService = LicenseService.getInstance();
            LicenseInfo currentLicense = licenseService.getCurrentLicense();
            
            if (currentLicense == null) {
                licenseInfoLabel.setText("未找到有效的许可证信息");
                deviceData.clear();
                showStatus("未激活许可证", false);
                return;
            }
            
            // 更新许可证信息
            updateLicenseInfo(currentLicense);
            
            // 更新设备列表
            updateDeviceList(currentLicense);
            
            showStatus("数据刷新完成", true);
            
        } catch (Exception e) {
            logger.error("刷新数据失败", e);
            showStatus("刷新数据失败: " + e.getMessage(), false);
        }
    }
    
    private void updateLicenseInfo(LicenseInfo license) {
        StringBuilder info = new StringBuilder();
        info.append("许可证ID: ").append(license.getLicenseId()).append("\n");
        info.append("客户名称: ").append(license.getCustomerName()).append("\n");
        info.append("最大设备数: ").append(license.getMaxDevices()).append("\n");
        info.append("已绑定设备: ").append(license.getActiveDeviceCount()).append("\n");
        info.append("创建时间: ").append(license.getCreatedDate().format(DATE_FORMATTER)).append("\n");
        info.append("有效期至: ").append(license.getExpiryDate().format(DATE_FORMATTER)).append("\n");
        
        if (license.isExpired()) {
            info.append("状态: 已过期");
            licenseInfoLabel.setStyle("-fx-background-color: #ffebee; -fx-padding: 10; -fx-border-color: #f44336; -fx-border-radius: 4;");
        } else {
            long remainingDays = LicenseService.getInstance().getRemainingDays();
            info.append("状态: 有效 (剩余 ").append(remainingDays).append(" 天)");
            
            if (remainingDays <= 30) {
                licenseInfoLabel.setStyle("-fx-background-color: #fff3e0; -fx-padding: 10; -fx-border-color: #ff9800; -fx-border-radius: 4;");
            } else {
                licenseInfoLabel.setStyle("-fx-background-color: #e8f5e8; -fx-padding: 10; -fx-border-color: #4caf50; -fx-border-radius: 4;");
            }
        }
        
        licenseInfoLabel.setText(info.toString());
    }
    
    private void updateDeviceList(LicenseInfo license) {
        deviceData.clear();
        
        String currentFingerprint = HardwareFingerprint.generateFingerprint();
        
        for (DeviceBinding binding : license.getDeviceBindings()) {
            if (binding.isActive()) {
                boolean isCurrentDevice = binding.getDeviceFingerprint().equals(currentFingerprint);
                
                DeviceBindingRow row = new DeviceBindingRow(
                        binding.getDeviceName(),
                        binding.getDisplayFingerprint(),
                        binding.getBindTime().format(DATE_FORMATTER),
                        binding.getLastUsedTime().format(DATE_FORMATTER),
                        isCurrentDevice ? "当前设备" : "其他设备",
                        binding
                );
                
                deviceData.add(row);
            }
        }
    }
    
    private void unbindSelectedDevice() {
        DeviceBindingRow selectedRow = deviceTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            return;
        }
        
        // 确认对话框
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认解绑");
        confirmAlert.setHeaderText("解绑设备");
        confirmAlert.setContentText("确定要解绑设备 \"" + selectedRow.getDeviceName() + "\" 吗？\n\n" +
                "解绑后该设备将无法继续使用此许可证，但可以重新绑定其他设备。");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                LicenseService licenseService = LicenseService.getInstance();
                LicenseInfo currentLicense = licenseService.getCurrentLicense();
                
                if (currentLicense != null) {
                    currentLicense.removeDeviceBinding(selectedRow.getBinding().getDeviceFingerprint());
                    refreshData();
                    showStatus("设备解绑成功", true);
                    logger.info("设备解绑成功: {}", selectedRow.getDeviceName());
                }
                
            } catch (Exception e) {
                logger.error("解绑设备失败", e);
                showStatus("解绑设备失败: " + e.getMessage(), false);
            }
        }
    }
    
    private void showCurrentDeviceFingerprint() {
        LicenseActivationDialog.showDeviceFingerprint((Stage) getOwner());
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
     * 设备绑定行数据类
     */
    public static class DeviceBindingRow {
        private final SimpleStringProperty deviceName;
        private final SimpleStringProperty displayFingerprint;
        private final SimpleStringProperty bindTime;
        private final SimpleStringProperty lastUsedTime;
        private final SimpleStringProperty status;
        private final DeviceBinding binding;
        
        public DeviceBindingRow(String deviceName, String displayFingerprint, String bindTime, 
                               String lastUsedTime, String status, DeviceBinding binding) {
            this.deviceName = new SimpleStringProperty(deviceName);
            this.displayFingerprint = new SimpleStringProperty(displayFingerprint);
            this.bindTime = new SimpleStringProperty(bindTime);
            this.lastUsedTime = new SimpleStringProperty(lastUsedTime);
            this.status = new SimpleStringProperty(status);
            this.binding = binding;
        }
        
        public String getDeviceName() { return deviceName.get(); }
        public String getDisplayFingerprint() { return displayFingerprint.get(); }
        public String getBindTime() { return bindTime.get(); }
        public String getLastUsedTime() { return lastUsedTime.get(); }
        public String getStatus() { return status.get(); }
        public DeviceBinding getBinding() { return binding; }
    }
    
    /**
     * 显示许可证管理对话框
     */
    public static void showLicenseManagement(Stage owner) {
        LicenseManagementDialog dialog = new LicenseManagementDialog(owner);
        dialog.showAndWait();
    }
}