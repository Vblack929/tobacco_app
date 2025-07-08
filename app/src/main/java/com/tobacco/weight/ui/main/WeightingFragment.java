package com.tobacco.weight.ui.main;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tobacco.weight.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 称重界面Fragment
 * 负责UI展示和用户交互
 */
@AndroidEntryPoint
public class WeightingFragment extends Fragment {
    
    private WeightingViewModel viewModel;
    private MainViewModel mainViewModel; // 添加MainViewModel用于ID card功能
    
    // UI控件
    private TextView tvFarmerName;
    private TextView tvContractNumber;
    private TextView tvCurrentWeight;
    private TextView tvDeviceStatus;
    private TextView tvPrecheckLevel;
    private TextView tvCurrentTime;
    private TextView tvStatusMessage;
    
    // 操作按钮
    private Button btnReadIdCard;
    private Button btnTare;
    private Button btnPrint;
    private Button btnUpperLevel;
    private Button btnMiddleLevel;
    private Button btnLowerLevel;
    
    // 等级选择按钮
    private Button btnSelectA;
    private Button btnSelectB;
    private Button btnSelectC;
    private Button btnSelectD;
    
    // 价格输入
    private EditText etPriceA;
    private EditText etPriceB;
    private EditText etPriceC;
    private EditText etPriceD;
    private EditText etActualPriceA;
    private EditText etActualPriceB;
    private EditText etActualPriceC;
    private EditText etActualPriceD;
    
    // 测试按钮
    private Button btnTest5kg;
    private Button btnTest10kg;
    private Button btnTest20kg;
    
    // 其他控件
    private EditText etContractNumber;
    private EditText etPrice;
    private TextView tvSettlementInfo;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weighing, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化ViewModels
        viewModel = new ViewModelProvider(this).get(WeightingViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class); // Activity-scoped for ID card data
        
        // 初始化UI控件
        initializeViews(view);
        
        // 设置监听器
        setupClickListeners();
        
        // 设置文本变化监听
        setupTextWatchers();
        
        // 观察ViewModel数据
        observeViewModel();
        
        // 观察ID card数据 - 仅显示姓名
        observeIdCardData();
    }
    
    /**
     * 初始化UI控件
     */
    private void initializeViews(View view) {
        // 信息显示
        tvFarmerName = view.findViewById(R.id.tv_farmer_name);
        tvContractNumber = view.findViewById(R.id.tv_contract_number);
        tvCurrentWeight = view.findViewById(R.id.tv_current_weight);
        tvDeviceStatus = view.findViewById(R.id.tv_device_status);
        tvPrecheckLevel = view.findViewById(R.id.tv_precheck_level);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvSettlementInfo = view.findViewById(R.id.tv_settlement_info);
        
        // 操作按钮
        btnReadIdCard = view.findViewById(R.id.btn_read_id_card);
        btnTare = view.findViewById(R.id.btn_tare);
        btnPrint = view.findViewById(R.id.btn_print);
        btnUpperLevel = view.findViewById(R.id.btn_upper_level);
        btnMiddleLevel = view.findViewById(R.id.btn_middle_level);
        btnLowerLevel = view.findViewById(R.id.btn_lower_level);
        
        // 等级选择按钮
        btnSelectA = view.findViewById(R.id.btn_select_a);
        btnSelectB = view.findViewById(R.id.btn_select_b);
        btnSelectC = view.findViewById(R.id.btn_select_c);
        btnSelectD = view.findViewById(R.id.btn_select_d);
        
        // 价格输入
        etPriceA = view.findViewById(R.id.et_price_a);
        etPriceB = view.findViewById(R.id.et_price_b);
        etPriceC = view.findViewById(R.id.et_price_c);
        etPriceD = view.findViewById(R.id.et_price_d);
        etActualPriceA = view.findViewById(R.id.et_actual_price_a);
        etActualPriceB = view.findViewById(R.id.et_actual_price_b);
        etActualPriceC = view.findViewById(R.id.et_actual_price_c);
        etActualPriceD = view.findViewById(R.id.et_actual_price_d);
        
        // 测试按钮
        btnTest5kg = view.findViewById(R.id.btn_test_weight_5kg);
        btnTest10kg = view.findViewById(R.id.btn_test_weight_10kg);
        btnTest20kg = view.findViewById(R.id.btn_test_weight_20kg);
        
        // 其他控件
        etContractNumber = view.findViewById(R.id.et_contract_number);
        etPrice = view.findViewById(R.id.et_price);
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 读取身份证 - 使用MainViewModel的真实方法
        btnReadIdCard.setOnClickListener(v -> {
            showToast("正在连接身份证读卡器...");
            mainViewModel.connectIdCardReader(); // 使用真实的ID card功能
        });
        
        // 去皮重
        btnTare.setOnClickListener(v -> {
            showToast("执行去皮重操作");
            viewModel.performTare();
        });
        
        // 打印标签
        btnPrint.setOnClickListener(v -> {
            showToast("开始打印标签");
            viewModel.printLabel();
        });
        
        // 烟叶等级选择
        btnUpperLevel.setOnClickListener(v -> {
            selectLevelButton(btnUpperLevel, "上等烟");
            viewModel.selectLevel("上等烟");
        });
        
        btnMiddleLevel.setOnClickListener(v -> {
            selectLevelButton(btnMiddleLevel, "中等烟");
            viewModel.selectLevel("中等烟");
        });
        
        btnLowerLevel.setOnClickListener(v -> {
            selectLevelButton(btnLowerLevel, "下等烟");
            viewModel.selectLevel("下等烟");
        });
        
        // 等级选择按钮
        btnSelectA.setOnClickListener(v -> {
            selectGradeButton(btnSelectA, "A");
            viewModel.selectLevel("A级");
        });
        
        btnSelectB.setOnClickListener(v -> {
            selectGradeButton(btnSelectB, "B");
            viewModel.selectLevel("B级");
        });
        
        btnSelectC.setOnClickListener(v -> {
            selectGradeButton(btnSelectC, "C");
            viewModel.selectLevel("C级");
        });
        
        btnSelectD.setOnClickListener(v -> {
            selectGradeButton(btnSelectD, "D");
            viewModel.selectLevel("D级");
        });
        
        // 测试按钮
        btnTest5kg.setOnClickListener(v -> {
            showToast("模拟5kg重量");
            viewModel.simulateWeight(5.0);
        });
        
        btnTest10kg.setOnClickListener(v -> {
            showToast("模拟10kg重量");
            viewModel.simulateWeight(10.0);
        });
        
        btnTest20kg.setOnClickListener(v -> {
            showToast("模拟20kg重量");
            viewModel.simulateWeight(20.0);
        });
    }
    
    /**
     * 设置文本变化监听
     */
    private void setupTextWatchers() {
        // 价格输入监听
        etPriceA.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setPriceA(s.toString());
            }
        });
        
        etPriceB.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setPriceB(s.toString());
            }
        });
        
        etPriceC.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setPriceC(s.toString());
            }
        });
        
        etPriceD.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                viewModel.setPriceD(s.toString());
            }
        });
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private void observeViewModel() {
        // 农户姓名 - 现在使用MainViewModel的真实ID card数据
        // (移除WeightingViewModel的模拟数据，使用真实数据)
        
        // 合同号
        viewModel.getContractNumber().observe(getViewLifecycleOwner(), contractNumber -> {
            tvContractNumber.setText(contractNumber);
        });
        
        // 当前重量
        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            tvCurrentWeight.setText(weight);
        });
        
        // 设备状态
        viewModel.getDeviceStatus().observe(getViewLifecycleOwner(), status -> {
            tvDeviceStatus.setText(status);
        });
        
        // 预检码等级
        viewModel.getPrecheckLevel().observe(getViewLifecycleOwner(), level -> {
            tvPrecheckLevel.setText(level);
        });
        
        // 当前时间
        viewModel.getCurrentTime().observe(getViewLifecycleOwner(), time -> {
            tvCurrentTime.setText(time);
        });
        
        // 状态消息
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            showToast(message);
        });
        
        // 重量稳定性
        viewModel.getIsWeightStable().observe(getViewLifecycleOwner(), isStable -> {
            // 更新重量显示的背景色
            if (isStable) {
                tvCurrentWeight.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                tvCurrentWeight.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
            }
        });
        
        // 价格数据
        viewModel.getPriceA().observe(getViewLifecycleOwner(), price -> {
            if (!etPriceA.getText().toString().equals(price)) {
                etPriceA.setText(price);
            }
        });
        
        viewModel.getPriceB().observe(getViewLifecycleOwner(), price -> {
            if (!etPriceB.getText().toString().equals(price)) {
                etPriceB.setText(price);
            }
        });
        
        viewModel.getPriceC().observe(getViewLifecycleOwner(), price -> {
            if (!etPriceC.getText().toString().equals(price)) {
                etPriceC.setText(price);
            }
        });
        
        viewModel.getPriceD().observe(getViewLifecycleOwner(), price -> {
            if (!etPriceD.getText().toString().equals(price)) {
                etPriceD.setText(price);
            }
        });
        
        // 设备连接状态
        viewModel.getScaleConnected().observe(getViewLifecycleOwner(), connected -> {
            // 可以根据连接状态更新UI
        });
        
        viewModel.getPrinterConnected().observe(getViewLifecycleOwner(), connected -> {
            btnPrint.setEnabled(connected);
        });
    }
    
    /**
     * 观察ID card数据 - 仅显示姓名
     */
    private void observeIdCardData() {
        // 农户姓名 - 从MainViewModel的真实ID card数据获取
        mainViewModel.getFarmerName().observe(getViewLifecycleOwner(), name -> {
            if (name != null && !name.trim().isEmpty()) {
                tvFarmerName.setText(name);
                showToast("✅ 身份证读取成功: " + name);
            } else {
                tvFarmerName.setText("未读取");
            }
        });
        
        // ID card连接状态 - 更新按钮状态
        mainViewModel.getIdCardConnected().observe(getViewLifecycleOwner(), connected -> {
            btnReadIdCard.setEnabled(connected);
            if (connected) {
                btnReadIdCard.setText("读取身份证");
            } else {
                btnReadIdCard.setText("读卡器未连接");
            }
        });
    }
    
    /**
     * 选择等级按钮
     */
    private void selectLevelButton(Button selectedButton, String level) {
        // 重置所有按钮状态
        resetLevelButtons();
        
        // 设置选中状态
        selectedButton.setSelected(true);
        selectedButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        
        showToast("已选择: " + level);
    }
    
    /**
     * 选择等级按钮
     */
    private void selectGradeButton(Button selectedButton, String grade) {
        // 重置所有等级按钮状态
        resetGradeButtons();
        
        // 设置选中状态
        selectedButton.setSelected(true);
        selectedButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        
        showToast("已选择等级: " + grade);
    }
    
    /**
     * 重置等级按钮状态
     */
    private void resetLevelButtons() {
        btnUpperLevel.setSelected(false);
        btnMiddleLevel.setSelected(false);
        btnLowerLevel.setSelected(false);
        
        btnUpperLevel.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        btnMiddleLevel.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        btnLowerLevel.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }
    
    /**
     * 重置等级按钮状态
     */
    private void resetGradeButtons() {
        btnSelectA.setSelected(false);
        btnSelectB.setSelected(false);
        btnSelectC.setSelected(false);
        btnSelectD.setSelected(false);
        
        btnSelectA.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        btnSelectB.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        btnSelectC.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        btnSelectD.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 简化的TextWatcher
     */
    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        
        @Override
        public void afterTextChanged(Editable s) {}
    }
} 