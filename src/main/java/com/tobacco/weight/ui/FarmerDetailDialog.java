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
        setWidth(1000);
        setHeight(500);
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        Label title = new Label(
                "农户: " + stats.farmerNameProperty().get() + "    身份证号: " + stats.idCardProperty().get());
        title.setPadding(new Insets(10));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        root.setTop(title);

        TableView<WeighingRecord> table = new TableView<>();
        TableColumn<WeighingRecord, String> precheckCol = new TableColumn<>("预检编号");
        precheckCol.setCellValueFactory(new PropertyValueFactory<>("precheckId"));
        precheckCol.setPrefWidth(200);
        precheckCol.setMinWidth(180);
        TableColumn<WeighingRecord, String> leafCol = new TableColumn<>("部叶类型");
        leafCol.setCellValueFactory(new PropertyValueFactory<>("leafType"));
        leafCol.setPrefWidth(120);
        TableColumn<WeighingRecord, Integer> bundleCol = new TableColumn<>("捆数");
        bundleCol.setCellValueFactory(new PropertyValueFactory<>("bundleCount"));
        bundleCol.setPrefWidth(80);
        TableColumn<WeighingRecord, Double> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        weightCol.setPrefWidth(120);
        TableColumn<WeighingRecord, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setPrefWidth(200);
        timeCol.setMinWidth(180);

        // 添加操作列 - 预览和打印按钮
        TableColumn<WeighingRecord, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(170);
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
            String contractNum = record.getContractNumber() != null ? record.getContractNumber() : "N/A";
            String farmerName = record.getFarmerName() != null ? record.getFarmerName() : "N/A";
            String precheckId = record.getPrecheckId() != null ? record.getPrecheckId() : "N/A";
            String leafType = record.getLeafType() != null ? record.getLeafType() : "N/A";
            String inspector = record.getOperator() != null ? record.getOperator() : "系统";
            String locationInfo = "实时录入";
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
                col.setAlignment(Pos.TOP_CENTER);
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
                text.setAlignment(Pos.CENTER_LEFT);
                text.setStyle("-fx-font-size: 12px; -fx-font-family: 'SimSun';");
                text.getChildren().addAll(
                        new Label("地址: " + locationInfo),
                        new Label("合同号: " + contractNum),
                        new Label("姓名: " + farmerName),
                        new Label("预检号: " + displayPrecheck),
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
                boolean printSuccess = printerManager.printText(compactLabel);

                Alert resultAlert = new Alert(printSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                resultAlert.setTitle(printSuccess ? "打印成功" : "打印失败");
                resultAlert.setHeaderText(printSuccess ? "标签打印完成（文本模式）" : "标签打印失败");
                resultAlert.setContentText(
                        printSuccess
                                ? "预检编号: " + getLast5Digits(record.getPrecheckId()) + "\n农户: " + record.getFarmerName()
                                : "请检查打印机连接状态");
                resultAlert.showAndWait();
                return;
            }

            // 创建标签信息
            PrinterManager.LabelInfo labelInfo = new PrinterManager.LabelInfo(
                    locationInfo, "身份证号", contractNum, farmerName, precheckId, leafType, inspector, "当前日期");

            // 使用新的图片打印方法
            PrinterManager printerManager = new PrinterManager();
            boolean printSuccess = printerManager.printLabelWithQRCode(qrCodeImage, labelInfo);

            // 显示打印结果
            Alert resultAlert = new Alert(printSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
            resultAlert.setTitle(printSuccess ? "打印成功" : "打印失败");
            resultAlert.setHeaderText(printSuccess ? "带二维码的标签打印完成" : "标签打印失败");
            resultAlert.setContentText(
                    printSuccess
                            ? "预检编号: " + getLast5Digits(record.getPrecheckId()) + "\n农户: " + record.getFarmerName()
                                    + "\n重量: "
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

    /**
     * 获取预检编号的后5位数字
     */
    private String getLast5Digits(String precheckId) {
        if (precheckId == null || precheckId.length() < 5) {
            return precheckId != null ? precheckId : "N/A";
        }
        return precheckId.substring(precheckId.length() - 5);
    }
}