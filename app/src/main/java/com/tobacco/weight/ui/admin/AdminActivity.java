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

        // 观察烟叶分类数据
        viewModel.getGradeAData().observe(this, data -> updateGradeData(data, "A"));
        viewModel.getGradeBData().observe(this, data -> updateGradeData(data, "B"));
        viewModel.getGradeCData().observe(this, data -> updateGradeData(data, "C"));
        viewModel.getGradeDData().observe(this, data -> updateGradeData(data, "D"));
    }

    private void updateGradeData(AdminViewModel.GradeData data, String grade) {
        if (data != null) {
            switch (grade) {
                case "A":
                    binding.tvGradeAPrecheckWeight.setText(String.format("%.2f", data.getPrecheckWeight()));
                    binding.tvGradeAActualWeight.setText(String.format("%.2f", data.getActualWeight()));
                    break;
                case "B":
                    binding.tvGradeBPrecheckWeight.setText(String.format("%.2f", data.getPrecheckWeight()));
                    binding.tvGradeBActualWeight.setText(String.format("%.2f", data.getActualWeight()));
                    break;
                case "C":
                    binding.tvGradeCPrecheckWeight.setText(String.format("%.2f", data.getPrecheckWeight()));
                    binding.tvGradeCActualWeight.setText(String.format("%.2f", data.getActualWeight()));
                    break;
                case "D":
                    binding.tvGradeDPrecheckWeight.setText(String.format("%.2f", data.getPrecheckWeight()));
                    binding.tvGradeDActualWeight.setText(String.format("%.2f", data.getActualWeight()));
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