package com.tobacco.weight.ui;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.data.FarmerStatistics;
import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.WeighingRecordRepository;
import com.tobacco.weight.hardware.ScaleManager;
import com.tobacco.weight.hardware.PrinterManager;
import com.tobacco.weight.hardware.IdCardReader;
import com.tobacco.weight.hardware.printer.WindowsSerialPrinter;
import com.tobacco.weight.hardware.printer.PrinterSimulator;
import com.tobacco.weight.hardware.printer.esc.EscBuilder;
import com.tobacco.weight.ui.HardwareDiagnosticsWindow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.ArrayList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.Map;
import javafx.scene.control.TableRow;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.text.SimpleDateFormat;

/**
 * 主界面控制器
 * 管理整个应用程序的UI状态和业务逻辑
 */
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private Stage primaryStage;

    // 硬件管理器
    private ScaleManager scaleManager;
    private PrinterManager printerManager;
    private IdCardReader idCardReader;

    // 新的打印机连接器
    private WindowsSerialPrinter serialPrinter;
    private PrinterSimulator printerSimulator;

    // 数据仓库
    private WeighingRecordRepository weighingRecordRepository;

    // 数据
    private ObservableList<FarmerStatistics> farmerStatisticsList;
    private ObservableList<WeighingRecord> weighingRecordsList;

    // UI组件 - 称重区域
    @FXML
    private TextField farmerNameField;
    @FXML
    private TextField contractNumberField;
    @FXML
    private Label currentWeightLabel;
    @FXML
    private Button upperLeafButton;
    @FXML
    private Button middleLeafButton;
    @FXML
    private Button lowerLeafButton;
    @FXML
    private Button confirmButton;
    @FXML
    private Button readIdCardButton;
    @FXML
    private TextField idCardNumberField;
    @FXML
    private Label idCardStatusIcon;

    // UI组件 - 统计区域
    @FXML
    private TextField precheckRatioField;
    @FXML
    private TextField upperRatioField;
    @FXML
    private TextField middleRatioField;
    @FXML
    private TextField lowerRatioField;
    @FXML
    private Label precheckIdLabel;
    @FXML
    private Label precheckDateLabel;

    // UI组件 - 管理区域
    @FXML
    private VBox farmerDataContainer;
    @FXML
    private VBox adminTableContainer;
    @FXML
    private Button exportAllDataButton;
    private TableView<FarmerStats> adminTable;

    // UI组件 - 状态栏
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label operationTipLabel;
    @FXML
    private Button diagnosticsButton;

    // 打印机连接UI组件
    @FXML
    private Label printerStatusLabel;
    @FXML
    private Button printerConnectButton;
    @FXML
    private Button printerTestButton;
    @FXML
    private Button systemPrintTestButton;

    private int precheckCounter = 100000000;
    private HardwareDiagnosticsWindow diagnosticsWindow;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化主界面控制器");

        // 初始化数据列表
        farmerStatisticsList = FXCollections.observableArrayList();
        weighingRecordsList = FXCollections.observableArrayList();

        // 初始化数据仓库
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        weighingRecordRepository = new WeighingRecordRepository(databaseManager);

        // 显示数据库路径
        String dbPath = databaseManager.getDbPath();
        logger.info("数据库文件位置: {}", dbPath);
        System.out.println("数据库文件位置: " + dbPath);

        // 初始化硬件管理器
        initializeHardwareManagers();

        // 设置UI事件监听器
        setupEventHandlers();

        // 初始化UI状态
        initializeUIState();

        // 加载初始数据
        loadInitialData();

        setupAdminTable();

        logger.info("主界面控制器初始化完成");

        // 定时刷新重量显示
        java.util.Timer timer = new java.util.Timer(true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                double weight = scaleManager.getCurrentWeight();
                Platform.runLater(() -> currentWeightLabel.setText(String.format("%.2f kg", weight)));
            }
        }, 0, 1000); // 每秒刷新一次
    }

    /**
     * 初始化硬件管理器
     */
    private void initializeHardwareManagers() {
        try {
            scaleManager = new ScaleManager();
            printerManager = new PrinterManager();
            idCardReader = new IdCardReader();

            // 初始化新的串口打印机连接器
            serialPrinter = new WindowsSerialPrinter();
            printerSimulator = new PrinterSimulator();

            // 设置硬件状态监听器
            scaleManager.setOnWeightChanged(this::updateCurrentWeight);
            scaleManager.setOnConnectionStatusChanged(this::updateScaleStatus);

            idCardReader.setOnIdCardRead(this::handleIdCardRead);
            idCardReader.setOnConnectionStatusChanged(this::updateIdCardStatus);
            idCardReader.setOnErrorOccurred(this::handleIdCardError);

            // 检查连接状态 - 在注册回调后调用
            idCardReader.checkConnectionStatus();

            logger.info("硬件管理器初始化完成");

        } catch (Exception e) {
            logger.error("硬件管理器初始化失败", e);
            showError("硬件初始化失败", "无法初始化硬件设备: " + e.getMessage());
        }
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 部叶选择按钮
        upperLeafButton.setOnAction(e -> selectLeafType("上部叶"));
        middleLeafButton.setOnAction(e -> selectLeafType("中部叶"));
        lowerLeafButton.setOnAction(e -> selectLeafType("下部叶"));

        // 确认按钮
        confirmButton.setOnAction(e -> confirmWeighing());

        // 身份证读取按钮
        readIdCardButton.setOnAction(e -> readIdCard());

        // 文本字段变化监听
        farmerNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateContractNumber();
        });

        // 导出所有数据按钮
        exportAllDataButton.setOnAction(e -> exportAllData());

        // 硬件诊断按钮
        diagnosticsButton.setOnAction(e -> openDiagnosticsWindow());

        // 打印机连接按钮
        printerConnectButton.setOnAction(e -> connectPrinter());
        printerTestButton.setOnAction(e -> testPrinter());
        systemPrintTestButton.setOnAction(e -> testSystemPrinter());
    }

    /**
     * 初始化UI状态
     */
    private void initializeUIState() {
        // 设置默认值
        currentWeightLabel.setText("0.00 kg");
        precheckRatioField.setText("0.0%");
        upperRatioField.setText("0.0%");
        middleRatioField.setText("0.0%");
        lowerRatioField.setText("0.0%");

        // 设置按钮状态
        confirmButton.setDisable(true);
        readIdCardButton.setDisable(true);

        // 设置初始ID卡读卡器状态为未连接
        if (idCardStatusIcon != null) {
            idCardStatusIcon.getStyleClass().add("disconnected");
            readIdCardButton.getStyleClass().add("disconnected");
        }

        // 设置状态栏
        statusLabel.setText("系统就绪");
        progressBar.setVisible(false);

        // 设置预检编号和日期
        precheckCounter = 100000000;
        precheckIdLabel.setText(String.format("YJ%09d", precheckCounter));

        // 设置当天日期
        java.time.LocalDate today = java.time.LocalDate.now();
        precheckDateLabel.setText(today.toString());
    }

    /**
     * 加载初始数据
     */
    private void loadInitialData() {
        try {
            // 在后台线程加载数据
            new Thread(() -> {
                try {
                    // 加载烟农统计数据
                    loadFarmerStatistics();

                    // 加载称重记录
                    loadWeighingRecords();

                    // 更新UI
                    Platform.runLater(() -> {
                        updateFarmerDataDisplay();
                    });

                } catch (Exception e) {
                    logger.error("加载初始数据失败", e);
                    Platform.runLater(() -> showError("数据加载失败", "无法加载初始数据: " + e.getMessage()));
                }
            }).start();

        } catch (Exception e) {
            logger.error("启动数据加载失败", e);
        }
    }

    /**
     * 选择部叶类型
     */
    private void selectLeafType(String leafType) {
        // 移除所有按钮的selected样式
        upperLeafButton.getStyleClass().removeAll("selected");
        middleLeafButton.getStyleClass().removeAll("selected");
        lowerLeafButton.getStyleClass().removeAll("selected");
        // 给选中的按钮添加selected
        Button selectedButton = getLeafTypeButton(leafType);
        if (selectedButton != null && !selectedButton.getStyleClass().contains("selected")) {
            selectedButton.getStyleClass().add("selected");
        }
        // 启用确认按钮
        confirmButton.setDisable(false);
        // 更新状态栏
        updateStatus("已选择: " + leafType);
        // 同步到上方提示栏
        operationTipLabel.setText("已选择: " + leafType);
        operationTipLabel.getStyleClass().removeAll("error", "success");
        operationTipLabel.getStyleClass().add("success");
        logger.info("选择部叶类型: {}", leafType);
    }

    /**
     * 获取部叶按钮
     */
    private Button getLeafTypeButton(String leafType) {
        switch (leafType) {
            case "上部叶":
                return upperLeafButton;
            case "中部叶":
                return middleLeafButton;
            case "下部叶":
                return lowerLeafButton;
            default:
                return null;
        }
    }

    /**
     * 确认称重
     */
    private void confirmWeighing() {
        try {
            System.out.println("=== 开始确认称重 ===");

            String farmerName = farmerNameField.getText().trim();
            if (farmerName.isEmpty()) {
                showError("输入错误", "请输入烟农姓名");
                return;
            }
            String contractNumber = contractNumberField.getText().trim();
            if (contractNumber.isEmpty()) {
                showError("输入错误", "合同号不能为空");
                return;
            }
            String idCardNumber = idCardNumberField.getText().trim();
            if (idCardNumber.isEmpty()) {
                showError("输入错误", "身份证号不能为空");
                return;
            }

            // 获取当前重量
            double weight = scaleManager.getCurrentWeight();
            if (weight <= 0) {
                showError("称重错误", "当前重量无效，请检查电子秤连接");
                return;
            }

            // 获取选中的部叶类型
            String leafType = getSelectedLeafType();
            if (leafType == null) {
                showError("选择错误", "请选择部叶类型");
                return;
            }

            System.out.println("称重信息: " + farmerName + ", " + idCardNumber + ", " + leafType + ", " + weight + "kg");

            // 生成新的预检编号（自增+1）
            String newPrecheckId = String.format("YJ%09d", ++precheckCounter);
            precheckIdLabel.setText(newPrecheckId);

            // 更新日期为当天
            java.time.LocalDate today = java.time.LocalDate.now();
            precheckDateLabel.setText(today.toString());

            // 创建称重记录
            WeighingRecord record = new WeighingRecord();
            record.setFarmerName(farmerName);
            record.setContractNumber(contractNumber);
            record.setLeafType(leafType);
            record.setWeight(weight);
            record.setOperator("操作员"); // TODO: 从系统获取当前用户
            // 设置预检编号
            record.setPrecheckId(newPrecheckId);
            record.setIdCardNumber(idCardNumber);

            System.out.println("记录创建完成: " + record.getFarmerName() + ", " + record.getIdCardNumber());

            // 保存到数据库和列表
            saveWeighingRecord(record);
            updateFarmerStatistics(record);
            resetWeighingUI();
            updateRatios();

            // 显示提示
            showInfo("称重完成", "称重完成 - 预检号: " + record.getPrecheckId() + " | " + leafType + " "
                    + String.format("%.2f", weight) + "kg");

            System.out.println("=== 确认称重完成 ===");

        } catch (Exception e) {
            System.out.println("确认称重异常: " + e.getMessage());
            e.printStackTrace();
            showError("异常", "称重失败: " + e.getMessage());
        }
    }

    /**
     * 获取选中的部叶类型
     */
    private String getSelectedLeafType() {
        if (upperLeafButton.getStyleClass().contains("selected"))
            return "上部叶";
        if (middleLeafButton.getStyleClass().contains("selected"))
            return "中部叶";
        if (lowerLeafButton.getStyleClass().contains("selected"))
            return "下部叶";
        return null;
    }

    /**
     * 读取身份证
     */
    private void readIdCard() {
        try {
            updateStatus("正在读取身份证...");
            idCardReader.readIdCard();

        } catch (Exception e) {
            logger.error("读取身份证失败", e);
            showError("读取失败", "无法读取身份证: " + e.getMessage());
        }
    }

    /**
     * 处理身份证读取结果
     */
    private void handleIdCardRead(FarmerInfo farmerInfo) {
        Platform.runLater(() -> {
            try {
                if (farmerInfo != null) {
                    // 填充烟农信息
                    farmerNameField.setText(farmerInfo.getFarmerName());
                    contractNumberField.setText(farmerInfo.getContractNumber());
                    idCardNumberField.setText(farmerInfo.getIdCardNumber()); // 填充身份证号

                    updateStatus("身份证读取成功: " + farmerInfo.getFarmerName());
                    showInfo("读取成功", "身份证信息已读取并填充");

                } else {
                    updateStatus("身份证读取失败");
                    showError("读取失败", "无法识别身份证信息");
                }

            } catch (Exception e) {
                logger.error("处理身份证读取结果失败", e);
                showError("处理失败", "处理身份证信息失败: " + e.getMessage());
            }
        });
    }

    /**
     * 处理身份证读卡器错误
     */
    private void handleIdCardError(String errorMessage) {
        Platform.runLater(() -> {
            logger.error("ID卡读卡器发生错误: {}", errorMessage);

            // 如果诊断窗口打开，将错误添加到日志中
            if (diagnosticsWindow != null && diagnosticsWindow.isShowing()) {
                diagnosticsWindow.addErrorLog("身份证读卡器错误: " + errorMessage);
            }

            showError("读卡器错误", "ID卡读卡器发生错误: " + errorMessage);
        });
    }

    /**
     * 更新当前重量显示
     */
    private void updateCurrentWeight(double weight) {
        logger.info("收到重量更新回调: {} kg", weight);

        Platform.runLater(() -> {
            logger.debug("在UI线程中更新重量显示: {} kg", weight);

            // 更新重量显示
            currentWeightLabel.setText(String.format("%.2f kg", weight));
            logger.debug("重量标签已更新: {}", currentWeightLabel.getText());

            // 更新状态信息
            updateStatus(String.format("当前重量: %.2f kg", weight));

            // 检查重量是否稳定（这里可以根据需要添加稳定性检测逻辑）
            if (weight > 0.01) {
                // 有重量时启用确认按钮
                confirmButton.setDisable(false);
                operationTipLabel.setText("请选择叶位类型");
                logger.debug("确认按钮已启用");
            } else {
                // 无重量时禁用确认按钮
                confirmButton.setDisable(true);
                operationTipLabel.setText("请放置物品称重");
                logger.debug("确认按钮已禁用");
            }

            logger.info("UI重量更新完成: {} kg", weight);
        });
    }

    /**
     * 更新电子秤状态
     */
    private void updateScaleStatus(boolean connected) {
        Platform.runLater(() -> {
            if (connected) {
                updateStatus("电子秤已连接");
            } else {
                updateStatus("电子秤未连接");
            }
        });
    }

    /**
     * 更新身份证读卡器状态
     */
    private void updateIdCardStatus(boolean connected) {
        Platform.runLater(() -> {
            readIdCardButton.setDisable(!connected);

            // 清除之前的状态样式
            idCardStatusIcon.getStyleClass().removeAll("connected", "disconnected", "connecting");
            readIdCardButton.getStyleClass().removeAll("connected", "disconnected", "connecting");

            if (connected) {
                updateStatus("身份证读卡器已连接");
                // 添加连接状态样式
                idCardStatusIcon.getStyleClass().add("connected");
                readIdCardButton.getStyleClass().add("connected");
                logger.info("ID卡读卡器连接状态更新: 已连接");
            } else {
                updateStatus("身份证读卡器未连接");
                // 添加断开连接状态样式
                idCardStatusIcon.getStyleClass().add("disconnected");
                readIdCardButton.getStyleClass().add("disconnected");
                logger.info("ID卡读卡器连接状态更新: 未连接");
            }
        });
    }

    /**
     * 开发者测试方法：模拟各种错误条件（用于测试诊断系统）
     * 在键盘输入特定快捷键时调用此方法
     */
    public void triggerTestErrors() {
        if (idCardReader != null) {
            new Thread(() -> {
                try {
                    // 模拟设备未找到错误
                    Thread.sleep(1000);
                    handleIdCardError("模拟错误: 设备未找到 [错误代码: 1001]");

                    Thread.sleep(2000);
                    // 模拟驱动问题
                    handleIdCardError("模拟错误: 驱动程序未安装 [错误代码: 1002]");

                    Thread.sleep(2000);
                    // 模拟通信错误
                    handleIdCardError("模拟错误: 通信失败 [错误代码: 1004]");

                    Thread.sleep(2000);
                    // 模拟读卡失败
                    handleIdCardError("模拟错误: 身份证读取失败 [错误代码: 2002]");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    /**
     * 更新合同号
     */
    private void updateContractNumber() {
        // TODO: 实现合同号自动生成逻辑
        String farmerName = farmerNameField.getText().trim();
        if (!farmerName.isEmpty()) {
            // 简单的合同号生成逻辑
            contractNumberField.setText("HT" + System.currentTimeMillis());
        }
    }

    /**
     * 保存称重记录
     */
    private void saveWeighingRecord(WeighingRecord record) {
        // 添加到内存列表
        weighingRecordsList.add(record);
        System.out.println("称重记录总数: " + weighingRecordsList.size());

        // 保存到数据库
        if (weighingRecordRepository != null) {
            weighingRecordRepository.insert(record, new WeighingRecordRepository.OnResultListener<Long>() {
                @Override
                public void onSuccess(Long recordId) {
                    Platform.runLater(() -> {
                        record.setId(recordId);
                        updateStatus("称重记录已保存到数据库，ID: " + recordId);
                        logger.info("称重记录保存成功，数据库ID: {}", recordId);
                    });
                }

                @Override
                public void onError(Exception e) {
                    Platform.runLater(() -> {
                        updateStatus("保存失败: " + e.getMessage());

                        // 检查是否是数据库结构问题
                        if (e.getMessage().contains("id_card_number")) {
                            handleDatabaseStructureError();
                        } else {
                            showError("保存失败", "称重记录保存到数据库失败: " + e.getMessage());
                        }
                        logger.error("称重记录保存失败", e);
                    });
                }
            });
        } else {
            logger.warn("数据仓库未初始化，无法保存到数据库");
        }

        updateRatios();
        refreshAdminTable();
    }

    /**
     * 更新烟农统计
     */
    private void updateFarmerStatistics(WeighingRecord record) {
        // TODO: 实现统计更新逻辑
    }

    /**
     * 重置称重UI
     */
    private void resetWeighingUI() {
        // 重置部叶按钮状态
        upperLeafButton.getStyleClass().removeAll("selected");
        middleLeafButton.getStyleClass().removeAll("selected");
        lowerLeafButton.getStyleClass().removeAll("selected");

        // 重置确认按钮状态
        confirmButton.setDisable(true);

        // 重置重量显示
        currentWeightLabel.setText("0.00 kg");

        // 更新比例
        updateRatios();
    }

    /**
     * 加载烟农统计数据
     */
    private void loadFarmerStatistics() {
        // TODO: 从数据库加载烟农统计数据
    }

    /**
     * 加载称重记录
     */
    private void loadWeighingRecords() {
        if (weighingRecordRepository != null) {
            weighingRecordRepository.findAll(new WeighingRecordRepository.OnResultListener<List<WeighingRecord>>() {
                @Override
                public void onSuccess(List<WeighingRecord> records) {
                    Platform.runLater(() -> {
                        weighingRecordsList.clear();
                        weighingRecordsList.addAll(records);
                        logger.info("从数据库加载了 {} 条称重记录", records.size());
                        updateStatus("已加载 " + records.size() + " 条称重记录");
                        refreshAdminTable();
                    });
                }

                @Override
                public void onError(Exception e) {
                    Platform.runLater(() -> {
                        logger.error("加载称重记录失败", e);
                        updateStatus("加载记录失败: " + e.getMessage());
                        showError("加载失败", "无法从数据库加载称重记录: " + e.getMessage());
                    });
                }
            });
        } else {
            logger.warn("数据仓库未初始化，无法从数据库加载记录");
        }
    }

    /**
     * 更新烟农数据显示
     */
    private void updateFarmerDataDisplay() {
        // TODO: 更新烟农数据表格显示
    }

    /**
     * 导出所有数据到Excel文件
     */
    private void exportAllData() {
        try {
            // 显示进度条
            progressBar.setVisible(true);
            updateStatus("正在准备导出数据...");

            // 在后台线程执行导出操作
            new Thread(() -> {
                try {
                    // 创建Excel工作簿
                    Workbook workbook = new XSSFWorkbook();
                    Sheet sheet = workbook.createSheet("预检记录");

                    // 创建标题行样式
                    CellStyle headerStyle = workbook.createCellStyle();
                    Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerFont.setFontHeightInPoints((short) 12);
                    headerStyle.setFont(headerFont);
                    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    headerStyle.setBorderBottom(BorderStyle.THIN);
                    headerStyle.setBorderTop(BorderStyle.THIN);
                    headerStyle.setBorderRight(BorderStyle.THIN);
                    headerStyle.setBorderLeft(BorderStyle.THIN);

                    // 创建数据行样式
                    CellStyle dataStyle = workbook.createCellStyle();
                    dataStyle.setBorderBottom(BorderStyle.THIN);
                    dataStyle.setBorderTop(BorderStyle.THIN);
                    dataStyle.setBorderRight(BorderStyle.THIN);
                    dataStyle.setBorderLeft(BorderStyle.THIN);

                    // 创建标题行
                    Row headerRow = sheet.createRow(0);
                    String[] headers = { "序号", "烟农姓名", "身份证号", "合同号", "部叶类型", "重量(kg)", "称重时间", "预检编号" };

                    for (int i = 0; i < headers.length; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                        sheet.setColumnWidth(i, 15 * 256); // 设置列宽
                    }

                    // 从数据库获取所有记录
                    final List<WeighingRecord> allRecords;
                    try {
                        allRecords = weighingRecordRepository.findAllSync();
                    } catch (Exception e) {
                        logger.error("导出时获取记录失败", e);
                        Platform.runLater(() -> {
                            showError("导出失败", "无法从数据库获取记录: " + e.getMessage());
                        });
                        return;
                    }

                    // 填充数据
                    int rowNum = 1;
                    for (WeighingRecord record : allRecords) {
                        Row row = sheet.createRow(rowNum++);

                        row.createCell(0).setCellValue(rowNum - 1); // 序号
                        row.createCell(1).setCellValue(record.getFarmerName()); // 烟农姓名
                        row.createCell(2).setCellValue(record.getIdCardNumber()); // 身份证号
                        row.createCell(3).setCellValue(record.getContractNumber()); // 合同号
                        row.createCell(4).setCellValue(record.getLeafType()); // 部叶类型
                        row.createCell(5).setCellValue(record.getWeight()); // 重量
                        row.createCell(6).setCellValue(
                                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getTimestamp())); // 称重时间
                        row.createCell(7).setCellValue(record.getPrecheckId()); // 预检编号

                        // 应用样式
                        for (int i = 0; i < 8; i++) {
                            row.getCell(i).setCellStyle(dataStyle);
                        }
                    }

                    // 创建导出文件夹
                    File exportDir = new File("exports");
                    if (!exportDir.exists()) {
                        exportDir.mkdirs();
                    }

                    // 生成文件名
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String fileName = "预检记录_" + timestamp + ".xlsx";
                    File file = new File(exportDir, fileName);

                    // 保存文件
                    try (FileOutputStream fileOut = new FileOutputStream(file)) {
                        workbook.write(fileOut);
                    }
                    workbook.close();

                    // 在UI线程显示成功消息
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        updateStatus("数据导出完成: " + fileName);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("导出成功");
                        alert.setHeaderText(null);
                        alert.setContentText("所有预检记录已成功导出到:\n" + file.getAbsolutePath() + "\n\n共导出 "
                                + allRecords.size() + " 条记录");

                        // 添加打开文件夹的按钮
                        ButtonType openFolderButton = new ButtonType("打开文件夹");
                        alert.getButtonTypes().add(openFolderButton);

                        alert.showAndWait().ifPresent(response -> {
                            if (response == openFolderButton) {
                                openExportFolder();
                            }
                        });
                    });

                } catch (Exception e) {
                    logger.error("导出数据失败", e);
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        updateStatus("导出失败");
                        showError("导出失败", "导出数据时发生错误:\n" + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("启动导出任务失败", e);
            progressBar.setVisible(false);
            updateStatus("导出失败");
            showError("导出失败", "无法启动导出任务:\n" + e.getMessage());
        }
    }

    /**
     * 打开导出文件夹
     */
    private void openExportFolder() {
        try {
            File exportDir = new File("exports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            // 使用系统默认程序打开文件夹
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                Runtime.getRuntime().exec("explorer.exe " + exportDir.getAbsolutePath());
            } else {
                Runtime.getRuntime().exec("open " + exportDir.getAbsolutePath());
            }

            updateStatus("已打开导出文件夹");

        } catch (Exception e) {
            logger.error("打开导出文件夹失败", e);
            showError("打开失败", "无法打开导出文件夹:\n" + e.getMessage());
        }
    }

    /**
     * 查看导出文件
     */
    private void viewExportFiles() {
        // TODO: 实现查看导出文件功能
        showInfo("文件查看", "查看导出文件功能待实现");
    }

    /**
     * 更新状态栏
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
        logger.info("状态更新: {}", status);
    }

    /**
     * 显示信息对话框
     */
    private void showInfo(String title, String message) {
        operationTipLabel.setText(message);
        operationTipLabel.getStyleClass().removeAll("error", "success");
        operationTipLabel.getStyleClass().add("success");
    }

    /**
     * 显示错误对话框
     */
    private void showError(String title, String message) {
        operationTipLabel.setText(message);
        operationTipLabel.getStyleClass().removeAll("error", "success");
        operationTipLabel.getStyleClass().add("error");
    }

    /**
     * 计算并刷新预检比例、上中下部叶比例
     */
    private void updateRatios() {
        double totalWeight = 0.0;
        double upperWeight = 0.0, middleWeight = 0.0, lowerWeight = 0.0;
        String currentFarmer = farmerNameField.getText().trim();
        double farmerTotalWeight = 0.0;

        for (WeighingRecord record : weighingRecordsList) {
            double w = record.getWeight();
            totalWeight += w;
            if ("上部叶".equals(record.getLeafType()))
                upperWeight += w;
            if ("中部叶".equals(record.getLeafType()))
                middleWeight += w;
            if ("下部叶".equals(record.getLeafType()))
                lowerWeight += w;
            if (record.getFarmerName().equals(currentFarmer))
                farmerTotalWeight += w;
        }

        if (totalWeight > 0) {
            upperRatioField.setText(String.format("%.1f%%", upperWeight * 100.0 / totalWeight));
            middleRatioField.setText(String.format("%.1f%%", middleWeight * 100.0 / totalWeight));
            lowerRatioField.setText(String.format("%.1f%%", lowerWeight * 100.0 / totalWeight));
            precheckRatioField.setText(String.format("%.1f%%", farmerTotalWeight * 100.0 / totalWeight));
        } else {
            upperRatioField.setText("0.0%");
            middleRatioField.setText("0.0%");
            lowerRatioField.setText("0.0%");
            precheckRatioField.setText("0.0%");
        }
    }

    private void setupAdminTable() {
        adminTable = new TableView<>();
        TableColumn<FarmerStats, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data -> data.getValue().farmerNameProperty());
        TableColumn<FarmerStats, String> idCol = new TableColumn<>("身份证号");
        idCol.setCellValueFactory(data -> data.getValue().idCardProperty());
        TableColumn<FarmerStats, Integer> countCol = new TableColumn<>("称重次数");
        countCol.setCellValueFactory(data -> data.getValue().countProperty().asObject());
        TableColumn<FarmerStats, Double> weightCol = new TableColumn<>("总重量(kg)");
        weightCol.setCellValueFactory(data -> data.getValue().totalWeightProperty().asObject());
        TableColumn<FarmerStats, Void> viewCol = new TableColumn<>("查看");
        viewCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("查看");
            {
                btn.setOnAction(e -> {
                    FarmerStats stats = getTableView().getItems().get(getIndex());
                    showFarmerRecords(stats);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        adminTable.setPrefHeight(200);
        adminTable.getColumns().setAll(nameCol, idCol, countCol, weightCol, viewCol);
        if (!adminTableContainer.getChildren().contains(adminTable)) {
            adminTableContainer.getChildren().add(adminTable);
        }
        refreshAdminTable();
    }

    private void showFarmerRecords(FarmerStats stats) {
        // 弹出子表格窗口，显示所有预检记录
        Stage dialog = new Stage();
        dialog.setTitle("预检记录 - " + stats.farmerNameProperty().get());
        TableView<com.tobacco.weight.data.WeighingRecord> recordTable = new TableView<>();
        TableColumn<com.tobacco.weight.data.WeighingRecord, String> precheckCol = new TableColumn<>("预检编号");
        precheckCol.setCellValueFactory(new PropertyValueFactory<>("precheckId"));
        TableColumn<com.tobacco.weight.data.WeighingRecord, String> leafCol = new TableColumn<>("部叶类型");
        leafCol.setCellValueFactory(new PropertyValueFactory<>("leafType"));
        TableColumn<com.tobacco.weight.data.WeighingRecord, Double> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        TableColumn<com.tobacco.weight.data.WeighingRecord, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        TableColumn<com.tobacco.weight.data.WeighingRecord, Void> exportCol = new TableColumn<>("操作");
        exportCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("导出");
            {
                btn.setOnAction(e -> {
                    com.tobacco.weight.data.WeighingRecord rec = getTableView().getItems().get(getIndex());
                    exportRecord(rec);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        recordTable.getColumns().addAll(precheckCol, leafCol, weightCol, timeCol, exportCol);
        recordTable.getItems().setAll(stats.getRecords());
        dialog.setScene(new javafx.scene.Scene(new VBox(recordTable)));
        dialog.setWidth(700);
        dialog.setHeight(400);
        dialog.show();
    }

    private void exportRecord(com.tobacco.weight.data.WeighingRecord record) {
        try {
            // 显示进度提示
            updateStatus("正在导出记录: " + record.getPrecheckId());

            // 在后台线程执行导出操作
            new Thread(() -> {
                try {
                    // 创建Excel工作簿
                    Workbook workbook = new XSSFWorkbook();
                    Sheet sheet = workbook.createSheet("预检记录详情");

                    // 创建标题行样式
                    CellStyle headerStyle = workbook.createCellStyle();
                    Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerFont.setFontHeightInPoints((short) 12);
                    headerStyle.setFont(headerFont);
                    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    headerStyle.setBorderBottom(BorderStyle.THIN);
                    headerStyle.setBorderTop(BorderStyle.THIN);
                    headerStyle.setBorderRight(BorderStyle.THIN);
                    headerStyle.setBorderLeft(BorderStyle.THIN);

                    // 创建数据行样式
                    CellStyle dataStyle = workbook.createCellStyle();
                    dataStyle.setBorderBottom(BorderStyle.THIN);
                    dataStyle.setBorderTop(BorderStyle.THIN);
                    dataStyle.setBorderRight(BorderStyle.THIN);
                    dataStyle.setBorderLeft(BorderStyle.THIN);

                    // 创建标题行
                    Row headerRow = sheet.createRow(0);
                    String[] headers = { "字段", "值" };

                    for (int i = 0; i < headers.length; i++) {
                        org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);
                        cell.setCellStyle(headerStyle);
                        sheet.setColumnWidth(i, 20 * 256); // 设置列宽
                    }

                    // 填充数据
                    int rowNum = 1;
                    String[][] data = {
                            { "预检编号", record.getPrecheckId() },
                            { "烟农姓名", record.getFarmerName() },
                            { "身份证号", record.getIdCardNumber() != null ? record.getIdCardNumber() : "" },
                            { "合同号", record.getContractNumber() != null ? record.getContractNumber() : "" },
                            { "部叶类型", record.getLeafType() },
                            { "重量(kg)", String.valueOf(record.getWeight()) },
                            { "称重时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(record.getTimestamp()) },
                            { "操作员", record.getOperator() != null ? record.getOperator() : "" },
                            { "仓库编号", record.getWarehouseNumber() != null ? record.getWarehouseNumber() : "" },
                            { "状态", record.getStatus() != null ? record.getStatus() : "正常" }
                    };

                    for (String[] rowData : data) {
                        Row row = sheet.createRow(rowNum++);

                        org.apache.poi.ss.usermodel.Cell fieldCell = row.createCell(0);
                        fieldCell.setCellValue(rowData[0]);
                        fieldCell.setCellStyle(dataStyle);

                        org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(1);
                        valueCell.setCellValue(rowData[1]);
                        valueCell.setCellStyle(dataStyle);
                    }

                    // 创建导出文件夹
                    File exportDir = new File("exports");
                    if (!exportDir.exists()) {
                        exportDir.mkdirs();
                    }

                    // 生成文件名
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    String fileName = "预检记录_" + record.getPrecheckId() + "_" + timestamp + ".xlsx";
                    File file = new File(exportDir, fileName);

                    // 保存文件
                    try (FileOutputStream fileOut = new FileOutputStream(file)) {
                        workbook.write(fileOut);
                    }
                    workbook.close();

                    // 在UI线程显示成功消息
                    Platform.runLater(() -> {
                        updateStatus("记录导出完成: " + fileName);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("导出成功");
                        alert.setHeaderText(null);
                        alert.setContentText("预检记录已成功导出到:\n" + file.getAbsolutePath());

                        // 添加打开文件夹的按钮
                        ButtonType openFolderButton = new ButtonType("打开文件夹");
                        alert.getButtonTypes().add(openFolderButton);

                        alert.showAndWait().ifPresent(response -> {
                            if (response == openFolderButton) {
                                openExportFolder();
                            }
                        });
                    });

                } catch (Exception e) {
                    logger.error("导出记录失败", e);
                    Platform.runLater(() -> {
                        updateStatus("导出失败: " + e.getMessage());
                        showError("导出失败", "导出记录时发生错误: " + e.getMessage());
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("导出记录失败", e);
            showError("导出失败", "导出记录时发生错误: " + e.getMessage());
        }
    }

    private void refreshAdminTable() {
        // 以身份证号为主键分组，若无身份证则用姓名分组
        Map<String, List<WeighingRecord>> grouped = weighingRecordsList.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> {
                    String id = r.getIdCardNumber();
                    if (id != null && !id.isEmpty())
                        return id;
                    // 若身份证号为空则用姓名分组
                    return r.getFarmerName() != null ? r.getFarmerName() : "未知";
                }));
        java.util.List<FarmerStats> stats = new java.util.ArrayList<>();
        for (Map.Entry<String, List<WeighingRecord>> entry : grouped.entrySet()) {
            List<WeighingRecord> list = entry.getValue();
            String name = list.isEmpty() ? "" : list.get(0).getFarmerName();
            String idCard = list.isEmpty() ? "" : list.get(0).getIdCardNumber();
            int count = list.size();
            double totalWeight = list.stream().mapToDouble(WeighingRecord::getWeight).sum();
            stats.add(new FarmerStats(name, idCard, count, totalWeight, list));
        }
        System.out.println("管理员表分组后总农户数: " + stats.size());
        adminTable.getItems().setAll(stats);
    }

    /**
     * 处理数据库结构错误
     */
    private void handleDatabaseStructureError() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("数据库结构错误");
        alert.setHeaderText("检测到数据库结构不匹配");
        alert.setContentText("数据库表结构需要更新以支持新功能。\n\n" +
                "选择操作：\n" +
                "• 自动修复：尝试自动更新数据库结构（推荐）\n" +
                "• 重建数据库：删除所有数据并重新创建数据库\n" +
                "• 取消：暂时跳过保存");

        ButtonType autoFixButton = new ButtonType("自动修复");
        ButtonType rebuildButton = new ButtonType("重建数据库");
        ButtonType cancelButton = new ButtonType("取消");

        alert.getButtonTypes().setAll(autoFixButton, rebuildButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == autoFixButton) {
                autoFixDatabase();
            } else if (response == rebuildButton) {
                rebuildDatabase();
            } else {
                // 用户选择取消，暂时跳过保存
                updateStatus("已跳过保存，请稍后重试");
            }
        });
    }

    /**
     * 自动修复数据库
     */
    private void autoFixDatabase() {
        try {
            updateStatus("正在修复数据库结构...");

            // 重新初始化数据库管理器，触发迁移
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            weighingRecordRepository = new WeighingRecordRepository(databaseManager);

            updateStatus("数据库修复完成，请重试保存");
            showInfo("修复完成", "数据库结构已自动修复，请重新进行称重操作");

        } catch (Exception e) {
            logger.error("自动修复数据库失败", e);
            showError("修复失败", "自动修复数据库失败: " + e.getMessage());
        }
    }

    /**
     * 重建数据库
     */
    private void rebuildDatabase() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认重建数据库");
        confirmAlert.setHeaderText("警告：此操作将删除所有数据");
        confirmAlert.setContentText("重建数据库将删除所有现有的称重记录和烟农信息。\n\n此操作不可撤销，确定要继续吗？");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    updateStatus("正在重建数据库...");

                    DatabaseManager databaseManager = DatabaseManager.getInstance();
                    databaseManager.rebuildTables();
                    weighingRecordRepository = new WeighingRecordRepository(databaseManager);

                    // 清空内存数据
                    weighingRecordsList.clear();
                    farmerStatisticsList.clear();

                    updateStatus("数据库重建完成");
                    showInfo("重建完成", "数据库已重建完成，可以开始新的称重操作");

                } catch (Exception e) {
                    logger.error("重建数据库失败", e);
                    showError("重建失败", "重建数据库失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 打开硬件诊断窗口
     */
    private void openDiagnosticsWindow() {
        if (diagnosticsWindow == null) {
            diagnosticsWindow = new HardwareDiagnosticsWindow(idCardReader, scaleManager, printerManager);
        }
        diagnosticsWindow.show();
    }

    /**
     * 连接打印机
     */
    private void connectPrinter() {
        // 如果已经连接，则断开连接
        if (serialPrinter != null && serialPrinter.isConnected()) {
            disconnectPrinter();
            return;
        }

        try {
            updateStatus("正在扫描打印机...");
            printerStatusLabel.setText("打印机: 扫描中...");

            // 在后台线程执行打印机连接
            new Thread(() -> {
                try {
                    // 扫描可用串口
                    String[] availablePorts = WindowsSerialPrinter.getAvailablePorts();
                    String[] printerPorts = WindowsSerialPrinter.findPrinterPorts();

                    Platform.runLater(() -> {
                        logger.info("发现 {} 个串口，{} 个可能的打印机端口",
                                availablePorts.length, printerPorts.length);

                        // 检查是否有虚拟端口（开发环境）
                        boolean hasVirtualPorts = false;
                        for (String port : availablePorts) {
                            if (port.startsWith("CNCA") || port.startsWith("CNCB")) {
                                hasVirtualPorts = true;
                                break;
                            }
                        }

                        if (hasVirtualPorts && printerPorts.length == 0) {
                            // 开发环境：有虚拟端口但没有真实打印机，启动模拟器
                            logger.info("检测到开发环境（有com0com但无真实打印机），启动模拟器");
                            startPrinterSimulator();
                        } else if (printerPorts.length > 0) {
                            // 生产环境或开发环境有真实打印机：尝试连接所有可能的打印机端口
                            logger.info("检测到 {} 个可能的打印机端口，开始自动连接", printerPorts.length);
                            tryConnectAllPrinterPorts(printerPorts);
                        } else {
                            // 没有发现任何可用端口
                            updateStatus("未找到可用的打印机端口，也未找到虚拟端口");
                            printerStatusLabel.setText("打印机: 无可用端口");
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logger.error("扫描打印机端口失败", e);
                        updateStatus("打印机扫描失败: " + e.getMessage());
                        printerStatusLabel.setText("打印机: 连接失败");
                    });
                }
            }).start();

        } catch (Exception e) {
            logger.error("连接打印机失败", e);
            updateStatus("连接打印机失败: " + e.getMessage());
            printerStatusLabel.setText("打印机: 连接失败");
        }
    }

    /**
     * 穷尽尝试连接所有可能的打印机端口
     */
    private void tryConnectAllPrinterPorts(String[] printerPorts) {
        // 在后台线程尝试连接
        new Thread(() -> {
            boolean connected = false;
            String connectedPort = null;

            Platform.runLater(() -> {
                updateStatus("正在尝试连接打印机端口...");
                printerStatusLabel.setText("打印机: 连接中...");
            });

            // 尝试所有端口，每个端口尝试多种波特率
            int[] baudRates = { 115200, 9600, 19200, 38400, 57600 };

            for (String port : printerPorts) {
                boolean portConnected = false;

                for (int baudRate : baudRates) {
                    try {
                        Platform.runLater(() -> {
                            updateStatus("正在尝试连接: " + port + " (波特率: " + baudRate + ")");
                            logger.info("尝试连接端口: {} 波特率: {}", port, baudRate);
                        });

                        // 尝试连接端口
                        if (serialPrinter.open(port, baudRate)) {
                            // 测试连接
                            if (serialPrinter.testConnection()) {
                                connected = true;
                                connectedPort = port + " (波特率: " + baudRate + ")";
                                portConnected = true;
                                logger.info("成功连接到打印机端口: {} 波特率: {}", port, baudRate);
                                break;
                            } else {
                                // 连接失败，关闭端口
                                serialPrinter.close();
                                logger.debug("端口 {} 波特率 {} 连接成功但测试失败", port, baudRate);
                            }
                        } else {
                            logger.debug("无法打开端口: {} 波特率: {}", port, baudRate);
                        }

                        // 稍微延时避免过快切换
                        Thread.sleep(200);

                    } catch (Exception e) {
                        logger.debug("尝试连接端口 {} 波特率 {} 时出现异常: {}", port, baudRate, e.getMessage());
                        try {
                            serialPrinter.close();
                        } catch (Exception ignored) {
                        }
                    }
                }

                // 如果这个端口成功连接，则退出端口循环
                if (portConnected) {
                    break;
                }
            }

            // 更新最终结果
            final boolean finalConnected = connected;
            final String finalPort = connectedPort;

            Platform.runLater(() -> {
                if (finalConnected) {
                    updateStatus("已连接到打印机: " + finalPort);
                    printerStatusLabel.setText("打印机: 已连接 (" + finalPort + ")");
                    printerTestButton.setDisable(false);
                    printerConnectButton.setText("断开连接");
                    logger.info("打印机连接成功: {}", finalPort);
                } else {
                    updateStatus("尝试了所有端口，均无法连接到打印机");
                    printerStatusLabel.setText("打印机: 连接失败");
                    logger.warn("穷尽尝试 {} 个端口后仍无法连接到打印机", printerPorts.length);
                }
            });

        }).start();
    }

    /**
     * 启动打印机模拟器
     */
    private void startPrinterSimulator() {
        try {
            // 查找com0com端口
            String[] availablePorts = WindowsSerialPrinter.getAvailablePorts();
            String simulatorPort = null;
            String clientPort = null;

            // 寻找成对的com0com端口
            for (String port : availablePorts) {
                if (port.startsWith("CNCA") || port.startsWith("CNCB")) {
                    if (port.endsWith("0")) {
                        simulatorPort = port;
                        clientPort = port.startsWith("CNCA") ? "CNCB0" : "CNCA0";
                    } else if (port.endsWith("4")) {
                        simulatorPort = port;
                        clientPort = port.startsWith("CNCA") ? "CNCB4" : "CNCA4";
                    }
                    if (simulatorPort != null)
                        break;
                }
            }

            if (simulatorPort != null) {
                // 启动模拟器
                printerSimulator.start(simulatorPort);

                // 连接到对应端口
                if (serialPrinter.open(clientPort)) {
                    updateStatus("已连接到打印机模拟器");
                    printerStatusLabel.setText("打印机: 已连接 (模拟器)");
                    printerTestButton.setDisable(false);
                    printerConnectButton.setText("断开连接");

                    logger.info("打印机模拟器已启动，监听端口: {}，客户端连接: {}", simulatorPort, clientPort);
                } else {
                    updateStatus("无法连接到模拟器端口: " + clientPort);
                    printerStatusLabel.setText("打印机: 连接失败");
                }
            } else {
                updateStatus("未找到com0com虚拟端口，请安装com0com");
                printerStatusLabel.setText("打印机: 需要com0com");
            }

        } catch (Exception e) {
            logger.error("启动打印机模拟器失败", e);
            updateStatus("启动模拟器失败: " + e.getMessage());
            printerStatusLabel.setText("打印机: 模拟器失败");
        }
    }

    /**
     * 连接到串口打印机
     */
    private void connectToSerialPrinter(String port) {
        try {
            if (serialPrinter.open(port)) {
                // 测试连接
                if (serialPrinter.testConnection()) {
                    updateStatus("已连接到打印机: " + port);
                    printerStatusLabel.setText("打印机: 已连接 (" + port + ")");
                    printerTestButton.setDisable(false);
                    printerConnectButton.setText("断开连接");

                    logger.info("成功连接到打印机端口: {}", port);
                } else {
                    serialPrinter.close();
                    updateStatus("打印机响应测试失败: " + port);
                    printerStatusLabel.setText("打印机: 测试失败");
                }
            } else {
                updateStatus("无法打开端口: " + port);
                printerStatusLabel.setText("打印机: 端口失败");
            }

        } catch (Exception e) {
            logger.error("连接串口打印机失败", e);
            updateStatus("连接失败: " + e.getMessage());
            printerStatusLabel.setText("打印机: 连接错误");
        }
    }

    /**
     * 断开打印机连接
     */
    private void disconnectPrinter() {
        try {
            if (serialPrinter != null) {
                serialPrinter.close();
            }
            if (printerSimulator != null) {
                printerSimulator.stop();
            }

            updateStatus("打印机已断开连接");
            printerStatusLabel.setText("打印机: 未连接");
            printerTestButton.setDisable(true);
            printerConnectButton.setText("连接打印机");

            logger.info("打印机连接已断开");

        } catch (Exception e) {
            logger.error("断开打印机连接失败", e);
            updateStatus("断开连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试打印机
     */
    private void testPrinter() {
        if (serialPrinter == null || !serialPrinter.isConnected()) {
            showError("测试失败", "打印机未连接");
            return;
        }

        try {
            updateStatus("正在测试打印...");

            // 生成测试小票
            EscBuilder esc = new EscBuilder();
            byte[] testReceipt = esc.reset()
                    .align(1).bold(true).fontSize(2, 2)
                    .text("打印机测试").feed()
                    .reset().align(0).fontSize(1, 1)
                    .text("测试时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).feed()
                    .text("系统: 烟叶称重系统 - Windows版").feed()
                    .separator('-', 32).feed()
                    .text("✓ 连接正常").feed()
                    .text("✓ 指令发送成功").feed()
                    .text("✓ 测试完成").feed()
                    .separator('=', 32).feed(3)
                    .cut()
                    .build();

            // 发送到打印机
            if (serialPrinter.write(testReceipt)) {
                updateStatus("测试打印完成");
                showInfo("测试成功", "打印机测试完成，请检查打印输出");
                logger.info("打印机测试完成，发送了 {} 字节", testReceipt.length);
            } else {
                updateStatus("测试打印失败");
                showError("测试失败", "无法发送测试数据到打印机");
            }

        } catch (Exception e) {
            logger.error("测试打印失败", e);
            updateStatus("测试打印失败: " + e.getMessage());
            showError("测试失败", "打印机测试失败: " + e.getMessage());
        }
    }

    /**
     * 系统打印机测试
     */
    private void testSystemPrinter() {
        try {
            updateStatus("正在执行系统打印测试...");
            boolean ok = printerManager.testPrinter();
            if (ok) {
                updateStatus("系统打印测试完成");
                showInfo("系统打印", "已将测试内容发送至默认打印机队列，请检查POS80 Printer输出");
            } else {
                updateStatus("系统打印测试失败");
                showError("系统打印", "无法发送测试内容，请检查系统打印机配置");
            }
        } catch (Exception e) {
            logger.error("系统打印测试失败", e);
            updateStatus("系统打印测试失败: " + e.getMessage());
            showError("系统打印", "测试失败: " + e.getMessage());
        }
    }

    /**
     * 设置主舞台
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}