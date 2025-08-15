package com.tobacco.weight.ui;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.WeighingRecordRepository;
import com.tobacco.weight.hardware.PrinterManager;
import com.tobacco.weight.util.QRCodeGenerator;
import java.awt.image.BufferedImage;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 农户信息详情对话框
 * 显示农户的称重记录
 */
public class FarmerInfoDetailDialog extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(FarmerInfoDetailDialog.class);
    private WeighingRecordRepository weighingRecordRepository;
    private TableView<WeighingRecord> recordTable;
    private Label statusLabel;
    private PrinterManager printerManager;
    private Button refreshButtonRef;

    public FarmerInfoDetailDialog(FarmerInfo farmerInfo) {
        setTitle("农户称重记录 - " + farmerInfo.getFarmerName());
        setWidth(1100);
        setHeight(600);
        initModality(Modality.APPLICATION_MODAL);

        this.currentFarmerInfo = farmerInfo; // 保存农户信息引用
        this.weighingRecordRepository = new WeighingRecordRepository(DatabaseManager.getInstance());
        this.printerManager = new PrinterManager();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // 创建头部信息
        VBox headerContent = createHeaderContent(farmerInfo);
        root.setTop(headerContent);

        // 创建称重记录表格
        VBox centerContent = createRecordTableContent();
        root.setCenter(centerContent);

        // 底部按钮区域
        HBox buttonBox = createButtonBox();
        root.setBottom(buttonBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        setScene(scene);

        // 加载称重记录
        loadWeighingRecords(farmerInfo);
    }

    /**
     * 创建头部信息区域
     */
    private VBox createHeaderContent(FarmerInfo farmerInfo) {
        VBox headerContent = new VBox(10);

        // 农户标题
        Label titleLabel = new Label("农户: " + farmerInfo.getFarmerName());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 基本信息
        HBox infoBox = new HBox(30);

        Label contractLabel = new Label("合同号: " + farmerInfo.getContractNumber());
        contractLabel.setStyle("-fx-font-size: 14px;");

        Label idCardLabel = new Label("身份证号: " + farmerInfo.getMaskedIdCardNumber());
        idCardLabel.setStyle("-fx-font-size: 14px;");

        infoBox.getChildren().addAll(contractLabel, idCardLabel);

        headerContent.getChildren().addAll(titleLabel, infoBox, new Separator());
        return headerContent;
    }

    /**
     * 创建称重记录表格内容
     */
    private VBox createRecordTableContent() {
        VBox tableContent = new VBox(10);
        tableContent.setAlignment(Pos.CENTER);

        // 表格标题
        Label tableTitle = new Label("称重记录");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // 创建表格
        recordTable = new TableView<>();
        recordTable.setPrefHeight(350);
        // 列宽以内容为准，不拉伸铺满
        recordTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        // 表格宽度由首选宽度控制，不随容器无限拉伸
        recordTable.setMaxWidth(Region.USE_PREF_SIZE);

        // 创建表格列
        TableColumn<WeighingRecord, String> precheckCol = new TableColumn<>("预检编号");
        precheckCol.setCellValueFactory(new PropertyValueFactory<>("precheckId"));
        precheckCol.setPrefWidth(240);
        precheckCol.setMinWidth(220);
        precheckCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<WeighingRecord, String> leafCol = new TableColumn<>("部叶类型");
        leafCol.setCellValueFactory(new PropertyValueFactory<>("leafType"));
        leafCol.setPrefWidth(90);
        leafCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<WeighingRecord, Integer> bundleCol = new TableColumn<>("捆数");
        bundleCol.setCellValueFactory(new PropertyValueFactory<>("bundleCount"));
        bundleCol.setPrefWidth(70);
        bundleCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<WeighingRecord, Double> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        weightCol.setPrefWidth(90);
        weightCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<WeighingRecord, String> timeCol = new TableColumn<>("称重时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        // 足够容纳完整时间（例如 2025-07-27 16:23:38.731）
        timeCol.setPrefWidth(240);
        timeCol.setMinWidth(220);
        timeCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<WeighingRecord, String> operatorCol = new TableColumn<>("操作员");
        operatorCol.setCellValueFactory(new PropertyValueFactory<>("operator"));
        operatorCol.setPrefWidth(90);
        operatorCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<WeighingRecord, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(70);
        statusCol.setStyle("-fx-alignment: CENTER;");

        // 操作按钮列
        TableColumn<WeighingRecord, Void> printCol = new TableColumn<>("操作");
        printCol.setPrefWidth(160);
        printCol.setCellFactory(new Callback<TableColumn<WeighingRecord, Void>, TableCell<WeighingRecord, Void>>() {
            @Override
            public TableCell<WeighingRecord, Void> call(TableColumn<WeighingRecord, Void> param) {
                return new TableCell<WeighingRecord, Void>() {
                    private final Button previewButton = new Button("打印预览");
                    private final Button printButton = new Button("直接打印");
                    private final HBox buttonBox = new HBox(5);

                    {
                        previewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6;");
                        printButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 6 2 6;");

                        previewButton.setOnAction(event -> {
                            WeighingRecord record = getTableView().getItems().get(getIndex());
                            showRecordPreview(record);
                        });

                        printButton.setOnAction(event -> {
                            WeighingRecord record = getTableView().getItems().get(getIndex());
                            printRecordLabel(record);
                        });

                        buttonBox.getChildren().addAll(previewButton, printButton);
                        buttonBox.setAlignment(Pos.CENTER);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonBox);
                        }
                    }
                };
            }
        });

        recordTable.getColumns().addAll(precheckCol, leafCol, bundleCol, weightCol, timeCol, operatorCol, statusCol,
                printCol);

        // 计算并设置表格首选宽度 = 各列宽度之和 + 滚动条/边框余量
        double tablePrefWidth = precheckCol.getPrefWidth() + leafCol.getPrefWidth() + bundleCol.getPrefWidth()
                + weightCol.getPrefWidth() + timeCol.getPrefWidth() + operatorCol.getPrefWidth()
                + statusCol.getPrefWidth() + printCol.getPrefWidth() + 40; // 余量
        recordTable.setPrefWidth(tablePrefWidth);

        // 状态标签
        statusLabel = new Label("正在加载称重记录...");
        statusLabel.setStyle("-fx-text-fill: #666666;");

        tableContent.getChildren().addAll(tableTitle, recordTable, statusLabel);
        VBox.setVgrow(recordTable, Priority.NEVER);

        return tableContent;
    }

    /**
     * 加载称重记录
     */
    private void loadWeighingRecords(FarmerInfo farmerInfo) {
        // 根据农户姓名和合同号查询称重记录
        weighingRecordRepository.findByFarmerName(farmerInfo.getFarmerName(),
                new WeighingRecordRepository.OnResultListener<List<WeighingRecord>>() {
                    @Override
                    public void onSuccess(List<WeighingRecord> records) {
                        Platform.runLater(() -> {
                            // 进一步筛选匹配合同号的记录
                            List<WeighingRecord> matchingRecords = records.stream()
                                    .filter(r -> farmerInfo.getContractNumber().equals(r.getContractNumber()))
                                    .toList();

                            recordTable.getItems().clear();
                            recordTable.getItems().addAll(matchingRecords);

                            if (matchingRecords.isEmpty()) {
                                statusLabel.setText("该农户暂无称重记录");
                            } else {
                                double totalWeight = matchingRecords.stream().mapToDouble(WeighingRecord::getWeight)
                                        .sum();
                                statusLabel.setText(String.format("共 %d 条记录，总重量: %.2f kg",
                                        matchingRecords.size(), totalWeight));
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Platform.runLater(() -> {
                            logger.error("加载称重记录失败", e);
                            statusLabel.setText("加载称重记录失败: " + e.getMessage());
                            showAlert("加载失败", "无法加载称重记录: " + e.getMessage());
                        });
                    }
                });
    }

    /**
     * 创建底部按钮区域
     */
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        Button refreshButton = new Button("刷新记录");
        refreshButton.setStyle("-fx-font-size: 14px; -fx-padding: 6 12;");
        this.refreshButtonRef = refreshButton;
        refreshButton.setOnAction(e -> doRefresh());

        Button closeButton = new Button("关闭");
        closeButton.setStyle("-fx-font-size: 14px; -fx-padding: 6 12;");
        closeButton.setOnAction(e -> {
            // 关闭仓库连接
            if (weighingRecordRepository != null) {
                weighingRecordRepository.shutdown();
            }
            close();
        });

        buttonBox.getChildren().addAll(refreshButton, closeButton);
        return buttonBox;
    }

    private void doRefresh() {
        try {
            statusLabel.setText("正在刷新记录...");
            recordTable.getItems().clear();
            if (currentFarmerInfo != null) {
                loadWeighingRecords(currentFarmerInfo);
            }
        } catch (Exception ex) {
            logger.error("刷新记录失败", ex);
            statusLabel.setText("刷新失败: " + ex.getMessage());
        }
    }

    /**
     * 显示称重记录预览
     */
    private void showRecordPreview(WeighingRecord record) {
        try {
            // 基本信息
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";

            // 获取站点名称和地址
            String stationName = getStationName(currentFarmerInfo.getIdCardNumber());
            String locationInfo = getFarmerAddress(currentFarmerInfo.getIdCardNumber());

            String displayPrecheck = getLast5Digits(precheckId);
            String currentDateStr = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            Image qrImage = QRCodeGenerator.generateQRCodeImage(contractNum, 110);

            // 预览窗口
            Stage previewStage = new Stage();
            previewStage.setTitle("标签预览 - " + record.getFarmerName());
            previewStage.initModality(Modality.APPLICATION_MODAL);
            previewStage.initOwner(this);
            previewStage.setResizable(false);

            // 目标尺寸：120×77mm，按DPI换算像素
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

            // 为lambda创建final副本
            final String fStationName = stationName;
            final String fLocationInfo = locationInfo;
            final String fContractNum = contractNum;
            final String fFarmerName = farmerName;
            final String fDisplayPrecheck = displayPrecheck;
            final String fLeafType = leafType;
            final String fInspector = inspector;
            final String fCurrentDateStr = currentDateStr;
            final Image fQrImage = qrImage;
            final double fColWidth = colWidth;
            final int fQrTarget = qrTarget;

            java.util.function.Supplier<VBox> buildStandardColumn = () -> {
                VBox col = new VBox(6);
                col.setAlignment(Pos.TOP_LEFT);
                col.setPrefWidth(fColWidth);
                col.setMaxWidth(fColWidth);
                if (fQrImage != null) {
                    ImageView qr = new ImageView(fQrImage);
                    qr.setFitWidth(fQrTarget);
                    qr.setFitHeight(fQrTarget);
                    qr.setPreserveRatio(true);
                    qr.setSmooth(false);
                    col.getChildren().add(qr);
                }
                VBox text = new VBox(2);
                text.setAlignment(Pos.TOP_LEFT);
                text.setStyle("-fx-font-size: 12px; -fx-font-family: 'SimSun';");
                text.getChildren().addAll(
                        new Label(fStationName),
                        new Label(fLocationInfo),
                        new Label(fContractNum),
                        new Label("姓名: " + fFarmerName),
                        new Label("预检号: " + getLast5Digits(fDisplayPrecheck)),
                        new Label("重量: " + String.format("%.2f kg", record.getWeight())),
                        new Label("部位: " + fLeafType),
                        new Label("检验: " + fInspector),
                        new Label("预检日期: " + fCurrentDateStr));
                col.getChildren().add(text);
                return col;
            };

            VBox leftCol = buildStandardColumn.get();
            VBox rightCol = buildStandardColumn.get();
            rightCol.setRotate(180); // 右联整体旋转180°，实现中心对称

            twoCols.getChildren().addAll(leftCol, rightCol);

            root.getChildren().addAll(titleLabel, twoCols);
            Scene scene = new Scene(root, targetWidthPx, targetHeightPx);
            previewStage.setScene(scene);
            previewStage.centerOnScreen();
            previewStage.showAndWait();
        } catch (Exception e) {
            logger.error("显示标签预览失败", e);
            showAlert("预览失败", "显示标签预览失败: " + e.getMessage());
        }
    }

    // 保存FarmerInfo引用
    private FarmerInfo currentFarmerInfo;

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
        String stationName = getStationName(currentFarmerInfo.getIdCardNumber());
        String address = getFarmerAddress(currentFarmerInfo.getIdCardNumber());

        // 生成合同号二维码（紧凑版用于实际打印）
        String qrCode = QRCodeGenerator.generateCompactQRCode(contractNum);

        // 构建标签内容 - 二维码在最上方，超紧凑
        String[] qrLines = qrCode.split("\n");

        // 调试信息
        logger.info("二维码行数: {}", qrLines.length);
        logger.info("二维码内容: {}", qrCode);

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

        logger.info("=== 标签纸张大小估算 ===");
        logger.info("标签内容行数: {}", lines.length);
        logger.info("最长行字符数: {}", maxLineLength);
        logger.info("估算宽度: {} mm", String.format("%.1f", estimatedWidth));
        logger.info("估算高度: {} mm", String.format("%.1f", estimatedHeight));
        logger.info("是否适合70x70mm纸张: {}", (estimatedWidth <= 70 && estimatedHeight <= 70) ? "是" : "否");
        logger.info("================================");

        return finalContent;
    }

    /**
     * 打印称重记录标签
     */
    private void printRecordLabel(WeighingRecord record) {
        try {
            // 获取基本信息
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";

            // 获取站点名称和地址
            String stationName = getStationName(currentFarmerInfo.getIdCardNumber());
            String locationInfo = getFarmerAddress(currentFarmerInfo.getIdCardNumber());

            // 生成二维码图片（用于打印）
            BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(contractNum, 80);

            if (qrCodeImage == null) {
                showAlert("生成失败", "无法生成二维码，将使用文本方式打印");
                // 降级到文本打印
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

                showSuccessAlert(allPrintSuccess ? "打印成功" : "部分打印失败",
                        String.format("预检编号: %s\n农户: %s\n重量: %.2f kg\n捆数: %d\n成功打印: %d/%d",
                                getLast5Digits(record.getPrecheckId()),
                                record.getFarmerName(),
                                record.getWeight(),
                                bundleCount,
                                printCount, bundleCount));
                return;
            }

            // 创建标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    locationInfo, "身份证号", contractNum, farmerName, precheckId, leafType, inspector, "当前日期");

            // 按捆数打印多份标签
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
            showSuccessAlert(allPrintSuccess ? "打印成功" : "部分打印失败",
                    String.format("预检编号: %s\n农户: %s\n重量: %.2f kg\n捆数: %d\n成功打印: %d/%d",
                            getLast5Digits(record.getPrecheckId()),
                            record.getFarmerName(),
                            record.getWeight(),
                            bundleCount,
                            printCount, bundleCount));

        } catch (Exception e) {
            logger.error("打印称重记录标签异常", e);
            showAlert("打印错误", "打印时发生错误: " + e.getMessage());
        }
    }

    /**
     * 显示成功提示信息
     */
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误提示信息
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示二维码扫描测试窗口
     */
    private void showQRCodeTest(WeighingRecord record) {
        try {
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";

            // 生成真实的二维码图片（300x300像素，足够大便于手机扫描）
            Image qrImage = QRCodeGenerator.generateQRCodeImage(contractNum, 300);

            if (qrImage == null) {
                showAlert("生成失败", "无法生成二维码图片");
                return;
            }

            // 创建测试窗口
            Stage testStage = new Stage();
            testStage.setTitle("二维码扫描测试 - " + contractNum);
            testStage.initModality(Modality.APPLICATION_MODAL);
            testStage.initOwner(this);

            // 创建ImageView显示真实二维码图片
            ImageView qrImageView = new ImageView(qrImage);
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
            logger.error("显示二维码测试失败", e);
            showAlert("测试失败", "无法显示二维码测试: " + e.getMessage());
        }
    }

    /**
     * 保存标签预览为图片文件
     */
    private void saveRecordPreview(WeighingRecord record) {
        try {
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";

            // 获取站点名称和地址
            String stationName = getStationName(currentFarmerInfo.getIdCardNumber());
            String locationInfo = getFarmerAddress(currentFarmerInfo.getIdCardNumber());

            // 生成用于打印的二维码图
            java.awt.image.BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(contractNum, 80);

            // 标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    locationInfo, "身份证号", contractNum, farmerName, precheckId, leafType, inspector, "当前日期");

            // 选择保存位置
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("保存标签预览");
            fileChooser.setInitialFileName("标签预览_" + precheckId + "_" + System.currentTimeMillis() + ".png");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PNG图片", "*.png"));

            String userHome = System.getProperty("user.home");
            java.io.File desktop = new java.io.File(userHome, "Desktop");
            if (desktop.exists()) {
                fileChooser.setInitialDirectory(desktop);
            }

            java.io.File selectedFile = fileChooser.showSaveDialog(this);
            if (selectedFile != null) {
                boolean success = printerManager.saveLabelPreview(qrCodeImage, labelInfo,
                        selectedFile.getAbsolutePath());
                if (success) {
                    showSuccessAlert("保存成功",
                            "标签预览已保存到:\n" + selectedFile.getAbsolutePath() + "\n\n尺寸: 70x70mm (198x198像素)");
                } else {
                    showAlert("保存失败", "无法保存标签预览");
                }
            }
        } catch (Exception e) {
            showAlert("保存失败", "保存标签预览失败: " + e.getMessage());
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
}