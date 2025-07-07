package com.tobacco.weight.ui.precheck;

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
import com.tobacco.weight.databinding.FragmentPrecheckBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 预检Fragment
 * 处理烟叶质量预检
 */
@AndroidEntryPoint
public class PrecheckFragment extends Fragment {
    
    private FragmentPrecheckBinding binding;
    private PrecheckViewModel viewModel;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_precheck, container, false);
        viewModel = new ViewModelProvider(this).get(PrecheckViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);
        
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupObservers();
    }
    
    private void setupObservers() {
        // 观察预检结果
        viewModel.getPrecheckResult().observe(getViewLifecycleOwner(), result -> {
            // 更新UI显示预检结果
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.unbind();
        }
    }
} 