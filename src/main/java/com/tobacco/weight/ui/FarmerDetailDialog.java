package com.tobacco.weight.ui;

import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.hardware.PrinterManager;
import com.tobacco.weight.util.QRCodeGenerator;
import com.tobacco.weight.database.WeighingRecordRepository;
import com.tobacco.weight.database.DatabaseManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.beans.property.SimpleStringProperty;

public class FarmerDetailDialog extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(FarmerDetailDialog.class);

    private WeighingRecordRepository weighingRecordRepository;
    private TableView<WeighingRecord> table;
    private Label statusLabel;
    private String currentFarmerName;
    private String currentIdCardNumber;
    private String currentContractNumber;
    private java.util.Timer refreshTimer;

    public FarmerDetailDialog(FarmerStats stats) {
        setTitle("农户详情 - " + stats.farmerNameProperty().get());
        setWidth(1000);
        setHeight(500);
        initModality(Modality.APPLICATION_MODAL);

        // 保存农户信息
        this.currentFarmerName = stats.farmerNameProperty().get();
        this.currentIdCardNumber = stats.idCardProperty().get();
        this.currentContractNumber = stats.getRecords().isEmpty() ? "" : stats.getRecords().get(0).getContractNumber();

        // 初始化数据仓库
        this.weighingRecordRepository = new WeighingRecordRepository(
                DatabaseManager.getInstance());

        BorderPane root = new BorderPane();
        Label title = new Label(
                "农户: " + currentFarmerName + "    身份证号: " + currentIdCardNumber);
        title.setPadding(new Insets(10));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        root.setTop(title);

        TableView<WeighingRecord> table = new TableView<>();
        TableColumn<WeighingRecord, String> precheckCol = new TableColumn<>("预检编号");
        precheckCol.setCellValueFactory(data -> {
            String precheckId = data.getValue().getPrecheckId();
            if (precheckId != null && precheckId.length() >= 5) {
                // 检查预检编号是否被错误地设置为身份证号（身份证号通常是18位）
                if (precheckId.length() == 18 && precheckId.matches("\\d{17}[\\dXx]")) {
                    // 这是身份证号，不是预检编号，显示错误提示
                    return new SimpleStringProperty("数据错误");
                } else {
                    // 显示完整的预检编号（17位：6+6+5）
                    return new SimpleStringProperty(precheckId);
                }
            } else {
                return new SimpleStringProperty(precheckId != null ? precheckId : "");
            }
        });
        precheckCol.setPrefWidth(200);
        precheckCol.setMinWidth(180);
        precheckCol.setStyle("-fx-alignment: CENTER;");
        TableColumn<WeighingRecord, String> leafCol = new TableColumn<>("部叶类型");
        leafCol.setCellValueFactory(new PropertyValueFactory<>("leafType"));
        leafCol.setPrefWidth(80);
        leafCol.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<WeighingRecord, Integer> bundleCol = new TableColumn<>("捆数");
        bundleCol.setCellValueFactory(new PropertyValueFactory<>("bundleCount"));
        bundleCol.setPrefWidth(60);
        bundleCol.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<WeighingRecord, String> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(data -> {
            double weight = data.getValue().getWeight();
            return new javafx.beans.property.SimpleStringProperty(String.format("%.2f", weight));
        });
        weightCol.setPrefWidth(90);
        weightCol.setStyle("-fx-alignment: CENTER;");
        
        TableColumn<WeighingRecord, String> timeCol = new TableColumn<>("称重时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setPrefWidth(240);
        timeCol.setMinWidth(200);
        timeCol.setStyle("-fx-alignment: CENTER;");

        // 添加操作员列
        TableColumn<WeighingRecord, String> operatorCol = new TableColumn<>("操作员");
        operatorCol.setCellValueFactory(new PropertyValueFactory<>("operator"));
        operatorCol.setPrefWidth(80);
        operatorCol.setMinWidth(60);
        operatorCol.setStyle("-fx-alignment: CENTER;");

        // 添加状态列
        TableColumn<WeighingRecord, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(60);
        statusCol.setMinWidth(50);
        statusCol.setStyle("-fx-alignment: CENTER;");

        // 添加操作列 - 预览和打印按钮
        TableColumn<WeighingRecord, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(180);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button previewBtn = new Button("打印预览");
            private final Button printBtn = new Button("直接打印");
            private final javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(5);

            {
                previewBtn.setOnAction(e -> {
                    WeighingRecord record = table.getItems().get(getIndex());
                    showReceiptPreview(record);
                });

                printBtn.setOnAction(e -> {
                    WeighingRecord record = table.getItems().get(getIndex());
                    printReceiptDirectly(record);
                });

                // 设置按钮样式
                previewBtn.setStyle("-fx-font-size: 14px; -fx-padding: 4 10 4 10;");
                printBtn.setStyle("-fx-font-size: 14px; -fx-padding: 4 10 4 10;");

                buttonBox.getChildren().addAll(previewBtn, printBtn);
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
            }
        });

        table.getColumns().addAll(precheckCol, leafCol, bundleCol, weightCol, timeCol, operatorCol, statusCol,
                actionCol);

        // 创建状态标签
        statusLabel = new Label("正在加载称重记录...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-padding: 10;");

        // 创建刷新按钮
        Button refreshButton = new Button("刷新记录");
        refreshButton.setOnAction(e -> loadWeighingRecords());
        refreshButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 16;");

        // 创建关闭按钮
        Button closeButton = new Button("关闭");
        closeButton.setOnAction(e -> close());
        closeButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 16;");

        // 底部按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(refreshButton, closeButton);

        // 创建底部容器
        VBox bottomContainer = new VBox(5);
        bottomContainer.setPadding(new Insets(10));
        bottomContainer.getChildren().addAll(statusLabel, buttonBox);

        root.setCenter(table);
        root.setBottom(bottomContainer);

        // 将本地table变量赋值给实例字段
        this.table = table;

        setScene(new Scene(root));

        // 加载称重记录
        loadWeighingRecords();

        // 启动定时刷新（每30秒刷新一次）
        startAutoRefresh();

        // 设置窗口关闭事件
        setOnCloseRequest(event -> {
            stopAutoRefresh();
        });
    }

    /**
     * 启动自动刷新
     */
    private void startAutoRefresh() {
        refreshTimer = new java.util.Timer(true);
        refreshTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // 在后台线程刷新数据
                Platform.runLater(() -> {
                    if (isShowing()) {
                        loadWeighingRecords();
                    }
                });
            }
        }, 30000, 30000); // 30秒后开始，每30秒执行一次
    }

    /**
     * 停止自动刷新
     */
    private void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    /**
     * 从数据库加载称重记录
     */
    private void loadWeighingRecords() {
        if (weighingRecordRepository == null) {
            statusLabel.setText("数据仓库未初始化");
            return;
        }

        statusLabel.setText("正在加载称重记录...");

        // 在后台线程查询数据库
        new Thread(() -> {
            try {
                // 根据农户姓名查询所有记录
                weighingRecordRepository.findByFarmerName(currentFarmerName,
                        new WeighingRecordRepository.OnResultListener<List<WeighingRecord>>() {
                            @Override
                            public void onSuccess(List<WeighingRecord> records) {
                                Platform.runLater(() -> {
                                    // 进一步筛选匹配合同号的记录（如果有合同号的话）
                                    List<WeighingRecord> matchingRecords = records;
                                    if (currentContractNumber != null && !currentContractNumber.isEmpty()) {
                                        matchingRecords = records.stream()
                                                .filter(r -> currentContractNumber.equals(r.getContractNumber()))
                                                .toList();
                                    }

                                    // 更新表格
                                    table.getItems().clear();
                                    table.getItems().addAll(matchingRecords);

                                    // 更新状态信息
                                    if (matchingRecords.isEmpty()) {
                                        statusLabel.setText("该农户暂无称重记录");
                                    } else {
                                        double totalWeight = matchingRecords.stream()
                                                .mapToDouble(record -> record.getWeight() * record.getBundleCount())
                                                .sum();
                                        statusLabel.setText(String.format("共 %d 条记录，总重量: %.2f kg",
                                                matchingRecords.size(), totalWeight));

                                        // 检查并修复错误的预检编号数据
                                        fixIncorrectPrecheckIds();
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                Platform.runLater(() -> {
                                    logger.error("加载称重记录失败", e);
                                    statusLabel.setText("加载称重记录失败: " + e.getMessage());
                                    showErrorAlert("加载失败", "无法加载称重记录: " + e.getMessage());
                                });
                            }
                        });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("查询称重记录时发生异常", e);
                    statusLabel.setText("查询失败: " + e.getMessage());
                    showErrorAlert("查询失败", "查询称重记录时发生异常: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 显示错误对话框
     */
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 显示称重小票预览对话框
     */
    private void showReceiptPreview(WeighingRecord record) {
        try {
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";
            // 获取站点名称和地址
            String stationName = getStationName(record.getIdCardNumber());
            String address = getFarmerAddress(record.getIdCardNumber());
            String displayPrecheck = getLast5Digits(precheckId);
            String currentDateStr = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            Image qrImage = QRCodeGenerator.generateQRCodeImage(contractNum, 110);

            Stage previewStage = new Stage();
            previewStage.setTitle("标签预览 - " + farmerName);
            previewStage.initModality(Modality.APPLICATION_MODAL);
            previewStage.initOwner(this);
            previewStage.setResizable(false);

            double dpi = javafx.stage.Screen.getPrimary().getDpi();
            double targetWidthPx = (120.0 / 25.4) * dpi;
            double targetHeightPx = (77.0 / 25.4) * dpi;

            VBox root = new VBox(8);
            double padding = 10;
            root.setPadding(new Insets(padding));
            root.setAlignment(Pos.CENTER);

            Label titleLabel = new Label("称重标签预览");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            HBox twoCols = new HBox(12);
            twoCols.setAlignment(Pos.CENTER);

            double availableWidth = targetWidthPx - 2 * padding;
            double colWidth = (availableWidth - twoCols.getSpacing()) / 2.0;
            int qrTarget = (int) Math.max(90, Math.min(110, colWidth * 0.5));

            java.util.function.Supplier<VBox> buildStandardColumn = () -> {
                VBox col = new VBox(6);
                col.setAlignment(Pos.TOP_LEFT);
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
                VBox text = new VBox(2);
                text.setAlignment(Pos.TOP_LEFT);
                text.setStyle("-fx-font-size: 12px; -fx-font-family: 'SimSun';");
                text.getChildren().addAll(
                        new Label(stationName),
                        new Label(address),
                        new Label(contractNum),
                        new Label("姓名: " + farmerName),
                        new Label("预检号: " + getLast5Digits(displayPrecheck)),
                        new Label("重量: " + String.format("%.2f kg", record.getWeight())),
                        new Label("部位: " + leafType),
                        new Label("检验: " + inspector),
                        new Label("预检日期: " + currentDateStr));
                col.getChildren().add(text);
                return col;
            };

            VBox leftCol = buildStandardColumn.get();
            VBox rightCol = buildStandardColumn.get();
            rightCol.setRotate(180);

            twoCols.getChildren().addAll(leftCol, rightCol);
            root.getChildren().addAll(titleLabel, twoCols);

            Scene scene = new Scene(root, targetWidthPx, targetHeightPx);
            previewStage.setScene(scene);
            previewStage.centerOnScreen();
            previewStage.showAndWait();
        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("预览失败");
            errorAlert.setHeaderText("显示标签预览失败");
            errorAlert.setContentText("错误信息: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * 直接打印称重小票
     */
    private void printReceiptDirectly(WeighingRecord record) {
        try {
            // 使用新的图片打印方法
            printReceiptWithQRCode(record);

        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("打印错误");
            errorAlert.setHeaderText("打印过程中发生错误");
            errorAlert.setContentText("错误信息: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * 保存标签预览为图片文件
     */
    private void saveLabelPreview(WeighingRecord record) {
        try {
            // 获取基本信息
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";
            String locationInfo = "默认地址";

            // 生成二维码图片
            java.awt.image.BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(contractNum, 80);

            // 创建标签信息
            com.tobacco.weight.hardware.PrinterManager.LabelInfo labelInfo = new com.tobacco.weight.hardware.PrinterManager.LabelInfo(
                    locationInfo, "身份证号", contractNum, farmerName, precheckId, leafType, inspector, "当前日期");

            // 创建文件选择对话框
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("保存标签预览");
            fileChooser.setInitialFileName("标签预览_" + precheckId + "_" + System.currentTimeMillis() + ".png");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PNG图片", "*.png"));

            // 设置初始目录为桌面
            String userHome = System.getProperty("user.home");
            java.io.File desktop = new java.io.File(userHome, "Desktop");
            if (desktop.exists()) {
                fileChooser.setInitialDirectory(desktop);
            }

            java.io.File selectedFile = fileChooser.showSaveDialog(this);
            if (selectedFile != null) {
                // 获取PrinterManager实例 - 需要从MainController获取
                boolean success = saveLabelPreviewToFile(qrCodeImage, labelInfo, selectedFile.getAbsolutePath());
                if (success) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("保存成功");
                    alert.setHeaderText("标签预览已保存");
                    alert.setContentText(
                            "标签预览已保存到:\n" + selectedFile.getAbsolutePath() + "\n\n尺寸: 70x70mm (198x198像素)");
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("保存失败");
                    alert.setHeaderText("无法保存标签预览");
                    alert.showAndWait();
                }
            }

        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("保存失败");
            errorAlert.setHeaderText("保存标签预览失败");
            errorAlert.setContentText("错误信息: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * 保存标签预览到文件的实际实现
     */
    private boolean saveLabelPreviewToFile(java.awt.image.BufferedImage qrCodeImage,
            com.tobacco.weight.hardware.PrinterManager.LabelInfo labelInfo, String filePath) {
        try {
            // 创建PrinterManager实例并保存
            com.tobacco.weight.hardware.PrinterManager printerManager = new com.tobacco.weight.hardware.PrinterManager();
            return printerManager.saveLabelPreview(qrCodeImage, labelInfo, filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 使用二维码图片打印标签
     */
    private void printReceiptWithQRCode(WeighingRecord record) {
        try {
            // 获取基本信息
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";
            String locationInfo = "默认地址";

            // 生成二维码图片（用于打印）
            BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(contractNum, 80);

            if (qrCodeImage == null) {
                // 降级到文本打印
                PrinterManager printerManager = new PrinterManager();
                String compactLabel = generateReceiptContent(record);

                // 按捆数打印多份文本标签
                boolean allPrintSuccess = true;
                int printCount = 0;
                int bundleCount = record.getBundleCount();

                for (int i = 0; i < bundleCount; i++) {
                    boolean printSuccess = printerManager.printText(compactLabel);
                    if (printSuccess) {
                        printCount++;
                        logger.info("成功打印第 {} 份文本标签，预检编号: {}", i + 1, record.getPrecheckId());
                    } else {
                        allPrintSuccess = false;
                        logger.error("打印第 {} 份文本标签失败，预检编号: {}", i + 1, record.getPrecheckId());
                    }
                }

                Alert resultAlert = new Alert(allPrintSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                resultAlert.setTitle(allPrintSuccess ? "打印成功" : "打印失败");
                resultAlert.setHeaderText(allPrintSuccess ? String.format("成功打印 %d 份文本标签", bundleCount) : "标签打印失败");
                resultAlert.setContentText(
                        allPrintSuccess
                                ? String.format("预检编号: %s\n农户: %s\n重量: %.2f kg\n捆数: %d\n成功打印: %d/%d",
                                        getLast5Digits(record.getPrecheckId()),
                                        record.getFarmerName(),
                                        record.getWeight(),
                                        bundleCount,
                                        printCount, bundleCount)
                                : String.format("部分文本标签打印失败，成功: %d/%d，请检查打印机连接状态", printCount, bundleCount));
                resultAlert.showAndWait();
                return;
            }

            // 创建标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    locationInfo, "身份证号", contractNum, farmerName, precheckId, leafType, inspector, "当前日期");

            // 按捆数打印多份标签
            PrinterManager printerManager = new PrinterManager();
            boolean allPrintSuccess = true;
            int printCount = 0;
            int bundleCount = record.getBundleCount();

            for (int i = 0; i < bundleCount; i++) {
                boolean printSuccess = printerManager.printLabelWithQRCode(qrCodeImage, labelInfo);
                if (printSuccess) {
                    printCount++;
                    logger.info("成功打印第 {} 份标签，预检编号: {}", i + 1, record.getPrecheckId());
                } else {
                    allPrintSuccess = false;
                    logger.error("打印第 {} 份标签失败，预检编号: {}", i + 1, record.getPrecheckId());
                }
            }

            // 显示打印结果
            Alert resultAlert = new Alert(allPrintSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            resultAlert.setTitle(allPrintSuccess ? "打印成功" : "打印失败");
            resultAlert.setHeaderText(allPrintSuccess ? String.format("成功打印 %d 份带二维码的标签", bundleCount) : "标签打印失败");
            resultAlert.setContentText(
                    allPrintSuccess
                            ? String.format("预检编号: %s\n农户: %s\n重量: %.2f kg\n捆数: %d\n成功打印: %d/%d",
                                    getLast5Digits(record.getPrecheckId()),
                                    record.getFarmerName(),
                                    record.getWeight(),
                                    bundleCount,
                                    printCount, bundleCount)
                            : String.format("部分标签打印失败，成功: %d/%d，请检查打印机连接状态和驱动程序", printCount, bundleCount));
            resultAlert.showAndWait();

        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("打印错误");
            errorAlert.setHeaderText("打印过程中发生错误");
            errorAlert.setContentText("错误信息: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * 显示二维码扫描测试窗口
     */
    private void showQRCodeTest(WeighingRecord record) {
        try {
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";

            // 生成大尺寸测试二维码
            Image testQRCode = QRCodeGenerator.generateQRCodeImage(contractNum, 300);

            if (testQRCode == null) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("生成失败");
                errorAlert.setContentText("无法生成二维码图片");
                errorAlert.showAndWait();
                return;
            }

            // 创建测试窗口
            Stage testStage = new Stage();
            testStage.setTitle("二维码扫描测试 - " + contractNum);
            testStage.initModality(Modality.APPLICATION_MODAL);
            testStage.initOwner(this);

            // 创建ImageView显示真实二维码图片
            ImageView qrImageView = new ImageView(testQRCode);
            qrImageView.setFitWidth(300);
            qrImageView.setFitHeight(300);
            qrImageView.setPreserveRatio(true);
            qrImageView.setSmooth(false); // 关闭平滑处理，保持二维码清晰

            // 说明文本
            Label instructionLabel = new Label(
                    "请用手机扫描以下二维码测试:\n" +
                            "合同号: " + contractNum + "\n\n" +
                            "✅ 这是真实的二维码图片，手机应该可以正常扫描！\n" +
                            "注意: 实际打印会使用更小的版本以适应70x70mm标签");
            instructionLabel.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-text-alignment: center;");
            instructionLabel.setWrapText(true);

            // 关闭按钮
            Button closeTestButton = new Button("关闭");
            closeTestButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20 8 20;");
            closeTestButton.setOnAction(e -> testStage.close());

            HBox testButtonBox = new HBox(closeTestButton);
            testButtonBox.setAlignment(Pos.CENTER);
            testButtonBox.setPadding(new Insets(10));

            // 布局
            VBox testLayout = new VBox(15);
            testLayout.setPadding(new Insets(20));
            testLayout.setAlignment(Pos.CENTER);
            testLayout.getChildren().addAll(instructionLabel, qrImageView, testButtonBox);

            Scene testScene = new Scene(testLayout);
            testStage.setScene(testScene);

            // 设置窗口大小
            testStage.setWidth(400);
            testStage.setHeight(550);
            testStage.centerOnScreen();

            testStage.show();

        } catch (Exception e) {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("测试失败");
            errorAlert.setContentText("无法显示二维码测试: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    /**
     * 生成称重小票内容 (70x70mm标签格式，包含二维码)
     */
    private String generateReceiptContent(WeighingRecord record) {
        StringBuilder content = new StringBuilder();

        // 获取基本信息
        String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
        String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
        String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
        String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
        String inspector = record.getOperator() != null ? record.getOperator() : "系统";

        // 获取站点名称和地址
        String stationName = getStationName(record.getIdCardNumber());
        String address = getFarmerAddress(record.getIdCardNumber());

        // 生成合同号二维码（超紧凑版）
        String qrCode = QRCodeGenerator.generateCompactQRCode(contractNum);

        // 构建标签内容 - 二维码在最上方，超紧凑
        String[] qrLines = qrCode.split("\n");

        // 首先输出二维码（不居中，节省空间）
        for (String qrLine : qrLines) {
            content.append(qrLine).append("\n");
        }

        // 信息列表（紧凑显示，包含所有必要字段）
        content.append(stationName.length() > 6 ? stationName.substring(0, 6) + ".." : stationName)
                .append("\n");
        content.append(address.length() > 6 ? address.substring(0, 6) + ".." : address)
                .append("\n");
        content.append(contractNum.length() > 15 ? contractNum.substring(0, 15) + ".." : contractNum)
                .append("\n");
        content.append("姓名:").append(farmerName.length() > 8 ? farmerName.substring(0, 8) + ".." : farmerName)
                .append("\n");
        content.append("预检:").append(precheckId.length() > 12 ? precheckId.substring(0, 12) + ".." : precheckId)
                .append("\n");
        content.append("重量:").append(String.format("%.2f", record.getWeight())).append("kg\n");
        content.append("部位:").append(leafType.length() > 6 ? leafType.substring(0, 6) + ".." : leafType).append("\n");
        content.append("检验:").append(inspector.length() > 6 ? inspector.substring(0, 6) + ".." : inspector)
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

        System.out.println("=== 标签纸张大小估算 ===");
        System.out.println("标签内容行数: " + lines.length);
        System.out.println("最长行字符数: " + maxLineLength);
        System.out.println("估算宽度: " + String.format("%.1f", estimatedWidth) + " mm");
        System.out.println("估算高度: " + String.format("%.1f", estimatedHeight) + " mm");
        System.out.println("是否适合70x70mm纸张: " + ((estimatedWidth <= 70 && estimatedHeight <= 70) ? "是" : "否"));
        System.out.println("================================");

        return finalContent;
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
            // 从数据库查询烟农地址
            String sql = "SELECT address FROM farmer_info WHERE id_card_number = ? AND address IS NOT NULL AND address != ''";
            try (java.sql.Connection conn = DatabaseManager.getInstance().getConnection();
                    java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, idCardNumber.trim());
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String address = rs.getString("address");
                        if (address != null && !address.trim().isEmpty()) {
                            return address.trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("查询烟农地址失败: {}", e.getMessage());
        }

        return "待完善";
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
            try (java.sql.Connection conn = DatabaseManager.getInstance().getConnection();
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
     * 修复错误的预检编号数据
     * 如果预检编号被错误地设置为身份证号，则尝试修复
     */
    private void fixIncorrectPrecheckIds() {
        if (weighingRecordRepository == null) {
            return;
        }

        // 在后台线程执行数据修复
        new Thread(() -> {
            try {
                // 根据农户姓名查询所有记录
                weighingRecordRepository.findByFarmerName(currentFarmerName,
                        new WeighingRecordRepository.OnResultListener<List<WeighingRecord>>() {
                            @Override
                            public void onSuccess(List<WeighingRecord> records) {
                                boolean hasFixed = false;

                                for (WeighingRecord record : records) {
                                    String precheckId = record.getPrecheckId();
                                    if (precheckId != null && precheckId.length() == 18 &&
                                            precheckId.matches("\\d{17}[\\dXx]")) {
                                        // 这是身份证号，需要修复为预检编号
                                        try {
                                            // 生成新的预检编号：身份证后6位+合同号后6位+随机序列
                                            // 获取烟农信息中实际的身份证号
                                            String actualIdCard = record.getIdCardNumber();
                                            if (actualIdCard == null || actualIdCard.trim().isEmpty()) {
                                                // 如果记录中没有身份证号，尝试从农户信息中获取
                                                actualIdCard = currentIdCardNumber;
                                            }

                                            String idCardLast6 = actualIdCard != null && actualIdCard.length() >= 6
                                                    ? actualIdCard.substring(actualIdCard.length() - 6)
                                                    : "000000";
                                            String contractLast6 = record.getContractNumber() != null &&
                                                    record.getContractNumber().length() >= 6
                                                            ? record.getContractNumber()
                                                                    .substring(record.getContractNumber().length() - 6)
                                                            : "000000";

                                            // 使用记录ID作为序列号
                                            String seq = String.format("%05d",
                                                    record.getId() != null ? record.getId().intValue() : 0);
                                            String newPrecheckId = idCardLast6 + contractLast6 + seq;

                                            // 更新记录
                                            record.setPrecheckId(newPrecheckId);
                                            hasFixed = true;

                                            logger.info("修复预检编号: {} -> {}", precheckId, newPrecheckId);
                                        } catch (Exception e) {
                                            logger.error("修复预检编号失败", e);
                                        }
                                    }
                                }

                                if (hasFixed) {
                                    // 刷新表格显示
                                    Platform.runLater(() -> {
                                        table.getItems().clear();
                                        table.getItems().addAll(records);
                                    });
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                logger.error("修复预检编号数据失败", e);
                            }
                        });

            } catch (Exception e) {
                logger.error("修复预检编号数据时发生异常", e);
            }
        }).start();
    }
}