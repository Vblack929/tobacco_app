package com.tobacco.weight.license;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 许可证管理界面
 * 供应商可以查看和管理许可证使用情况
 */
public class LicenseManagementUI extends Application {
    private static final Logger logger = LoggerFactory.getLogger(LicenseManagementUI.class);
    
    private DeviceManager deviceManager;
    private TableView<DeviceManager.DeviceInfo> deviceTable;
    private ObservableList<DeviceManager.DeviceInfo> deviceList;
    private TextField licenseIdField;
    private Label statusLabel;
    private Label statsLabel;
    
    @Override
    public void start(Stage primaryStage) {
        deviceManager = new DeviceManager();
        deviceList = FXCollections.observableArrayList();
        
        primaryStage.setTitle("烟叶称重系统 - 许可证管理");
        primaryStage.setScene(createMainScene());
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
        
        // 设置关闭事件
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });
    }
    
    /**
     * 创建主界面
     */
    private Scene createMainScene() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        // 标题
        Label titleLabel = new Label("许可证设备管理");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // 许可证输入区域
        HBox licenseInputBox = createLicenseInputBox();
        
        // 统计信息区域
        statsLabel = new Label("请输入许可证ID查看设备信息");
        statsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // 设备表格
        deviceTable = createDeviceTable();
        
        // 操作按钮区域
        HBox buttonBox = createButtonBox();
        
        // 状态栏
        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        
        root.getChildren().addAll(
            titleLabel,
            new Separator(),
            licenseInputBox,
            statsLabel,
            deviceTable,
            buttonBox,
            new Separator(),
            statusLabel
        );
        
        VBox.setVgrow(deviceTable, Priority.ALWAYS);
        
        return new Scene(root, 800, 600);
    }
    
    /**
     * 创建许可证输入区域
     */
    private HBox createLicenseInputBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Label label = new Label("许可证ID:");
        licenseIdField = new TextField();
        licenseIdField.setPromptText("输入许可证ID，例如: YC-TWW-2025-PROD-0001");
        licenseIdField.setPrefWidth(300);
        
        Button loadButton = new Button("加载设备信息");
        loadButton.setOnAction(e -> loadDeviceInfo());
        
        Button refreshButton = new Button("刷新");
        refreshButton.setOnAction(e -> refreshDeviceInfo());
        
        box.getChildren().addAll(label, licenseIdField, loadButton, refreshButton);
        
        return box;
    }
    
    /**
     * 创建设备表格
     */
    private TableView<DeviceManager.DeviceInfo> createDeviceTable() {
        TableView<DeviceManager.DeviceInfo> table = new TableView<>();
        table.setItems(deviceList);
        
        // 设备指纹列
        TableColumn<DeviceManager.DeviceInfo, String> fingerprintCol = new TableColumn<>("设备指纹");
        fingerprintCol.setCellValueFactory(new PropertyValueFactory<>("deviceFingerprint"));
        fingerprintCol.setPrefWidth(200);
        
        // 设备名称列
        TableColumn<DeviceManager.DeviceInfo, String> nameCol = new TableColumn<>("设备名称");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        nameCol.setPrefWidth(150);
        
        // 绑定时间列
        TableColumn<DeviceManager.DeviceInfo, String> bindTimeCol = new TableColumn<>("绑定时间");
        bindTimeCol.setCellValueFactory(new PropertyValueFactory<>("bindTime"));
        bindTimeCol.setPrefWidth(150);
        
        // 最后使用时间列
        TableColumn<DeviceManager.DeviceInfo, String> lastUsedCol = new TableColumn<>("最后使用");
        lastUsedCol.setCellValueFactory(new PropertyValueFactory<>("lastUsedTime"));
        lastUsedCol.setPrefWidth(150);
        
        // IP地址列
        TableColumn<DeviceManager.DeviceInfo, String> ipCol = new TableColumn<>("IP地址");
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        ipCol.setPrefWidth(120);
        
        // 状态列
        TableColumn<DeviceManager.DeviceInfo, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cellData -> {
            boolean active = cellData.getValue().isActive();
            return new javafx.beans.property.SimpleStringProperty(active ? "活跃" : "停用");
        });
        statusCol.setPrefWidth(80);
        
        table.getColumns().addAll(fingerprintCol, nameCol, bindTimeCol, lastUsedCol, ipCol, statusCol);
        
        // 设置行样式
        table.setRowFactory(tv -> {
            TableRow<DeviceManager.DeviceInfo> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    if (newItem.isActive()) {
                        row.setStyle("-fx-background-color: #e8f5e8;");
                    } else {
                        row.setStyle("-fx-background-color: #ffe8e8;");
                    }
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
        
        return table;
    }
    
    /**
     * 创建操作按钮区域
     */
    private HBox createButtonBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        
        Button unbindButton = new Button("解绑设备");
        unbindButton.setOnAction(e -> unbindSelectedDevice());
        
        Button deactivateButton = new Button("停用设备");
        deactivateButton.setOnAction(e -> deactivateSelectedDevice());
        
        Button reactivateButton = new Button("重新激活");
        reactivateButton.setOnAction(e -> reactivateSelectedDevice());
        
        Button viewHistoryButton = new Button("查看历史");
        viewHistoryButton.setOnAction(e -> viewActivationHistory());
        
        Button exportButton = new Button("导出数据");
        exportButton.setOnAction(e -> exportDeviceData());
        
        box.getChildren().addAll(
            unbindButton, deactivateButton, reactivateButton, 
            new Separator(), viewHistoryButton, exportButton
        );
        
        return box;
    }
    
    /**
     * 加载设备信息
     */
    private void loadDeviceInfo() {
        String licenseId = licenseIdField.getText().trim();
        if (licenseId.isEmpty()) {
            showAlert("错误", "请输入许可证ID");
            return;
        }
        
        updateStatus("正在加载设备信息...");
        
        // 在后台线程中执行
        new Thread(() -> {
            try {
                List<DeviceManager.DeviceInfo> devices = deviceManager.getBoundDevices(licenseId);
                DeviceManager.DeviceUsageStats stats = deviceManager.getDeviceUsageStats(licenseId);
                
                Platform.runLater(() -> {
                    deviceList.clear();
                    deviceList.addAll(devices);
                    
                    updateStatsLabel(stats);
                    updateStatus(String.format("已加载 %d 个设备", devices.size()));
                });
                
            } catch (Exception e) {
                logger.error("加载设备信息失败", e);
                Platform.runLater(() -> {
                    updateStatus("加载失败: " + e.getMessage());
                    showAlert("错误", "加载设备信息失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 刷新设备信息
     */
    private void refreshDeviceInfo() {
        if (!licenseIdField.getText().trim().isEmpty()) {
            loadDeviceInfo();
        }
    }
    
    /**
     * 解绑选中的设备
     */
    private void unbindSelectedDevice() {
        DeviceManager.DeviceInfo selectedDevice = deviceTable.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) {
            showAlert("提示", "请选择要解绑的设备");
            return;
        }
        
        String licenseId = licenseIdField.getText().trim();
        if (licenseId.isEmpty()) {
            showAlert("错误", "许可证ID不能为空");
            return;
        }
        
        // 确认对话框
        Optional<ButtonType> result = showConfirmDialog(
            "确认解绑",
            String.format("确定要解绑设备 '%s' 吗？\n\n设备指纹: %s\n\n此操作不可撤销！", 
                selectedDevice.getDeviceName(), selectedDevice.getDeviceFingerprint())
        );
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            updateStatus("正在解绑设备...");
            
            new Thread(() -> {
                try {
                    boolean success = deviceManager.unbindDevice(licenseId, selectedDevice.getDeviceFingerprint());
                    
                    Platform.runLater(() -> {
                        if (success) {
                            updateStatus("设备解绑成功");
                            refreshDeviceInfo();
                        } else {
                            updateStatus("设备解绑失败");
                            showAlert("错误", "设备解绑失败");
                        }
                    });
                    
                } catch (Exception e) {
                    logger.error("解绑设备失败", e);
                    Platform.runLater(() -> {
                        updateStatus("解绑失败: " + e.getMessage());
                        showAlert("错误", "解绑设备失败: " + e.getMessage());
                    });
                }
            }).start();
        }
    }
    
    /**
     * 停用选中的设备
     */
    private void deactivateSelectedDevice() {
        DeviceManager.DeviceInfo selectedDevice = deviceTable.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) {
            showAlert("提示", "请选择要停用的设备");
            return;
        }
        
        if (!selectedDevice.isActive()) {
            showAlert("提示", "设备已经是停用状态");
            return;
        }
        
        String licenseId = licenseIdField.getText().trim();
        performDeviceOperation("停用", licenseId, selectedDevice.getDeviceFingerprint(), 
            () -> deviceManager.deactivateDevice(licenseId, selectedDevice.getDeviceFingerprint()));
    }
    
    /**
     * 重新激活选中的设备
     */
    private void reactivateSelectedDevice() {
        DeviceManager.DeviceInfo selectedDevice = deviceTable.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) {
            showAlert("提示", "请选择要重新激活的设备");
            return;
        }
        
        if (selectedDevice.isActive()) {
            showAlert("提示", "设备已经是活跃状态");
            return;
        }
        
        String licenseId = licenseIdField.getText().trim();
        performDeviceOperation("重新激活", licenseId, selectedDevice.getDeviceFingerprint(), 
            () -> deviceManager.reactivateDevice(licenseId, selectedDevice.getDeviceFingerprint()));
    }
    
    /**
     * 执行设备操作
     */
    private void performDeviceOperation(String operation, String licenseId, String deviceFingerprint, 
                                      java.util.function.Supplier<Boolean> operationFunc) {
        updateStatus("正在" + operation + "设备...");
        
        new Thread(() -> {
            try {
                boolean success = operationFunc.get();
                
                Platform.runLater(() -> {
                    if (success) {
                        updateStatus("设备" + operation + "成功");
                        refreshDeviceInfo();
                    } else {
                        updateStatus("设备" + operation + "失败");
                        showAlert("错误", "设备" + operation + "失败");
                    }
                });
                
            } catch (Exception e) {
                logger.error(operation + "设备失败", e);
                Platform.runLater(() -> {
                    updateStatus(operation + "失败: " + e.getMessage());
                    showAlert("错误", operation + "设备失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * 查看激活历史
     */
    private void viewActivationHistory() {
        final String licenseId = licenseIdField.getText().trim();
        if (licenseId.isEmpty()) {
            showAlert("错误", "请输入许可证ID");
            return;
        }
        
        // 创建历史查看窗口
        Stage historyStage = new Stage();
        historyStage.initModality(Modality.APPLICATION_MODAL);
        historyStage.setTitle("激活历史 - " + licenseId);
        
        final TextArea historyArea = new TextArea();
        historyArea.setEditable(false);
        historyArea.setPrefSize(600, 400);
        historyArea.setText("正在加载激活历史...");
        
        VBox historyBox = new VBox(10);
        historyBox.setPadding(new Insets(15));
        historyBox.getChildren().add(historyArea);
        
        Scene historyScene = new Scene(historyBox, 650, 450);
        historyStage.setScene(historyScene);
        historyStage.show();
        
        // 在后台加载历史数据
        new Thread(() -> {
            try {
                // 这里应该实现获取激活历史的逻辑
                StringBuilder historyBuilder = new StringBuilder();
                historyBuilder.append("激活历史功能正在开发中...\n\n");
                historyBuilder.append("许可证ID: ").append(licenseId).append("\n");
                historyBuilder.append("查询时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
                
                final String historyText = historyBuilder.toString();
                Platform.runLater(() -> historyArea.setText(historyText));
                
            } catch (Exception e) {
                logger.error("加载激活历史失败", e);
                Platform.runLater(() -> historyArea.setText("加载激活历史失败: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * 导出设备数据
     */
    private void exportDeviceData() {
        if (deviceList.isEmpty()) {
            showAlert("提示", "没有可导出的设备数据");
            return;
        }
        
        StringBuilder exportData = new StringBuilder();
        exportData.append("许可证设备导出报告\n");
        exportData.append("导出时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        exportData.append("许可证ID: ").append(licenseIdField.getText()).append("\n\n");
        
        exportData.append("设备列表:\n");
        exportData.append("序号\t设备指纹\t设备名称\t绑定时间\t最后使用\tIP地址\t状态\n");
        
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceManager.DeviceInfo device = deviceList.get(i);
            exportData.append(String.format("%d\t%s\t%s\t%s\t%s\t%s\t%s\n",
                i + 1,
                device.getDeviceFingerprint(),
                device.getDeviceName(),
                device.getBindTime(),
                device.getLastUsedTime(),
                device.getIpAddress(),
                device.isActive() ? "活跃" : "停用"
            ));
        }
        
        // 显示导出数据
        Stage exportStage = new Stage();
        exportStage.initModality(Modality.APPLICATION_MODAL);
        exportStage.setTitle("导出数据");
        
        TextArea exportArea = new TextArea(exportData.toString());
        exportArea.setEditable(false);
        exportArea.setPrefSize(800, 500);
        
        VBox exportBox = new VBox(10);
        exportBox.setPadding(new Insets(15));
        exportBox.getChildren().add(exportArea);
        
        Scene exportScene = new Scene(exportBox, 850, 550);
        exportStage.setScene(exportScene);
        exportStage.show();
    }
    
    /**
     * 更新统计标签
     */
    private void updateStatsLabel(DeviceManager.DeviceUsageStats stats) {
        String statsText = String.format(
            "设备统计: 总计 %d 台，活跃 %d 台，停用 %d 台，最大允许 %d 台，剩余槽位 %d 个",
            stats.getTotalDevices(),
            stats.getActiveDevices(),
            stats.getInactiveDevices(),
            stats.getMaxDevices(),
            stats.getAvailableSlots()
        );
        statsLabel.setText(statsText);
    }
    
    /**
     * 更新状态标签
     */
    private void updateStatus(String status) {
        statusLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " - " + status);
    }
    
    /**
     * 显示警告对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 显示确认对话框
     */
    private Optional<ButtonType> showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }
    
    /**
     * 主方法
     */
    public static void main(String[] args) {
        launch(args);
    }
}