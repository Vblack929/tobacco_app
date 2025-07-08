package com.tobacco.weight.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.tobacco.weight.R;
import com.tobacco.weight.databinding.ActivityAdminBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 管理员界面
 * 显示详细的预检信息和烟叶分类数据
 */
@AndroidEntryPoint
public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private AdminViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_admin);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        setupUI();
        setupObservers();
    }

    private void setupUI() {
        // 设置标题
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("管理员界面");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 设置点击事件
        binding.btnRefreshData.setOnClickListener(v -> {
            viewModel.refreshData();
            Toast.makeText(this, "数据已刷新", Toast.LENGTH_SHORT).show();
        });

        binding.btnExportData.setOnClickListener(v -> {
            viewModel.exportData();
            Toast.makeText(this, "数据导出中...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupObservers() {
        // 观察预检数据
        viewModel.getTotalPrecheckCount().observe(this, count -> {
            binding.tvTotalPrecheckCount.setText(String.valueOf(count));
        });

        viewModel.getCurrentPrecheckCount().observe(this, count -> {
            binding.tvCurrentPrecheckCount.setText(String.valueOf(count));
        });

        viewModel.getTotalPrecheckWeight().observe(this, weight -> {
            binding.tvTotalPrecheckWeight.setText(String.format("%.2f kg", weight));
        });

        viewModel.getAverageWeight().observe(this, weight -> {
            binding.tvAverageWeight.setText(String.format("%.2f kg", weight));
        });

        // 观察烟农数据
        viewModel.getFarmerAData().observe(this, data -> updateFarmerData(data, "A"));
        viewModel.getFarmerBData().observe(this, data -> updateFarmerData(data, "B"));
        viewModel.getFarmerCData().observe(this, data -> updateFarmerData(data, "C"));
        viewModel.getFarmerDData().observe(this, data -> updateFarmerData(data, "D"));
    }

    private void updateFarmerData(AdminViewModel.FarmerData data, String farmer) {
        if (data != null) {
            switch (farmer) {
                case "A": // 张三
                    binding.tvFarmerABundleCount.setText(String.valueOf(data.getBundleCount()));
                    binding.tvFarmerATotalWeight.setText(String.format("%.2f kg", data.getTotalWeight()));
                    break;
                case "B": // 李四
                    binding.tvFarmerBBundleCount.setText(String.valueOf(data.getBundleCount()));
                    binding.tvFarmerBTotalWeight.setText(String.format("%.2f kg", data.getTotalWeight()));
                    break;
                case "C": // 王五
                    binding.tvFarmerCBundleCount.setText(String.valueOf(data.getBundleCount()));
                    binding.tvFarmerCTotalWeight.setText(String.format("%.2f kg", data.getTotalWeight()));
                    break;
                case "D": // 赵六
                    binding.tvFarmerDBundleCount.setText(String.valueOf(data.getBundleCount()));
                    binding.tvFarmerDTotalWeight.setText(String.format("%.2f kg", data.getTotalWeight()));
                    break;
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}