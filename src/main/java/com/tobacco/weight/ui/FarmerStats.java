package com.tobacco.weight.ui;

import com.tobacco.weight.data.WeighingRecord;
import javafx.beans.property.*;
import java.util.List;

public class FarmerStats {
    private final StringProperty farmerName = new SimpleStringProperty();
    private final StringProperty idCard = new SimpleStringProperty();
    private final IntegerProperty count = new SimpleIntegerProperty();
    private final DoubleProperty totalWeight = new SimpleDoubleProperty();
    private final List<WeighingRecord> records;

    public FarmerStats(String farmerName, String idCard, int count, double totalWeight, List<WeighingRecord> records) {
        this.farmerName.set(farmerName);
        this.idCard.set(idCard);
        this.count.set(count);
        this.totalWeight.set(totalWeight);
        this.records = records;
    }

    public StringProperty farmerNameProperty() {
        return farmerName;
    }

    public StringProperty idCardProperty() {
        return idCard;
    }

    public IntegerProperty countProperty() {
        return count;
    }

    public DoubleProperty totalWeightProperty() {
        return totalWeight;
    }

    public List<WeighingRecord> getRecords() {
        return records;
    }
}