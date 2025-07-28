package com.tobacco.weight.ui.settings;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.tobacco.weight.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 设置Activity
 * 处理应用设置
 */
@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // 设置标题栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_settings);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 