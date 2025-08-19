package com.tobacco.weight.ui;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.data.FarmerStatistics;
import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.util.QRCodeGenerator;
import com.tobacco.weight.database.DatabaseManager;
import java.awt.image.BufferedImage;
import com.tobacco.weight.database.WeighingRecordRepository;
import com.tobacco.weight.hardware.ScaleManager;
import com.tobacco.weight.hardware.PrinterManager;
import com.tobacco.weight.hardware.IdCardReader;
import com.tobacco.weight.service.AdminAuthService;

import com.tobacco.weight.ui.HardwareDiagnosticsWindow;
import com.tobacco.weight.ui.FarmerStats;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

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

    // 数据仓库
    private WeighingRecordRepository weighingRecordRepository;
    private DatabaseManager databaseManager;
    private com.tobacco.weight.database.FarmerInfoDao farmerInfoDao;

    // 防抖定时器，避免快速输入时触发多次查询
    private java.util.Timer debounceTimer;

    // 缓存最近查询的结果，避免重复查询
    private java.util.Map<String, FarmerInfoAndContractAmount> queryCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int CACHE_SIZE_LIMIT = 100; // 缓存大小限制

    // 查询状态管理，避免重复查询
    private String lastQueriedIdCard = null;
    private long lastQueryTime = 0;
    private static final long QUERY_COOLDOWN_MS = 2000; // 2秒冷却时间

    // 当前活动的文本框（用于数字键盘输入）
    private TextField currentActiveTextField;

    // 服务
    private AdminAuthService adminAuthService;

    // 数据
    private ObservableList<FarmerStatistics> farmerStatisticsList;
    private ObservableList<WeighingRecord> weighingRecordsList;

    // UI组件 - 称重区域
    @FXML
    private TextField farmerNameField;
    @FXML
    private TextField contractNumberField;
    @FXML
    private TextField idCardNumberField;

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
    private Label idCardStatusIcon;
    @FXML
    private TextField bundleCountField;

    // UI组件 - 统计区域
    @FXML
    private TextField contractAmountField;
    @FXML
    private TextField precheckRatioField;
    @FXML
    private TextField upperRatioField;
    @FXML
    private TextField middleRatioField;
    @FXML
    private TextField lowerRatioField;

    // UI组件 - 管理区域
    @FXML
    private VBox farmerDataContainer;
    @FXML
    private Button adminLoginButton;
    @FXML
    private HBox adminStatusContainer;
    @FXML
    private Label adminStatusLabel;
    @FXML
    private Button openAdminPanelButton;
    @FXML
    private Button exportAllDataButton;
    @FXML
    private Button adminLogoutButton;
    @FXML
    private VBox adminTableContainer;

    // UI组件 - 布局容器（用于响应式设置）
    @FXML
    private VBox rootContainer; // FXML中的主VBox
    @FXML
    private HBox mainContentArea; // 主要内容区域的HBox

    // Admin table for farmer statistics
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

    // 打印机测试UI组件（已删除）

    // 农户注册管理按钮
    @FXML
    private Button farmerRegistrationButton;

    // 标签预览区域
    @FXML
    private VBox labelPreviewArea;

    @FXML
    private ImageView qrCodeImageView;

    @FXML
    private VBox labelInfoContainer;

    // 数字键盘按钮
    @FXML
    private Button key0, key1, key2, key3, key4, key5, key6, key7, key8, key9;
    @FXML
    private Button keyClear, keyBack;

    private HardwareDiagnosticsWindow diagnosticsWindow;

    // 定时器用于刷新重量显示
    private java.util.Timer weightRefreshTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化主界面控制器");

        // 初始化数据列表
        farmerStatisticsList = FXCollections.observableArrayList();
        weighingRecordsList = FXCollections.observableArrayList();

        // 初始化数据仓库和服务
        databaseManager = DatabaseManager.getInstance();
        weighingRecordRepository = new WeighingRecordRepository(databaseManager);
        farmerInfoDao = new com.tobacco.weight.database.FarmerInfoDao(databaseManager);
        adminAuthService = AdminAuthService.getInstance();

        // 显示数据库路径
        String dbPath = databaseManager.getDbPath();
        logger.info("数据库文件位置: {}", dbPath);
        // 数据库文件位置已初始化

        // 初始化硬件管理器
        initializeHardwareManagers();

        // 设置UI事件监听器
        setupEventHandlers();

        // 初始化UI状态
        initializeUIState();

        // 加载初始数据
        loadInitialData();

        // 初始化管理员状态
        updateAdminLoginStatus();

        // 设置响应式布局
        setupResponsiveLayout();

        logger.info("主界面控制器初始化完成");

        // 定时刷新重量显示
        weightRefreshTimer = new java.util.Timer(true);
        weightRefreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (scaleManager != null) {
                    double weight = scaleManager.getCurrentWeight();
                    Platform.runLater(() -> {
                        if (currentWeightLabel != null) {
                            currentWeightLabel.setText(String.format("%.2f kg", weight));
                        }
                    });
                }
            }
        }, 0, 1000); // 每秒刷新一次

        // 初始化封签预览区域
        updateLabelPreview();
    }

    /**
     * 初始化硬件管理器
     */
    private void initializeHardwareManagers() {
        try {
            scaleManager = new ScaleManager();
            printerManager = new PrinterManager();

            // 身份证读卡器初始化 - 单独处理可能的native库加载错误
            try {
                idCardReader = new IdCardReader();

                // 设置身份证读卡器监听器
                idCardReader.setOnIdCardRead(this::handleIdCardRead);
                idCardReader.setOnConnectionStatusChanged(this::updateIdCardStatus);
                idCardReader.setOnErrorOccurred(this::handleIdCardError);

                // 检查连接状态 - 在注册回调后调用
                idCardReader.checkConnectionStatus();

                // 如果初始化期间已发生错误（构造函数先于回调设置执行），此处补发一次详细错误
                if (!idCardReader.isConnected()) {
                    String detailed = idCardReader.getDetailedErrorMessage();
                    if (detailed != null && !detailed.trim().isEmpty()) {
                        handleIdCardError(detailed);
                    }
                }

                logger.info("身份证读卡器初始化成功");

            } catch (UnsatisfiedLinkError | Exception idCardEx) {
                logger.warn("身份证读卡器初始化失败，应用将在无身份证读取功能下运行: {}", idCardEx.getMessage());
                idCardReader = null;

                // 更新UI状态为不可用
                Platform.runLater(() -> {
                    if (idCardStatusIcon != null) {
                        idCardStatusIcon.setText("●");
                        idCardStatusIcon.setStyle("-fx-text-fill: #ff4444;");
                    }
                    if (readIdCardButton != null) {
                        readIdCardButton.setDisable(true);
                        readIdCardButton.setText("身份证功能不可用");
                    }
                });
            }

            // 设置其他硬件状态监听器
            scaleManager.setOnWeightChanged(this::updateCurrentWeight);
            scaleManager.setOnConnectionStatusChanged(this::updateScaleStatus);

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

        // 农户管理按钮

        // 系统打印测试按钮（已删除）

        // 农户注册管理按钮
        farmerRegistrationButton.setOnAction(e -> openFarmerRegistrationWindow());

        // 管理员登录相关按钮
        adminLoginButton.setOnAction(e -> openAdminLogin());
        openAdminPanelButton.setOnAction(e -> openAdminPanel());
        adminLogoutButton.setOnAction(e -> handleAdminLogout());

        // 数字键盘事件处理
        setupNumberKeypad();
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
        // 置空捆数，避免默认填入1
        bundleCountField.clear();

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

        // 设置预检编号（不再显示在UI上）
        // 废弃本地计数器，改用数据库的系统级序列
        // private int precheckCounter = -1;
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

        // 更新封签预览的文本字段（不更新二维码）
        updatePreviewLabelsOnly();
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

            // 验证捆数
            String bundleCountText = bundleCountField.getText().trim();
            int bundleCount = 1;
            try {
                bundleCount = Integer.parseInt(bundleCountText);
                if (bundleCount <= 0) {
                    showError("输入错误", "捆数必须为正整数");
                    return;
                }
            } catch (NumberFormatException e) {
                showError("输入错误", "请输入有效的捆数");
                return;
            }

            // 获取当前重量
            double totalWeight = scaleManager.getCurrentWeight();
            if (totalWeight <= 0) {
                showError("称重错误", "当前重量无效，请检查电子秤连接");
                return;
            }

            // 计算单捆重量
            double weight = totalWeight / bundleCount;

            // 获取选中的部叶类型
            String leafType = getSelectedLeafType();
            if (leafType == null) {
                showError("选择错误", "请选择部叶类型");
                return;
            }

            // 获取地址信息
            String stationName = getStationName(idCardNumber);
            String farmerAddress = getFarmerAddress(idCardNumber);
            String address = stationName;
            if (farmerAddress != null && !farmerAddress.trim().isEmpty() && !"待完善".equals(farmerAddress.trim())) {
                address = farmerAddress;
            } else {
                address = "实时录入";
            }

            // 称重信息记录

            // 生成新的预检编号：身份证后6位+合同号后6位+当前第几次称重（五位数）
            String idCardLast6 = idCardNumber.length() >= 6 ? idCardNumber.substring(idCardNumber.length() - 6)
                    : String.format("%06d", 0);
            String contractLast6 = contractNumber.length() >= 6 ? contractNumber.substring(contractNumber.length() - 6)
                    : String.format("%06d", 0);

            // 使用系统级全局序列（00001-99999）
            int seq = databaseManager.getAndIncrementPrecheckSeq();
            String weighingCountStr = String.format("%05d", seq);

            // 完整的预检编号
            String fullPrecheckId = idCardLast6 + contractLast6 + weighingCountStr;
            // 界面上只显示后5位
            String displayPrecheckId = weighingCountStr;

            // 更新日期为当天
            java.time.LocalDate today = java.time.LocalDate.now();

            // 创建称重记录
            WeighingRecord record = new WeighingRecord();
            record.setFarmerName(farmerName);
            record.setContractNumber(contractNumber);
            record.setLeafType(leafType);
            record.setWeight(weight);
            record.setOperator("操作员"); // TODO: 从系统获取当前用户
            // 设置预检编号（存储完整编号，显示后5位）
            record.setPrecheckId(fullPrecheckId);
            record.setIdCardNumber(idCardNumber);
            record.setBundleCount(bundleCount);
            record.setAddress(address);

            // 记录创建完成

            // 保存到数据库和列表
            saveWeighingRecord(record);
            updateFarmerStatistics(record);
            resetWeighingUI();
            updateRatios();

            // 更新封签预览区域，显示最终确认的信息
            updateFinalLabelPreview(farmerName, contractNumber, idCardNumber, address, leafType, displayPrecheckId,
                    today.toString(), weight);

            // 显示提示并询问是否打印小票
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.setTitle("称重完成");
            alert.setHeaderText("称重完成 - 预检号: " + getLast5Digits(displayPrecheckId));
            alert.setContentText(
                    leafType + " " + String.format("%.2f", weight) + "kg" + " (捆数: " + bundleCount + ")\n\n是否打印称重小票？");

            javafx.scene.control.ButtonType previewButton = new javafx.scene.control.ButtonType("预览小票");
            javafx.scene.control.ButtonType printButton = new javafx.scene.control.ButtonType("打印小票");
            javafx.scene.control.ButtonType skipButton = new javafx.scene.control.ButtonType("跳过",
                    javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(previewButton, printButton, skipButton);

            java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == previewButton) {
                    // 用户选择预览小票
                    showReceiptPreview(farmerName, contractNumber, leafType, weight, "操作员", bundleCount,
                            record.getPrecheckId());
                } else if (result.get() == printButton) {
                    // 用户选择打印小票，使用新的图片打印方法
                    printLabelWithQRCode(farmerName, contractNumber, leafType, weight, "操作员", bundleCount,
                            record.getPrecheckId());
                }
            }

        } catch (Exception e) {
            logger.error("确认称重异常", e);
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
            if (idCardReader == null) {
                showError("功能不可用", "身份证读卡器功能不可用，请手动输入农户信息");
                return;
            }

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

                    // 自动填充合同量
                    loadContractAmountForFarmer(farmerInfo.getContractNumber());

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
     * 为指定合同号加载合同量
     */
    private void loadContractAmountForFarmer(String contractNumber) {
        if (contractNumber == null || contractNumber.trim().isEmpty()) {
            contractAmountField.clear();
            return;
        }

        // 在后台线程查询合同量
        new Thread(() -> {
            try {
                loadContractAmountByContractNumberSync(contractNumber);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("加载合同量失败: {}", contractNumber, e);
                    contractAmountField.clear();
                    updateRatios();
                });
            }
        }).start();
    }

    /**
     * 根据身份证号一次性加载所有烟农信息（姓名、合同号、合同量）
     */
    private void loadFarmerInfoByIdCard(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            // 清空所有相关字段
            clearFarmerInfoFields();
            return;
        }

        // 检查冷却时间和重复查询
        long currentTime = System.currentTimeMillis();
        if (idCardNumber.equals(lastQueriedIdCard) &&
                (currentTime - lastQueryTime) < QUERY_COOLDOWN_MS) {
            // 静默跳过重复查询，不输出日志
            return;
        }

        // 更新查询状态
        lastQueriedIdCard = idCardNumber;
        lastQueryTime = currentTime;

        // 在后台线程一次性查询所有信息
        new Thread(() -> {
            try {
                // 一次性查询烟农信息和合同量
                FarmerInfoAndContractAmount result = loadFarmerInfoAndContractAmountByIdCard(idCardNumber.trim());

                Platform.runLater(() -> {
                    if (result != null && result.farmerInfo != null) {
                        // 找到烟农信息，一次性填充所有字段
                        farmerNameField.setText(result.farmerInfo.getFarmerName());
                        contractNumberField.setText(result.farmerInfo.getContractNumber());

                        if (result.contractAmount != null && result.contractAmount > 0) {
                            contractAmountField.setText(String.format("%.2f", result.contractAmount));
                            logger.info("根据身份证号一次性填充所有信息: {} -> 姓名: {}, 合同号: {}, 合同量: {}",
                                    idCardNumber, result.farmerInfo.getFarmerName(),
                                    result.farmerInfo.getContractNumber(), result.contractAmount);
                        } else {
                            contractAmountField.setText("0.00");
                            logger.info("根据身份证号填充烟农信息: {} -> 姓名: {}, 合同号: {}, 合同量: 0.00",
                                    idCardNumber, result.farmerInfo.getFarmerName(),
                                    result.farmerInfo.getContractNumber());
                        }

                        // 更新比例计算
                        updateRatios();
                        // 更新封签预览（包含地址信息）
                        updatePreviewLabelsOnly();

                    } else {
                        // 未找到烟农信息，尝试仅查询合同量
                        logger.info("未找到身份证号 {} 对应的烟农信息，尝试仅查询合同量", idCardNumber);
                        clearFarmerInfoFields();

                        // 尝试查询合同量
                        try {
                            loadContractAmountByIdCardSync(idCardNumber);
                        } catch (Exception e) {
                            logger.error("查询合同量失败: {}", e.getMessage());
                            contractAmountField.setText("0.00");
                            updateRatios();
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("根据身份证号加载烟农信息失败: {}", idCardNumber, e);
                    clearFarmerInfoFields();
                });
            }
        }).start();
    }

    /**
     * 根据身份证号加载合同量（异步版本，用于UI直接调用）
     */
    private void loadContractAmountByIdCard(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            contractAmountField.clear();
            return;
        }

        // 在后台线程查询合同量
        new Thread(() -> {
            try {
                loadContractAmountByIdCardSync(idCardNumber);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("根据身份证号加载合同量失败: {}", idCardNumber, e);
                    contractAmountField.setText("0.00"); // 出错时也设置为0
                    updateRatios();
                });
            }
        }).start();
    }

    /**
     * 根据身份证号同步加载合同量（用于内部调用，避免并发问题）
     */
    private void loadContractAmountByIdCardSync(String idCardNumber) throws Exception {
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            Platform.runLater(() -> {
                contractAmountField.clear();
                updateRatios();
            });
            return;
        }

        try {
            // 先根据身份证号查询合同号
            String contractNumber = weighingRecordRepository.getContractNumberByIdCard(idCardNumber);

            if (contractNumber != null) {
                // 再根据合同号查询合同量
                java.util.Map<String, Double> contractAmounts = weighingRecordRepository.getContractAmountsSync();
                Double amount = contractAmounts.get(contractNumber);

                Platform.runLater(() -> {
                    if (amount != null && amount > 0) {
                        contractAmountField.setText(String.format("%.2f", amount));
                        logger.info("根据身份证号自动填充合同量: {} -> 合同号: {} -> 合同量: {}", idCardNumber, contractNumber,
                                amount);
                    } else {
                        contractAmountField.setText("0.00"); // 如果为空则置0
                        logger.info("未找到身份证号 {} 对应的合同量信息，设置为0", idCardNumber);
                    }
                    // 更新比例计算
                    updateRatios();
                });
            } else {
                Platform.runLater(() -> {
                    contractAmountField.setText("0.00"); // 未找到合同号时设置为0
                    logger.info("未找到身份证号 {} 对应的合同号，设置为0", idCardNumber);
                    updateRatios();
                });
            }

        } catch (Exception e) {
            logger.error("根据身份证号同步加载合同量失败: {}", idCardNumber, e);
            throw e; // 重新抛出异常，让调用者处理
        }
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
            // logger.debug("在UI线程中更新重量显示: {} kg", weight);

            // 更新重量显示
            currentWeightLabel.setText(String.format("%.2f kg", weight));
            // logger.debug("重量标签已更新: {}", currentWeightLabel.getText());

            // 更新状态信息
            updateStatus(String.format("当前重量: %.2f kg", weight));

            // 实时更新标签预览
            updateLabelPreview();

            // 检查重量是否稳定（这里可以根据需要添加稳定性检测逻辑）
            if (weight > 0.01) {
                // 有重量时启用确认按钮
                confirmButton.setDisable(false);
                operationTipLabel.setText("请选择叶位类型");
                // logger.debug("确认按钮已启用");
            } else {
                // 无重量时禁用确认按钮
                confirmButton.setDisable(true);
                operationTipLabel.setText("请放置物品称重");
                // logger.debug("确认按钮已禁用");
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
        // System.out.println("称重记录总数: " + weighingRecordsList.size());

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
                        row.createCell(7).setCellValue(getLast5Digits(record.getPrecheckId())); // 预检编号

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
        if (operationTipLabel != null) {
            operationTipLabel.setText(status);
        }
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
     * 计算并刷新完成比例、上中下部叶比例
     * 完成比例 = 当前农户总重量 / 合同量
     * 部叶比例 = 各部叶总重量 / 所有部叶总重量之和
     */
    private void updateRatios() {
        String currentFarmer = farmerNameField.getText().trim();
        String currentContract = contractNumberField.getText().trim();

        // 计算当前农户的各类重量（使用总重量：单捆重量 × 捆数）
        double farmerTotalWeight = 0.0;
        double farmerUpperWeight = 0.0, farmerMiddleWeight = 0.0, farmerLowerWeight = 0.0;

        for (WeighingRecord record : weighingRecordsList) {
            // 只统计当前农户的记录
            if (record.getFarmerName().equals(currentFarmer)) {
                double totalWeight = record.getWeight() * record.getBundleCount();
                farmerTotalWeight += totalWeight;

                if ("上部叶".equals(record.getLeafType()))
                    farmerUpperWeight += totalWeight;
                else if ("中部叶".equals(record.getLeafType()))
                    farmerMiddleWeight += totalWeight;
                else if ("下部叶".equals(record.getLeafType()))
                    farmerLowerWeight += totalWeight;
            }
        }

        // 计算部叶比例（基于农户总重量）
        if (farmerTotalWeight > 0) {
            upperRatioField.setText(String.format("%.1f%%", farmerUpperWeight * 100.0 / farmerTotalWeight));
            middleRatioField.setText(String.format("%.1f%%", farmerMiddleWeight * 100.0 / farmerTotalWeight));
            lowerRatioField.setText(String.format("%.1f%%", farmerLowerWeight * 100.0 / farmerTotalWeight));
        } else {
            upperRatioField.setText("0.0%");
            middleRatioField.setText("0.0%");
            lowerRatioField.setText("0.0%");
        }

        // 计算完成比例（预检总量 / 合同量）
        try {
            String contractAmountText = contractAmountField.getText().trim();
            if (!contractAmountText.isEmpty() && farmerTotalWeight > 0) {
                double contractAmount = Double.parseDouble(contractAmountText);
                if (contractAmount > 0) {
                    double completionRatio = (farmerTotalWeight / contractAmount) * 100.0;
                    precheckRatioField.setText(String.format("%.1f%%", completionRatio));
                } else {
                    precheckRatioField.setText("0.0%");
                }
            } else {
                precheckRatioField.setText("0.0%");
            }
        } catch (NumberFormatException e) {
            precheckRatioField.setText("0.0%");
            logger.warn("合同量格式错误: {}", contractAmountField.getText());
        }
    }

    private void setupAdminTable() {
        adminTable = new TableView<>();

        // 设置表格列宽度策略为自适应内容
        adminTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<FarmerStats, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data -> data.getValue().farmerNameProperty());
        nameCol.setStyle("-fx-alignment: CENTER;");
        nameCol.setPrefWidth(80);

        TableColumn<FarmerStats, String> idCol = new TableColumn<>("身份证号");
        idCol.setCellValueFactory(data -> {
            String idCard = data.getValue().idCardProperty().get();
            if (idCard != null && !idCard.isEmpty()) {
                // 检查是否是真正的身份证号（18位数字+X）
                if (idCard.length() == 18 && idCard.matches("\\d{17}[\\dXx]")) {
                    // 这是真正的身份证号，显示脱敏版本
                    return new SimpleStringProperty(maskIdCardNumber(idCard));
                } else {
                    // 其他情况，可能是预检编号或其他数据
                    return new SimpleStringProperty("格式错误");
                }
            } else {
                return new SimpleStringProperty("");
            }
        });
        idCol.setStyle("-fx-alignment: CENTER;");
        idCol.setPrefWidth(150);

        TableColumn<FarmerStats, Integer> countCol = new TableColumn<>("称重次数");
        countCol.setCellValueFactory(data -> data.getValue().countProperty().asObject());
        countCol.setStyle("-fx-alignment: CENTER;");
        countCol.setPrefWidth(80);

        TableColumn<FarmerStats, Double> weightCol = new TableColumn<>("总重量(kg)");
        weightCol.setCellValueFactory(data -> data.getValue().totalWeightProperty().asObject());
        weightCol.setStyle("-fx-alignment: CENTER;");
        weightCol.setPrefWidth(100);

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
        viewCol.setStyle("-fx-alignment: CENTER;");
        viewCol.setPrefWidth(80);

        // 设置表格
        adminTable.setPrefHeight(200);
        adminTable.getColumns().setAll(nameCol, idCol, countCol, weightCol, viewCol);

        // 添加按钮容器
        HBox buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER_LEFT);
        buttonContainer.setPadding(new Insets(5));

        // 添加刷新按钮
        Button refreshButton = new Button("刷新数据");
        refreshButton.setOnAction(e -> refreshAdminTable());
        refreshButton.setStyle("-fx-font-size: 14px; -fx-padding: 8px 16px;");

        buttonContainer.getChildren().add(refreshButton);

        // 将按钮容器和表格添加到adminTableContainer
        if (!adminTableContainer.getChildren().contains(buttonContainer)) {
            adminTableContainer.getChildren().add(0, buttonContainer);
        }
        if (!adminTableContainer.getChildren().contains(adminTable)) {
            adminTableContainer.getChildren().add(adminTable);
        }

        refreshAdminTable();
    }

    private void showFarmerRecords(FarmerStats stats) {
        // 弹出子表格窗口，显示所有预检记录
        Stage dialog = new Stage();
        dialog.setTitle("预检记录 - " + stats.farmerNameProperty().get());
        dialog.setWidth(1100);
        TableView<com.tobacco.weight.data.WeighingRecord> recordTable = new TableView<>();
        recordTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        TableColumn<com.tobacco.weight.data.WeighingRecord, String> precheckCol = new TableColumn<>("预检编号");
        precheckCol.setCellValueFactory(new PropertyValueFactory<>("precheckId"));
        precheckCol.setPrefWidth(180);
        precheckCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<com.tobacco.weight.data.WeighingRecord, String> leafCol = new TableColumn<>("部叶类型");
        leafCol.setCellValueFactory(new PropertyValueFactory<>("leafType"));
        leafCol.setPrefWidth(80);
        leafCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<com.tobacco.weight.data.WeighingRecord, Integer> bundleCol = new TableColumn<>("捆数");
        bundleCol.setCellValueFactory(new PropertyValueFactory<>("bundleCount"));
        bundleCol.setPrefWidth(60);
        bundleCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<com.tobacco.weight.data.WeighingRecord, Double> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(cellData -> {
            com.tobacco.weight.data.WeighingRecord record = cellData.getValue();
            double singleBundleWeight = record.getWeight();
            return new javafx.beans.property.SimpleObjectProperty<>(singleBundleWeight);
        });
        weightCol.setPrefWidth(80);
        weightCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<com.tobacco.weight.data.WeighingRecord, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setPrefWidth(160);
        timeCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<com.tobacco.weight.data.WeighingRecord, Void> exportCol = new TableColumn<>("操作");
        exportCol.setPrefWidth(80);
        exportCol.setStyle("-fx-alignment: CENTER;");
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
        recordTable.getColumns().addAll(precheckCol, leafCol, bundleCol, weightCol, timeCol, exportCol);
        recordTable.getItems().setAll(stats.getRecords());
        dialog.setScene(new javafx.scene.Scene(new VBox(recordTable)));
        dialog.setWidth(700);
        dialog.setHeight(400);
        dialog.show();
    }

    private void exportRecord(com.tobacco.weight.data.WeighingRecord record) {
        try {
            // 显示进度提示
            updateStatus("正在导出记录: " + getLast5Digits(record.getPrecheckId()));

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
                            { "预检编号", getLast5Digits(record.getPrecheckId()) },
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
                    String fileName = "预检记录_" + getLast5Digits(record.getPrecheckId()) + "_" + timestamp + ".xlsx";
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
        logger.info("=== 开始刷新管理员表格 ===");

        // 检查容器是否存在，如果不存在则跳过表格更新
        if (adminTableContainer == null) {
            logger.warn("adminTableContainer为null，跳过管理员表格更新");
            return;
        }

        // 确保表格已初始化
        if (adminTable == null) {
            setupAdminTable();
        }

        // 以农户姓名为主键分组
        Map<String, List<WeighingRecord>> grouped = weighingRecordsList.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> {
                    return r.getFarmerName() != null ? r.getFarmerName() : "未知";
                }));
        java.util.List<FarmerStats> stats = new java.util.ArrayList<>();
        for (Map.Entry<String, List<WeighingRecord>> entry : grouped.entrySet()) {
            List<WeighingRecord> list = entry.getValue();
            String name = list.isEmpty() ? "" : list.get(0).getFarmerName();

            // 优先从农户信息表中获取身份证号
            String idCard = getFarmerIdCardByName(name);

            // 如果农户信息表中没有，则从称重记录中获取
            if (idCard == null || idCard.isEmpty()) {
                if (!list.isEmpty()) {
                    WeighingRecord firstRecord = list.get(0);
                    idCard = firstRecord.getIdCardNumber();
                    if (idCard == null) {
                        idCard = "";
                    }
                } else {
                    idCard = "";
                }
            }

            int count = list.size();
            double totalWeight = list.stream().mapToDouble(record -> record.getWeight() * record.getBundleCount())
                    .sum();

            stats.add(new FarmerStats(name, idCard, count, totalWeight, list));
        }

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
     * 打开农户注册管理窗口
     */
    private void openFarmerRegistrationWindow() {
        try {
            updateStatus("正在打开农户注册管理窗口...");
            FarmerRegistrationWindow farmerWindow = new FarmerRegistrationWindow();
            farmerWindow.show();
            updateStatus("农户注册管理窗口已打开");
        } catch (Exception e) {
            logger.error("打开农户注册管理窗口失败", e);
            updateStatus("打开农户注册管理窗口失败: " + e.getMessage());
            showError("窗口错误", "无法打开农户注册管理窗口: " + e.getMessage());
        }
    }

    /**
     * 设置数字键盘事件处理
     */
    private void setupNumberKeypad() {
        // 数字键 0-9
        key0.setOnAction(e -> appendToActiveTextField("0"));
        key1.setOnAction(e -> appendToActiveTextField("1"));
        key2.setOnAction(e -> appendToActiveTextField("2"));
        key3.setOnAction(e -> appendToActiveTextField("3"));
        key4.setOnAction(e -> appendToActiveTextField("4"));
        key5.setOnAction(e -> appendToActiveTextField("5"));
        key6.setOnAction(e -> appendToActiveTextField("6"));
        key7.setOnAction(e -> appendToActiveTextField("7"));
        key8.setOnAction(e -> appendToActiveTextField("8"));
        key9.setOnAction(e -> appendToActiveTextField("9"));

        // 清除按钮
        keyClear.setOnAction(e -> clearActiveTextField());

        // 退格按钮（在合同量字段时作为小数点按钮）
        keyBack.setOnAction(e -> {
            if (currentActiveTextField == contractAmountField) {
                appendToActiveTextField("."); // 合同量字段时输入小数点
            } else {
                backspaceActiveTextField(); // 其他字段时执行退格
            }
        });

        // 设置文本框焦点监听器（重新布置后bundleCountField已作为右列输入框）
        setupTextFieldFocusListeners();
    }

    /**
     * 设置文本框焦点监听器
     */
    private void setupTextFieldFocusListeners() {
        // 为所有数字输入文本框添加焦点监听器
        addFocusListener(farmerNameField);
        addFocusListener(contractNumberField);
        addFocusListener(idCardNumberField);

        addFocusListener(bundleCountField);
        addFocusListener(contractAmountField);

        // 为所有文本字段添加文本变化监听器，实时更新封签预览
        addTextChangeListener(farmerNameField);
        addTextChangeListener(contractNumberField);
        addTextChangeListener(idCardNumberField);

        addTextChangeListener(bundleCountField);

        // 添加特殊监听器：当合同号改变时自动加载合同量
        contractNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!oldVal.equals(newVal) && newVal != null && !newVal.trim().isEmpty()) {
                loadContractAmountForFarmer(newVal.trim());
            }
        });

        // 添加监听器：当农户名或合同量改变时更新比例
        farmerNameField.textProperty().addListener((obs, oldVal, newVal) -> updateRatios());
        contractAmountField.textProperty().addListener((obs, oldVal, newVal) -> updateRatios());

        // 添加监听器：当身份证号改变时自动检索烟农信息和合同量（带防抖和去重）
        idCardNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!oldVal.equals(newVal) && newVal != null && !newVal.trim().isEmpty()) {
                // 取消之前的定时器
                if (debounceTimer != null) {
                    debounceTimer.cancel();
                }

                // 创建新的防抖定时器，500ms后执行查询（增加延迟，减少频繁触发）
                debounceTimer = new java.util.Timer(true);
                debounceTimer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        // 检查当前输入框的值是否还是这个值（防止用户已经修改了）
                        String currentValue = idCardNumberField.getText().trim();
                        if (newVal.trim().equals(currentValue) && currentValue.length() >= 15) { // 身份证号至少15位才触发查询
                            loadFarmerInfoByIdCard(currentValue);
                        }
                    }
                }, 500); // 500ms防抖延迟
            }
        });

        // 默认激活捆数输入框
        currentActiveTextField = bundleCountField;
        updateKeypadButtonText(); // 设置初始按钮文本

        // 初始化封签预览（只显示默认值，不实时更新）
        updateLabelPreview();
    }

    /**
     * 为文本框添加焦点监听器
     */
    private void addFocusListener(TextField textField) {
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                currentActiveTextField = textField;
                updateKeypadButtonText(); // 更新键盘按钮文本
                // 静默更新活动文本框，不输出DEBUG日志
            }
        });
    }

    /**
     * 为文本框添加文本变化监听器
     */
    private void addTextChangeListener(TextField textField) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            // 文本变化时实时更新封签预览的文本字段，但不更新二维码
            updatePreviewLabelsOnly();
        });
    }

    /**
     * 根据当前活动的文本框更新键盘按钮文本
     */
    private void updateKeypadButtonText() {
        if (currentActiveTextField == contractAmountField) {
            keyBack.setText("."); // 合同量字段时显示小数点
        } else {
            keyBack.setText("⌫"); // 其他字段时显示退格符号
        }
    }

    /**
     * 向当前活动的文本框追加数字
     */
    private void appendToActiveTextField(String digit) {
        if (currentActiveTextField == null) {
            return;
        }

        String currentText = currentActiveTextField.getText();

        // 根据不同的文本框设置不同的输入限制
        int maxLength = getMaxLengthForTextField(currentActiveTextField);

        // 特殊处理合同量字段的小数点
        if (currentActiveTextField == contractAmountField) {
            // 如果输入的是小数点
            if (".".equals(digit)) {
                // 如果已经有小数点，不允许再输入
                if (currentText.contains(".")) {
                    return;
                }
                // 如果文本为空，自动添加0
                if (currentText.isEmpty()) {
                    currentActiveTextField.setText("0.");
                    return;
                }
            }
            // 如果已有小数点，限制小数位数为2位
            if (currentText.contains(".")) {
                String[] parts = currentText.split("\\.");
                if (parts.length > 1 && parts[1].length() >= 2 && !".".equals(digit)) {
                    return; // 小数位已经2位了，不允许再输入数字
                }
            }
        }

        if (currentText.length() < maxLength) {
            currentActiveTextField.setText(currentText + digit);
        }
    }

    /**
     * 清除当前活动的文本框
     */
    private void clearActiveTextField() {
        if (currentActiveTextField != null) {
            currentActiveTextField.clear();
        }
    }

    /**
     * 退格当前活动的文本框
     */
    private void backspaceActiveTextField() {
        if (currentActiveTextField == null) {
            return;
        }

        String currentText = currentActiveTextField.getText();
        if (!currentText.isEmpty()) {
            currentActiveTextField.setText(currentText.substring(0, currentText.length() - 1));
        }
    }

    /**
     * 获取文本框的最大输入长度
     */
    private int getMaxLengthForTextField(TextField textField) {
        if (textField == bundleCountField) {
            return 3; // 捆数最多3位数
        } else if (textField == contractAmountField) {
            return 8; // 合同量最多8位数（包含小数点），如99999.99
        } else if (textField == idCardNumberField) {
            return 18; // 身份证号18位
        } else if (textField == contractNumberField) {
            return 20; // 合同号最多20位
        } else if (textField == farmerNameField) {
            return 10; // 姓名最多10个字符
        }
        return 20; // 默认长度
    }

    /**
     * 设置主舞台
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // 设置窗口关闭事件处理器
        primaryStage.setOnCloseRequest(event -> {
            cleanup();
        });
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        logger.info("开始清理资源...");

        try {
            // 停止定时器
            if (weightRefreshTimer != null) {
                weightRefreshTimer.cancel();
                weightRefreshTimer = null;
                logger.info("重量刷新定时器已停止");
            }

            // 关闭硬件管理器
            if (scaleManager != null) {
                scaleManager.disconnect();
                logger.info("电子秤管理器已关闭");
            }

            if (printerManager != null) {
                // PrinterManager 目前没有close方法，但如果有后台线程需要在这里关闭
                logger.info("打印机管理器已关闭");
            }

            if (idCardReader != null) {
                // IdCardReader 目前没有close方法，但如果有后台线程需要在这里关闭
                logger.info("身份证读卡器已关闭");
            }

            // 关闭数据库连接池和仓库
            if (weighingRecordRepository != null) {
                weighingRecordRepository.shutdown();
                logger.info("称重记录仓库已关闭");
            }

            // 关闭其他可能的Repository
            // 注意：这些Repository可能在其他地方创建，需要确保都被正确关闭

            // 关闭诊断窗口
            if (diagnosticsWindow != null) {
                // HardwareDiagnosticsWindow 没有 close() 方法，但有内部的 Stage
                // 我们只需要将引用设为 null，Stage 会在窗口关闭时自动清理
                diagnosticsWindow = null;
                logger.info("诊断窗口引用已清理");
            }

            logger.info("资源清理完成");

        } catch (Exception e) {
            logger.error("清理资源时发生错误", e);
        }
    }

    /**
     * 设置响应式布局 - 暂时禁用动态调整，使用FXML固定高度
     */
    private void setupResponsiveLayout() {
        // 暂时禁用响应式布局，使用FXML中的固定高度设置
        logger.info("使用FXML固定高度布局");
    }

    /**
     * 根据窗口高度调整布局
     */
    private void adjustLayoutForWindowHeight(double windowHeight) {
        Platform.runLater(() -> {
            try {
                logger.info("调整布局，窗口高度: {}", windowHeight);

                // 计算各区域的理想高度 - 确保不重叠
                double availableHeight = windowHeight - 120; // 减去状态栏和更多间距
                double mainContentHeight = availableHeight * 0.40; // 40%给主要内容（减少）
                double adminPanelHeight = availableHeight * 0.45; // 45%给管理员面板
                // 剩余15%作为间距缓冲

                // 设置最小和最大限制
                final double finalMainContentHeight = Math.max(280, Math.min(400, mainContentHeight));
                final double finalAdminPanelHeight = Math.max(200, Math.min(320, adminPanelHeight));

                // 应用高度设置
                if (mainContentArea != null) {
                    mainContentArea.setPrefHeight(finalMainContentHeight);
                    mainContentArea.setMinHeight(finalMainContentHeight * 0.8);
                    mainContentArea.setMaxHeight(finalMainContentHeight * 1.2);
                }

                // 通过CSS类名查找管理员面板
                if (rootContainer != null) {
                    rootContainer.getChildren().stream()
                            .filter(node -> node.getStyleClass().contains("admin-panel"))
                            .findFirst()
                            .ifPresent(adminPanel -> {
                                if (adminPanel instanceof VBox) {
                                    VBox adminVBox = (VBox) adminPanel;
                                    adminVBox.setPrefHeight(finalAdminPanelHeight);
                                    adminVBox.setMinHeight(finalAdminPanelHeight * 0.8);
                                    adminVBox.setMaxHeight(finalAdminPanelHeight * 1.2);
                                }
                            });
                }

                logger.info("布局调整完成 - 主要内容: {}px, 管理员面板: {}px",
                        finalMainContentHeight, finalAdminPanelHeight);

            } catch (Exception e) {
                logger.error("调整布局时出错", e);
            }
        });
    }

    /**
     * 显示小票预览对话框
     */
    private void showReceiptPreview(String farmerName, String contractNumber, String leafType,
            double weight, String operator, int bundleCount, String precheckId) {
        try {
            String safeContract = contractNumber != null ? contractNumber : "N/A";
            String safeFarmerName = farmerName != null ? farmerName : "N/A";
            String safePrecheck = precheckId != null ? getLast5Digits(precheckId) : "N/A";
            String safeLeafType = leafType != null ? leafType : "N/A";
            String safeInspector = operator != null ? operator : "系统";
            String locationInfo = "实时录入";

            // 生成二维码图片（两栏显示时适当缩小）
            Image qrImage = QRCodeGenerator.generateQRCodeImage(contractNumber != null ? contractNumber : "N/A", 110);

            javafx.stage.Stage previewStage = new javafx.stage.Stage();
            previewStage.setTitle("标签预览 - " + safeFarmerName);
            previewStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            previewStage.initOwner(primaryStage);
            previewStage.setResizable(false);

            // 目标物理尺寸：120mm(宽) x 77mm(高)
            double dpi = javafx.stage.Screen.getPrimary().getDpi();
            double targetWidthPx = (120.0 / 25.4) * dpi; // px
            double targetHeightPx = (77.0 / 25.4) * dpi; // px

            javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(8);
            double padding = 10;
            root.setPadding(new javafx.geometry.Insets(padding));
            root.setAlignment(javafx.geometry.Pos.CENTER);

            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("称重标签预览");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            // 两栏容器
            javafx.scene.layout.HBox twoCols = new javafx.scene.layout.HBox(12);
            twoCols.setAlignment(javafx.geometry.Pos.CENTER);

            // 计算列宽与二维码大小（尽量适配目标宽度）
            double availableWidth = targetWidthPx - 2 * padding; // 近似
            double colWidth = (availableWidth - twoCols.getSpacing()) / 2.0;
            int qrTarget = (int) Math.max(90, Math.min(110, colWidth * 0.5)); // 90~110之间

            // 构建标准列：二维码在上、文字在下
            java.util.function.Supplier<javafx.scene.layout.VBox> buildStandardColumn = () -> {
                javafx.scene.layout.VBox col = new javafx.scene.layout.VBox(6);
                col.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                col.setStyle("-fx-font-family: 'SimSun';");
                col.setPrefWidth(colWidth);
                col.setMaxWidth(colWidth);

                if (qrImage != null) {
                    ImageView qr = new ImageView(qrImage);
                    qr.setFitWidth(qrTarget);
                    qr.setFitHeight(qrTarget);
                    qr.setPreserveRatio(true);
                    qr.setSmooth(false);
                    col.getChildren().add(qr);
                }

                // 获取烟农真实地址和站点名称（从当前UI字段获取身份证号）
                String currentIdCardNumber = idCardNumberField.getText().trim();
                String farmerAddress = getFarmerAddress(currentIdCardNumber);
                String stationName = getStationName(currentIdCardNumber);

                String[] items = new String[] {
                        stationName,
                        farmerAddress,
                        safeContract,
                        "姓名: " + safeFarmerName,
                        "预检号: " + safePrecheck,
                        "重量: " + String.format("%.2f kg", weight),
                        "部位: " + safeLeafType,
                        "检验: " + safeInspector,
                        "预检日期: " + java.time.LocalDate.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                };
                javafx.scene.layout.VBox textBox = new javafx.scene.layout.VBox(2);
                textBox.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                textBox.setStyle("-fx-font-size: 12px;");
                for (String s : items) {
                    textBox.getChildren().add(new javafx.scene.control.Label(s));
                }
                col.getChildren().add(textBox);
                return col;
            };

            javafx.scene.layout.VBox leftCol = buildStandardColumn.get();
            javafx.scene.layout.VBox rightCol = buildStandardColumn.get();
            rightCol.setRotate(180);

            twoCols.getChildren().setAll(leftCol, rightCol);

            root.getChildren().addAll(titleLabel, twoCols);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, targetWidthPx, targetHeightPx);
            previewStage.setScene(scene);
            previewStage.centerOnScreen();
            previewStage.showAndWait();
        } catch (Exception e) {
            logger.error("显示小票预览失败", e);
            showError("预览失败", "显示小票预览失败: " + e.getMessage());
        }
    }

    /**
     * 生成小票内容 (70x70mm标签格式，包含二维码)
     */
    private String generateReceiptContent(String farmerName, String contractNumber, String leafType,
            double weight, String operator, int bundleCount, String precheckId) {
        StringBuilder content = new StringBuilder();

        // 获取基本信息
        String safeContract = contractNumber != null ? contractNumber : "N/A";
        String safeFarmerName = farmerName != null ? farmerName : "N/A";
        String safePrecheck = precheckId != null ? getLast5Digits(precheckId) : "N/A";
        String safeLeafType = leafType != null ? leafType : "N/A";
        String safeInspector = operator != null ? operator : "系统";

        // 获取烟农真实地址和站点名称（从当前UI字段获取身份证号）
        String currentIdCardNumber = idCardNumberField.getText().trim();
        String locationInfo = getFarmerAddress(currentIdCardNumber);
        String stationName = getStationName(currentIdCardNumber);
        if (locationInfo == null || locationInfo.trim().isEmpty() || "待完善".equals(locationInfo.trim())) {
            locationInfo = "实时录入";
        }

        // 生成合同号二维码（超紧凑版）
        String qrCode = QRCodeGenerator.generateCompactQRCode(safeContract);

        // 构建标签内容 - 二维码在最上方，超紧凑
        String[] qrLines = qrCode.split("\n");

        // 首先输出二维码（不居中，节省空间）
        for (String qrLine : qrLines) {
            content.append(qrLine).append("\n");
        }

        // 信息列表（极度紧凑显示，只保留核心信息）
        content.append(stationName.length() > 6 ? stationName.substring(0, 6) + ".." : stationName)
                .append("\n");
        content.append(locationInfo.length() > 6 ? locationInfo.substring(0, 6) + ".." : locationInfo)
                .append("\n");
        content.append(safeContract.length() > 15 ? safeContract.substring(0, 15) + ".." : safeContract)
                .append("\n");
        content.append("姓名:")
                .append(safeFarmerName.length() > 8 ? safeFarmerName.substring(0, 8) + ".." : safeFarmerName)
                .append("\n");
        content.append("预检:").append(safePrecheck.length() > 12 ? safePrecheck.substring(0, 12) + ".." : safePrecheck)
                .append("\n");
        // 计算单捆重量
        double singleBundleWeight = weight / bundleCount;
        content.append("重量:").append(String.format("%.2f", singleBundleWeight)).append("kg\n");
        content.append("部位:").append(safeLeafType.length() > 6 ? safeLeafType.substring(0, 6) + ".." : safeLeafType)
                .append("\n");
        content.append("检验:").append(safeInspector.length() > 6 ? safeInspector.substring(0, 6) + ".." : safeInspector)
                .append("\n");

        // 计算标签纸张大小并输出日志
        String finalContent = content.toString();
        String[] lines = finalContent.split("\n");
        int maxLineLength = 0;
        for (String line : lines) {
            if (line.length() > maxLineLength) {
                maxLineLength = line.length();
            }
        }

        // 估算纸张大小（假设每个字符约3mm宽，每行约4mm高）
        double estimatedWidth = maxLineLength * 3.0; // mm
        double estimatedHeight = lines.length * 4.0; // mm

        // System.out.println("=== 标签纸张大小估算 ===");
        // System.out.println("标签内容行数: " + lines.length);
        // System.out.println("最长行字符数: " + maxLineLength);
        // System.out.println("估算宽度: " + String.format("%.1f", estimatedWidth) + " mm");
        // System.out.println("估算高度: " + String.format("%.1f", estimatedHeight) + "
        // mm");
        // System.out.println("是否适合70x70mm纸张: " + ((estimatedWidth <= 70 &&
        // estimatedHeight <= 70) ? "是" : "否"));
        // System.out.println("================================");

        return finalContent;
    }

    /**
     * 打开管理员登录窗口
     */
    private void openAdminLogin() {
        try {
            AdminLoginWindow loginWindow = new AdminLoginWindow();
            loginWindow.showAndWait();

            // 更新登录状态
            updateAdminLoginStatus();

        } catch (Exception e) {
            logger.error("打开管理员登录窗口失败", e);
            showError("错误", "无法打开登录窗口: " + e.getMessage());
        }
    }

    /**
     * 打开管理员面板
     */
    private void openAdminPanel() {
        try {
            if (!adminAuthService.isLoggedIn()) {
                showError("权限错误", "请先登录管理员账户");
                return;
            }

            if (!adminAuthService.hasPermission("VIEW_ADMIN_PANEL")) {
                showError("权限不足", "您没有访问管理员面板的权限");
                return;
            }

            // 获取所有称重记录
            weighingRecordRepository
                    .findAll(new WeighingRecordRepository.OnResultListener<java.util.List<WeighingRecord>>() {
                        @Override
                        public void onSuccess(java.util.List<WeighingRecord> records) {
                            Platform.runLater(() -> {
                                AdminWindow adminWindow = new AdminWindow(records);
                                adminWindow.show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            Platform.runLater(() -> {
                                logger.error("加载称重记录失败", e);
                                showError("加载失败", "无法加载称重记录: " + e.getMessage());
                            });
                        }
                    });

        } catch (Exception e) {
            logger.error("打开管理员面板失败", e);
            showError("错误", "无法打开管理员面板: " + e.getMessage());
        }
    }

    /**
     * 处理管理员登出
     */
    private void handleAdminLogout() {
        try {
            adminAuthService.logout();
            updateAdminLoginStatus();
            updateStatus("管理员已登出");
        } catch (Exception e) {
            logger.error("管理员登出失败", e);
            showError("错误", "登出失败: " + e.getMessage());
        }
    }

    /**
     * 更新管理员登录状态显示
     */
    private void updateAdminLoginStatus() {
        try {
            if (adminAuthService.isLoggedIn()) {
                // 已登录状态
                var currentAdmin = adminAuthService.getCurrentAdmin();
                adminStatusLabel.setText("已登录: " + currentAdmin.getFullName() + " (" + currentAdmin.getRole() + ")");

                // 显示登录状态区域
                adminStatusContainer.setVisible(true);
                openAdminPanelButton.setVisible(true);
                exportAllDataButton.setVisible(true);
                adminLogoutButton.setVisible(true);

                // 隐藏登录按钮
                adminLoginButton.setVisible(false);

            } else {
                // 未登录状态
                adminStatusLabel.setText("");

                // 隐藏登录状态区域
                adminStatusContainer.setVisible(false);
                openAdminPanelButton.setVisible(false);
                exportAllDataButton.setVisible(false);
                adminLogoutButton.setVisible(false);

                // 显示登录按钮
                adminLoginButton.setVisible(true);
            }
        } catch (Exception e) {
            logger.error("更新管理员登录状态失败", e);
        }
    }

    /**
     * 显示二维码扫描测试窗口
     */

    /**
     * 保存标签预览为图片文件
     */
    private void saveLabelPreview(String farmerName, String contractNumber, String leafType,
            double weight, String operator, int bundleCount, String precheckId) {
        try {
            // 获取基本信息
            String safeContract = contractNumber != null ? contractNumber : "N/A";
            String safeFarmerName = farmerName != null ? farmerName : "N/A";
            String safePrecheck = precheckId != null ? getLast5Digits(precheckId) : "N/A";
            String safeLeafType = leafType != null ? leafType : "N/A";
            String safeInspector = operator != null ? operator : "系统";
            String locationInfo = "实时录入";

            // 获取站点名称和地址
            String idCardNumber = idCardNumberField.getText().trim();
            if (idCardNumber.isEmpty())
                idCardNumber = "XXX";
            String stationName = getStationName(idCardNumber);
            String address = getFarmerAddress(idCardNumber);
            if (address == null || address.trim().isEmpty() || "待完善".equals(address.trim())) {
                address = "实时录入";
            }
            String currentDate = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 生成二维码图片
            BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(safeContract, 80);

            // 计算单捆重量
            double singleBundleWeight = weight / bundleCount;

            // 创建标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    address, idCardNumber, safeContract, safeFarmerName, safePrecheck, safeLeafType, safeInspector,
                    currentDate, singleBundleWeight);

            // 创建文件选择对话框
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("保存标签预览");
            fileChooser.setInitialFileName("标签预览_" + safePrecheck + "_" + System.currentTimeMillis() + ".png");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PNG图片", "*.png"));

            // 设置初始目录为桌面
            String userHome = System.getProperty("user.home");
            File desktop = new File(userHome, "Desktop");
            if (desktop.exists()) {
                fileChooser.setInitialDirectory(desktop);
            }

            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            if (selectedFile != null) {
                boolean success = printerManager.saveLabelPreview(qrCodeImage, labelInfo,
                        selectedFile.getAbsolutePath());
                if (success) {
                    updateStatus("标签预览已保存到: " + selectedFile.getName());
                    showInfo("保存成功", "标签预览已保存到:\n" + selectedFile.getAbsolutePath() + "\n\n尺寸: 70x70mm (198x198像素)");
                } else {
                    showError("保存失败", "无法保存标签预览");
                }
            }

        } catch (Exception e) {
            logger.error("保存标签预览失败", e);
            showError("保存失败", "保存标签预览失败: " + e.getMessage());
        }
    }

    /**
     * 打印带二维码的标签
     */
    private void printLabelWithQRCode(String farmerName, String contractNumber, String leafType,
            double weight, String operator, int bundleCount, String precheckId) {
        try {
            // 获取基本信息
            String safeContract = contractNumber != null ? contractNumber : "N/A";
            String safeFarmerName = farmerName != null ? farmerName : "N/A";
            String safePrecheck = precheckId != null ? getLast5Digits(precheckId) : "N/A";
            String safeLeafType = leafType != null ? leafType : "N/A";
            String safeInspector = operator != null ? operator : "系统";

            // 地址字段已删除，使用默认值
            String address = "待完善";
            String idCardNumber = idCardNumberField.getText().trim();
            if (idCardNumber.isEmpty())
                idCardNumber = "XXX";
            String currentDate = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 生成二维码图片（用于打印）
            BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(safeContract, 80);

            if (qrCodeImage == null) {
                showError("生成失败", "无法生成二维码，将使用文本方式打印");
                // 降级到文本打印
                String compactLabel = generateReceiptContent(farmerName, contractNumber, leafType, weight, operator,
                        bundleCount, precheckId);

                // 按捆数打印多份文本标签
                boolean allPrintSuccess = true;
                int printCount = 0;

                for (int i = 0; i < bundleCount; i++) {
                    boolean printSuccess = printerManager.printText(compactLabel);
                    if (printSuccess) {
                        printCount++;
                        logger.info("成功打印第 {} 份文本标签，预检编号: {}", i + 1, precheckId);
                    } else {
                        allPrintSuccess = false;
                        logger.error("打印第 {} 份文本标签失败，预检编号: {}", i + 1, precheckId);
                    }
                }

                if (allPrintSuccess) {
                    updateStatus(String.format("成功打印 %d 份文本标签", bundleCount));
                } else {
                    updateStatus(String.format("部分文本标签打印失败，成功: %d/%d", printCount, bundleCount));
                    showError("打印失败", String.format("部分文本标签打印失败，成功: %d/%d，请检查打印机连接", printCount, bundleCount));
                }
                return;
            }

            // 计算单捆重量
            double singleBundleWeight = weight / bundleCount;

            // 创建标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    address, idCardNumber, safeContract, safeFarmerName, safePrecheck, safeLeafType, safeInspector,
                    currentDate, singleBundleWeight);

            // 按捆数打印多份标签
            boolean allPrintSuccess = true;
            int printCount = 0;

            for (int i = 0; i < bundleCount; i++) {
                boolean printSuccess = printerManager.printLabelWithQRCode(qrCodeImage, labelInfo);
                if (printSuccess) {
                    printCount++;
                    logger.info("成功打印第 {} 份标签，预检编号: {}", i + 1, precheckId);
                } else {
                    allPrintSuccess = false;
                    logger.error("打印第 {} 份标签失败，预检编号: {}", i + 1, precheckId);
                }
            }

            if (allPrintSuccess) {
                updateStatus(String.format("成功打印 %d 份带二维码的标签", bundleCount));
                logger.info("成功打印所有标签，预检编号: {}，捆数: {}", precheckId, bundleCount);
            } else {
                updateStatus(String.format("部分标签打印失败，成功: %d/%d", printCount, bundleCount));
                showError("打印失败", String.format("部分标签打印失败，成功: %d/%d，请检查打印机连接", printCount, bundleCount));
                logger.error("部分标签打印失败，预检编号: {}，成功: {}/{}", precheckId, printCount, bundleCount);
            }

        } catch (Exception e) {
            logger.error("打印带二维码标签异常", e);
            showError("打印错误", "打印时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新封签预览
     */
    private void updateLabelPreview() {
        try {
            // 获取当前输入的信息
            String farmerName = farmerNameField.getText().trim();
            String contractNumber = contractNumberField.getText().trim();
            String idCardNumber = idCardNumberField.getText().trim();
            String leafType = getSelectedLeafType();

            // 设置默认值并创建final副本
            final String finalFarmerName = farmerName.isEmpty() ? "XXX" : farmerName;
            final String finalContractNumber = contractNumber.isEmpty() ? "XXXXX" : contractNumber;
            final String finalIdCardNumber = idCardNumber.isEmpty() ? "XXX" : idCardNumber;
            // 获取站点名称
            final String stationName = getStationName(idCardNumber);
            final String finalLeafType = leafType == null ? "X部叶" : leafType;

            // 获取当前日期
            final String currentDate = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 生成预检编号预览：身份证后6位+合同号后6位+当前第几次称重（五位数）
            String idCardLast6 = finalIdCardNumber.length() >= 6
                    ? finalIdCardNumber.substring(finalIdCardNumber.length() - 6)
                    : "000000";
            String contractLast6 = finalContractNumber.length() >= 6
                    ? finalContractNumber.substring(finalContractNumber.length() - 6)
                    : "000000";
            int nextSeq = databaseManager.peekNextPrecheckSeq();
            String weighingCountStr = String.format("%05d", nextSeq);
            final String displayPrecheckId = weighingCountStr;

            // 更新预览区域 - 只在初始化时显示默认值，不实时更新
            Platform.runLater(() -> {
                updatePreviewLabelsOnly();
            });

        } catch (Exception e) {
            logger.error("更新封签预览失败", e);
        }
    }

    /**
     * 更新最终封签预览（用于确认打印时）
     */
    private void updateFinalLabelPreview(String farmerName, String contractNumber, String idCardNumber,
            String address, String leafType, String precheckId, String date, double weight) {
        try {
            if (qrCodeImageView != null && contractNumber != null && !contractNumber.isEmpty()) {
                try {
                    BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(contractNumber, 140);
                    if (qrCodeImage != null) {
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(qrCodeImage, "PNG", baos);
                        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
                        javafx.scene.image.Image fxImage = new javafx.scene.image.Image(bais);
                        qrCodeImageView.setImage(fxImage);
                    }
                } catch (Exception e) {
                    logger.error("生成二维码失败", e);
                    qrCodeImageView.setImage(null);
                }
            }

            // 仅更新信息容器
            if (labelInfoContainer != null) {
                // 获取站点名称
                String stationName = getStationName(idCardNumber);

                // 如果地址为空，尝试从烟农信息中获取
                String finalAddress = address;
                if (address == null || address.trim().isEmpty() || "待完善".equals(address.trim())) {
                    finalAddress = getFarmerAddress(idCardNumber);
                }

                // weight参数已经是单捆重量，直接使用
                double singleBundleWeight = weight;

                updateInfoByIndex(0, stationName);
                updateInfoByIndex(1, finalAddress);
                updateInfoByIndex(2, idCardNumber);
                updateInfoByIndex(3, "姓名: " + farmerName);
                updateInfoByIndex(4, "预检编号: " + getLast5Digits(precheckId));
                updateInfoByIndex(5, "重量: " + String.format("%.2f kg", singleBundleWeight));
                updateInfoByIndex(6, "烟叶部位: " + leafType);
                updateInfoByIndex(7, "预检日期: " + date);
            }
        } catch (Exception e) {
            logger.error("更新最终封签预览失败", e);
        }
    }

    /**
     * 更新预览标签的具体内容（用于实时预览）
     */
    private void updatePreviewLabels(String farmerName, String contractNumber, String idCardNumber,
            String address, String leafType, String precheckId, String date, double weight) {
        try {
            if (labelInfoContainer != null) {
                // 获取站点名称
                String stationName = getStationName(idCardNumber);

                // 如果地址为空，尝试从烟农信息中获取
                String finalAddress = address;
                if (address == null || address.trim().isEmpty() || "待完善".equals(address.trim())) {
                    finalAddress = getFarmerAddress(idCardNumber);
                }

                // weight参数已经是单捆重量，直接使用
                double singleBundleWeight = weight;

                updateInfoByIndex(0, stationName);
                updateInfoByIndex(1, finalAddress);
                updateInfoByIndex(2, idCardNumber);
                updateInfoByIndex(3, "姓名: " + farmerName);
                updateInfoByIndex(4, "预检编号: " + getLast5Digits(precheckId));
                updateInfoByIndex(5, "重量: " + String.format("%.2f kg", singleBundleWeight));
                updateInfoByIndex(6, "烟叶部位: " + leafType);
                updateInfoByIndex(7, "预检日期: " + date);
            }
        } catch (Exception e) {
            logger.error("更新预览标签失败", e);
        }
    }

    /**
     * 只更新预览标签的文本内容（不更新二维码）
     */
    private void updatePreviewLabelsOnly() {
        try {
            String farmerName = farmerNameField.getText().trim();
            String contractNumber = contractNumberField.getText().trim();
            String idCardNumber = idCardNumberField.getText().trim();
            String leafType = getSelectedLeafType();

            // 获取站点名称
            final String stationName = getStationName(idCardNumber);
            String finalFarmerName = farmerName.isEmpty() ? "XXX" : farmerName;
            String finalContractNumber = contractNumber.isEmpty() ? "XXXXX" : contractNumber;
            String finalIdCardNumber = idCardNumber.isEmpty() ? "XXX" : idCardNumber;
            String finalLeafType = leafType == null ? "X部叶" : leafType;

            String currentDate = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            int nextSeq2 = databaseManager.peekNextPrecheckSeq();
            String weighingCountStr = String.format("%05d", nextSeq2);

            if (labelInfoContainer != null) {
                // 获取当前实时重量
                double currentWeight = getCurrentWeight();

                // 计算单捆重量
                double singleBundleWeight = currentWeight;
                try {
                    String bundleCountText = bundleCountField.getText();
                    if (bundleCountText != null && !bundleCountText.trim().isEmpty()) {
                        int bundleCount = Integer.parseInt(bundleCountText.trim());
                        if (bundleCount > 0) {
                            singleBundleWeight = currentWeight / bundleCount;
                        }
                    }
                } catch (NumberFormatException e) {
                    // 如果解析失败，使用原始重量
                }

                // 获取地址
                String address = getFarmerAddress(idCardNumber);

                updateInfoByIndex(0, stationName);
                updateInfoByIndex(1, address);
                updateInfoByIndex(2, finalIdCardNumber);
                updateInfoByIndex(3, "姓名: " + finalFarmerName);
                updateInfoByIndex(4, "预检编号: " + getLast5Digits(weighingCountStr));
                updateInfoByIndex(5, "重量: " + String.format("%.2f kg", singleBundleWeight));
                updateInfoByIndex(6, "烟叶部位: " + finalLeafType);
                updateInfoByIndex(7, "预检日期: " + currentDate);
            }
        } catch (Exception e) {
            logger.error("更新预览标签文本失败", e);
        }
    }

    private void updateInfoByIndex(int targetIndex, String newText) {
        if (labelInfoContainer != null) {
            int idx = 0;
            for (javafx.scene.Node node : labelInfoContainer.getChildren()) {
                if (node instanceof Label) {
                    if (idx == targetIndex) {
                        ((Label) node).setText(newText);
                        return;
                    }
                    idx++;
                }
            }
        }
    }

    /**
     * 获取预检编号的后5位数字
     */
    private String getLast5Digits(String precheckId) {
        if (precheckId == null || precheckId.length() < 5) {
            return precheckId != null ? precheckId : "N/A";
        }
        return precheckId.substring(precheckId.length() - 5);
    }

    /**
     * 根据身份证号获取烟农地址
     */
    private String getFarmerAddress(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            return "待完善";
        }

        try {
            // 从缓存中查找烟农信息
            FarmerInfoAndContractAmount cachedResult = queryCache.get(idCardNumber.trim());
            if (cachedResult != null && cachedResult.farmerInfo != null) {
                String address = cachedResult.farmerInfo.getAddress();
                if (address != null && !address.trim().isEmpty()) {
                    return address.trim();
                }
            }

            // 如果缓存中没有，尝试从数据库查询
            try {
                FarmerInfo farmerInfo = farmerInfoDao.findByIdCardNumber(idCardNumber.trim());
                if (farmerInfo != null) {
                    String address = farmerInfo.getAddress();
                    if (address != null && !address.trim().isEmpty()) {
                        return address.trim();
                    }
                }
            } catch (Exception e) {
                logger.debug("查询烟农地址失败: {}", e.getMessage());
            }

            return "待完善";
        } catch (Exception e) {
            logger.debug("获取烟农地址失败: {}", e.getMessage());
            return "待完善";
        }
    }

    /**
     * 根据身份证号获取站点名称
     */
    private String getStationName(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
            return "未知站点";
        }

        try {
            // 从farmer_contracts表查询站点名称
            String sql = "SELECT station FROM farmer_contracts WHERE national_id = ? AND station IS NOT NULL AND station != ''";
            try (java.sql.Connection conn = databaseManager.getConnection();
                    java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, idCardNumber.trim());
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String station = rs.getString("station");
                        if (station != null && !station.trim().isEmpty()) {
                            return station.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("查询站点名称失败: {}", e.getMessage());
        }

        return "未知站点";
    }

    /**
     * 烟农信息和合同量的结果类
     */
    private static class FarmerInfoAndContractAmount {
        final FarmerInfo farmerInfo;
        final Double contractAmount;

        FarmerInfoAndContractAmount(FarmerInfo farmerInfo, Double contractAmount) {
            this.farmerInfo = farmerInfo;
            this.contractAmount = contractAmount;
        }
    }

    /**
     * 根据身份证号一次性查询烟农信息和合同量
     */
    private FarmerInfoAndContractAmount loadFarmerInfoAndContractAmountByIdCard(String idCardNumber) throws Exception {
        // 1. 先检查缓存
        FarmerInfoAndContractAmount cachedResult = queryCache.get(idCardNumber);
        if (cachedResult != null) {
            // 静默从缓存获取数据，不输出日志
            return cachedResult;
        }

        try {
            // 2. 根据身份证号查询烟农信息
            FarmerInfo farmerInfo = farmerInfoDao.findByIdCardNumber(idCardNumber);

            if (farmerInfo != null && farmerInfo.getContractNumber() != null) {
                // 2. 如果找到烟农信息和合同号，直接查询合同量
                try {
                    java.util.Map<String, Double> contractAmounts = weighingRecordRepository.getContractAmountsSync();
                    Double amount = contractAmounts.get(farmerInfo.getContractNumber());

                    // 静默记录查询完成，不输出DEBUG日志

                    FarmerInfoAndContractAmount result = new FarmerInfoAndContractAmount(farmerInfo, amount);

                    // 缓存结果
                    cacheResult(idCardNumber, result);

                    return result;

                } catch (Exception e) {
                    logger.warn("查询合同量失败，但烟农信息查询成功: {}", e.getMessage());
                    FarmerInfoAndContractAmount result = new FarmerInfoAndContractAmount(farmerInfo, null);
                    cacheResult(idCardNumber, result);
                    return result;
                }
            } else {
                // 3. 如果没找到烟农信息，尝试通过身份证号查询合同量
                try {
                    String contractNumber = weighingRecordRepository.getContractNumberByIdCard(idCardNumber);
                    if (contractNumber != null) {
                        java.util.Map<String, Double> contractAmounts = weighingRecordRepository
                                .getContractAmountsSync();
                        Double amount = contractAmounts.get(contractNumber);

                        // 静默记录查询结果，不输出DEBUG日志

                        // 创建一个临时的FarmerInfo对象
                        FarmerInfo tempFarmerInfo = new FarmerInfo("未知", contractNumber, idCardNumber);
                        FarmerInfoAndContractAmount result = new FarmerInfoAndContractAmount(tempFarmerInfo, amount);

                        // 缓存结果
                        cacheResult(idCardNumber, result);

                        return result;
                    }
                } catch (Exception e) {
                    logger.warn("通过身份证号查询合同量失败: {}", e.getMessage());
                }

                // 缓存空结果，避免重复查询不存在的记录
                cacheResult(idCardNumber, null);
                return null;
            }

        } catch (Exception e) {
            logger.error("一次性查询烟农信息和合同量失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 缓存查询结果
     */
    private void cacheResult(String idCardNumber, FarmerInfoAndContractAmount result) {
        // 如果缓存太大，清理一些旧记录
        if (queryCache.size() >= CACHE_SIZE_LIMIT) {
            // 简单清理策略：随机删除一些记录
            java.util.Iterator<String> iterator = queryCache.keySet().iterator();
            int deleteCount = CACHE_SIZE_LIMIT / 4; // 删除1/4的记录
            for (int i = 0; i < deleteCount && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
            // 静默清理缓存，不输出DEBUG日志
        }

        queryCache.put(idCardNumber, result);
        // 静默缓存结果，不输出DEBUG日志
    }

    /**
     * 清空烟农信息相关字段
     */
    private void clearFarmerInfoFields() {
        farmerNameField.clear();
        contractNumberField.clear();
        contractAmountField.clear();
        // 更新比例计算
        updateRatios();
        // 更新封签预览
        updatePreviewLabelsOnly();
    }

    /**
     * 根据合同号加载合同量（异步版本，用于UI直接调用）
     */
    private void loadContractAmountByContractNumber(String contractNumber) {
        if (contractNumber == null || contractNumber.trim().isEmpty()) {
            contractAmountField.clear();
            return;
        }

        // 在后台线程查询合同量
        new Thread(() -> {
            try {
                loadContractAmountByContractNumberSync(contractNumber);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("根据合同号加载合同量失败: {}", contractNumber, e);
                    contractAmountField.setText("0.00"); // 出错时也设置为0
                    updateRatios();
                });
            }
        }).start();
    }

    /**
     * 根据合同号同步加载合同量（用于内部调用，避免并发问题）
     */
    private void loadContractAmountByContractNumberSync(String contractNumber) throws Exception {
        if (contractNumber == null || contractNumber.trim().isEmpty()) {
            Platform.runLater(() -> {
                contractAmountField.clear();
                updateRatios();
            });
            return;
        }

        try {
            // 根据合同号查询合同量
            java.util.Map<String, Double> contractAmounts = weighingRecordRepository.getContractAmountsSync();
            Double amount = contractAmounts.get(contractNumber);

            Platform.runLater(() -> {
                if (amount != null && amount > 0) {
                    contractAmountField.setText(String.format("%.2f", amount));
                    logger.info("根据合同号自动填充合同量: {} -> 合同量: {}", contractNumber, amount);
                } else {
                    contractAmountField.setText("0.00"); // 如果为空则置0
                    logger.info("未找到合同号 {} 的合同量信息，设置为0", contractNumber);
                }
                // 更新比例计算
                updateRatios();
            });

        } catch (Exception e) {
            logger.error("根据合同号同步加载合同量失败: {}", contractNumber, e);
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    /**
     * 获取当前实时重量
     */
    private double getCurrentWeight() {
        try {
            // 从重量显示标签获取当前重量
            if (currentWeightLabel != null) {
                String weightText = currentWeightLabel.getText();
                if (weightText != null && !weightText.isEmpty()) {
                    // 提取数字部分，去掉 "kg" 等文字
                    String numericPart = weightText.replaceAll("[^0-9.]", "");
                    if (!numericPart.isEmpty()) {
                        return Double.parseDouble(numericPart);
                    }
                }
            }

            // 如果无法获取，返回默认值
            return 0.0;
        } catch (Exception e) {
            logger.error("获取当前重量失败", e);
            return 0.0;
        }
    }

    /**
     * 根据农户姓名查询身份证号
     */
    private String getFarmerIdCardByName(String farmerName) {
        if (farmerName == null || farmerName.trim().isEmpty()) {
            logger.warn("农户姓名为空，无法查询身份证号");
            return null;
        }

        logger.info("开始查询农户 {} 的身份证号", farmerName);

        try {
            // 从农户信息表中查询身份证号
            String sql = "SELECT id_card_number FROM farmer_info WHERE farmer_name = ? LIMIT 1";
            logger.info("执行SQL查询: {}", sql);
            logger.info("查询参数: farmer_name = {}", farmerName.trim());

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, farmerName.trim());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String idCard = rs.getString("id_card_number");
                        logger.info("从farmer_info表查询到农户 {} 的身份证号: {}", farmerName, idCard);
                        return idCard != null && !idCard.trim().isEmpty() ? idCard.trim() : null;
                    } else {
                        logger.warn("在farmer_info表中未找到农户 {} 的记录", farmerName);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("查询农户 {} 的身份证号失败: {}", farmerName, e.getMessage(), e);
        }

        return null;
    }

    /**
     * 脱敏身份证号
     */
    private String maskIdCardNumber(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.length() < 10) {
            return idCardNumber;
        }
        String maskedNumber = idCardNumber.substring(0, 6) + "****" + idCardNumber.substring(10);
        return maskedNumber;
    }

}