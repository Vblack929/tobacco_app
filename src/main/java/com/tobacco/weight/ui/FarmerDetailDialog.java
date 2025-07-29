package com.tobacco.weight.ui;

import com.tobacco.weight.data.WeighingRecord;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
        TableColumn<WeighingRecord, Double> weightCol = new TableColumn<>("重量(kg)");
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        TableColumn<WeighingRecord, String> timeCol = new TableColumn<>("时间");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        table.getColumns().addAll(precheckCol, leafCol, weightCol, timeCol);
        table.getItems().addAll(stats.getRecords());
        root.setCenter(table);

        setScene(new Scene(root));
    }
}