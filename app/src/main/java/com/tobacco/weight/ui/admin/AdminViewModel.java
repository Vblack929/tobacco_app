package com.tobacco.weight.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Random;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 管理员界面ViewModel
 * 管理预检数据和烟叶分类统计信息
 */
@HiltViewModel
public class AdminViewModel extends ViewModel {

    private final MutableLiveData<Integer> totalPrecheckCount = new MutableLiveData<>(50);
    private final MutableLiveData<Integer> currentPrecheckCount = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalPrecheckWeight = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> averageWeight = new MutableLiveData<>(0.0);

    // 各等级烟叶数据
    private final MutableLiveData<GradeData> gradeAData = new MutableLiveData<>();
    private final MutableLiveData<GradeData> gradeBData = new MutableLiveData<>();
    private final MutableLiveData<GradeData> gradeCData = new MutableLiveData<>();
    private final MutableLiveData<GradeData> gradeDData = new MutableLiveData<>();

    // 各烟农数据
    private final MutableLiveData<FarmerData> farmerAData = new MutableLiveData<>();
    private final MutableLiveData<FarmerData> farmerBData = new MutableLiveData<>();
    private final MutableLiveData<FarmerData> farmerCData = new MutableLiveData<>();
    private final MutableLiveData<FarmerData> farmerDData = new MutableLiveData<>();

    private final Random random = new Random();

    @Inject
    public AdminViewModel() {
        initializeData();
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

    /**
     * 烟农数据模型
     */
    public static class FarmerData {
        private int bundleCount;
        private double totalWeight;

        public FarmerData(int bundleCount, double totalWeight) {
            this.bundleCount = bundleCount;
            this.totalWeight = totalWeight;
        }

        public int getBundleCount() {
            return bundleCount;
        }

        public void setBundleCount(int bundleCount) {
            this.bundleCount = bundleCount;
        }

        public double getTotalWeight() {
            return totalWeight;
        }

        public void setTotalWeight(double totalWeight) {
            this.totalWeight = totalWeight;
        }
    }

    private void initializeData() {
        // 初始化各等级数据
        gradeAData.setValue(new GradeData(25.00, 23.60));
        gradeBData.setValue(new GradeData(20.00, 18.45));
        gradeCData.setValue(new GradeData(15.00, 14.75));
        gradeDData.setValue(new GradeData(10.00, 9.50));

        // 初始化各烟农数据
        farmerAData.setValue(new FarmerData(50, 125.30)); // 张三
        farmerBData.setValue(new FarmerData(38, 96.75)); // 李四
        farmerCData.setValue(new FarmerData(42, 108.20)); // 王五
        farmerDData.setValue(new FarmerData(35, 87.90)); // 赵六
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
        // 更新烟农数据
        updateFarmerData();
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
     * 更新各烟农数据
     */
    private void updateFarmerData() {
        // 模拟实际称重对各烟农数据的影响
        FarmerData currentA = farmerAData.getValue();
        FarmerData currentB = farmerBData.getValue();
        FarmerData currentC = farmerCData.getValue();
        FarmerData currentD = farmerDData.getValue();

        if (currentA != null) {
            int newBundleCount = currentA.getBundleCount() + random.nextInt(3) - 1; // -1 到 1
            double newWeight = currentA.getTotalWeight() + (random.nextDouble() - 0.5) * 5.0;
            farmerAData.setValue(new FarmerData(Math.max(0, newBundleCount), Math.max(0, newWeight)));
        }

        if (currentB != null) {
            int newBundleCount = currentB.getBundleCount() + random.nextInt(3) - 1;
            double newWeight = currentB.getTotalWeight() + (random.nextDouble() - 0.5) * 4.0;
            farmerBData.setValue(new FarmerData(Math.max(0, newBundleCount), Math.max(0, newWeight)));
        }

        if (currentC != null) {
            int newBundleCount = currentC.getBundleCount() + random.nextInt(3) - 1;
            double newWeight = currentC.getTotalWeight() + (random.nextDouble() - 0.5) * 3.0;
            farmerCData.setValue(new FarmerData(Math.max(0, newBundleCount), Math.max(0, newWeight)));
        }

        if (currentD != null) {
            int newBundleCount = currentD.getBundleCount() + random.nextInt(3) - 1;
            double newWeight = currentD.getTotalWeight() + (random.nextDouble() - 0.5) * 3.0;
            farmerDData.setValue(new FarmerData(Math.max(0, newBundleCount), Math.max(0, newWeight)));
        }
    }

    /**
     * 刷新数据
     */
    public void refreshData() {
        // 模拟数据刷新
        updateGradeData();
        updateFarmerData();
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

    // 烟农数据的Getter方法
    public LiveData<FarmerData> getFarmerAData() {
        return farmerAData;
    }

    public LiveData<FarmerData> getFarmerBData() {
        return farmerBData;
    }

    public LiveData<FarmerData> getFarmerCData() {
        return farmerCData;
    }

    public LiveData<FarmerData> getFarmerDData() {
        return farmerDData;
    }

}