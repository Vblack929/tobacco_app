package com.tobacco.weight.ui.records;

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
import com.tobacco.weight.databinding.FragmentRecordsBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 记录Fragment
 * 显示和管理称重记录
 */
@AndroidEntryPoint
public class RecordsFragment extends Fragment {
    
    private FragmentRecordsBinding binding;
    private RecordsViewModel viewModel;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_records, container, false);
        viewModel = new ViewModelProvider(this).get(RecordsViewModel.class);
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
        // 观察记录列表
        viewModel.getRecords().observe(getViewLifecycleOwner(), records -> {
            // 更新记录列表UI
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