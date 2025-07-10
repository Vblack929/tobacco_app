package com.tobacco.weight.ui.main;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.tobacco.weight.hardware.idcard.IdCardData;
import com.tobacco.weight.hardware.printer.PrinterManager;
import com.tobacco.weight.hardware.printer.LabelData;
import com.tobacco.weight.ui.admin.AdminActivity;
import com.tobacco.weight.utils.DataExportUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 称重界面Fragment
 * 主要负责称重操作界面，不再包含管理员功能
 */
@AndroidEntryPoint
public class WeightingFragment extends Fragment {

    private WeightingViewModel viewModel;
    private MainViewModel mainViewModel;

    // 注入打印机管理器
    @Inject
    PrinterManager printerManager;

    // UI组件
    private EditText etFarmerName;
    private TextView tvContractNumber;
    private TextView tvCurrentWeight;
    private Button btnUpperLevel;
    private Button btnMiddleLevel;
    private Button btnLowerLevel;
    private Button btnConfirm;
    private Button btnPrint;
    private Button btnReadIdCard;
    private Button btnOpenAdminInterface;

    // 预检比例输入框
    private EditText etPrecheckRatio;
    private EditText etUpperRatio;
    private EditText etMiddleRatio;
    private EditText etLowerRatio;

    // 预检信息显示
    private TextView tvPrecheckId;
    private TextView tvPrecheckDate;

    // 数据库状态
    private TextView tvDatabaseCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weighing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(WeightingViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 初始化UI
        initializeBasicViews(view);
        setupBasicClickListeners();

        // 观察数据变化
        observeBasicViewModel();
        observeIdCardData();

        // 设置打印机状态监听
        if (printerManager != null) {
            printerManager.setCallback(new PrinterManager.PrinterCallback() {
                @Override
                public void onConnectionSuccess(String devicePath) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyConnectionSuccess(devicePath);
                        });
                    }
                }

                @Override
                public void onConnectionFailed(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyConnectionFailed(error);
                        });
                    }
                }

                @Override
                public void onPrintComplete() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Print completed successfully
                            viewModel.notifyPrintStatusUpdate("打印完成");
                        });
                    }
                }

                @Override
                public void onPrintError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyPrintFailure("打印失败", "标签打印过程中出现错误", error);
                        });
                    }
                }

                @Override
                public void onStatusUpdate(String status) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyPrintStatusUpdate(status);
                        });
                    }
                }
            });
        }
    }

    /**
     * 观察身份证数据变化
     */
    private void observeIdCardData() {
        if (mainViewModel != null) {
            mainViewModel.getIdCardData().observe(getViewLifecycleOwner(), idCardData -> {
                if (idCardData != null) {
                    updateUIWithIdCardData(idCardData);
                }
            });

            // 观察身份证读取状态 (commented out - method doesn't exist)
            // mainViewModel.getIdCardReadStatus().observe(getViewLifecycleOwner(), status
            // -> {
            // if (status != null) {
            // Log.d("WeightingFragment", "ID Card Read Status: " + status);
            // }
            // });
        }
    }

    /**
     * 使用身份证数据更新UI
     */
    private void updateUIWithIdCardData(IdCardData idCardData) {
        if (etFarmerName != null && idCardData.getName() != null) {
            etFarmerName.setText(idCardData.getName());
        }

        if (viewModel != null) {
            viewModel.onRealIdCardDataReceived(idCardData);
        }

        Toast.makeText(getContext(),
                "✅ 身份证读取成功: " + idCardData.getName(),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * 观察基本数据变化
     */
    private void observeBasicViewModel() {
        if (viewModel != null) {
            // 观察当前重量
            viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
                if (tvCurrentWeight != null && weight != null) {
                    tvCurrentWeight.setText(weight); // Already formatted as "5.00 kg"
                }
            });

            // 观察合同号
            viewModel.getContractNumber().observe(getViewLifecycleOwner(), contractNumber -> {
                if (tvContractNumber != null && contractNumber != null) {
                    tvContractNumber.setText(contractNumber);
                }
            });

            // 观察预检比例
            viewModel.getPrecheckRatio().observe(getViewLifecycleOwner(), ratio -> {
                if (etPrecheckRatio != null && ratio != null) {
                    etPrecheckRatio.setText(ratio); // Already formatted as "4.0%"
                }
            });

            // 观察各部叶预检比例 - 新增观察者
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

            // 观察预检ID
            viewModel.getCurrentPrecheckId().observe(getViewLifecycleOwner(), precheckId -> {
                if (tvPrecheckId != null && precheckId != null) {
                    tvPrecheckId.setText(precheckId);
                }
            });

            // 观察预检日期
            viewModel.getCurrentPrecheckDate().observe(getViewLifecycleOwner(), precheckDate -> {
                if (tvPrecheckDate != null && precheckDate != null) {
                    tvPrecheckDate.setText(precheckDate);
                }
            });

            // 观察打印事件
            viewModel.getPrintEvent().observe(getViewLifecycleOwner(), this::handlePrintEvent);
        }
    }

    /**
     * 处理打印事件
     */
    private void handlePrintEvent(WeightingViewModel.PrintEvent event) {
        if (event == null)
            return;

        switch (event.getType()) {
            case PRINT_SUCCESS:
                WeightingViewModel.PrintData printData = event.getPrintData();
                if (printData != null) {
                    showPrintSuccessDialog(
                            printData.getFarmerName(),
                            printData.getTobaccoLevel(),
                            printData.getPrecheckId(),
                            printData.getPrintDate());
                }
                break;
            case PRINT_FAILURE:
                showPrintFailureDialog(
                        "打印错误",
                        event.getMessage(),
                        event.getDetails(),
                        event.getPrintData() != null ? event.getPrintData().getPrecheckId() : "未知");
                break;
        }
    }

    /**
     * 初始化基本视图
     */
    private void initializeBasicViews(View view) {
        // 初始化基本UI组件
        etFarmerName = view.findViewById(R.id.et_farmer_name);
        tvContractNumber = view.findViewById(R.id.tv_contract_number);
        tvCurrentWeight = view.findViewById(R.id.tv_current_weight);
        btnUpperLevel = view.findViewById(R.id.btn_upper_level);
        btnMiddleLevel = view.findViewById(R.id.btn_middle_level);
        btnLowerLevel = view.findViewById(R.id.btn_lower_level);
        btnConfirm = view.findViewById(R.id.btn_confirm);
        btnPrint = view.findViewById(R.id.btn_print);
        btnReadIdCard = view.findViewById(R.id.btn_read_id_card);
        btnOpenAdminInterface = view.findViewById(R.id.btn_open_admin_interface);

        // 初始化预检比例输入框
        etPrecheckRatio = view.findViewById(R.id.et_precheck_ratio);
        etUpperRatio = view.findViewById(R.id.et_upper_ratio);
        etMiddleRatio = view.findViewById(R.id.et_middle_ratio);
        etLowerRatio = view.findViewById(R.id.et_lower_ratio);

        // 初始化预检信息显示
        tvPrecheckId = view.findViewById(R.id.tv_precheck_id);
        tvPrecheckDate = view.findViewById(R.id.tv_precheck_date);

        // 禁用预检比例编辑
        if (etPrecheckRatio != null) {
            etPrecheckRatio.setFocusable(false);
            etPrecheckRatio.setClickable(false);
            etPrecheckRatio.setText("4.0%");
        }
        if (etUpperRatio != null) {
            etUpperRatio.setFocusable(false);
            etUpperRatio.setClickable(false);
            // 移除硬编码，将从ViewModel动态获取
        }
        if (etMiddleRatio != null) {
            etMiddleRatio.setFocusable(false);
            etMiddleRatio.setClickable(false);
            // 移除硬编码，将从ViewModel动态获取
        }
        if (etLowerRatio != null) {
            etLowerRatio.setFocusable(false);
            etLowerRatio.setClickable(false);
            // 移除硬编码，将从ViewModel动态获取
        }

        // 初始化数据库状态控件
        tvDatabaseCount = view.findViewById(R.id.tv_database_count);

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

                    Toast.makeText(getContext(), "✅ 称重记录已保存", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 打印按钮点击事件
        if (btnPrint != null) {
            btnPrint.setOnClickListener(v -> {
                printCurrentRecord();
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

        // 管理员界面按钮点击事件
        if (btnOpenAdminInterface != null) {
            btnOpenAdminInterface.setOnClickListener(v -> {
                openAdminInterface();
            });
        }

        // 长按打印按钮快速切换模式
        setupPrintButtonLongClick();
    }

    /**
     * 打开管理员界面
     */
    private void openAdminInterface() {
        Intent intent = new Intent(getContext(), AdminActivity.class);
        startActivity(intent);
    }

    /**
     * 打印当前记录 - 使用ViewModel架构
     */
    private void printCurrentRecord() {
        if (viewModel == null) {
            Toast.makeText(getContext(), "系统未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        // 从UI获取农户姓名
        String farmerNameFromUI = etFarmerName != null ? etFarmerName.getText().toString().trim() : "";

        // 使用ViewModel准备打印数据
        WeightingViewModel.PrintData printData = viewModel.preparePrintData(farmerNameFromUI);

        // 验证数据
        if (!viewModel.validatePrintData(printData)) {
            Toast.makeText(getContext(), "打印数据验证失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示打印开始提示
        Toast.makeText(getContext(), "正在准备打印标签...", Toast.LENGTH_SHORT).show();

        try {
            // 设置打印机回调，通过ViewModel传递事件
            printerManager.setCallback(new PrinterManager.PrinterCallback() {
                @Override
                public void onConnectionSuccess(String devicePath) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyConnectionSuccess(devicePath);
                        });
                    }
                }

                @Override
                public void onConnectionFailed(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyConnectionFailed(error);
                        });
                    }
                }

                @Override
                public void onPrintComplete() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // Print completed successfully
                            viewModel.notifyPrintStatusUpdate("打印完成");
                        });
                    }
                }

                @Override
                public void onPrintError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyPrintFailure("打印失败", "标签打印过程中出现错误", error);
                        });
                    }
                }

                @Override
                public void onStatusUpdate(String status) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            viewModel.notifyPrintStatusUpdate(status);
                        });
                    }
                }
            });

            // 创建标签数据
            LabelData labelData = LabelData.createTobaccoWeighingLabel(
                    printData.getFarmerName(),
                    printData.getPrecheckId(),
                    printData.getTobaccoLevel(),
                    printData.getPrintDate(),
                    printData.getContractNumber());

            // 执行打印
            printerManager.printLabel(labelData);

        } catch (Exception e) {
            viewModel.notifyPrintFailure("系统错误", "打印系统出现异常", e.getMessage());
        }
    }

    /**
     * 显示打印成功界面
     */
    private void showPrintSuccessDialog(String farmerName, String tobaccoLevel, String precheckId, String printDate) {
        if (getContext() == null)
            return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                getContext());

        // 设置标题和图标
        builder.setTitle("✅ 打印成功");
        builder.setIcon(android.R.drawable.ic_dialog_info);

        // 构建详细信息
        StringBuilder message = new StringBuilder();
        message.append("烟叶称重标签已成功打印！\n\n");
        message.append("📋 打印详情:\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append("🧑 农户姓名: ").append(farmerName).append("\n");
        message.append("🌿 烟叶等级: ").append(tobaccoLevel).append("\n");
        message.append("🏷️  预检编号: ").append(precheckId).append("\n");
        message.append("📅 打印时间: ").append(printDate).append("\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        message.append("✨ 标签包含条形码和二维码，方便后续扫描识别。");

        builder.setMessage(message.toString());

        // 设置按钮
        builder.setPositiveButton("继续称重", (dialog, which) -> {
            dialog.dismiss();
            // 可选：重置界面为下一次称重做准备
            resetForNextWeighing();
        });

        builder.setNeutralButton("重新打印", (dialog, which) -> {
            dialog.dismiss();
            // 重新调用打印功能
            printCurrentRecord();
        });

        builder.setNegativeButton("关闭", (dialog, which) -> {
            dialog.dismiss();
        });

        // 显示对话框
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // 设置消息文本样式
        if (dialog.findViewById(android.R.id.message) != null) {
            TextView messageView = dialog.findViewById(android.R.id.message);
            messageView.setTextSize(14);
            messageView.setLineSpacing(1.2f, 1.0f);
        }
    }

    /**
     * 显示打印失败界面
     */
    private void showPrintFailureDialog(String errorType, String errorMessage, String errorDetails, String precheckId) {
        if (getContext() == null)
            return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                getContext());

        // 设置标题和图标
        builder.setTitle("❌ " + errorType);
        builder.setIcon(android.R.drawable.ic_dialog_alert);

        // 构建错误信息
        StringBuilder message = new StringBuilder();
        message.append(errorMessage).append("\n\n");
        message.append("🔍 错误详情:\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append(errorDetails != null ? errorDetails : "未知错误").append("\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        if (precheckId != null && !precheckId.equals("未生成")) {
            message.append("📋 相关记录: ").append(precheckId).append("\n\n");
        }

        message.append("💡 建议解决方案:\n");
        message.append("• 检查打印机电源和连接线\n");
        message.append("• 确认打印机纸张充足\n");
        message.append("• 检查USB连接是否稳定\n");
        message.append("• 尝试重新连接打印机");

        builder.setMessage(message.toString());

        // 设置按钮
        builder.setPositiveButton("重试打印", (dialog, which) -> {
            dialog.dismiss();
            // 重新尝试打印
            printCurrentRecord();
        });

        builder.setNeutralButton("检查设置", (dialog, which) -> {
            dialog.dismiss();
            Toast.makeText(getContext(), "请检查打印机设置和连接", Toast.LENGTH_LONG).show();
            // 这里可以跳转到设置页面
        });

        builder.setNegativeButton("关闭", (dialog, which) -> {
            dialog.dismiss();
        });

        // 显示对话框
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        // 设置消息文本样式
        if (dialog.findViewById(android.R.id.message) != null) {
            TextView messageView = dialog.findViewById(android.R.id.message);
            messageView.setTextSize(14);
            messageView.setLineSpacing(1.2f, 1.0f);
            messageView.setTextColor(0xFF424242); // 深灰色更易读
        }
    }

    /**
     * 重置界面为下一次称重做准备
     */
    private void resetForNextWeighing() {
        // 重置等级选择
        resetLevelButtons();

        // 可选：清空农户姓名（根据业务需求决定）
        // if (etFarmerName != null) {
        // etFarmerName.setText("");
        // }

        // 生成新的合同号
        if (viewModel != null) {
            viewModel.generateNewContractNumber();
        }

        Toast.makeText(getContext(), "准备进行下一次称重", Toast.LENGTH_SHORT).show();
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

    /**
     * 快速切换到测试模式
     */
    public void quickEnableTestMode() {
        if (viewModel != null) {
            viewModel.enableTestMode();
            Toast.makeText(getContext(), "🧪 已切换到测试模式", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 快速切换到真实模式
     */
    public void quickEnableRealMode() {
        if (viewModel != null) {
            viewModel.enableRealMode();
            Toast.makeText(getContext(), "🔧 已切换到真实模式", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换打印模式
     */
    public void togglePrintMode() {
        if (viewModel != null) {
            viewModel.toggleTestMode();
        }
    }

    /**
     * 获取当前打印模式状态
     */
    public String getCurrentPrintModeStatus() {
        if (viewModel != null) {
            boolean isTestMode = Boolean.TRUE.equals(viewModel.getIsTestMode().getValue());
            return isTestMode ? "🧪 当前: 测试模式" : "🔧 当前: 真实模式";
        }
        return "❓ 模式未知";
    }

    /**
     * 显示当前打印模式
     */
    public void showCurrentPrintMode() {
        String status = getCurrentPrintModeStatus();
        Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
    }

    /**
     * 测试所有打印模式切换（开发调试用）
     */
    public void testAllPrintModes() {
        if (viewModel == null)
            return;

        // Show current mode
        showCurrentPrintMode();

        // Test mode sequence after delays
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            quickEnableTestMode();

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                quickEnableRealMode();

                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    quickEnableTestMode(); // Back to test mode
                }, 1500);
            }, 1500);
        }, 1000);
    }

    /**
     * 长按打印按钮快速切换模式
     */
    private void setupPrintButtonLongClick() {
        if (btnPrint != null) {
            btnPrint.setOnLongClickListener(v -> {
                togglePrintMode();
                return true; // Consume the event
            });
        }
    }
}