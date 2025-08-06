package com.tobacco.weight.ui;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.database.DatabaseManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 农户管理对话框
 * 用于导入和管理农户信息
 */
public class FarmerManagementDialog {

    private static final Logger logger = LoggerFactory.getLogger(FarmerManagementDialog.class);

    private final DatabaseManager databaseManager;
    private final Stage dialog;
    private final TableView<FarmerDisplayInfo> farmerTable;
    private final ObservableList<FarmerDisplayInfo> farmerList;
    private TextField searchField;
    private Label statusLabel;
    private ProgressBar progressBar;

    /**
     * 用于表格显示的农户信息
     */
    public static class FarmerDisplayInfo {
        private final SimpleStringProperty farmerName;
        private final SimpleStringProperty idCardNumber;
        private final SimpleStringProperty contractNumber;
        private final SimpleStringProperty address;
        private final SimpleStringProperty status;

        public FarmerDisplayInfo(String farmerName, String idCardNumber, String contractNumber, String address,
                String status) {
            this.farmerName = new SimpleStringProperty(farmerName);
            this.idCardNumber = new SimpleStringProperty(idCardNumber);
            this.contractNumber = new SimpleStringProperty(contractNumber);
            this.address = new SimpleStringProperty(address);
            this.status = new SimpleStringProperty(status);
        }

        // Properties for TableView binding
        public SimpleStringProperty farmerNameProperty() {
            return farmerName;
        }

        public SimpleStringProperty idCardNumberProperty() {
            return idCardNumber;
        }

        public SimpleStringProperty contractNumberProperty() {
            return contractNumber;
        }

        public SimpleStringProperty addressProperty() {
            return address;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        // Getters
        public String getFarmerName() {
            return farmerName.get();
        }

        public String getIdCardNumber() {
            return idCardNumber.get();
        }

        public String getContractNumber() {
            return contractNumber.get();
        }

        public String getAddress() {
            return address.get();
        }

        public String getStatus() {
            return status.get();
        }

        /**
         * 获取脱敏的身份证号
         */
        public String getMaskedIdCardNumber() {
            String idCard = getIdCardNumber();
            if (idCard == null || idCard.length() < 8) {
                return "****";
            }
            return idCard.substring(0, 4) + "****" + idCard.substring(idCard.length() - 4);
        }
    }

    public FarmerManagementDialog(Window owner, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        this.farmerList = FXCollections.observableArrayList();

        // 创建对话框
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("农户注册管理");
        dialog.setWidth(1000);
        dialog.setHeight(700);

        // 创建主布局
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(15));

        // 创建顶部工具栏
        HBox toolbar = createToolbar();

        // 创建搜索栏
        HBox searchBar = createSearchBar();

        // 创建表格
        farmerTable = createFarmerTable();

        // 创建状态栏
        HBox statusBar = createStatusBar();

        // 组装布局
        mainLayout.getChildren().addAll(toolbar, searchBar, farmerTable, statusBar);
        VBox.setVgrow(farmerTable, Priority.ALWAYS);

        // 设置场景
        Scene scene = new Scene(mainLayout);
        dialog.setScene(scene);

        // 初始化数据
        loadFarmers();
    }

    /**
     * 创建工具栏
     */
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(5));

        Button refreshButton = new Button("刷新数据");
        refreshButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 15;");
        refreshButton.setOnAction(e -> loadFarmers());

        Button addButton = new Button("新增农户");
        addButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 15;");
        addButton.setOnAction(e -> addNewFarmer());

        // 进度条
        progressBar = new ProgressBar();
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);

        toolbar.getChildren().addAll(refreshButton, addButton, new Label("   "), progressBar);

        return toolbar;
    }

    /**
     * 创建搜索栏
     */
    private HBox createSearchBar() {
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(5));

        Label searchLabel = new Label("搜索条件:");
        searchLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        searchField = new TextField();
        searchField.setPromptText("输入农户姓名或合同号进行搜索...");
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-font-size: 14px;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterFarmers(newVal));

        Button clearButton = new Button("清除");
        clearButton.setStyle("-fx-font-size: 14px; -fx-padding: 5 10;");
        clearButton.setOnAction(e -> searchField.clear());

        searchBar.getChildren().addAll(searchLabel, searchField, clearButton);

        return searchBar;
    }

    /**
     * 创建农户表格
     */
    private TableView<FarmerDisplayInfo> createFarmerTable() {
        TableView<FarmerDisplayInfo> table = new TableView<>();
        table.setItems(farmerList);
        table.setStyle("-fx-font-size: 14px;");

        // 农户姓名列
        TableColumn<FarmerDisplayInfo, String> nameCol = new TableColumn<>("农户姓名");
        nameCol.setCellValueFactory(data -> data.getValue().farmerNameProperty());
        nameCol.setPrefWidth(120);

        // 身份证号列（脱敏显示）
        TableColumn<FarmerDisplayInfo, String> idCardCol = new TableColumn<>("身份证号");
        idCardCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMaskedIdCardNumber()));
        idCardCol.setPrefWidth(150);

        // 合同号列
        TableColumn<FarmerDisplayInfo, String> contractCol = new TableColumn<>("合同号");
        contractCol.setCellValueFactory(data -> data.getValue().contractNumberProperty());
        contractCol.setPrefWidth(200);

        // 地址列
        TableColumn<FarmerDisplayInfo, String> addressCol = new TableColumn<>("地址");
        addressCol.setCellValueFactory(data -> data.getValue().addressProperty());
        addressCol.setPrefWidth(200);

        // 状态列
        TableColumn<FarmerDisplayInfo, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());
        statusCol.setPrefWidth(80);

        // 操作列
        TableColumn<FarmerDisplayInfo, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(120);
        actionCol.setCellFactory(param -> new TableCell<FarmerDisplayInfo, Void>() {
            private final Button viewButton = new Button("查看详情");

            {
                viewButton.setStyle("-fx-font-size: 12px; -fx-padding: 3 8;");
                viewButton.setOnAction(e -> {
                    FarmerDisplayInfo farmer = getTableView().getItems().get(getIndex());
                    showFarmerDetails(farmer);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : viewButton);
            }
        });

        table.getColumns().addAll(nameCol, idCardCol, contractCol, addressCol, statusCol, actionCol);

        return table;
    }

    /**
     * 创建状态栏
     */
    private HBox createStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("就绪");
        statusLabel.setStyle("-fx-font-size: 12px;");

        statusBar.getChildren().add(statusLabel);

        return statusBar;
    }

    /**
     * 加载农户数据
     */
    private void loadFarmers() {
        Task<List<FarmerDisplayInfo>> loadTask = new Task<List<FarmerDisplayInfo>>() {
            @Override
            protected List<FarmerDisplayInfo> call() throws Exception {
                return loadFarmersFromDatabase();
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    farmerList.clear();
                    farmerList.addAll(getValue());
                    updateStatus("已加载 " + getValue().size() + " 条农户记录");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showError("加载失败", "加载农户数据时发生错误: " + getException().getMessage());
                });
            }
        };

        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    /**
     * 从数据库加载农户信息
     */
    private List<FarmerDisplayInfo> loadFarmersFromDatabase() throws SQLException {
        List<FarmerDisplayInfo> farmers = new ArrayList<>();

        String sql = """
                SELECT farmer_name, id_card_number, contract_number, address,
                       CASE WHEN id_card_number IS NOT NULL AND id_card_number != '' THEN '正常' ELSE '待完善' END as status
                FROM farmer_info
                ORDER BY created_at DESC
                """;

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                farmers.add(new FarmerDisplayInfo(
                        rs.getString("farmer_name"),
                        rs.getString("id_card_number"),
                        rs.getString("contract_number"),
                        rs.getString("address") != null ? rs.getString("address") : "",
                        rs.getString("status")));
            }
        }

        return farmers;
    }

    /**
     * 过滤农户列表
     */
    private void filterFarmers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            farmerTable.setItems(farmerList);
            return;
        }

        ObservableList<FarmerDisplayInfo> filteredList = farmerList
                .filtered(farmer -> farmer.getFarmerName().toLowerCase().contains(searchText.toLowerCase()) ||
                        farmer.getContractNumber().toLowerCase().contains(searchText.toLowerCase()));

        farmerTable.setItems(filteredList);
        updateStatus("搜索到 " + filteredList.size() + " 条记录");
    }

    /**
     * 新增农户
     */
    private void addNewFarmer() {
        // TODO: 实现新增农户对话框
        showInfo("功能开发中", "新增农户功能正在开发中，请使用Excel导入功能。");
    }

    /**
     * 显示农户详情
     */
    private void showFarmerDetails(FarmerDisplayInfo farmer) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("农户详情");
        alert.setHeaderText("农户信息详情");

        String details = String.format(
                "农户姓名: %s\n" +
                        "身份证号: %s\n" +
                        "合同号: %s\n" +
                        "地址: %s\n" +
                        "状态: %s",
                farmer.getFarmerName(),
                farmer.getIdCardNumber(),
                farmer.getContractNumber(),
                farmer.getAddress(),
                farmer.getStatus());

        alert.setContentText(details);
        alert.showAndWait();
    }

    /**
     * 更新状态
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
        logger.info("农户管理状态: {}", status);
    }

    /**
     * 显示错误信息
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示信息
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示对话框
     */
    public void show() {
        dialog.show();
    }

    /**
     * 显示对话框并等待
     */
    public void showAndWait() {
        dialog.showAndWait();
    }
}