package com.tobacco.weight.ui.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.tobacco.weight.R;
import com.tobacco.weight.data.FarmerStatistics;
import com.tobacco.weight.data.WeighingRecord;
import com.tobacco.weight.data.repository.WeightRecordRepository;
import com.tobacco.weight.databinding.ActivityAdminBinding;
import com.tobacco.weight.utils.DataExportUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 管理员界面（一级）
 * 显示所有农户的称重统计信息和数据管理功能
 */
@AndroidEntryPoint
public class AdminActivity extends AppCompatActivity {

    private ActivityAdminBinding binding;
    private AdminViewModel viewModel;

    // 注入数据仓库用于数据访问
    @Inject
    WeightRecordRepository weightRecordRepository;

    // UI组件
    private LinearLayout layoutFarmerDataContainer;
    private TextView tvDataStatus;
    private TextView tvTotalFarmerCount;
    private TextView tvTotalRecordCount;
    private TextView tvTotalWeight;
    private TextView tvScrollHint; // 新增滑动提示

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_admin);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        setupUI();
        setupObservers();
        loadFarmerData();
    }

    private void setupUI() {
        // 设置标题和导航
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("管理员界面（一级）");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 获取UI组件引用
        layoutFarmerDataContainer = binding.layoutFarmerDataContainer;
        tvDataStatus = binding.tvDataStatus;
        tvTotalFarmerCount = binding.tvTotalFarmerCount;
        tvTotalRecordCount = binding.tvTotalRecordCount;
        tvTotalWeight = binding.tvTotalWeight;
        tvScrollHint = binding.tvScrollHint; // 初始化滑动提示

        // 设置按钮点击事件
        binding.btnRefreshData.setOnClickListener(v -> {
            refreshData();
            Toast.makeText(this, "正在刷新数据...", Toast.LENGTH_SHORT).show();
        });

        binding.btnExportAllData.setOnClickListener(v -> {
            exportAllRecords();
        });
    }

    private void setupObservers() {
        // 观察农户统计数据
        viewModel.getFarmerStatisticsList().observe(this, farmerStatsList -> {
            if (farmerStatsList != null) {
                updateFarmerTable(farmerStatsList);
                updateSystemStatistics(farmerStatsList);
            }
        });

        // 观察系统统计数据
        viewModel.getTotalFarmerCount().observe(this, count -> {
            if (count != null) {
                tvTotalFarmerCount.setText(String.valueOf(count));
            }
        });

        viewModel.getTotalLeafCount().observe(this, count -> {
            if (count != null) {
                tvTotalRecordCount.setText(String.valueOf(count));
            }
        });

        viewModel.getTotalWeight().observe(this, weight -> {
            if (weight != null) {
                tvTotalWeight.setText(String.format("%.2f kg", weight));
            }
        });
    }

    /**
     * 加载农户数据
     */
    private void loadFarmerData() {
        tvDataStatus.setText("正在加载农户数据...");
        tvDataStatus.setVisibility(View.VISIBLE);
        viewModel.refreshData();
    }

    /**
     * 刷新数据
     */
    private void refreshData() {
        tvDataStatus.setText("正在刷新数据...");
        tvDataStatus.setVisibility(View.VISIBLE);
        viewModel.refreshData();
    }

    /**
     * 更新农户表格
     */
    private void updateFarmerTable(List<AdminViewModel.FarmerStatistics> farmerStatsList) {
        // 清除现有的农户行
        layoutFarmerDataContainer.removeAllViews();

        if (farmerStatsList == null || farmerStatsList.isEmpty()) {
            tvDataStatus.setText("暂无农户数据");
            tvDataStatus.setVisibility(View.VISIBLE);
            tvScrollHint.setVisibility(View.GONE); // 隐藏滑动提示
            return;
        }

        // 隐藏状态提示
        tvDataStatus.setVisibility(View.GONE);

        // 为每个农户创建数据行
        for (int i = 0; i < farmerStatsList.size(); i++) {
            AdminViewModel.FarmerStatistics stats = farmerStatsList.get(i);
            LinearLayout farmerRow = createFarmerRow(stats, i);
            layoutFarmerDataContainer.addView(farmerRow);
        }

        // 当农户数据较多时显示滑动提示（超过5个农户）
        if (farmerStatsList.size() > 5) {
            tvScrollHint.setVisibility(View.VISIBLE);
            tvScrollHint.setText("⬆⬇ 上下滑动查看更多农户数据 (共" + farmerStatsList.size() + "位农户)");
        } else {
            tvScrollHint.setVisibility(View.GONE);
        }
    }

    /**
     * 创建单个农户数据行
     */
    private LinearLayout createFarmerRow(AdminViewModel.FarmerStatistics stats, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(12, 12, 12, 12);

        // 设置交替行背景
        if (index % 2 == 0) {
            row.setBackgroundColor(0xFFFFFFFF); // 白色
        } else {
            row.setBackgroundColor(0xFFF5F5F5); // 浅灰色
        }

        // 农户姓名
        TextView tvName = createTableCell(stats.getFarmerName(), 3);
        tvName.setTextColor(0xFF212121);
        tvName.setTextSize(16);
        row.addView(tvName);

        // 记录总数
        TextView tvCount = createTableCell(String.valueOf(stats.getRecordCount()), 2);
        tvCount.setTextColor(0xFF4CAF50);
        tvCount.setTextSize(16);
        row.addView(tvCount);

        // 总重量
        TextView tvWeight = createTableCell(String.format("%.2f kg", stats.getTotalWeight()), 3);
        tvWeight.setTextColor(0xFF2196F3);
        tvWeight.setTextSize(16);
        row.addView(tvWeight);

        // 操作按钮
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        buttonContainer.setGravity(android.view.Gravity.CENTER);

        Button viewButton = createViewButton(stats);
        buttonContainer.addView(viewButton);
        row.addView(buttonContainer);

        return row;
    }

    /**
     * 创建表格单元格
     */
    private TextView createTableCell(String text, int weight) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        textView.setText(text);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setPadding(8, 8, 8, 8);
        return textView;
    }

    /**
     * 创建查看按钮
     */
    private Button createViewButton(AdminViewModel.FarmerStatistics stats) {
        Button button = new Button(this);
        button.setText("查看");
        button.setTextSize(12);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(0xFF2196F3);
        button.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(4, 4, 4, 4);
        button.setLayoutParams(params);

        // 设置点击监听器 - 打开Level 2详情对话框
        button.setOnClickListener(v -> {
            showFarmerDetailDialog(stats);
        });

        return button;
    }

    /**
     * 显示农户详细信息对话框（Level 2）
     */
    private void showFarmerDetailDialog(AdminViewModel.FarmerStatistics stats) {
        // 创建Dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_farmer_detail);
        dialog.setCancelable(true);

        // 获取Dialog中的控件
        TextView tvFarmerName = dialog.findViewById(R.id.tv_detail_farmer_name);
        TextView tvIdNumber = dialog.findViewById(R.id.tv_detail_id_number);
        TextView tvTotalWeight = dialog.findViewById(R.id.tv_detail_total_weight);
        TextView tvRecordCount = dialog.findViewById(R.id.tv_detail_record_count);
        LinearLayout layoutRecordsContainer = dialog.findViewById(R.id.layout_records_container);
        Button btnExportFarmer = dialog.findViewById(R.id.btn_export_farmer_data);
        Button btnClose = dialog.findViewById(R.id.btn_close_dialog);

        // 设置基本数据
        tvFarmerName.setText(stats.getFarmerName());
        tvIdNumber.setText(stats.getMaskedIdCardNumber());
        tvTotalWeight.setText(String.format("%.2f kg", stats.getTotalWeight()));
        tvRecordCount.setText(String.valueOf(stats.getRecordCount()));

        // 清空记录容器并显示加载状态
        layoutRecordsContainer.removeAllViews();
        TextView loadingText = new TextView(this);
        loadingText.setText("正在加载记录...");
        loadingText.setGravity(android.view.Gravity.CENTER);
        loadingText.setPadding(16, 16, 16, 16);
        loadingText.setTextColor(0xFF757575);
        layoutRecordsContainer.addView(loadingText);

        // 加载农户的所有个人记录
        weightRecordRepository.getRecordsByIdCard(stats.getIdCardNumber()).observe(this,
                new androidx.lifecycle.Observer<List<com.tobacco.weight.data.model.WeightRecord>>() {
                    @Override
                    public void onChanged(List<com.tobacco.weight.data.model.WeightRecord> records) {
                        // Remove observer after first load to avoid memory leaks
                        weightRecordRepository.getRecordsByIdCard(stats.getIdCardNumber()).removeObserver(this);

                        runOnUiThread(() -> {
                            layoutRecordsContainer.removeAllViews(); // 清除加载状态

                            if (records != null && !records.isEmpty()) {
                                // 为每条记录创建显示行
                                for (int i = 0; i < records.size(); i++) {
                                    LinearLayout recordRow = createSimpleRecordRow(records.get(i), i);
                                    layoutRecordsContainer.addView(recordRow);
                                }

                                // 强制刷新布局以确保滚动正常工作
                                layoutRecordsContainer.requestLayout();
                            } else {
                                // 没有记录时显示提示
                                TextView noRecordsText = new TextView(AdminActivity.this);
                                noRecordsText.setText("暂无记录");
                                noRecordsText.setGravity(android.view.Gravity.CENTER);
                                noRecordsText.setPadding(16, 32, 16, 32);
                                noRecordsText.setTextColor(0xFF757575);
                                noRecordsText.setTextSize(16);
                                layoutRecordsContainer.addView(noRecordsText);
                            }
                        });
                    }
                });

        // 设置导出按钮点击事件
        btnExportFarmer.setOnClickListener(v -> {
            exportFarmerRecords(stats.getFarmerName());
            dialog.dismiss();
        });

        // 设置关闭按钮点击事件
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 显示Dialog
        dialog.show();

        // 设置Dialog窗口大小
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        }
    }

    /**
     * 创建简化的单条记录显示行 (时间、总捆数、预检编号、查看按钮)
     */
    private LinearLayout createSimpleRecordRow(com.tobacco.weight.data.model.WeightRecord record, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 12, 8, 12);

        // 设置交替行背景
        if (index % 2 == 0) {
            row.setBackgroundColor(0xFFFFFFFF); // 白色
        } else {
            row.setBackgroundColor(0xFFF9F9F9); // 浅灰色
        }

        // 格式化时间
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        String timeStr = sdf.format(new Date(record.getTimestamp()));

        // 时间 (权重3)
        TextView tvTime = createRecordCell(timeStr, 3);
        tvTime.setTextSize(13);
        tvTime.setTextColor(0xFF212121);
        row.addView(tvTime);

        // 总捆数 (权重2) - 使用新的数据结构
        TextView tvBundles = createRecordCell(String.valueOf(record.getTotalBundles()), 2);
        tvBundles.setTextSize(13);
        tvBundles.setTextColor(0xFF4CAF50);
        row.addView(tvBundles);

        // 预检编号 (权重4) - 替换合同编号
        TextView tvPrecheckNumber = createRecordCell(getDisplayText(record.getPreCheckNumber()), 4);
        tvPrecheckNumber.setTextSize(13);
        tvPrecheckNumber.setTextColor(0xFF2196F3);
        row.addView(tvPrecheckNumber);

        // 查看按钮 (权重2)
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2));
        buttonContainer.setGravity(android.view.Gravity.CENTER);
        buttonContainer.setPadding(4, 0, 4, 0);

        Button viewButton = new Button(this);
        viewButton.setText("查看");
        viewButton.setTextSize(11);
        viewButton.setTextColor(0xFFFFFFFF);
        viewButton.setBackgroundColor(0xFF2196F3);
        
        // 设置按钮大小
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (32 * getResources().getDisplayMetrics().density) // 32dp height
        );
        buttonParams.setMargins(2, 2, 2, 2);
        viewButton.setLayoutParams(buttonParams);
        viewButton.setPadding(12, 4, 12, 4);

        // 设置点击监听器 - 显示个人记录详情（Level 3）
        viewButton.setOnClickListener(v -> {
            showRecordDetailDialog(record);
        });

        buttonContainer.addView(viewButton);
        row.addView(buttonContainer);

        // 移除整行点击监听器，现在只通过按钮点击
        row.setClickable(false);
        row.setFocusable(false);
        
        return row;
    }

    /**
     * 创建记录单元格
     */
    private TextView createRecordCell(String text, int weight) {
        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        textView.setText(text);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setPadding(4, 4, 4, 4);
        return textView;
    }

    /**
     * 根据烟叶部位获取颜色
     */
    private int getColorForTobaccoPart(String tobaccoPart) {
        if (tobaccoPart == null) return 0xFF757575;
        
        switch (tobaccoPart) {
            case "上部叶":
                return 0xFFE57373; // 红色
            case "中部叶":
                return 0xFF81C784; // 绿色
            case "下部叶":
                return 0xFF64B5F6; // 蓝色
            default:
                return 0xFF757575; // 灰色
        }
    }

    /**
     * 显示记录详细信息对话框（Level 3）- 类似主界面右侧显示
     */
    private void showRecordDetailDialog(com.tobacco.weight.data.model.WeightRecord record) {
        // 创建Dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_record_detail);
        dialog.setCancelable(true);

        // === 获取Dialog中的控件 ===
        
        // 基本信息
        TextView tvFarmerName = dialog.findViewById(R.id.tv_record_farmer_name);
        TextView tvIdCardNumber = dialog.findViewById(R.id.tv_record_id_card_number);
        TextView tvFarmerAddress = dialog.findViewById(R.id.tv_record_farmer_address);
        TextView tvFarmerGender = dialog.findViewById(R.id.tv_record_farmer_gender);
        
        // 记录信息
        TextView tvRecordNumber = dialog.findViewById(R.id.tv_record_number);
        TextView tvPrecheckNumber = dialog.findViewById(R.id.tv_record_precheck_number);
        TextView tvWarehouseNumber = dialog.findViewById(R.id.tv_record_warehouse_number);
        TextView tvOperatorName = dialog.findViewById(R.id.tv_record_operator_name);
        
        // 烟叶信息
        TextView tvTobaccoPart = dialog.findViewById(R.id.tv_record_tobacco_part);
        TextView tvGrade = dialog.findViewById(R.id.tv_record_grade);
        TextView tvWeight = dialog.findViewById(R.id.tv_record_weight);
        TextView tvBundles = dialog.findViewById(R.id.tv_record_bundles);
        TextView tvMoistureContent = dialog.findViewById(R.id.tv_record_moisture_content);
        TextView tvImpurityRate = dialog.findViewById(R.id.tv_record_impurity_rate);
        
        // 价格信息
        TextView tvPrice = dialog.findViewById(R.id.tv_record_price);
        TextView tvTotalAmount = dialog.findViewById(R.id.tv_record_total_amount);
        
        // 系统信息
        TextView tvTimestamp = dialog.findViewById(R.id.tv_record_timestamp);
        TextView tvUpdateTime = dialog.findViewById(R.id.tv_record_update_time);
        TextView tvStatus = dialog.findViewById(R.id.tv_record_status);
        TextView tvPrintStatus = dialog.findViewById(R.id.tv_record_print_status);
        TextView tvExportStatus = dialog.findViewById(R.id.tv_record_export_status);
        TextView tvQrCode = dialog.findViewById(R.id.tv_record_qr_code);
        TextView tvRemark = dialog.findViewById(R.id.tv_record_remark);
        
        Button btnClose = dialog.findViewById(R.id.btn_close_record_dialog);

        // === 填充基本信息 ===
        tvFarmerName.setText(getDisplayText(record.getFarmerName()));
        tvIdCardNumber.setText(getDisplayText(record.getIdCardNumber()));
        tvFarmerAddress.setText(getDisplayText(record.getFarmerAddress()));
        tvFarmerGender.setText(getDisplayText(record.getFarmerGender()));
        
        // === 填充记录信息 ===
        tvRecordNumber.setText(getDisplayText(record.getRecordNumber()));
        tvPrecheckNumber.setText(getDisplayText(record.getPreCheckNumber()));
        tvWarehouseNumber.setText(getDisplayText(record.getWarehouseNumber()));
        tvOperatorName.setText(getDisplayText(record.getOperatorName()));
        
        // === 填充烟叶信息 ===
        
        // 使用新的详细烟叶分级数据结构
        if (record.hasMultipleTobaccoParts()) {
            // 多部位烟叶记录，显示详细分解
            tvTobaccoPart.setText("混合烟叶 (" + record.getPrimaryTobaccoPart() + "为主)");
            tvTobaccoPart.setTextColor(0xFF757575); // 灰色表示混合
            
            // 显示详细分解信息
            String tobaccoBreakdown = record.getTobaccoPartsDescription();
            tvGrade.setText(tobaccoBreakdown);
            tvGrade.setTextSize(12); // 稍小字体显示详细信息
            
        } else {
            // 单一部位烟叶记录
            tvTobaccoPart.setText(getDisplayText(record.getPrimaryTobaccoPart()));
            tvTobaccoPart.setTextColor(getColorForTobaccoPart(record.getPrimaryTobaccoPart()));
            tvGrade.setText(getDisplayText(record.getTobaccoGrade()));
        }
        
        // 显示总重量（来自新的数据结构）
        if (record.getTotalWeight() > 0) {
            tvWeight.setText(String.format("%.2f kg", record.getTotalWeight()));
        } else {
            tvWeight.setText("未称重");
        }
        
        // 显示总捆数（来自新的数据结构）
        if (record.getTotalBundles() > 0) {
            tvBundles.setText(String.format("%d 捆", record.getTotalBundles()));
            
            // 如果有多个部位，添加详细捆数信息
            if (record.hasMultipleTobaccoParts()) {
                String bundleDetails = "";
                if (record.getUpperLeafBundles() > 0) {
                    bundleDetails += "上部:" + record.getUpperLeafBundles() + "捆 ";
                }
                if (record.getMiddleLeafBundles() > 0) {
                    bundleDetails += "中部:" + record.getMiddleLeafBundles() + "捆 ";
                }
                if (record.getLowerLeafBundles() > 0) {
                    bundleDetails += "下部:" + record.getLowerLeafBundles() + "捆";
                }
                
                if (!bundleDetails.trim().isEmpty()) {
                    tvBundles.setText(String.format("%d 捆 (%s)", record.getTotalBundles(), bundleDetails.trim()));
                }
            }
        } else {
            tvBundles.setText("0 捆");
        }
        
        // 处理水分含量和杂质率
        if (record.getMoistureContent() > 0) {
            tvMoistureContent.setText(String.format("%.2f%%", record.getMoistureContent()));
        } else {
            tvMoistureContent.setText("未设置");
        }
        
        if (record.getImpurityRate() > 0) {
            tvImpurityRate.setText(String.format("%.2f%%", record.getImpurityRate()));
        } else {
            tvImpurityRate.setText("未设置");
        }
        
        // === 填充价格信息 ===
        if (record.getPurchasePrice() > 0) {
            tvPrice.setText(String.format("%.2f 元/kg", record.getPurchasePrice()));
        } else {
            tvPrice.setText("未设置");
        }
        
        if (record.getTotalAmount() > 0) {
            tvTotalAmount.setText(String.format("%.2f 元", record.getTotalAmount()));
        } else {
            tvTotalAmount.setText("未计算");
        }
        
        // === 填充系统信息 ===
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        // 记录时间
        if (record.getCreateTime() != null) {
            tvTimestamp.setText(sdf.format(record.getCreateTime()));
        } else if (record.getTimestamp() > 0) {
            tvTimestamp.setText(sdf.format(new Date(record.getTimestamp())));
        } else {
            tvTimestamp.setText("未知");
        }
        
        // 更新时间
        if (record.getUpdateTime() != null) {
            tvUpdateTime.setText(sdf.format(record.getUpdateTime()));
        } else {
            tvUpdateTime.setText("未更新");
        }
        
        // 状态信息
        tvStatus.setText(getDisplayText(record.getStatus()));
        
        // 打印状态
        if (record.isPrinted()) {
            if (record.getPrintCount() > 0) {
                tvPrintStatus.setText(String.format("已打印 (%d次)", record.getPrintCount()));
            } else {
                tvPrintStatus.setText("已打印");
            }
        } else {
            tvPrintStatus.setText("未打印");
        }
        
        // 导出状态
        tvExportStatus.setText(record.isExported() ? "已导出" : "未导出");
        
        // 二维码和备注
        tvQrCode.setText(getDisplayText(record.getQrCode()));
        tvRemark.setText(getDisplayText(record.getRemark()));

        // 设置关闭按钮点击事件
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 显示Dialog
        dialog.show();

        // 设置Dialog窗口大小 - 加大高度以容纳更多内容
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        }
    }
    
    /**
     * 获取显示文本，处理null值
     */
    private String getDisplayText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "未设置";
        }
        return text;
    }

    /**
     * 更新系统统计信息
     */
    private void updateSystemStatistics(List<AdminViewModel.FarmerStatistics> farmerStatsList) {
        if (farmerStatsList == null)
            return;

        int totalFarmers = farmerStatsList.size();
        int totalRecords = 0;
        double totalWeight = 0.0;

        for (AdminViewModel.FarmerStatistics stats : farmerStatsList) {
            totalRecords += stats.getRecordCount(); // Use record count instead of leaf count
            totalWeight += stats.getTotalWeight();
        }

        tvTotalFarmerCount.setText(String.valueOf(totalFarmers));
        tvTotalRecordCount.setText(String.valueOf(totalRecords));
        tvTotalWeight.setText(String.format("%.2f kg", totalWeight));
    }

    /**
     * 导出所有记录
     */
    private void exportAllRecords() {
        if (weightRecordRepository == null) {
            Toast.makeText(this, "数据服务未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在准备导出所有数据...", Toast.LENGTH_SHORT).show();

        // 获取数据库中的所有记录
        weightRecordRepository.getAllRecords().observe(this, records -> {
            if (records != null && !records.isEmpty()) {
                // 使用导出工具导出数据
                DataExportUtils.exportAllRecordsToCSV(this, records, new DataExportUtils.ExportCallback() {
                    @Override
                    public void onSuccess(String message, String filePath, File file) {
                        runOnUiThread(() -> {
                            showExportSuccessDialog(message, file);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(AdminActivity.this, error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 导出指定农户的记录
     */
    private void exportFarmerRecords(String farmerName) {
        if (weightRecordRepository == null) {
            Toast.makeText(this, "数据服务未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在准备导出" + farmerName + "的数据...", Toast.LENGTH_SHORT).show();

        // 获取指定农户的记录
        weightRecordRepository.getRecordsByFarmerName(farmerName).observe(this, records -> {
            if (records != null && !records.isEmpty()) {
                // 使用导出工具导出数据
                DataExportUtils.exportFarmerRecordsToCSV(this, records, farmerName,
                        new DataExportUtils.ExportCallback() {
                            @Override
                            public void onSuccess(String message, String filePath, File file) {
                                runOnUiThread(() -> {
                                    showExportSuccessDialog(message, file);
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(AdminActivity.this, error, Toast.LENGTH_LONG).show();
                                });
                            }
                        });
            } else {
                Toast.makeText(this, farmerName + "没有称重记录", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 显示导出成功的Dialog
     */
    private void showExportSuccessDialog(String message, File file) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("导出成功");
        builder.setMessage(message + "\n\n您希望：");

        // 打开文件
        builder.setPositiveButton("打开文件", (dialog, which) -> {
            DataExportUtils.openCsvFile(this, file);
        });

        // 打开文件夹
        builder.setNeutralButton("打开文件夹", (dialog, which) -> {
            DataExportUtils.openExportFolder(this);
        });

        // 仅关闭
        builder.setNegativeButton("关闭", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setIcon(android.R.drawable.ic_dialog_info);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}