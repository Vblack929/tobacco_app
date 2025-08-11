package com.tobacco.weight.ui;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.data.LocationData;
import com.tobacco.weight.database.FarmerInfoRepository;
import com.tobacco.weight.database.DatabaseManager;
import com.tobacco.weight.service.FarmerImportService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * 农户注册管理窗口
 * 提供农户信息的查看、搜索和筛选功能
 */
public class FarmerRegistrationWindow {

    private static final Logger logger = LoggerFactory.getLogger(FarmerRegistrationWindow.class);

    private Stage stage;
    private FarmerInfoRepository farmerInfoRepository;

    // UI组件
    private TextField searchField;
    private ComboBox<String> townshipFilter;
    private ComboBox<String> villageFilter;
    private TableView<FarmerDisplayInfo> farmerTable;
    private ObservableList<FarmerDisplayInfo> farmerList;
    private Label statusLabel;
    private Button refreshButton;
    private Button addFarmerButton;
    private Button viewDetailsButton;

    // 数据
    private List<FarmerInfo> allFarmers;
    private List<String> allLocations;

    public FarmerRegistrationWindow() {
        this.farmerInfoRepository = new FarmerInfoRepository(DatabaseManager.getInstance());
        this.farmerList = FXCollections.observableArrayList();
        this.allFarmers = new ArrayList<>();
        this.allLocations = new ArrayList<>();
        initializeWindow();
        initializeLocationFilters();
        loadData();
    }

    /**
     * 初始化窗口
     */
    private void initializeWindow() {
        stage = new Stage();
        stage.setTitle("农户注册管理");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(1300);
        stage.setHeight(750);

        // 创建主布局
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // 顶部搜索区域
        VBox topSection = createTopSection();
        root.setTop(topSection);

        // 中间农户列表
        VBox centerSection = createCenterSection();
        root.setCenter(centerSection);

        // 底部状态栏
        HBox bottomSection = createBottomSection();
        root.setBottom(bottomSection);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        stage.setScene(scene);
    }

    /**
     * 创建顶部搜索区域
     */
    private VBox createTopSection() {
        VBox topSection = new VBox(10);

        // 搜索栏
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("搜索农户:");
        searchLabel.setMinWidth(80);

        searchField = new TextField();
        searchField.setPromptText("输入农户姓名、合同号或身份证号进行搜索...");
        searchField.setPrefWidth(450);
        searchField.textProperty().addListener((obs, oldText, newText) -> performSearch());

        Button clearSearchButton = new Button("清除");
        clearSearchButton.setOnAction(e -> {
            searchField.clear();
            performSearch();
        });

        searchBox.getChildren().addAll(searchLabel, searchField, clearSearchButton);

        // 筛选栏
        HBox filterBox = new HBox(15);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        Label filterLabel = new Label("地区筛选:");
        filterLabel.setMinWidth(80);

        // 乡镇选择
        Label townshipLabel = new Label("乡镇:");
        townshipFilter = new ComboBox<>();
        townshipFilter.setPromptText("选择乡镇");
        townshipFilter.setPrefWidth(180);
        townshipFilter.setMaxWidth(Double.MAX_VALUE);
        townshipFilter.getItems().add("全部");
        townshipFilter.setValue("全部");
        townshipFilter.setOnAction(e -> onTownshipChanged());

        // 村庄选择
        Label villageLabel = new Label("村庄:");
        villageFilter = new ComboBox<>();
        villageFilter.setPromptText("选择村庄");
        villageFilter.setPrefWidth(180);
        villageFilter.setMaxWidth(Double.MAX_VALUE);
        villageFilter.getItems().add("全部");
        villageFilter.setValue("全部");
        villageFilter.setOnAction(e -> performFilter());
        villageFilter.setDisable(true); // 初始禁用

        // 操作按钮
        refreshButton = new Button("刷新数据");
        refreshButton.setOnAction(e -> {
            refreshLocationData();
            loadData();
        });

        addFarmerButton = new Button("新增农户");
        addFarmerButton.setOnAction(e -> showAddFarmerDialog());

        Button refreshLocationButton = new Button("刷新地区");
        refreshLocationButton.setOnAction(e -> refreshLocationData());

        Button importExcelButton = new Button("导入Excel");
        importExcelButton.setOnAction(e -> showImportExcelDialog());

        filterBox.getChildren().addAll(filterLabel, townshipLabel, townshipFilter,
                villageLabel, villageFilter, refreshButton, refreshLocationButton, addFarmerButton, importExcelButton);

        topSection.getChildren().addAll(searchBox, filterBox);
        return topSection;
    }

    /**
     * 创建中间农户列表区域
     */
    private VBox createCenterSection() {
        VBox centerSection = new VBox(10);

        Label titleLabel = new Label("注册农户列表");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // 创建农户表格
        farmerTable = new TableView<>();
        farmerTable.setItems(farmerList);
        farmerTable.setRowFactory(tv -> {
            TableRow<FarmerDisplayInfo> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showFarmerDetails(row.getItem());
                }
            });
            return row;
        });

        // 创建表格列
        createTableColumns();

        // 详情按钮
        viewDetailsButton = new Button("查看详情");
        viewDetailsButton.setOnAction(e -> {
            FarmerDisplayInfo selected = farmerTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showFarmerDetails(selected);
            } else {
                showAlert("请选择一个农户");
            }
        });

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.getChildren().add(viewDetailsButton);

        centerSection.getChildren().addAll(titleLabel, farmerTable, buttonBox);
        VBox.setVgrow(farmerTable, Priority.ALWAYS);

        return centerSection;
    }

    /**
     * 创建表格列
     */
    private void createTableColumns() {
        // 农户姓名列
        TableColumn<FarmerDisplayInfo, String> nameCol = new TableColumn<>("农户姓名");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("farmerName"));
        nameCol.setPrefWidth(150);

        // 身份证号列（脱敏）
        TableColumn<FarmerDisplayInfo, String> idCardCol = new TableColumn<>("身份证号");
        idCardCol.setCellValueFactory(new PropertyValueFactory<>("idCardMasked"));
        idCardCol.setPrefWidth(200);

        // 合同号列
        TableColumn<FarmerDisplayInfo, String> contractCol = new TableColumn<>("合同号");
        contractCol.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));
        contractCol.setPrefWidth(180);

        // 地址列
        TableColumn<FarmerDisplayInfo, String> addressCol = new TableColumn<>("地址");
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addressCol.setPrefWidth(250);

        // 合同量列（由称重记录累计重量近似展示）
        TableColumn<FarmerDisplayInfo, String> contractAmountCol = new TableColumn<>("合同量(kg)");
        contractAmountCol.setCellValueFactory(new PropertyValueFactory<>("contractAmount"));
        contractAmountCol.setPrefWidth(120);

        // 注册状态列
        TableColumn<FarmerDisplayInfo, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        farmerTable.getColumns().addAll(nameCol, idCardCol, contractCol, addressCol, contractAmountCol, statusCol);

        // 设置表格列宽策略为自动调整
        farmerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * 创建底部状态栏
     */
    private HBox createBottomSection() {
        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER_LEFT);
        bottomSection.setPadding(new Insets(10, 0, 0, 0));

        statusLabel = new Label("准备就绪");
        statusLabel.setStyle("-fx-text-fill: #666666;");

        bottomSection.getChildren().add(statusLabel);
        return bottomSection;
    }

    /**
     * 加载数据
     */
    private void loadData() {
        updateStatus("正在加载农户数据...");

        // 加载农户列表
        farmerInfoRepository.findAll(new FarmerInfoRepository.OnResultListener<List<FarmerInfo>>() {
            @Override
            public void onSuccess(List<FarmerInfo> farmers) {
                Platform.runLater(() -> {
                    allFarmers.clear();
                    allFarmers.addAll(farmers);
                    // 计算每个合同的累计重量作为合同量
                    java.util.Map<String, Double> amountMap;
                    try {
                        amountMap = new com.tobacco.weight.database.WeighingRecordRepository(
                                DatabaseManager.getInstance())
                                .getTotalWeightByContractSync();
                    } catch (Exception ex) {
                        amountMap = java.util.Collections.emptyMap();
                    }
                    updateFarmerTableWithAmount(farmers, amountMap);
                    updateStatus("已加载 " + farmers.size() + " 个农户");
                });
            }

            @Override
            public void onError(Exception e) {
                Platform.runLater(() -> {
                    logger.error("加载农户数据失败", e);
                    showAlert("加载农户数据失败: " + e.getMessage());
                    updateStatus("加载失败");
                });
            }
        });

        // 地区筛选器已在初始化时加载静态数据
    }

    /**
     * 更新农户表格
     */
    private void updateFarmerTable(List<FarmerInfo> farmers) {
        // 兼容旧调用，默认不带合同量
        updateFarmerTableWithAmount(farmers, java.util.Collections.emptyMap());
    }

    private void updateFarmerTableWithAmount(List<FarmerInfo> farmers, java.util.Map<String, Double> amountMap) {
        farmerList.clear();
        for (FarmerInfo farmer : farmers) {
            FarmerDisplayInfo info = new FarmerDisplayInfo(farmer);
            Double amt = amountMap != null ? amountMap.get(farmer.getContractNumber()) : null;
            if (amt != null) {
                info.setContractAmount(String.format("%.2f", amt));
            } else {
                info.setContractAmount("");
            }
            farmerList.add(info);
        }
    }

    /**
     * 初始化地区筛选器
     */
    private void initializeLocationFilters() {
        try {
            // 刷新地区数据缓存以确保最新数据
            LocationData.refreshCache();

            // 初始化乡镇筛选器
            townshipFilter.getItems().clear();
            townshipFilter.getItems().add("全部");
            townshipFilter.getItems().addAll(LocationData.getAllTownships());
            townshipFilter.setValue("全部");

            // 初始化村庄筛选器
            villageFilter.getItems().clear();
            villageFilter.getItems().add("全部");
            villageFilter.setValue("全部");
            villageFilter.setDisable(true);

            logger.info("地区筛选器初始化完成，乡镇数量: {}", LocationData.getAllTownships().size());
        } catch (Exception e) {
            logger.error("初始化地区筛选器失败", e);
            showAlert("初始化地区筛选器失败: " + e.getMessage());
        }
    }

    /**
     * 乡镇选择变化时的处理
     */
    private void onTownshipChanged() {
        String selectedTownship = townshipFilter.getValue();

        // 清空村庄选择
        villageFilter.getItems().clear();
        villageFilter.getItems().add("全部");
        villageFilter.setValue("全部");

        if (selectedTownship != null && !"全部".equals(selectedTownship)) {
            // 根据选择的乡镇加载对应的村庄
            List<String> villages = LocationData.getVillagesByTownship(selectedTownship);
            villageFilter.getItems().addAll(villages);
            villageFilter.setDisable(false);
        } else {
            villageFilter.setDisable(true);
        }

        // 触发筛选
        performFilter();
    }

    /**
     * 执行搜索
     */
    private void performSearch() {
        String searchTerm = searchField.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // 如果搜索为空，显示所有农户（考虑地区筛选）
            performFilter();
            return;
        }

        updateStatus("正在搜索...");

        farmerInfoRepository.searchByNameOrContract(searchTerm.trim(),
                new FarmerInfoRepository.OnResultListener<List<FarmerInfo>>() {
                    @Override
                    public void onSuccess(List<FarmerInfo> farmers) {
                        Platform.runLater(() -> {
                            // 如果有地区筛选，需要进一步筛选结果
                            List<FarmerInfo> filteredFarmers = applyLocationFilter(farmers);
                            updateFarmerTable(filteredFarmers);
                            updateStatus("搜索到 " + filteredFarmers.size() + " 个农户");
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Platform.runLater(() -> {
                            logger.error("搜索失败", e);
                            showAlert("搜索失败: " + e.getMessage());
                            updateStatus("搜索失败");
                        });
                    }
                });
    }

    /**
     * 执行地区筛选
     */
    private void performFilter() {
        String selectedTownship = townshipFilter.getValue();
        String selectedVillage = villageFilter.getValue();

        // 构建筛选条件
        List<FarmerInfo> filteredFarmers = new ArrayList<>(allFarmers);

        if (selectedTownship != null && !"全部".equals(selectedTownship)) {
            filteredFarmers = filteredFarmers.stream()
                    .filter(farmer -> {
                        if (farmer.getAddress() == null || farmer.getAddress().trim().isEmpty()) {
                            return false;
                        }
                        LocationData.LocationInfo locationInfo = LocationData.parseAddress(farmer.getAddress());
                        return selectedTownship.equals(locationInfo.getTownship());
                    })
                    .toList();
        }

        if (selectedVillage != null && !"全部".equals(selectedVillage)) {
            filteredFarmers = filteredFarmers.stream()
                    .filter(farmer -> {
                        if (farmer.getAddress() == null || farmer.getAddress().trim().isEmpty()) {
                            return false;
                        }
                        LocationData.LocationInfo locationInfo = LocationData.parseAddress(farmer.getAddress());
                        return selectedVillage.equals(locationInfo.getVillage());
                    })
                    .toList();
        }

        // 如果有搜索条件，进一步筛选
        String searchTerm = searchField.getText();
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            filteredFarmers = filteredFarmers.stream()
                    .filter(f -> f.getFarmerName().contains(searchTerm.trim()) ||
                            f.getContractNumber().contains(searchTerm.trim()))
                    .toList();
        }

        updateFarmerTable(filteredFarmers);

        // 更新状态信息
        StringBuilder statusText = new StringBuilder();
        if (selectedTownship != null && !"全部".equals(selectedTownship)) {
            statusText.append("乡镇: ").append(selectedTownship);
            if (selectedVillage != null && !"全部".equals(selectedVillage)) {
                statusText.append(", 村庄: ").append(selectedVillage);
            }
            statusText.append(" - ");
        }
        statusText.append("筛选到 ").append(filteredFarmers.size()).append(" 个农户");
        updateStatus(statusText.toString());
    }

    /**
     * 应用地区筛选到给定的农户列表
     */
    private List<FarmerInfo> applyLocationFilter(List<FarmerInfo> farmers) {
        String selectedTownship = townshipFilter.getValue();
        String selectedVillage = villageFilter.getValue();

        List<FarmerInfo> filteredFarmers = farmers;

        if (selectedTownship != null && !"全部".equals(selectedTownship)) {
            filteredFarmers = filteredFarmers.stream()
                    .filter(farmer -> {
                        if (farmer.getAddress() == null || farmer.getAddress().trim().isEmpty()) {
                            return false;
                        }
                        LocationData.LocationInfo locationInfo = LocationData.parseAddress(farmer.getAddress());
                        return selectedTownship.equals(locationInfo.getTownship());
                    })
                    .toList();
        }

        if (selectedVillage != null && !"全部".equals(selectedVillage)) {
            filteredFarmers = filteredFarmers.stream()
                    .filter(farmer -> {
                        if (farmer.getAddress() == null || farmer.getAddress().trim().isEmpty()) {
                            return false;
                        }
                        LocationData.LocationInfo locationInfo = LocationData.parseAddress(farmer.getAddress());
                        return selectedVillage.equals(locationInfo.getVillage());
                    })
                    .toList();
        }

        return filteredFarmers;
    }

    /**
     * 显示农户详情
     */
    private void showFarmerDetails(FarmerDisplayInfo displayInfo) {
        // 找到对应的完整FarmerInfo对象
        FarmerInfo farmerInfo = allFarmers.stream()
                .filter(f -> f.getFarmerName().equals(displayInfo.getFarmerName()) &&
                        f.getContractNumber().equals(displayInfo.getContractNumber()))
                .findFirst()
                .orElse(null);

        if (farmerInfo != null) {
            FarmerInfoDetailDialog dialog = new FarmerInfoDetailDialog(farmerInfo);
            dialog.show();
        }
    }

    /**
     * 显示新增农户对话框
     */
    private void showAddFarmerDialog() {
        // TODO: 实现新增农户对话框
        showAlert("新增农户功能待实现");
    }

    /**
     * 显示导入Excel对话框
     */
    private void showImportExcelDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Excel文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel文件", "*.xlsx", "*.xls"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }

        // 询问工作表名称
        TextInputDialog sheetDialog = new TextInputDialog("明细表");
        sheetDialog.setTitle("选择工作表");
        sheetDialog.setHeaderText("请输入要导入的工作表名称");
        sheetDialog.setContentText("工作表名称:");

        Optional<String> sheetResult = sheetDialog.showAndWait();
        if (!sheetResult.isPresent()) {
            return;
        }

        String sheetName = sheetResult.get().trim();
        showImportProgressDialog(selectedFile, sheetName);
    }

    /**
     * 显示导入进度对话框
     */
    private void showImportProgressDialog(File excelFile, String sheetName) {
        // 创建进度对话框
        Stage progressStage = new Stage();
        progressStage.setTitle("导入Excel");
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initOwner(stage);
        progressStage.setWidth(400);
        progressStage.setHeight(250);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("正在导入Excel文件");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label fileLabel = new Label("文件: " + excelFile.getName());

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);

        Label statusLabel = new Label("准备导入...");
        statusLabel.setStyle("-fx-text-fill: #666666;");

        Button cancelButton = new Button("取消");
        cancelButton.setOnAction(e -> progressStage.close());

        layout.getChildren().addAll(titleLabel, fileLabel, progressBar, statusLabel, cancelButton);

        Scene scene = new Scene(layout);
        progressStage.setScene(scene);
        progressStage.show();

        // 开始导入
        FarmerImportService importService = new FarmerImportService(DatabaseManager.getInstance());

        Thread importThread = new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("正在读取Excel文件...");
                    progressBar.setProgress(0.1);
                });

                FarmerImportService.ImportResult result = importService.importFromExcel(
                        excelFile, "明细表");

                Platform.runLater(() -> {
                    progressBar.setProgress(1.0);
                    progressStage.close();

                    // 显示导入结果
                    showImportResultDialog(result);

                    // 刷新数据
                    loadData();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressStage.close();
                    logger.error("导入Excel失败", e);
                    showAlert("导入失败: " + e.getMessage());
                });
            }
        });

        importThread.setDaemon(true);
        importThread.start();
    }

    /**
     * 显示导入结果对话框
     */
    private void showImportResultDialog(FarmerImportService.ImportResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("导入结果");
        alert.setHeaderText("Excel导入完成");

        StringBuilder content = new StringBuilder();
        content.append("总行数: ").append(result.getTotalRows()).append("\n");
        content.append("成功导入: ").append(result.getSuccessCount()).append("\n");
        content.append("失败: ").append(result.getFailureCount()).append("\n");
        content.append("重复: ").append(result.getDuplicateCount()).append("\n");

        if (!result.getErrorMessages().isEmpty()) {
            content.append("\n错误信息:\n");
            for (String error : result.getErrorMessages()) {
                content.append("- ").append(error).append("\n");
            }
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * 刷新地区数据
     */
    private void refreshLocationData() {
        try {
            updateStatus("正在刷新地区数据...");

            // 刷新LocationData缓存
            LocationData.refreshCache();

            // 重新初始化地区筛选器
            String currentTownship = townshipFilter.getValue();
            String currentVillage = villageFilter.getValue();

            // 重新加载乡镇数据
            townshipFilter.getItems().clear();
            townshipFilter.getItems().add("全部");
            townshipFilter.getItems().addAll(LocationData.getAllTownships());

            // 恢复之前的选择（如果还存在）
            if (currentTownship != null && townshipFilter.getItems().contains(currentTownship)) {
                townshipFilter.setValue(currentTownship);

                // 重新加载村庄数据
                onTownshipChanged();

                // 恢复村庄选择
                if (currentVillage != null && villageFilter.getItems().contains(currentVillage)) {
                    villageFilter.setValue(currentVillage);
                }
            } else {
                townshipFilter.setValue("全部");
                villageFilter.getItems().clear();
                villageFilter.getItems().add("全部");
                villageFilter.setValue("全部");
                villageFilter.setDisable(true);
            }

            updateStatus("地区数据刷新完成，当前乡镇数量: " + LocationData.getAllTownships().size());
            logger.info("地区数据刷新完成");

        } catch (Exception e) {
            logger.error("刷新地区数据失败", e);
            updateStatus("刷新地区数据失败: " + e.getMessage());
            showAlert("刷新地区数据失败: " + e.getMessage());
        }
    }

    /**
     * 更新状态信息
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * 显示提示信息
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示窗口
     */
    public void show() {
        stage.show();
    }

    /**
     * 关闭窗口
     */
    public void close() {
        if (farmerInfoRepository != null) {
            farmerInfoRepository.shutdown();
        }
        stage.close();
    }

    /**
     * 农户显示信息类（用于TableView）
     */
    public static class FarmerDisplayInfo {
        private final SimpleStringProperty farmerName;
        private final SimpleStringProperty idCardMasked;
        private final SimpleStringProperty contractNumber;
        private final SimpleStringProperty address;
        private final SimpleStringProperty gender;
        private final SimpleStringProperty contractAmount; // 新增：合同量（累计重量）
        private final SimpleStringProperty status;

        public FarmerDisplayInfo(FarmerInfo farmerInfo) {
            this.farmerName = new SimpleStringProperty(farmerInfo.getFarmerName());
            this.idCardMasked = new SimpleStringProperty(farmerInfo.getMaskedIdCardNumber());
            this.contractNumber = new SimpleStringProperty(farmerInfo.getContractNumber());
            this.address = new SimpleStringProperty(farmerInfo.getAddress());
            this.gender = new SimpleStringProperty(farmerInfo.getGender());
            // 合同量默认空，加载数据后填充
            this.contractAmount = new SimpleStringProperty("");
            this.status = new SimpleStringProperty("正常"); // 默认状态
        }

        // Getters for TableView
        public String getFarmerName() {
            return farmerName.get();
        }

        public String getIdCardMasked() {
            return idCardMasked.get();
        }

        public String getContractNumber() {
            return contractNumber.get();
        }

        public String getAddress() {
            return address.get();
        }

        public String getGender() {
            return gender.get();
        }

        public String getContractAmount() {
            return contractAmount.get();
        }

        public void setContractAmount(String value) {
            this.contractAmount.set(value);
        }

        public String getStatus() {
            return status.get();
        }

        // Property getters for TableView
        public SimpleStringProperty farmerNameProperty() {
            return farmerName;
        }

        public SimpleStringProperty idCardMaskedProperty() {
            return idCardMasked;
        }

        public SimpleStringProperty contractNumberProperty() {
            return contractNumber;
        }

        public SimpleStringProperty addressProperty() {
            return address;
        }

        public SimpleStringProperty genderProperty() {
            return gender;
        }

        public SimpleStringProperty contractAmountProperty() {
            return contractAmount;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }
    }
}