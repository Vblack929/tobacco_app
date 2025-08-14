package com.tobacco.weight.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Excel文件密码输入对话框
 * 用于输入加密Excel文件的密码
 */
public class PasswordDialog {

    private Stage dialogStage;
    private String password;
    private boolean cancelled = false;

    /**
     * 显示密码输入对话框
     * 
     * @param owner 父窗口
     * @param fileName 文件名
     * @return 用户输入的密码，如果取消则返回null
     */
    public static String showPasswordDialog(Window owner, String fileName) {
        PasswordDialog dialog = new PasswordDialog();
        return dialog.show(owner, fileName);
    }

    private String show(Window owner, String fileName) {
        dialogStage = new Stage();
        dialogStage.setTitle("输入文件密码");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner);
        dialogStage.setResizable(false);
        dialogStage.setWidth(400);
        dialogStage.setHeight(200);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        // 提示信息
        Label infoLabel = new Label("Excel文件已加密，请输入密码：");
        infoLabel.setStyle("-fx-font-size: 14px;");

        Label fileLabel = new Label("文件：" + fileName);
        fileLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        // 密码输入框
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefWidth(300);

        // 提示文本
        Label hintLabel = new Label("提示：如果文件密码是0807，可以直接点击确定");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button okButton = new Button("确定");
        okButton.setPrefWidth(80);
        okButton.setDefaultButton(true);
        okButton.setOnAction(e -> {
            password = passwordField.getText().trim();
            if (password.isEmpty()) {
                password = "0807"; // 默认密码
            }
            cancelled = false;
            dialogStage.close();
        });

        Button cancelButton = new Button("取消");
        cancelButton.setPrefWidth(80);
        cancelButton.setCancelButton(true);
        cancelButton.setOnAction(e -> {
            cancelled = true;
            dialogStage.close();
        });

        buttonBox.getChildren().addAll(okButton, cancelButton);

        layout.getChildren().addAll(infoLabel, fileLabel, passwordField, hintLabel, buttonBox);

        Scene scene = new Scene(layout);
        dialogStage.setScene(scene);

        // 设置焦点到密码输入框
        Platform.runLater(passwordField::requestFocus);

        // 显示对话框并等待
        dialogStage.showAndWait();

        return cancelled ? null : password;
    }

    /**
     * 显示简单的确认对话框（使用默认密码0807）
     */
    public static boolean showPasswordConfirmDialog(Window owner, String fileName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("文件密码确认");
        alert.setHeaderText("检测到加密的Excel文件");
        alert.setContentText("文件：" + fileName + "\n\n是否使用默认密码 0807 打开文件？");

        ButtonType yesButton = new ButtonType("使用密码 0807");
        ButtonType noButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
    }
}
