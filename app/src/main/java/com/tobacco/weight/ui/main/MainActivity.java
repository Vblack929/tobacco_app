package com.tobacco.weight.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.tobacco.weight.R;
import com.tobacco.weight.ui.admin.AdminActivity;
import com.tobacco.weight.ui.main.WeightingFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 主活动
 * 承载称重界面Fragment
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏和保持屏幕常亮
        setupScreenSettings();

        setContentView(R.layout.activity_main);

        // 设置ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("烟叶称重系统");
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        // 加载称重界面Fragment
        if (savedInstanceState == null) {
            loadWeightingFragment();
        }
    }

    /**
     * 配置屏幕设置
     */
    private void setupScreenSettings() {
        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 设置全屏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * 加载称重界面Fragment
     */
    private void loadWeightingFragment() {
        WeightingFragment fragment = new WeightingFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_admin) {
            // 打开管理员界面
            Intent intent = new Intent(this, AdminActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}