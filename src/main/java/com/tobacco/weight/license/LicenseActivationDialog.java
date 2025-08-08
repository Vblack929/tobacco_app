package com.tobacco.weight.license;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * 简单的激活码输入对话框（本地校验）
 */
class LicenseActivationDialog {

    private final Stage dialog;
    private final TextField codeField;
    private String resultCode;

    LicenseActivationDialog(Window owner) {
        dialog = new Stage();
        dialog.setTitle("软件激活");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        Label title = new Label("请输入激活码以使用本软件");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        codeField = new PasswordField();
        codeField.setPromptText("输入统一分发的激活码");
        codeField.setPrefWidth(360);

        HBox buttons = new HBox(12);
        Button ok = new Button("激活");
        Button cancel = new Button("取消");
        buttons.getChildren().addAll(ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            resultCode = codeField.getText();
            dialog.close();
        });

        cancel.setCancelButton(true);
        cancel.setOnAction(e -> {
            resultCode = null;
            dialog.close();
        });

        VBox root = new VBox(14, title, codeField, buttons);
        root.setPadding(new Insets(18));
        root.setPrefWidth(420);

        dialog.setScene(new Scene(root));
    }

    /**
     * 显示并返回用户输入的激活码；若取消返回 null
     */
    String showAndWaitForCode() {
        resultCode = null;
        dialog.showAndWait();
        return resultCode;
    }
}
