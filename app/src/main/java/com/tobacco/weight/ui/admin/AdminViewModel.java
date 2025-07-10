package com.tobacco.weight.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tobacco.weight.data.entity.FarmerInfoEntity;
import com.tobacco.weight.data.repository.FarmerInfoRepository;
import com.tobacco.weight.data.repository.WeightRecordRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 管理员界面ViewModel
 * 管理农户统计数据和系统统计信息，集成真实数据库数据
 */
@HiltViewModel
public class AdminViewModel extends ViewModel {

    private final FarmerInfoRepository farmerInfoRepository;
    private final WeightRecordRepository weightRecordRepository;

    // 系统统计数据
    private final MutableLiveData<Integer> totalFarmerCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> totalLeafCount = new MutableLiveData<>(0);
    private final MutableLiveData<Double> totalWeight = new MutableLiveData<>(0.0);

    // 农户统计列表
    private final MutableLiveData<List<FarmerStatistics>> farmerStatisticsList = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public AdminViewModel(
            FarmerInfoRepository farmerInfoRepository,
            WeightRecordRepository weightRecordRepository) {
        this.farmerInfoRepository = farmerInfoRepository;
        this.weightRecordRepository = weightRecordRepository;
        
        // 初始化时加载数据
        refreshData();
    }

    /**
     * 刷新所有数据
     */
    public void refreshData() {
        loadSystemStatistics();
        loadFarmerStatistics();
    }

    /**
     * 加载系统统计数据
     */
    private void loadSystemStatistics() {
        // 获取农户总数
        farmerInfoRepository.getTotalFarmerCount(new FarmerInfoRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                totalFarmerCount.postValue(count);
            }

            @Override
            public void onFailure(String error) {
                totalFarmerCount.postValue(0);
            }
        });

        // 获取称重记录统计
        weightRecordRepository.getTotalLeafCount(new WeightRecordRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                totalLeafCount.postValue(count);
            }

            @Override
            public void onFailure(String error) {
                totalLeafCount.postValue(0);
            }
        });

        // 获取总重量
        weightRecordRepository.getTotalWeight(new WeightRecordRepository.WeightCallback() {
            @Override
            public void onSuccess(double weight) {
                totalWeight.postValue(weight);
            }

            @Override
            public void onFailure(String error) {
                totalWeight.postValue(0.0);
            }
        });
    }

    /**
     * 加载农户统计数据
     */
    private void loadFarmerStatistics() {
        // 获取所有活跃农户
        farmerInfoRepository.getAllActiveFarmers(new FarmerInfoRepository.FarmerListCallback() {
            @Override
            public void onSuccess(List<FarmerInfoEntity> farmers) {
                if (farmers == null || farmers.isEmpty()) {
                    farmerStatisticsList.postValue(new ArrayList<>());
                    return;
                }

                // 为每个农户获取统计数据
                loadFarmerStatisticsForFarmers(farmers);
            }

            @Override
            public void onFailure(String error) {
                farmerStatisticsList.postValue(new ArrayList<>());
            }
        });
    }

    /**
     * 为农户列表获取统计数据
     */
    private void loadFarmerStatisticsForFarmers(List<FarmerInfoEntity> farmers) {
        List<FarmerStatistics> statsList = new ArrayList<>();
        final int[] completedCount = {0}; // 使用数组来在回调中修改

        for (FarmerInfoEntity farmer : farmers) {
            // 获取每个农户的称重记录统计
            weightRecordRepository.getFarmerRecordStatistics(farmer.getIdCardNumber(),
                    new WeightRecordRepository.FarmerStatsCallback() {
                        @Override
                        public void onSuccess(int totalLeafCount, double totalWeight, String lastRecordDate) {
                            // 创建农户统计对象
                            FarmerStatistics stats = new FarmerStatistics(
                                    farmer.getFarmerName(),
                                    farmer.getIdCardNumber(),
                                    totalLeafCount,
                                    totalWeight,
                                    lastRecordDate != null ? lastRecordDate : "暂无记录"
                            );
                            
                            synchronized (statsList) {
                                statsList.add(stats);
                                completedCount[0]++;
                                
                                // 所有农户数据都加载完成后更新UI
                                if (completedCount[0] == farmers.size()) {
                                    // 按照总重量降序排序
                                    statsList.sort((a, b) -> Double.compare(b.getTotalWeight(), a.getTotalWeight()));
                                    farmerStatisticsList.postValue(statsList);
                                }
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            // 即使获取统计失败，也要创建一个空统计对象
                            FarmerStatistics stats = new FarmerStatistics(
                                    farmer.getFarmerName(),
                                    farmer.getIdCardNumber(),
                                    0,
                                    0.0,
                                    "暂无记录"
                            );
                            
                            synchronized (statsList) {
                                statsList.add(stats);
                                completedCount[0]++;
                                
                                if (completedCount[0] == farmers.size()) {
                                    statsList.sort((a, b) -> Double.compare(b.getTotalWeight(), a.getTotalWeight()));
                                    farmerStatisticsList.postValue(statsList);
                                }
                            }
                        }
                    });
        }
    }

    // === Getter方法，供UI观察 ===

    public LiveData<Integer> getTotalFarmerCount() {
        return totalFarmerCount;
    }

    public LiveData<Integer> getTotalLeafCount() {
        return totalLeafCount;
    }

    public LiveData<Double> getTotalWeight() {
        return totalWeight;
    }

    public LiveData<List<FarmerStatistics>> getFarmerStatisticsList() {
        return farmerStatisticsList;
    }

    // === 数据模型类 ===

    /**
     * 农户统计信息类
     */
    public static class FarmerStatistics {
        private final String farmerName;
        private final String idCardNumber;
        private final int leafCount;
        private final double totalWeight;
        private final String lastRecordDate;

        public FarmerStatistics(String farmerName, String idCardNumber, int leafCount, 
                               double totalWeight, String lastRecordDate) {
            this.farmerName = farmerName;
            this.idCardNumber = idCardNumber;
            this.leafCount = leafCount;
            this.totalWeight = totalWeight;
            this.lastRecordDate = lastRecordDate;
        }

        public String getFarmerName() {
            return farmerName;
        }

        public String getIdCardNumber() {
            return idCardNumber;
        }

        public int getLeafCount() {
            return leafCount;
        }

        public double getTotalWeight() {
            return totalWeight;
        }

        public String getLastRecordDate() {
            return lastRecordDate;
        }

        /**
         * 获取遮蔽的身份证号码用于显示
         */
        public String getMaskedIdCardNumber() {
            if (idCardNumber == null || idCardNumber.length() < 8) {
                return "****";
            }
            return idCardNumber.substring(0, 4) + "****" + 
                   idCardNumber.substring(idCardNumber.length() - 4);
        }

        @Override
        public String toString() {
            return "FarmerStatistics{" +
                    "farmerName='" + farmerName + '\'' +
                    ", leafCount=" + leafCount +
                    ", totalWeight=" + totalWeight +
                    ", lastRecordDate='" + lastRecordDate + '\'' +
                    '}';
        }
    }

    // === 向后兼容的数据类（保留原有接口） ===

    /**
     * 农户数据类（向后兼容）
     */
    public static class FarmerData {
        private final int bundleCount;
        private final double totalWeight;

        public FarmerData(int bundleCount, double totalWeight) {
            this.bundleCount = bundleCount;
            this.totalWeight = totalWeight;
        }

        public int getBundleCount() {
            return bundleCount;
        }

        public double getTotalWeight() {
            return totalWeight;
        }
    }

    // === 向后兼容的LiveData，供老代码使用 ===

    public LiveData<FarmerData> getFarmerAData() {
        MutableLiveData<FarmerData> data = new MutableLiveData<>(new FarmerData(0, 0.0));
        return data;
    }

    public LiveData<FarmerData> getFarmerBData() {
        MutableLiveData<FarmerData> data = new MutableLiveData<>(new FarmerData(0, 0.0));
        return data;
    }

    public LiveData<FarmerData> getFarmerCData() {
        MutableLiveData<FarmerData> data = new MutableLiveData<>(new FarmerData(0, 0.0));
        return data;
    }

    public LiveData<FarmerData> getFarmerDData() {
        MutableLiveData<FarmerData> data = new MutableLiveData<>(new FarmerData(0, 0.0));
        return data;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理资源（如果需要）
    }
}