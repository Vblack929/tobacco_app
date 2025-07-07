package com.tobacco.weight.ui.weighing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.tobacco.weight.R;
import com.tobacco.weight.databinding.FragmentWeighingBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 称重Fragment
 * 处理实时称重显示和操作
 */
@AndroidEntryPoint
public class WeighingFragment extends Fragment {

    private FragmentWeighingBinding binding;
    private WeighingViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_weighing, container, false);
        viewModel = new ViewModelProvider(this).get(WeighingViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupObservers();
        setupClickListeners();

        // 验证按钮状态
        validateButtonStates();
    }

    private void validateButtonStates() {
        android.util.Log.d("WeighingFragment", "验证按钮状态:");
        android.util.Log.d("WeighingFragment", "轻重量按钮 - 可用: " + binding.btnSimulateLight.isEnabled() + ", 可点击: "
                + binding.btnSimulateLight.isClickable());
        android.util.Log.d("WeighingFragment", "重重量按钮 - 可用: " + binding.btnSimulateHeavy.isEnabled() + ", 可点击: "
                + binding.btnSimulateHeavy.isClickable());
        android.util.Log.d("WeighingFragment", "重置按钮 - 可用: " + binding.btnResetPrecheck.isEnabled() + ", 可点击: "
                + binding.btnResetPrecheck.isClickable());

        // 强制设置按钮为可用状态
        if (!binding.btnSimulateLight.isEnabled() || !binding.btnSimulateLight.isClickable()) {
            binding.btnSimulateLight.setEnabled(true);
            binding.btnSimulateLight.setClickable(true);
            android.util.Log.d("WeighingFragment", "强制启用轻重量按钮");
        }

        if (!binding.btnSimulateHeavy.isEnabled() || !binding.btnSimulateHeavy.isClickable()) {
            binding.btnSimulateHeavy.setEnabled(true);
            binding.btnSimulateHeavy.setClickable(true);
            android.util.Log.d("WeighingFragment", "强制启用重重量按钮");
        }

        if (!binding.btnResetPrecheck.isEnabled() || !binding.btnResetPrecheck.isClickable()) {
            binding.btnResetPrecheck.setEnabled(true);
            binding.btnResetPrecheck.setClickable(true);
            android.util.Log.d("WeighingFragment", "强制启用重置按钮");
        }
    }

    private void setupObservers() {
        // 观察称重数据
        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            if (weight != null) {
                binding.tvCurrentWeight.setText(String.format("%.2f kg", weight));
            }
        });

        // 观察称重状态
        viewModel.getWeighingState().observe(getViewLifecycleOwner(), state -> {
            updateWeighingUI(state);
        });

        // 观察状态消息
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                // 可以显示Toast或更新状态文本
                android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // 观察预检比例
        viewModel.getPrecheckRatio().observe(getViewLifecycleOwner(), ratio -> {
            if (ratio != null) {
                binding.tvPrecheckRatio.setText(ratio);
            }
        });

        // 观察预检重量
        viewModel.getPrecheckWeight().observe(getViewLifecycleOwner(), weight -> {
            if (weight != null) {
                binding.tvPrecheckWeight.setText(weight);
            }
        });
    }

    private void setupClickListeners() {
        // 使用布局中实际存在的按钮ID
        binding.btnTare.setOnClickListener(v -> viewModel.clearWeight());
        binding.btnPrint.setOnClickListener(v -> viewModel.saveRecord());
        binding.btnUpperLevel.setOnClickListener(v -> viewModel.startWeighing());
        binding.btnMiddleLevel.setOnClickListener(v -> viewModel.stopWeighing());
        binding.btnLowerLevel.setOnClickListener(v -> viewModel.saveRecord());

        // 确保模拟按钮初始状态为可用
        binding.btnSimulateLight.setEnabled(true);
        binding.btnSimulateHeavy.setEnabled(true);
        binding.btnResetPrecheck.setEnabled(true);

        // 重量模拟按钮
        binding.btnSimulateLight.setOnClickListener(v -> {
            android.util.Log.d("WeighingFragment", "轻重量按钮被点击");
            viewModel.simulateLightWeight();
            android.widget.Toast.makeText(getContext(), "模拟轻重量物品放置", android.widget.Toast.LENGTH_SHORT).show();
        });

        binding.btnSimulateHeavy.setOnClickListener(v -> {
            android.util.Log.d("WeighingFragment", "重重量按钮被点击");
            viewModel.simulateHeavyWeight();
            android.widget.Toast.makeText(getContext(), "模拟重重量物品放置", android.widget.Toast.LENGTH_SHORT).show();
        });

        // 预检数据重置按钮
        binding.btnResetPrecheck.setOnClickListener(v -> {
            android.util.Log.d("WeighingFragment", "重置按钮被点击");
            viewModel.resetPrecheckData();
            android.widget.Toast.makeText(getContext(), "预检数据已重置", android.widget.Toast.LENGTH_SHORT).show();
        });

        // 测试按钮
        if (binding.btnTestWeight5kg != null) {
            binding.btnTestWeight5kg.setOnClickListener(v -> simulateWeight(5.0));
        }
        if (binding.btnTestWeight10kg != null) {
            binding.btnTestWeight10kg.setOnClickListener(v -> simulateWeight(10.0));
        }
        if (binding.btnTestWeight20kg != null) {
            binding.btnTestWeight20kg.setOnClickListener(v -> simulateWeight(20.0));
        }
    }

    private void updateWeighingUI(WeighingViewModel.WeighingState state) {
        switch (state) {
            case IDLE:
                binding.btnUpperLevel.setEnabled(true);
                binding.btnMiddleLevel.setEnabled(false);
                binding.btnPrint.setEnabled(false);
                binding.btnTare.setEnabled(true);
                break;
            case WEIGHING:
                binding.btnUpperLevel.setEnabled(false);
                binding.btnMiddleLevel.setEnabled(true);
                binding.btnPrint.setEnabled(false);
                binding.btnTare.setEnabled(true);
                break;
            case COMPLETED:
                binding.btnUpperLevel.setEnabled(true);
                binding.btnMiddleLevel.setEnabled(false);
                binding.btnPrint.setEnabled(true);
                binding.btnTare.setEnabled(true);
                break;
        }

        // 确保模拟按钮始终可用
        binding.btnSimulateLight.setEnabled(true);
        binding.btnSimulateHeavy.setEnabled(true);
        binding.btnResetPrecheck.setEnabled(true);
    }

    private void simulateWeight(double weight) {
        // 模拟称重数据，仅用于测试
        if (viewModel != null) {
            // 这里可以调用ViewModel的方法来模拟重量
            android.widget.Toast.makeText(getContext(),
                    "模拟重量: " + weight + " kg",
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.unbind();
        }
    }
}