package com.tobacco.weight;

import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.license.HybridLicenseService;
import com.tobacco.weight.ui.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 烟叶称重系统主应用程序
 * JavaFX应用程序入口点
 */
public class TobaccoWeightApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(TobaccoWeightApp.class);
    private MainController mainController;

    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("启动烟叶称重系统...");

            // 初始化数据库
            DatabaseManager.getInstance();
            logger.info("数据库初始化完成");

            // 混合许可证验证（在线+离线）
            if (!HybridLicenseService.getInstance().ensureLicensed(primaryStage)) {
                logger.warn("许可证验证失败，应用程序将退出");
                Platform.exit();
                return;
            }

            // 加载主界面FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            // 获取控制器
            mainController = loader.getController();
            if (mainController != null) {
                mainController.setPrimaryStage(primaryStage);
            }

            // 设置场景
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            // 设置主窗口
            primaryStage.setTitle("烟叶称重系统 - Windows版");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.setMaximized(true);

            // 显示窗口
            primaryStage.show();

            logger.info("应用程序启动成功");

        } catch (IOException e) {
            logger.error("加载主界面失败", e);
            showErrorDialog("启动失败", "无法加载主界面: " + e.getMessage());
        } catch (Exception e) {
            logger.error("应用程序启动失败", e);
            showErrorDialog("启动失败", "应用程序启动失败: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        try {
            logger.info("正在关闭应用程序...");

            // 清理主控制器资源
            if (mainController != null) {
                mainController.cleanup();
            }

            // 关闭数据库连接
            DatabaseManager.getInstance().closeConnection();

            logger.info("应用程序已关闭");

            // 强制关闭JavaFX平台和JVM，确保所有线程都被终止
            Platform.exit();
            System.exit(0);

        } catch (Exception e) {
            logger.error("关闭应用程序时发生错误", e);
            // 即使出现错误也要强制退出
            Platform.exit();
            System.exit(1);
        }
    }

    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String title, String message) {
        try {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            logger.error("显示错误对话框失败", e);
        }
    }

    /**
     * 应用程序入口点
     */
    public static void main(String[] args) {
        logger.info("启动烟叶称重系统...");
        launch(args);
    }
}