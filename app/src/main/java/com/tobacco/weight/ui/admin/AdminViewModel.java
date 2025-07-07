package com.tobacco.weight.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.ui.weighing.WeighingViewModel;

import java.util.Random;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 管理员界面ViewModel
 * 管理预检数据和烟叶分类统计信息
 */
@HiltViewModel
public class AdminViewModel extends ViewModel implements WeighingViewModel.DataSyncCallback {

    private final MutableLiveData<Integer> totalPrecheckCount = new MutableLiveData<>(50);
    private final MutableLiveData<Integer> currentPrecheckCount = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalPrecheckWeight = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> averageWeight = new MutableLiveData<>(0.0);

    // 各等级烟叶数据
    private final MutableLiveData<GradeData> gradeAData = new MutableLiveData<>();
    private final MutableLiveData<GradeData> gradeBData = new MutableLiveData<>();
    private final MutableLiveData<GradeData> gradeCData = new MutableLiveData<>();
    private final MutableLiveData<GradeData> gradeDData = new MutableLiveData<>();

    private final Random random = new Random();

    @Inject
    public AdminViewModel() {
        initializeData();
        // 设置数据同步回调
        WeighingViewModel.setDataSyncCallback(this);
    }

    /**
     * 烟叶等级数据模型
     */
    public static class GradeData {
        private double precheckWeight;
        private double actualWeight;

        public GradeData(double precheckWeight, double actualWeight) {
            this.precheckWeight = precheckWeight;
            this.actualWeight = actualWeight;
        }

        public double getPrecheckWeight() {
            return precheckWeight;
        }

        public void setPrecheckWeight(double precheckWeight) {
            this.precheckWeight = precheckWeight;
        }

        public double getActualWeight() {
            return actualWeight;
        }

        public void setActualWeight(double actualWeight) {
            this.actualWeight = actualWeight;
        }
    }

    private void initializeData() {
        // 初始化各等级数据
        gradeAData.setValue(new GradeData(25.00, 23.60));
        gradeBData.setValue(new GradeData(20.00, 18.45));
        gradeCData.setValue(new GradeData(15.00, 14.75));
        gradeDData.setValue(new GradeData(10.00, 9.50));
    }

    /**
     * 更新预检数据
     */
    public void updatePrecheckData(int count, double weight) {
        currentPrecheckCount.setValue(count);
        totalPrecheckWeight.setValue(weight);

        // 计算平均重量
        if (count > 0) {
            averageWeight.setValue(weight / count);
        } else {
            averageWeight.setValue(0.0);
        }

        // 随机更新各等级数据（模拟实际称重影响）
        updateGradeData();
    }

    /**
     * 更新各等级数据
     */
    private void updateGradeData() {
        // 模拟实际称重对各等级数据的影响
        GradeData currentA = gradeAData.getValue();
        GradeData currentB = gradeBData.getValue();
        GradeData currentC = gradeCData.getValue();
        GradeData currentD = gradeDData.getValue();

        if (currentA != null) {
            double newWeightA = currentA.getActualWeight() + (random.nextDouble() - 0.5) * 2.0;
            gradeAData.setValue(new GradeData(currentA.getPrecheckWeight(), Math.max(0, newWeightA)));
        }

        if (currentB != null) {
            double newWeightB = currentB.getActualWeight() + (random.nextDouble() - 0.5) * 1.5;
            gradeBData.setValue(new GradeData(currentB.getPrecheckWeight(), Math.max(0, newWeightB)));
        }

        if (currentC != null) {
            double newWeightC = currentC.getActualWeight() + (random.nextDouble() - 0.5) * 1.0;
            gradeCData.setValue(new GradeData(currentC.getPrecheckWeight(), Math.max(0, newWeightC)));
        }

        if (currentD != null) {
            double newWeightD = currentD.getActualWeight() + (random.nextDouble() - 0.5) * 0.5;
            gradeDData.setValue(new GradeData(currentD.getPrecheckWeight(), Math.max(0, newWeightD)));
        }
    }

    /**
     * 刷新数据
     */
    public void refreshData() {
        // 模拟数据刷新
        updateGradeData();
    }

    /**
     * 导出数据
     */
    public void exportData() {
        // 模拟数据导出
        // 实际项目中可以实现Excel导出或其他格式
    }

    /**
     * 重置数据
     */
    public void resetData() {
        currentPrecheckCount.setValue(0);
        totalPrecheckWeight.setValue(0.0);
        averageWeight.setValue(0.0);
        initializeData();
    }

    // Getters
    public LiveData<Integer> getTotalPrecheckCount() {
        return totalPrecheckCount;
    }

    public LiveData<Integer> getCurrentPrecheckCount() {
        return currentPrecheckCount;
    }

    public LiveData<Double> getTotalPrecheckWeight() {
        return totalPrecheckWeight;
    }

    public LiveData<Double> getAverageWeight() {
        return averageWeight;
    }

    public LiveData<GradeData> getGradeAData() {
        return gradeAData;
    }

    public LiveData<GradeData> getGradeBData() {
        return gradeBData;
    }

    public LiveData<GradeData> getGradeCData() {
        return gradeCData;
    }

    public LiveData<GradeData> getGradeDData() {
        return gradeDData;
    }

    /**
     * 实现数据同步回调接口
     */
    @Override
    public void onDataUpdated(int count, double weight) {
        // 更新预检数据
        currentPrecheckCount.postValue(count);
        totalPrecheckWeight.postValue(weight);

        // 计算平均重量
        if (count > 0) {
            averageWeight.postValue(weight / count);
        } else {
            averageWeight.postValue(0.0);
        }

        // 更新各等级数据
        updateGradeData();
    }
}