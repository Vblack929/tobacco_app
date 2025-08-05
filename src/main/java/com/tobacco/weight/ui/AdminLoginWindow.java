package com.tobacco.weight.ui;

import com.tobacco.weight.data.AdminAccount;
import com.tobacco.weight.service.AdminAuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 管理员登录窗口
 */
public class AdminLoginWindow extends Stage {

    private static final Logger logger = LoggerFactory.getLogger(AdminLoginWindow.class);
    
    private AdminAuthService authService;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button cancelButton;
    private Label statusLabel;
    private Label defaultAccountsLabel;
    
    private boolean loginSuccessful = false;

    public AdminLoginWindow() {
        this.authService = AdminAuthService.getInstance();
        initializeWindow();
    }

    private void initializeWindow() {
        setTitle("管理员登录");
        setWidth(400);
        setHeight(350);
        setResizable(false);
        initModality(Modality.APPLICATION_MODAL);

        // 创建主布局
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");

        // 标题
        Label titleLabel = new Label("管理员系统登录");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // 登录表单
        VBox formLayout = createLoginForm();

        // 默认账户提示
        VBox accountInfoLayout = createAccountInfoLayout();

        // 按钮布局
        HBox buttonLayout = createButtonLayout();

        // 状态标签
        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        mainLayout.getChildren().addAll(titleLabel, formLayout, accountInfoLayout, buttonLayout, statusLabel);

        Scene scene = new Scene(mainLayout);
        setScene(scene);

        // 设置焦点
        usernameField.requestFocus();
    }

    private VBox createLoginForm() {
        VBox formLayout = new VBox(15);
        formLayout.setAlignment(Pos.CENTER);

        // 用户名输入
        VBox usernameBox = new VBox(5);
        Label usernameLabel = new Label("用户名:");
        usernameLabel.setStyle("-fx-font-weight: bold;");
        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setPrefWidth(250);
        usernameField.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // 密码输入
        VBox passwordBox = new VBox(5);
        Label passwordLabel = new Label("密码:");
        passwordLabel.setStyle("-fx-font-weight: bold;");
        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefWidth(250);
        passwordField.setStyle("-fx-font-size: 14px; -fx-padding: 8px;");
        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        // 回车键登录
        passwordField.setOnAction(e -> handleLogin());

        formLayout.getChildren().addAll(usernameBox, passwordBox);
        return formLayout;
    }

    private VBox createAccountInfoLayout() {
        VBox infoLayout = new VBox(8);
        infoLayout.setAlignment(Pos.CENTER);
        infoLayout.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10px; -fx-background-radius: 5px;");

        Label infoTitle = new Label("默认账户信息:");
        infoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #34495e;");

        defaultAccountsLabel = new Label();
        defaultAccountsLabel.setText(
            "管理员: admin / admin123\n" +
            "超级管理员: superadmin / super123"
        );
        defaultAccountsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-text-alignment: center;");

        infoLayout.getChildren().addAll(infoTitle, defaultAccountsLabel);
        return infoLayout;
    }

    private HBox createButtonLayout() {
        HBox buttonLayout = new HBox(15);
        buttonLayout.setAlignment(Pos.CENTER);

        loginButton = new Button("登录");
        loginButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 20px; -fx-background-radius: 3px;");
        loginButton.setOnAction(e -> handleLogin());

        cancelButton = new Button("取消");
        cancelButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 20px; -fx-background-radius: 3px;");
        cancelButton.setOnAction(e -> handleCancel());

        buttonLayout.getChildren().addAll(loginButton, cancelButton);
        return buttonLayout;
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 清除之前的状态信息
        statusLabel.setText("");

        // 禁用按钮防止重复点击
        loginButton.setDisable(true);
        cancelButton.setDisable(true);

        try {
            AdminAuthService.LoginResult result = authService.login(username, password);

            if (result.isSuccess()) {
                loginSuccessful = true;
                statusLabel.setText("登录成功！");
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12px;");

                // 延迟一下让用户看到成功消息
                javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        Thread.sleep(500);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        close();
                        openAdminInterface(result.getAccount());
                    }
                };

                new Thread(task).start();

            } else {
                statusLabel.setText(result.getMessage());
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
                passwordField.clear();
                usernameField.selectAll();
                usernameField.requestFocus();
            }

        } catch (Exception e) {
            logger.error("登录过程中发生异常", e);
            statusLabel.setText("登录失败：系统错误");
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        } finally {
            // 重新启用按钮
            loginButton.setDisable(false);
            cancelButton.setDisable(false);
        }
    }

    private void handleCancel() {
        loginSuccessful = false;
        close();
    }

    private void openAdminInterface(AdminAccount account) {
        try {
            // 根据管理员级别打开相应的界面
            if (account.isSuperAdmin()) {
                logger.info("打开超级管理员界面");
                // 这里可以打开更高级的管理界面
                openAdminWindow();
            } else {
                logger.info("打开普通管理员界面");
                openAdminWindow();
            }
        } catch (Exception e) {
            logger.error("打开管理员界面失败", e);
            showAlert("错误", "无法打开管理员界面：" + e.getMessage());
        }
    }

    private void openAdminWindow() {
        // 打开一级管理员窗口 (AdminWindow)
        try {
            // 从数据库获取称重记录
            com.tobacco.weight.database.DatabaseManager databaseManager = com.tobacco.weight.database.DatabaseManager.getInstance();
            com.tobacco.weight.database.WeighingRecordRepository repository = new com.tobacco.weight.database.WeighingRecordRepository(databaseManager);
            
            repository.findAll(new com.tobacco.weight.database.WeighingRecordRepository.OnResultListener<java.util.List<com.tobacco.weight.data.WeighingRecord>>() {
                @Override
                public void onSuccess(java.util.List<com.tobacco.weight.data.WeighingRecord> records) {
                    javafx.application.Platform.runLater(() -> {
                        AdminWindow adminWindow = new AdminWindow(records);
                        adminWindow.show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    javafx.application.Platform.runLater(() -> {
                        logger.error("加载称重记录失败", e);
                        showAlert("错误", "无法加载称重记录：" + e.getMessage());
                    });
                }
            });
        } catch (Exception e) {
            logger.error("打开管理员窗口失败", e);
            showAlert("错误", "无法打开管理员窗口：" + e.getMessage());
        }
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}