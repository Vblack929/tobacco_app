package com.tobacco.weight.ui;

import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.hardware.PrinterManager;
import com.tobacco.weight.util.QRCodeGenerator;
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
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;

public class FarmerDetailDialog extends Stage {
    public FarmerDetailDialog(FarmerStats stats) {
        setTitle("农户详情 - " + stats.farmerNameProperty().get());
        setWidth(700);
        setHeight(500);
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        Label title = new Label(
                "农户: " + stats.farmerNameProperty().get() + "    身份证号: " + stats.idCardProperty().get());
        title.setPadding(new Insets(10));
        root.setTop(title);

        TableView<WeighingRecord> table = new TableView<>();
        TableColumn<WeighingRecord, String> precheckCol = new TableColumn<>("预检编号");
        precheckCol.setCellValueFactory(new PropertyValueFactory<>("precheckId"));
        TableColumn<WeighingRecord, String> leafCol = new TableColumn<>("部叶类型");
        leafCol.setCellValueFactory(new PropertyValueFactory<>("leafType"));
        TableColumn<WeighingRecord, Integer> bundleCol = new TableColumn<>("捆数");
        bundleCol.setCellValueFactory(new PropertyValueFactory<>("bundleCount"));
        TableColumn<WeighingRecord, Double> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        TableColumn<WeighingRecord, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // 添加操作列 - 预览和打印按钮
        TableColumn<WeighingRecord, Void> actionCol = new TableColumn<>("操作");
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
                previewBtn.setStyle("-fx-font-size: 12px; -fx-padding: 3 8 3 8;");
                printBtn.setStyle("-fx-font-size: 12px; -fx-padding: 3 8 3 8;");

                buttonBox.getChildren().addAll(previewBtn, printBtn);
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttonBox);
            }
        });

        table.getColumns().addAll(precheckCol, leafCol, bundleCol, weightCol, timeCol, actionCol);
        table.getItems().addAll(stats.getRecords());
        root.setCenter(table);

        setScene(new Scene(root));
    }

    /**
     * 显示称重小票预览对话框
     */
    private void showReceiptPreview(WeighingRecord record) {
        try {
            // 获取基本信息用于生成二维码和文本
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";
            String locationInfo = "默认地址"; // 管理员界面没有直接的地址信息

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
                printReceiptWithQRCode(record);
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
                boolean printSuccess = printerManager.printText(compactLabel);

                Alert resultAlert = new Alert(printSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                resultAlert.setTitle(printSuccess ? "打印成功" : "打印失败");
                resultAlert.setHeaderText(printSuccess ? "标签打印完成（文本模式）" : "标签打印失败");
                resultAlert.setContentText(
                        printSuccess
                                ? "预检编号: " + record.getPrecheckId() + "\n农户: " + record.getFarmerName()
                                : "请检查打印机连接状态");
                resultAlert.showAndWait();
                return;
            }

            // 创建标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    locationInfo, contractNum, farmerName, precheckId, leafType, inspector);

            // 使用新的图片打印方法
            PrinterManager printerManager = new PrinterManager();
            boolean printSuccess = printerManager.printLabelWithQRCode(qrCodeImage, labelInfo);

            // 显示打印结果
            Alert resultAlert = new Alert(printSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            resultAlert.setTitle(printSuccess ? "打印成功" : "打印失败");
            resultAlert.setHeaderText(printSuccess ? "带二维码的标签打印完成" : "标签打印失败");
            resultAlert.setContentText(
                    printSuccess
                            ? "预检编号: " + record.getPrecheckId() + "\n农户: " + record.getFarmerName() + "\n重量: "
                                    + String.format("%.2f kg", record.getWeight())
                            : "请检查打印机连接状态和驱动程序");
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

        // 乡镇村信息暂时使用默认值（由于没有直接的地址信息）
        String locationInfo = "默认地址";

        // 生成合同号二维码（超紧凑版）
        String qrCode = QRCodeGenerator.generateCompactQRCode(contractNum);

        // 构建标签内容 - 二维码在最上方，超紧凑
        String[] qrLines = qrCode.split("\n");

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

        System.out.println("=== 标签纸张大小估算 ===");
        System.out.println("标签内容行数: " + lines.length);
        System.out.println("最长行字符数: " + maxLineLength);
        System.out.println("估算宽度: " + String.format("%.1f", estimatedWidth) + " mm");
        System.out.println("估算高度: " + String.format("%.1f", estimatedHeight) + " mm");
        System.out.println("是否适合70x70mm纸张: " + ((estimatedWidth <= 70 && estimatedHeight <= 70) ? "是" : "否"));
        System.out.println("================================");

        return finalContent;
    }
}