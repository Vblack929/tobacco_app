package com.tobacco.weight.ui;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.database.WeighingRecordRepository;
import com.tobacco.weight.hardware.PrinterManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
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

        // 打印按钮列
        TableColumn<WeighingRecord, Void> printCol = new TableColumn<>("操作");
        printCol.setPrefWidth(80);
        printCol.setCellFactory(new Callback<TableColumn<WeighingRecord, Void>, TableCell<WeighingRecord, Void>>() {
            @Override
            public TableCell<WeighingRecord, Void> call(TableColumn<WeighingRecord, Void> param) {
                return new TableCell<WeighingRecord, Void>() {
                    private final Button printButton = new Button("打印标签");

                    {
                        printButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 8 2 8;");
                        printButton.setOnAction(event -> {
                            WeighingRecord record = getTableView().getItems().get(getIndex());
                            printRecordLabel(record);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(printButton);
                        }
                    }
                };
            }
        });

        recordTable.getColumns().addAll(precheckCol, leafCol, bundleCol, weightCol, timeCol, operatorCol, statusCol, printCol);

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
        weighingRecordRepository.findByFarmerName(farmerInfo.getFarmerName(), new WeighingRecordRepository.OnResultListener<List<WeighingRecord>>() {
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
                        double totalWeight = matchingRecords.stream().mapToDouble(WeighingRecord::getWeight).sum();
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
     * 打印称重记录标签
     */
    private void printRecordLabel(WeighingRecord record) {
        try {
            // 使用PrinterManager打印称重小票
            boolean printSuccess = printerManager.printWeighingReceipt(
                record.getFarmerName(),
                record.getContractNumber(),
                record.getLeafType(),
                record.getWeight(),
                record.getOperator() != null ? record.getOperator() : "系统",
                record.getBundleCount(),
                record.getPrecheckId()
            );

            if (printSuccess) {
                showSuccessAlert("打印成功", "称重记录标签打印完成\n预检编号: " + record.getPrecheckId());
                logger.info("成功打印称重记录标签，预检编号: {}", record.getPrecheckId());
            } else {
                showAlert("打印失败", "标签打印失败，请检查打印机连接");
                logger.error("打印称重记录标签失败，预检编号: {}", record.getPrecheckId());
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
}