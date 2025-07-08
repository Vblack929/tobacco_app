package com.tobacco.weight.ui.main;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tobacco.weight.R;
import com.tobacco.weight.data.FarmerStatistics;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 称重界面Fragment - 测试版本
 * 逐步恢复功能
 */
@AndroidEntryPoint
public class WeightingFragment extends Fragment {

    private WeightingViewModel viewModel;
    private MainViewModel mainViewModel;

    // 基本UI控件
    private EditText etFarmerName;
    private TextView tvContractNumber;
    private TextView tvCurrentWeight;
    private Button btnUpperLevel;
    private Button btnMiddleLevel;
    private Button btnLowerLevel;
    private Button btnConfirm;
    private Button btnReadIdCard;

    // 预检比例控件
    private EditText etPrecheckRatio;
    private EditText etUpperRatio;
    private EditText etMiddleRatio;
    private EditText etLowerRatio;

    // 预检信息控件
    private TextView tvPrecheckId;
    private TextView tvPrecheckDate;

    // 管理员界面动态容器
    private LinearLayout layoutFarmerDataContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weighing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 尝试初始化ViewModels
        try {
            viewModel = new ViewModelProvider(this).get(WeightingViewModel.class);
            mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
            Toast.makeText(getContext(), "ViewModel初始化成功！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "ViewModel初始化失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // 初始化基本UI控件（不依赖ViewModel）
        initializeBasicViews(view);

        // 设置基本的点击监听器
        setupBasicClickListeners();

        // 尝试添加简化的观察者逻辑
        try {
            observeBasicViewModel();
            Toast.makeText(getContext(), "基础观察者设置成功！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "观察者设置失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // 尝试添加MainViewModel观察者逻辑
        try {
            observeIdCardData();
            Toast.makeText(getContext(), "身份证观察者设置成功！", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "身份证观察者设置失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // 显示成功消息
        Toast.makeText(getContext(), "所有功能加载成功！", Toast.LENGTH_LONG).show();
    }

    /**
     * 观察身份证数据
     */
    private void observeIdCardData() {
        if (mainViewModel == null)
            return;

        // 烟农姓名 - 从MainViewModel的身份证数据获取
        mainViewModel.getFarmerName().observe(getViewLifecycleOwner(), name -> {
            if (name != null && !name.trim().isEmpty() && etFarmerName != null) {
                etFarmerName.setText(name);
                Toast.makeText(getContext(), "✅ 身份证读取成功: " + name, Toast.LENGTH_SHORT).show();
            }
        });

        // ID card连接状态 - 更新按钮状态
        mainViewModel.getIdCardConnected().observe(getViewLifecycleOwner(), connected -> {
            if (btnReadIdCard != null) {
                btnReadIdCard.setEnabled(connected);
                if (connected) {
                    btnReadIdCard.setText("读取身份证");
                } else {
                    btnReadIdCard.setText("读卡器未连接");
                }
            }
        });
    }

    /**
     * 观察基本的ViewModel数据
     */
    private void observeBasicViewModel() {
        if (viewModel == null)
            return;

        // 只观察最基本的几个LiveData
        viewModel.getFarmerName().observe(getViewLifecycleOwner(), farmerName -> {
            if (etFarmerName != null && farmerName != null) {
                etFarmerName.setText(farmerName);
            }
        });

        viewModel.getContractNumber().observe(getViewLifecycleOwner(), contractNumber -> {
            if (tvContractNumber != null && contractNumber != null) {
                tvContractNumber.setText(contractNumber);
            }
        });

        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            if (tvCurrentWeight != null && weight != null) {
                tvCurrentWeight.setText(weight);
            }
        });

        viewModel.getSelectedLevel().observe(getViewLifecycleOwner(), level -> {
            // 可以在这里更新UI显示选中的等级
            if (level != null && !level.equals("未选择")) {
                Toast.makeText(getContext(), "当前选择: " + level, Toast.LENGTH_SHORT).show();
            }
        });

        // 观察预检比例数据
        viewModel.getPrecheckRatio().observe(getViewLifecycleOwner(), ratio -> {
            if (etPrecheckRatio != null && ratio != null) {
                etPrecheckRatio.setText(ratio);
            }
        });

        viewModel.getUpperRatio().observe(getViewLifecycleOwner(), ratio -> {
            if (etUpperRatio != null && ratio != null) {
                etUpperRatio.setText(ratio);
            }
        });

        viewModel.getMiddleRatio().observe(getViewLifecycleOwner(), ratio -> {
            if (etMiddleRatio != null && ratio != null) {
                etMiddleRatio.setText(ratio);
            }
        });

        viewModel.getLowerRatio().observe(getViewLifecycleOwner(), ratio -> {
            if (etLowerRatio != null && ratio != null) {
                etLowerRatio.setText(ratio);
            }
        });

        // 观察预检编号和日期数据
        viewModel.getCurrentPrecheckId().observe(getViewLifecycleOwner(), precheckId -> {
            if (tvPrecheckId != null && precheckId != null) {
                tvPrecheckId.setText(precheckId);
            }
        });

        viewModel.getCurrentPrecheckDate().observe(getViewLifecycleOwner(), precheckDate -> {
            if (tvPrecheckDate != null && precheckDate != null) {
                tvPrecheckDate.setText(precheckDate);
            }
        });

        // 观察烟农数据变化，实时更新管理员界面
        viewModel.getFarmerName().observe(getViewLifecycleOwner(), name -> {
            updateAdminInterface();
        });
    }

    /**
     * 初始化基本UI控件
     */
    private void initializeBasicViews(View view) {
        // 尝试找到基本控件
        etFarmerName = view.findViewById(R.id.et_farmer_name);
        tvContractNumber = view.findViewById(R.id.tv_contract_number);
        tvCurrentWeight = view.findViewById(R.id.tv_current_weight);
        btnUpperLevel = view.findViewById(R.id.btn_upper_level);
        btnMiddleLevel = view.findViewById(R.id.btn_middle_level);
        btnLowerLevel = view.findViewById(R.id.btn_lower_level);
        btnConfirm = view.findViewById(R.id.btn_confirm);
        btnReadIdCard = view.findViewById(R.id.btn_read_id_card);

        // 初始化预检比例控件
        etPrecheckRatio = view.findViewById(R.id.et_precheck_ratio);
        etUpperRatio = view.findViewById(R.id.et_upper_ratio);
        etMiddleRatio = view.findViewById(R.id.et_middle_ratio);
        etLowerRatio = view.findViewById(R.id.et_lower_ratio);

        // 初始化预检信息控件
        tvPrecheckId = view.findViewById(R.id.tv_precheck_id);
        tvPrecheckDate = view.findViewById(R.id.tv_precheck_date);

        // 设置预检比例控件为只读（用于显示自动计算结果）
        if (etPrecheckRatio != null) {
            etPrecheckRatio.setFocusable(false);
            etPrecheckRatio.setClickable(false);
            etPrecheckRatio.setText("0.0%");
        }
        if (etUpperRatio != null) {
            etUpperRatio.setFocusable(false);
            etUpperRatio.setClickable(false);
            etUpperRatio.setText("0.0%");
        }
        if (etMiddleRatio != null) {
            etMiddleRatio.setFocusable(false);
            etMiddleRatio.setClickable(false);
            etMiddleRatio.setText("0.0%");
        }
        if (etLowerRatio != null) {
            etLowerRatio.setFocusable(false);
            etLowerRatio.setClickable(false);
            etLowerRatio.setText("0.0%");
        }

        // 初始化管理员界面动态容器
        layoutFarmerDataContainer = view.findViewById(R.id.layout_farmer_data_container);

        // 设置一些测试数据（会被ViewModel数据覆盖）
        if (etFarmerName != null) {
            etFarmerName.setText("张三");
        }
        if (tvContractNumber != null) {
            tvContractNumber.setText("HT10000001");
        }
        if (tvCurrentWeight != null) {
            tvCurrentWeight.setText("5.00 kg");
        }
    }

    /**
     * 设置基本的点击监听器
     */
    private void setupBasicClickListeners() {
        if (btnUpperLevel != null) {
            btnUpperLevel.setOnClickListener(v -> {
                selectLevelButton(btnUpperLevel, "上部叶");
                if (viewModel != null) {
                    viewModel.selectLevel("上部叶");
                }
            });
        }

        if (btnMiddleLevel != null) {
            btnMiddleLevel.setOnClickListener(v -> {
                selectLevelButton(btnMiddleLevel, "中部叶");
                if (viewModel != null) {
                    viewModel.selectLevel("中部叶");
                }
            });
        }

        if (btnLowerLevel != null) {
            btnLowerLevel.setOnClickListener(v -> {
                selectLevelButton(btnLowerLevel, "下部叶");
                if (viewModel != null) {
                    viewModel.selectLevel("下部叶");
                }
            });
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String farmerName = etFarmerName != null ? etFarmerName.getText().toString().trim() : "";

                if (farmerName.isEmpty()) {
                    Toast.makeText(getContext(), "请输入烟农姓名", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (viewModel != null) {
                    // 先同步姓名到ViewModel
                    viewModel.setFarmerName(farmerName);

                    // 然后确认称重
                    viewModel.confirmWeighing();

                    // 重置按钮状态
                    resetLevelButtons();

                    // 立即更新管理员界面
                    updateAdminInterface();

                    Toast.makeText(getContext(), "✅ 称重记录已保存", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnReadIdCard != null) {
            btnReadIdCard.setOnClickListener(v -> {
                Toast.makeText(getContext(), "正在连接身份证读卡器...", Toast.LENGTH_SHORT).show();
                if (mainViewModel != null) {
                    mainViewModel.connectIdCardReader();
                }
                if (viewModel != null) {
                    viewModel.generateNewContractNumber();
                }
            });
        }
    }

    /**
     * 更新管理员界面数据（动态创建行）
     */
    private void updateAdminInterface() {
        if (viewModel == null || layoutFarmerDataContainer == null)
            return;

        // 清除现有的行
        layoutFarmerDataContainer.removeAllViews();

        // 获取所有烟农统计数据
        java.util.Map<String, FarmerStatistics> allStatistics = viewModel.getAllFarmerStatistics();

        // 获取烟农名称列表（按添加顺序）
        java.util.List<String> farmerNames = new java.util.ArrayList<>();
        for (FarmerStatistics stats : allStatistics.values()) {
            if (!farmerNames.contains(stats.getFarmerName())) {
                farmerNames.add(stats.getFarmerName());
            }
        }

        // 动态创建每个烟农的行（显示所有数据，支持滚动）
        for (int i = 0; i < farmerNames.size(); i++) {
            String farmerName = farmerNames.get(i);
            FarmerStatistics stats = allStatistics.get(farmerName);

            if (stats != null) {
                LinearLayout farmerRow = createFarmerRow(farmerName, stats, i);
                layoutFarmerDataContainer.addView(farmerRow);
            }
        }
    }

    /**
     * 创建单个烟农数据行
     */
    private LinearLayout createFarmerRow(String farmerName, FarmerStatistics stats, int index) {
        LinearLayout row = new LinearLayout(getContext());
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rowParams);
        row.setOrientation(LinearLayout.HORIZONTAL);

        // 交替背景色
        int backgroundColor = (index % 2 == 0) ? 0xFFFFFFFF : 0xFFF5F5F5;
        row.setBackgroundColor(backgroundColor);

        // 姓名
        TextView nameView = createTableCell(farmerName, 2);
        row.addView(nameView);

        // 预检捆数
        TextView bundleView = createTableCell(String.valueOf(stats.getTotalBundles()), 2);
        row.addView(bundleView);

        // 预检重量
        TextView weightView = createTableCell(String.format("%.2f kg", stats.getTotalWeight()), 2);
        row.addView(weightView);

        // 查看按钮
        Button viewButton = createViewButton(farmerName);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                0, 72, 1);
        buttonParams.setMargins(4, 4, 4, 4);
        viewButton.setLayoutParams(buttonParams);
        row.addView(viewButton);

        return row;
    }

    /**
     * 创建表格单元格
     */
    private TextView createTableCell(String text, int weight) {
        TextView textView = new TextView(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, 72, weight);
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTextSize(11);
        textView.setTextColor(0xFF212121);
        textView.setGravity(android.view.Gravity.CENTER);
        return textView;
    }

    /**
     * 创建查看按钮
     */
    private Button createViewButton(String farmerName) {
        Button button = new Button(getContext());
        button.setText("查看");
        button.setTextSize(10);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(0xFF2196F3);

        // 设置点击监听器
        button.setOnClickListener(v -> {
            showFarmerDetailDialog(farmerName);
        });

        return button;
    }

    /**
     * 显示烟农详细信息Dialog
     */
    private void showFarmerDetailDialog(String farmerName) {
        if (getContext() == null || viewModel == null)
            return;

        // 获取烟农统计数据
        java.util.Map<String, FarmerStatistics> allStatistics = viewModel.getAllFarmerStatistics();
        FarmerStatistics stats = allStatistics.get(farmerName);

        if (stats == null) {
            Toast.makeText(getContext(), "未找到该烟农的详细信息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建Dialog
        Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_farmer_detail);
        dialog.setCancelable(true);

        // 获取Dialog中的控件
        TextView tvFarmerName = dialog.findViewById(R.id.tv_detail_farmer_name);
        TextView tvContractAmount = dialog.findViewById(R.id.tv_detail_contract_amount);
        TextView tvBundleCount = dialog.findViewById(R.id.tv_detail_bundle_count);
        TextView tvTotalWeight = dialog.findViewById(R.id.tv_detail_total_weight);
        TextView tvUpperRatio = dialog.findViewById(R.id.tv_detail_upper_ratio);
        TextView tvMiddleRatio = dialog.findViewById(R.id.tv_detail_middle_ratio);
        TextView tvLowerRatio = dialog.findViewById(R.id.tv_detail_lower_ratio);
        TextView tvTime = dialog.findViewById(R.id.tv_detail_time);
        Button btnClose = dialog.findViewById(R.id.btn_close_dialog);

        // 设置数据
        tvFarmerName.setText(farmerName);
        tvContractAmount.setText("100.0 kg"); // 可以从ViewModel获取实际合同量
        tvBundleCount.setText(String.valueOf(stats.getTotalBundles()));
        tvTotalWeight.setText(String.format("%.2f kg", stats.getTotalWeight()));

        // 获取各部叶比例（从ViewModel获取）
        String upperRatio = viewModel.getUpperRatio().getValue();
        String middleRatio = viewModel.getMiddleRatio().getValue();
        String lowerRatio = viewModel.getLowerRatio().getValue();

        tvUpperRatio.setText(upperRatio != null ? upperRatio : "0.0%");
        tvMiddleRatio.setText(middleRatio != null ? middleRatio : "0.0%");
        tvLowerRatio.setText(lowerRatio != null ? lowerRatio : "0.0%");

        // 设置当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        tvTime.setText(sdf.format(new Date()));

        // 设置关闭按钮点击事件
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 显示Dialog
        dialog.show();

        // 设置Dialog窗口大小
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.8),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * 选择等级按钮并更新视觉状态
     */
    private void selectLevelButton(Button selectedButton, String level) {
        // 重置所有按钮状态
        resetLevelButtons();

        // 设置选中状态
        if (selectedButton != null) {
            selectedButton.setSelected(true);
            selectedButton.setAlpha(0.8f); // 视觉反馈
        }

        Toast.makeText(getContext(), "✅ 已选择: " + level, Toast.LENGTH_SHORT).show();
    }

    /**
     * 重置等级按钮状态
     */
    private void resetLevelButtons() {
        if (btnUpperLevel != null) {
            btnUpperLevel.setSelected(false);
            btnUpperLevel.setAlpha(1.0f);
        }
        if (btnMiddleLevel != null) {
            btnMiddleLevel.setSelected(false);
            btnMiddleLevel.setAlpha(1.0f);
        }
        if (btnLowerLevel != null) {
            btnLowerLevel.setSelected(false);
            btnLowerLevel.setAlpha(1.0f);
        }
    }
}