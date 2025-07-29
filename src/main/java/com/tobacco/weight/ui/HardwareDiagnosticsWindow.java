package com.tobacco.weight.ui;

import com.tobacco.weight.hardware.IdCardReader;
import com.tobacco.weight.hardware.ScaleManager;
import com.tobacco.weight.hardware.PrinterManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 硬件诊断窗口
 * 提供详细的硬件连接状态、错误信息和系统诊断
 */
public class HardwareDiagnosticsWindow {
    
    private static final Logger logger = LoggerFactory.getLogger(HardwareDiagnosticsWindow.class);
    
    private Stage stage;
    private IdCardReader idCardReader;
    private ScaleManager scaleManager;
    private PrinterManager printerManager;
    
    // UI组件
    private Label idCardStatusLabel;
    private Label scaleStatusLabel;
    private Label printerStatusLabel;
    private TextArea errorLogArea;
    private TextArea systemInfoArea;
    private Button retryIdCardButton;
    private Button retryScaleButton;
    private Button retryPrinterButton;
    private Button refreshButton;
    private ProgressIndicator progressIndicator;
    
    private List<String> errorLog;
    
    public HardwareDiagnosticsWindow(IdCardReader idCardReader, ScaleManager scaleManager, PrinterManager printerManager) {
        this.idCardReader = idCardReader;
        this.scaleManager = scaleManager;
        this.printerManager = printerManager;
        this.errorLog = new ArrayList<>();
        
        createWindow();
        setupEventHandlers();
        refreshDiagnostics();
    }
    
    private void createWindow() {
        stage = new Stage();
        stage.setTitle("硬件诊断 - 烟叶称重系统");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f5f5;");
        
        // 标题
        Label titleLabel = new Label("硬件连接诊断");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // 硬件状态区域
        VBox statusSection = createStatusSection();
        
        // 系统信息区域
        VBox systemSection = createSystemInfoSection();
        
        // 错误日志区域
        VBox errorSection = createErrorLogSection();
        
        // 操作按钮区域
        HBox buttonSection = createButtonSection();
        
        root.getChildren().addAll(titleLabel, statusSection, systemSection, errorSection, buttonSection);
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        stage.setScene(scene);
    }
    
    private VBox createStatusSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; " +
                        "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label sectionTitle = new Label("硬件连接状态");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // 身份证读卡器状态
        HBox idCardStatus = new HBox(10);
        idCardStatus.setAlignment(Pos.CENTER_LEFT);
        Label idCardLabel = new Label("身份证读卡器:");
        idCardLabel.setMinWidth(120);
        idCardStatusLabel = new Label("检查中...");
        retryIdCardButton = new Button("重试连接");
        retryIdCardButton.setStyle("-fx-font-size: 11px;");
        retryIdCardButton.setOnAction(e -> retryIdCardConnection());
        
        idCardStatus.getChildren().addAll(idCardLabel, idCardStatusLabel, retryIdCardButton);
        
        // 电子秤状态
        HBox scaleStatus = new HBox(10);
        scaleStatus.setAlignment(Pos.CENTER_LEFT);
        Label scaleLabel = new Label("电子秤:");
        scaleLabel.setMinWidth(120);
        scaleStatusLabel = new Label("检查中...");
        retryScaleButton = new Button("重试连接");
        retryScaleButton.setStyle("-fx-font-size: 11px;");
        scaleStatus.getChildren().addAll(scaleLabel, scaleStatusLabel, retryScaleButton);
        
        // 打印机状态
        HBox printerStatus = new HBox(10);
        printerStatus.setAlignment(Pos.CENTER_LEFT);
        Label printerLabel = new Label("打印机:");
        printerLabel.setMinWidth(120);
        printerStatusLabel = new Label("检查中...");
        retryPrinterButton = new Button("重试连接");
        retryPrinterButton.setStyle("-fx-font-size: 11px;");
        printerStatus.getChildren().addAll(printerLabel, printerStatusLabel, retryPrinterButton);
        
        section.getChildren().addAll(sectionTitle, idCardStatus, scaleStatus, printerStatus);
        return section;
    }
    
    private VBox createSystemInfoSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; " +
                        "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label sectionTitle = new Label("系统信息");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        systemInfoArea = new TextArea();
        systemInfoArea.setEditable(false);
        systemInfoArea.setPrefRowCount(6);
        systemInfoArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        
        section.getChildren().addAll(sectionTitle, systemInfoArea);
        return section;
    }
    
    private VBox createErrorLogSection() {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; " +
                        "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        Label sectionTitle = new Label("错误日志和诊断信息");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        errorLogArea = new TextArea();
        errorLogArea.setEditable(false);
        errorLogArea.setPrefRowCount(8);
        errorLogArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        
        section.getChildren().addAll(sectionTitle, errorLogArea);
        VBox.setVgrow(section, Priority.ALWAYS);
        return section;
    }
    
    private HBox createButtonSection() {
        HBox section = new HBox(10);
        section.setAlignment(Pos.CENTER);
        
        refreshButton = new Button("刷新诊断");
        refreshButton.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        
        Button clearLogButton = new Button("清除日志");
        clearLogButton.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        clearLogButton.setOnAction(e -> clearErrorLog());
        
        Button closeButton = new Button("关闭");
        closeButton.setStyle("-fx-font-size: 12px; -fx-padding: 8 16;");
        closeButton.setOnAction(e -> stage.close());
        
        progressIndicator = new ProgressIndicator();
        progressIndicator.setVisible(false);
        progressIndicator.setPrefSize(20, 20);
        
        section.getChildren().addAll(refreshButton, clearLogButton, closeButton, progressIndicator);
        return section;
    }
    
    private void setupEventHandlers() {
        refreshButton.setOnAction(e -> refreshDiagnostics());
        
        retryIdCardButton.setOnAction(e -> retryIdCardConnection());
        retryScaleButton.setOnAction(e -> retryScaleConnection());
        retryPrinterButton.setOnAction(e -> retryPrinterConnection());
    }
    
    private void refreshDiagnostics() {
        progressIndicator.setVisible(true);
        
        new Thread(() -> {
            try {
                // 检查系统信息
                Platform.runLater(this::updateSystemInfo);
                
                // 检查硬件状态
                Platform.runLater(this::updateHardwareStatus);
                
                // 更新错误日志
                Platform.runLater(this::updateErrorLog);
                
            } catch (Exception e) {
                logger.error("刷新诊断信息失败", e);
                addErrorLog("刷新诊断信息失败: " + e.getMessage());
            } finally {
                Platform.runLater(() -> progressIndicator.setVisible(false));
            }
        }).start();
    }
    
    private void updateSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("操作系统: ").append(System.getProperty("os.name")).append(" ")
            .append(System.getProperty("os.version")).append("\n");
        info.append("Java版本: ").append(System.getProperty("java.version")).append("\n");
        info.append("Java厂商: ").append(System.getProperty("java.vendor")).append("\n");
        info.append("Java路径: ").append(System.getProperty("java.home")).append("\n");
        info.append("用户目录: ").append(System.getProperty("user.home")).append("\n");
        info.append("工作目录: ").append(System.getProperty("user.dir")).append("\n");
        info.append("可用内存: ").append(Runtime.getRuntime().maxMemory() / 1024 / 1024).append(" MB\n");
        info.append("已用内存: ").append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024).append(" MB\n");
        
        systemInfoArea.setText(info.toString());
    }
    
    private void updateHardwareStatus() {
        Platform.runLater(() -> {
            // 身份证读卡器状态
            if (idCardReader != null) {
                boolean connected = idCardReader.isConnected();
                String deviceInfo = idCardReader.getDeviceName();
                
                if (connected) {
                    idCardStatusLabel.setText("✅ 已连接 - " + deviceInfo);
                    idCardStatusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    String errorMsg = idCardReader.getDetailedErrorMessage();
                    idCardStatusLabel.setText("❌ 未连接 - " + errorMsg.split("\n")[0]); // 只显示第一行错误信息
                    idCardStatusLabel.setStyle("-fx-text-fill: red;");
                }
            } else {
                idCardStatusLabel.setText("❌ 未初始化");
                idCardStatusLabel.setStyle("-fx-text-fill: red;");
            }
            
            // 电子秤状态  
            if (scaleManager != null) {
                boolean connected = scaleManager.isConnected();
                if (connected) {
                    scaleStatusLabel.setText("✅ 已连接");
                    scaleStatusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    scaleStatusLabel.setText("❌ 未连接");
                    scaleStatusLabel.setStyle("-fx-text-fill: red;");
                }
            } else {
                scaleStatusLabel.setText("❌ 未初始化");
                scaleStatusLabel.setStyle("-fx-text-fill: red;");
            }
            
            // 打印机状态
            if (printerManager != null) {
                boolean connected = printerManager.isConnected();
                if (connected) {
                    printerStatusLabel.setText("✅ 已连接");
                    printerStatusLabel.setStyle("-fx-text-fill: green;");
                } else {
                    printerStatusLabel.setText("❌ 未连接");
                    printerStatusLabel.setStyle("-fx-text-fill: red;");
                }
            } else {
                printerStatusLabel.setText("❌ 未初始化");
                printerStatusLabel.setStyle("-fx-text-fill: red;");
            }
        });
    }
    
    private void updateErrorLog() {
        StringBuilder log = new StringBuilder();
        log.append("=== 硬件诊断日志 ===\n");
        log.append("更新时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        if (errorLog.isEmpty()) {
            log.append("暂无错误信息。\n\n");
        } else {
            for (String error : errorLog) {
                log.append(error).append("\n");
            }
            log.append("\n");
        }
        
        // 添加故障排除建议
        log.append("=== 故障排除建议 ===\n");
        log.append("1. 检查硬件设备是否正确连接到计算机\n");
        log.append("2. 确认设备驱动程序已正确安装\n");
        log.append("3. 检查USB端口和数据线是否正常工作\n");
        log.append("4. 尝试重启硬件设备\n");
        log.append("5. 检查防火墙或安全软件是否阻止了设备通信\n");
        log.append("6. 确认应用程序具有访问硬件设备的权限\n");
        log.append("7. 如果问题持续存在，请联系技术支持\n");
        
        errorLogArea.setText(log.toString());
    }
    
    private void retryIdCardConnection() {
        addErrorLog("尝试重新连接身份证读卡器...");
        
        new Thread(() -> {
            try {
                if (idCardReader != null) {
                    idCardReader.disconnect();
                    Platform.runLater(() -> addErrorLog("已断开连接，等待1秒..."));
                    
                    Thread.sleep(1000);
                    
                    Platform.runLater(() -> addErrorLog("开始重新连接..."));
                    boolean success = idCardReader.connect();
                    
                    Platform.runLater(() -> {
                        if (success) {
                            addErrorLog("✅ 身份证读卡器重连成功");
                        } else {
                            addErrorLog("❌ 身份证读卡器重连失败");
                            addErrorLog("详细错误: " + idCardReader.getDetailedErrorMessage());
                        }
                        updateHardwareStatus();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    addErrorLog("❌ 身份证读卡器重连异常: " + e.getMessage());
                    updateHardwareStatus();
                });
            }
        }).start();
    }
    
    private void retryScaleConnection() {
        addErrorLog("尝试重新连接电子秤...");
        // 类似的重连逻辑
    }
    
    private void retryPrinterConnection() {
        addErrorLog("尝试重新连接打印机...");
        // 类似的重连逻辑
    }
    
    public void addErrorLog(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logEntry = "[" + timestamp + "] " + message;
        errorLog.add(logEntry);
        
        // 限制日志条数
        if (errorLog.size() > 100) {
            errorLog.remove(0);
        }
        
        Platform.runLater(this::updateErrorLog);
    }
    
    private void clearErrorLog() {
        errorLog.clear();
        updateErrorLog();
    }
    
    public void show() {
        refreshDiagnostics();
        stage.show();
    }
    
    public void showAndWait() {
        refreshDiagnostics();
        stage.showAndWait();
    }
    
    /**
     * 检查窗口是否可见
     */
    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }
    
    /**
     * 从外部添加错误日志（用于主界面错误通知）
     */
    public static void logError(String message) {
        // 这个方法可以在有诊断窗口实例时调用addErrorLog
        // 为了简化，直接使用logger记录
        LoggerFactory.getLogger(HardwareDiagnosticsWindow.class).info("外部错误: {}", message);
    }
}