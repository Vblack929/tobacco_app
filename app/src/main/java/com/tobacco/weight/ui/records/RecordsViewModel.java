package com.tobacco.weight.ui.records;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.data.model.WeightRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 记录ViewModel
 * 管理称重记录数据
 */
@HiltViewModel
public class RecordsViewModel extends ViewModel {

    // 数据绑定属性
    private String searchQuery = "";
    private String startDate = "开始日期";
    private String endDate = "结束日期";
    private int totalRecords = 0;
    private double totalWeight = 0.0;
    private double totalAmount = 0.0;
    private double avgPrice = 0.0;
    private boolean isEmpty = true;
    private boolean isLoading = false;

    // LiveData 属性
    private final MutableLiveData<List<WeightRecord>> records = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<WeightRecord>> filteredRecords = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> exportStatus = new MutableLiveData<>("");

    // 原始数据
    private List<WeightRecord> allRecords = new ArrayList<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Inject
    public RecordsViewModel() {
        // 初始化，加载记录
        loadRecords();
    }

    // 数据绑定的 Getter 和 Setter 方法
    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        // 实时搜索
        applyFilter();
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(double totalWeight) {
        this.totalWeight = totalWeight;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(double avgPrice) {
        this.avgPrice = avgPrice;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    // LiveData Getter 方法
    public LiveData<List<WeightRecord>> getRecords() {
        return filteredRecords;
    }

    public LiveData<String> getExportStatus() {
        return exportStatus;
    }

    // 业务逻辑方法
    public void selectStartDate() {
        // TODO: 打开日期选择器
        // 这里可以发送事件到Fragment来处理UI操作
        String selectedDate = dateFormat.format(new Date());
        setStartDate(selectedDate);
        applyFilter();
    }

    public void selectEndDate() {
        // TODO: 打开日期选择器
        String selectedDate = dateFormat.format(new Date());
        setEndDate(selectedDate);
        applyFilter();
    }

    public void applyFilter() {
        List<WeightRecord> filtered = new ArrayList<>();

        for (WeightRecord record : allRecords) {
            // 应用搜索条件
            if (matchesSearchQuery(record)) {
                // TODO: 应用日期筛选
                filtered.add(record);
            }
        }

        filteredRecords.setValue(filtered);
        updateStatistics(filtered);
    }

    public void exportRecords() {
        setLoading(true);
        // TODO: 实现导出逻辑
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 模拟导出过程
                exportStatus.postValue("导出成功：已导出 " + totalRecords + " 条记录");
                setLoading(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exportStatus.postValue("导出失败：" + e.getMessage());
                setLoading(false);
            }
        }).start();
    }

    public void addNewRecord() {
        // TODO: 跳转到称重界面或打开新增记录对话框
    }

    public void refreshData() {
        setLoading(true);
        loadRecords();
    }

    private void loadRecords() {
        // TODO: 从数据库加载记录
        // 模拟数据加载
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 模拟加载时间

                List<WeightRecord> recordList = generateSampleRecords();
                allRecords = recordList;

                // 更新UI
                filteredRecords.postValue(recordList);
                updateStatistics(recordList);
                setLoading(false);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                setLoading(false);
            }
        }).start();
    }

    private boolean matchesSearchQuery(WeightRecord record) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return true;
        }

        String query = searchQuery.toLowerCase();
        return (record.getFarmerName() != null && record.getFarmerName().toLowerCase().contains(query)) ||
                (record.getRecordNumber() != null && record.getRecordNumber().toLowerCase().contains(query)) ||
                (record.getIdCardNumber() != null && record.getIdCardNumber().toLowerCase().contains(query));
    }

    private void updateStatistics(List<WeightRecord> recordList) {
        setTotalRecords(recordList.size());
        setEmpty(recordList.isEmpty());

        if (recordList.isEmpty()) {
            setTotalWeight(0.0);
            setTotalAmount(0.0);
            setAvgPrice(0.0);
            return;
        }

        double sumWeight = 0.0;
        double sumAmount = 0.0;

        for (WeightRecord record : recordList) {
            sumWeight += record.getWeight();
            sumAmount += record.getTotalAmount();
        }

        setTotalWeight(sumWeight);
        setTotalAmount(sumAmount);
        setAvgPrice(sumWeight > 0 ? sumAmount / sumWeight : 0.0);
    }

    private List<WeightRecord> generateSampleRecords() {
        List<WeightRecord> records = new ArrayList<>();

        // 生成一些示例数据
        for (int i = 1; i <= 5; i++) {
            WeightRecord record = new WeightRecord();
            record.setRecordNumber("WR202401" + String.format("%03d", i));
            record.setFarmerName("农户" + i);
            record.setIdCardNumber("11010119900101000" + i);
            record.setTobaccoPart(i % 2 == 0 ? "上二棚" : "下二棚");
            record.setWeight(50.0 + i * 10);
            record.setTotalAmount((50.0 + i * 10) * 18.5);
            record.setTimestamp(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000);
            record.setOperatorName("操作员");
            record.setWarehouseNumber("WH001");
            record.setStatus("已完成");
            records.add(record);
        }

        return records;
    }

    public void deleteRecord(WeightRecord record) {
        allRecords.remove(record);
        applyFilter();
    }

    public void editRecord(WeightRecord record) {
        // TODO: 打开编辑记录对话框
    }
}