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

    public FarmerInfoDetailDialog(FarmerInfo farmerInfo) {
        setTitle("农户称重记录 - " + farmerInfo.getFarmerName());
        setWidth(950);
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

        // 表格标题
        Label tableTitle = new Label("称重记录");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // 创建表格
        recordTable = new TableView<>();
        recordTable.setPrefHeight(350);

        // 创建表格列
        TableColumn<WeighingRecord, String> precheckCol = new TableColumn<>("预检编号");
        precheckCol.setCellValueFactory(new PropertyValueFactory<>("precheckId"));
        precheckCol.setPrefWidth(120);

        TableColumn<WeighingRecord, String> leafCol = new TableColumn<>("部叶类型");
        leafCol.setCellValueFactory(new PropertyValueFactory<>("leafType"));
        leafCol.setPrefWidth(100);

        TableColumn<WeighingRecord, Integer> bundleCol = new TableColumn<>("捆数");
        bundleCol.setCellValueFactory(new PropertyValueFactory<>("bundleCount"));
        bundleCol.setPrefWidth(60);

        TableColumn<WeighingRecord, Double> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        weightCol.setPrefWidth(100);

        TableColumn<WeighingRecord, String> timeCol = new TableColumn<>("称重时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setPrefWidth(150);

        TableColumn<WeighingRecord, String> operatorCol = new TableColumn<>("操作员");
        operatorCol.setCellValueFactory(new PropertyValueFactory<>("operator"));
        operatorCol.setPrefWidth(80);

        TableColumn<WeighingRecord, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(60);

        // 操作按钮列
        TableColumn<WeighingRecord, Void> printCol = new TableColumn<>("操作");
        printCol.setPrefWidth(150);
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

        // 状态标签
        statusLabel = new Label("正在加载称重记录...");
        statusLabel.setStyle("-fx-text-fill: #666666;");

        tableContent.getChildren().addAll(tableTitle, recordTable, statusLabel);
        VBox.setVgrow(recordTable, Priority.ALWAYS);

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
        refreshButton.setOnAction(e -> {
            // 刷新记录数据（这里需要保存农户信息以便重新加载）
            statusLabel.setText("正在刷新记录...");
            recordTable.getItems().clear();
        });

        Button closeButton = new Button("关闭");
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

    /**
     * 显示称重记录预览
     */
    private void showRecordPreview(WeighingRecord record) {
        try {
            // 获取基本信息用于生成二维码和文本
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";

            // 从地址中提取乡镇村信息
            String locationInfo = "待完善";
            if (currentFarmerInfo != null && currentFarmerInfo.getAddress() != null
                    && !currentFarmerInfo.getAddress().isEmpty()) {
                try {
                    LocationData.LocationInfo location = LocationData.parseAddress(currentFarmerInfo.getAddress());
                    if (location.hasValidTownship() && location.hasValidVillage()) {
                        locationInfo = location.getTownship() + location.getVillage();
                    } else if (location.hasValidTownship()) {
                        locationInfo = location.getTownship();
                    } else if (location.hasValidVillage()) {
                        locationInfo = location.getVillage();
                    } else {
                        locationInfo = "解析失败";
                    }
                } catch (Exception e) {
                    locationInfo = "解析错误";
                }
            }

            // 生成真实的二维码图片用于预览
            Image qrImage = QRCodeGenerator.generateQRCodeImage(contractNum, 150);

            // 创建自定义预览窗口
            Stage previewStage = new Stage();
            previewStage.setTitle("标签预览 - " + record.getFarmerName());
            previewStage.initModality(Modality.APPLICATION_MODAL);
            previewStage.initOwner(this);

            // 创建主布局
            VBox mainLayout = new VBox(10);
            mainLayout.setPadding(new Insets(15));
            mainLayout.setAlignment(Pos.CENTER);

            // 添加标题
            Label titleLabel = new Label("称重标签预览");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");

            // 创建标签内容区域
            VBox labelContent = new VBox(5);
            labelContent.setAlignment(Pos.CENTER);
            labelContent.setStyle(
                    "-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 10; -fx-background-color: white;");

            // 添加二维码图片
            if (qrImage != null) {
                ImageView qrImageView = new ImageView(qrImage);
                qrImageView.setFitWidth(150);
                qrImageView.setFitHeight(150);
                qrImageView.setPreserveRatio(true);
                qrImageView.setSmooth(false);
                labelContent.getChildren().add(qrImageView);
            } else {
                Label qrErrorLabel = new Label("二维码生成失败");
                qrErrorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                labelContent.getChildren().add(qrErrorLabel);
            }

            // 添加文本信息
            VBox textInfo = new VBox(3);
            textInfo.setAlignment(Pos.CENTER_LEFT);
            textInfo.setStyle("-fx-font-family: 'SimSun'; -fx-font-size: 12px;");

            textInfo.getChildren().addAll(
                    new Label("地址: "
                            + (locationInfo.length() > 12 ? locationInfo.substring(0, 12) + ".." : locationInfo)),
                    new Label("合同: " + (contractNum.length() > 20 ? contractNum.substring(0, 20) + ".." : contractNum)),
                    new Label("姓名: " + (farmerName.length() > 10 ? farmerName.substring(0, 10) + ".." : farmerName)),
                    new Label("预检: " + (precheckId.length() > 15 ? precheckId.substring(0, 15) + ".." : precheckId)),
                    new Label("部位: " + (leafType.length() > 8 ? leafType.substring(0, 8) + ".." : leafType)),
                    new Label("检验: " + (inspector.length() > 8 ? inspector.substring(0, 8) + ".." : inspector)));

            labelContent.getChildren().add(textInfo);

            // 创建按钮区域
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setPadding(new Insets(10));

            Button printButton = new Button("确认打印");
            printButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20 8 20;");
            printButton.setOnAction(e -> {
                printRecordLabel(record);
                previewStage.close();
            });

            Button testQRButton = new Button("扫描测试");
            testQRButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20 8 20;");
            testQRButton.setOnAction(e -> {
                showQRCodeTest(record);
            });

            Button closeButton = new Button("关闭");
            closeButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 20 8 20;");
            closeButton.setOnAction(e -> previewStage.close());

            buttonBox.getChildren().addAll(printButton, testQRButton, closeButton);

            // 组装完整布局
            mainLayout.getChildren().addAll(titleLabel, labelContent, buttonBox);

            // 创建场景并设置窗口大小
            Scene scene = new Scene(mainLayout);
            previewStage.setScene(scene);

            // 设置窗口大小
            previewStage.setWidth(350);
            previewStage.setHeight(450);

            // 居中显示
            previewStage.centerOnScreen();

            // 显示预览窗口
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

        // 从地址中提取乡镇村信息
        String locationInfo = "待完善";
        if (currentFarmerInfo != null && currentFarmerInfo.getAddress() != null
                && !currentFarmerInfo.getAddress().isEmpty()) {
            try {
                LocationData.LocationInfo location = LocationData.parseAddress(currentFarmerInfo.getAddress());
                if (location.hasValidTownship() && location.hasValidVillage()) {
                    locationInfo = location.getTownship() + location.getVillage();
                } else if (location.hasValidTownship()) {
                    locationInfo = location.getTownship();
                } else if (location.hasValidVillage()) {
                    locationInfo = location.getVillage();
                } else {
                    locationInfo = "解析失败";
                }
            } catch (Exception e) {
                locationInfo = "解析错误";
            }
        }

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
        content.append("镇:").append(locationInfo.length() > 6 ? locationInfo.substring(0, 6) + ".." : locationInfo)
                .append("\n");
        content.append("合同:").append(contractNum.length() > 15 ? contractNum.substring(0, 15) + ".." : contractNum)
                .append("\n");
        content.append("姓名:").append(farmerName.length() > 8 ? farmerName.substring(0, 8) + ".." : farmerName)
                .append("\n");
        content.append("预检:").append(precheckId.length() > 12 ? precheckId.substring(0, 12) + ".." : precheckId)
                .append("\n");
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

            // 从地址中提取乡镇村信息
            String locationInfo = "待完善";
            if (currentFarmerInfo != null && currentFarmerInfo.getAddress() != null
                    && !currentFarmerInfo.getAddress().isEmpty()) {
                try {
                    LocationData.LocationInfo location = LocationData.parseAddress(currentFarmerInfo.getAddress());
                    if (location.hasValidTownship() && location.hasValidVillage()) {
                        locationInfo = location.getTownship() + location.getVillage();
                    } else if (location.hasValidTownship()) {
                        locationInfo = location.getTownship();
                    } else if (location.hasValidVillage()) {
                        locationInfo = location.getVillage();
                    } else {
                        locationInfo = "解析失败";
                    }
                } catch (Exception e) {
                    locationInfo = "解析错误";
                }
            }

            // 生成二维码图片（用于打印）
            BufferedImage qrCodeImage = QRCodeGenerator.generateQRCodeForPrint(contractNum, 80);

            if (qrCodeImage == null) {
                showAlert("生成失败", "无法生成二维码，将使用文本方式打印");
                // 降级到文本打印
                String compactLabel = generateReceiptContent(record);
                boolean printSuccess = printerManager.printText(compactLabel);
                if (printSuccess) {
                    showSuccessAlert("打印成功", "标签打印完成（文本模式）\n预检编号: " + record.getPrecheckId());
                } else {
                    showAlert("打印失败", "标签打印失败，请检查打印机连接");
                }
                return;
            }

            // 创建标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    locationInfo, contractNum, farmerName, precheckId, leafType, inspector);

            // 使用新的图片打印方法
            boolean printSuccess = printerManager.printLabelWithQRCode(qrCodeImage, labelInfo);

            if (printSuccess) {
                showSuccessAlert("打印成功", "带二维码的标签打印完成\n预检编号: " + record.getPrecheckId());
                logger.info("成功打印带二维码标签，预检编号: {}", record.getPrecheckId());
            } else {
                showAlert("打印失败", "标签打印失败，请检查打印机连接");
                logger.error("打印带二维码标签失败，预检编号: {}", record.getPrecheckId());
            }

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
}