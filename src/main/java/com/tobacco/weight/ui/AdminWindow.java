package com.tobacco.weight.ui;

import com.tobacco.weight.data.WeighingRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 一级管理员窗口：显示所有农户统计信息
 */
public class AdminWindow extends Stage {
    private TableView<FarmerStats> table;
    private ObservableList<FarmerStats> statsList;
    private List<WeighingRecord> allRecords;

    public AdminWindow(List<WeighingRecord> records) {
        this.allRecords = records;
        setTitle("管理员界面（一级）");
        setWidth(900);
        setHeight(600);
        initModality(Modality.APPLICATION_MODAL);

        BorderPane root = new BorderPane();
        table = new TableView<>();
        statsList = FXCollections.observableArrayList();
        table.setItems(statsList);

        TableColumn<FarmerStats, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data -> data.getValue().farmerNameProperty());
        TableColumn<FarmerStats, String> idCol = new TableColumn<>("身份证号");
        idCol.setCellValueFactory(data -> data.getValue().idCardProperty());
        TableColumn<FarmerStats, Integer> countCol = new TableColumn<>("称重次数");
        countCol.setCellValueFactory(data -> data.getValue().countProperty().asObject());
        TableColumn<FarmerStats, Double> weightCol = new TableColumn<>("总重量(kg)");
        weightCol.setCellValueFactory(data -> data.getValue().totalWeightProperty().asObject());
        TableColumn<FarmerStats, Void> actionCol = new TableColumn<>("操作");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("查看");
            {
                btn.setOnAction(e -> showDetail(statsList.get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(nameCol, idCol, countCol, weightCol, actionCol);

        Button refreshBtn = new Button("刷新数据");
        refreshBtn.setOnAction(e -> refreshStats());
        HBox topBar = new HBox(10, refreshBtn);
        topBar.setPadding(new Insets(10));

        root.setTop(topBar);
        root.setCenter(table);
        setScene(new Scene(root));
        refreshStats();
    }

    private void refreshStats() {
        Map<String, List<WeighingRecord>> grouped = allRecords.stream()
                .collect(Collectors.groupingBy(WeighingRecord::getFarmerName));
        List<FarmerStats> stats = new ArrayList<>();
        for (Map.Entry<String, List<WeighingRecord>> entry : grouped.entrySet()) {
            String name = entry.getKey();
            List<WeighingRecord> list = entry.getValue();
            String id = list.get(0).getPrecheckId(); // 可替换为身份证号字段
            int count = list.size();
            double totalWeight = list.stream().mapToDouble(WeighingRecord::getWeight).sum();
            stats.add(new FarmerStats(name, id, count, totalWeight, list));
        }
        statsList.setAll(stats);
    }

    private void showDetail(FarmerStats stats) {
        FarmerDetailDialog dialog = new FarmerDetailDialog(stats);
        dialog.showAndWait();
    }
}