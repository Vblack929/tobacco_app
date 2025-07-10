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

    // 注入硬件管理器
    @Inject
    PrinterManager printerManager;
    
    @Inject
    com.tobacco.weight.hardware.idcard.IdCardManager idCardManager;

    // RxJava disposables for managing subscriptions
    private io.reactivex.rxjava3.disposables.CompositeDisposable disposables = new io.reactivex.rxjava3.disposables.CompositeDisposable();

    // UI组件
    private EditText etFarmerName;
    private EditText etIdCardNumber;  // Changed from tvContractNumber to editable ID card field
    private TextView tvCurrentWeight;
    private Button btnUpperLevel;
    private Button btnMiddleLevel;
    private Button btnLowerLevel;
    private Button btnPrint;
    private Button btnReadIdCard;
    private Button btnOpenAdminInterface;
    
    // 新增: 会话相关UI组件
    private EditText etPrecheckNumber;
    private EditText etBundleCount;
    private TextView tvSessionStatus;
    private Button btnStartSession;
    private Button btnResetSession;
    private Button btnConfirmSave;
    private Button btnCancelSession;

    // 这些控件已被移除，相关功能已迁移到右侧统计面板
    // etPrecheckRatio, etUpperRatio, etMiddleRatio, etLowerRatio
    // tvPrecheckId, tvPrecheckDate

    // 数据库状态控件已移除
    // private TextView tvDatabaseCount;
    
    // 新增：右侧统计显示控件
    private TextView tvSessionDisplayStatus;
    private TextView tvSessionPrecheckNumber;
    private TextView tvTotalBundles;
    private TextView tvTotalWeight;
    private TextView tvUpperLeafStats;
    private TextView tvMiddleLeafStats;
    private TextView tvLowerLeafStats;
    private TextView tvCurrentScaleWeight;
    private TextView tvFarmerNameDisplay;
    private TextView tvIdCardNumberDisplay;  // Changed from tvContractNumberDisplay to ID card display

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

        // 初始化并观察身份证读卡器
        initializeIdCardManager();

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
                            // Print completed successfully - notify success
                            // Note: printData may not be available in this context, will handle in printCurrentRecord()
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
     * 观察身份证数据变化 (保留用于兼容性)
     */
    private void observeIdCardData() {
        if (mainViewModel != null) {
            mainViewModel.getIdCardData().observe(getViewLifecycleOwner(), idCardData -> {
                if (idCardData != null) {
                    updateUIWithIdCardData(idCardData);
                }
            });
        }
    }

    /**
     * 初始化身份证读卡器管理器
     */
    private void initializeIdCardManager() {
        if (idCardManager != null && getContext() != null) {
            try {
                // 初始化身份证读卡器
                idCardManager.initialize(getContext());
                
                // 订阅连接状态流
                disposables.add(
                    idCardManager.connectionStream()
                        .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe(
                            this::onIdCardManagerConnectionChanged,
                            throwable -> android.util.Log.e("WeightingFragment", 
                                "ID Card connection stream error", throwable)
                        )
                );
                
                // 订阅身份证数据流
                disposables.add(
                    idCardManager.cardDataStream()
                        .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                        .subscribe(
                            this::onIdCardManagerDataReceived,
                            throwable -> android.util.Log.e("WeightingFragment", 
                                "ID Card data stream error", throwable)
                        )
                );
                
                android.util.Log.d("WeightingFragment", "✅ IdCardManager初始化完成");
                
            } catch (Exception e) {
                android.util.Log.e("WeightingFragment", "❌ IdCardManager初始化失败", e);
                // 如果真实硬件初始化失败，通知失败状态
                if (viewModel != null) {
                    viewModel.notifyIdCardConnectionFailed("硬件初始化失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 处理身份证读卡器连接状态变化
     */
    private void onIdCardManagerConnectionChanged(Boolean connected) {
        android.util.Log.d("WeightingFragment", "IdCardManager连接状态变化: " + connected);
        
        if (viewModel != null) {
            if (connected) {
                String deviceType = com.tobacco.weight.hardware.idcard.IdCardManager.isNativeLibraryAvailable() 
                    ? "真实身份证读卡器" : "身份证模拟器";
                viewModel.notifyIdCardConnectionSuccess(deviceType);
            } else {
                viewModel.notifyIdCardConnectionFailed("身份证读卡器断开连接");
            }
        }
    }

    /**
     * 处理身份证数据接收
     */
    private void onIdCardManagerDataReceived(com.tobacco.weight.hardware.idcard.IdCardData idCardData) {
        android.util.Log.d("WeightingFragment", "IdCardManager收到身份证数据: " + 
            (idCardData != null ? idCardData.getName() : "null"));
        
        if (idCardData != null && idCardData.isValid()) {
            // 更新UI
            updateUIWithIdCardData(idCardData);
            
            // 通知成功
            if (viewModel != null) {
                viewModel.notifyIdCardSuccess(idCardData);
            }
        } else {
            // 数据无效，通知失败
            if (viewModel != null) {
                viewModel.notifyIdCardFailure("数据错误", "身份证数据无效或不完整", 
                    "请确认身份证放置正确且无损坏");
            }
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
            // 触发成功事件，通过事件系统显示详细对话框
            viewModel.notifyIdCardSuccess(idCardData);
        }

        // 移除原来的简单Toast，现在通过事件系统显示详细对话框
        // Toast.makeText(getContext(),
        //         "✅ 身份证读取成功: " + idCardData.getName(),
        //         Toast.LENGTH_SHORT).show();
    }

    /**
     * 观察基本数据变化
     */
    private void observeBasicViewModel() {
        if (viewModel != null) {
            // 观察当前重量
            viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
                if (tvCurrentWeight != null && weight != null) {
                    tvCurrentWeight.setText(weight);
                }
            });

            // 观察会话状态
            viewModel.getSessionStatus().observe(getViewLifecycleOwner(), status -> {
                if (tvSessionStatus != null && status != null) {
                    tvSessionStatus.setText(status);
                }
            });
            
            // 观察会话状态，更新按钮状态和显示
            viewModel.getSessionState().observe(getViewLifecycleOwner(), state -> {
                updateSessionButtonStates(state);
                updateSessionStatisticsDisplay();
            });

            // 观察打印事件
            viewModel.getPrintEvent().observe(getViewLifecycleOwner(), this::handlePrintEvent);
            
            // 观察身份证事件
            viewModel.getIdCardEvent().observe(getViewLifecycleOwner(), this::handleIdCardEvent);
            
            // 观察当前重量显示在右侧
            viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
                if (tvCurrentScaleWeight != null && weight != null) {
                    tvCurrentScaleWeight.setText(weight);
                }
            });
            
            // 观察农户姓名显示在右侧
            viewModel.getFarmerName().observe(getViewLifecycleOwner(), name -> {
                if (tvFarmerNameDisplay != null && name != null) {
                    tvFarmerNameDisplay.setText(name);
                }
            });
            
            // 观察身份证号显示在右侧
            viewModel.getIdCardNumberInput().observe(getViewLifecycleOwner(), idCardNumber -> {
                if (tvIdCardNumberDisplay != null && idCardNumber != null) {
                    // Show masked ID card number for privacy
                    String displayText = idCardNumber.isEmpty() ? "未设置" : maskIdCardNumber(idCardNumber);
                    tvIdCardNumberDisplay.setText(displayText);
                }
            });

            // 观察农户姓名输入字段
            viewModel.getFarmerName().observe(getViewLifecycleOwner(), name -> {
                if (etFarmerName != null && name != null && !name.equals(etFarmerName.getText().toString())) {
                    etFarmerName.setText(name);
                }
            });

            // 观察当前身份证号输入字段
            viewModel.getIdCardNumberInput().observe(getViewLifecycleOwner(), idCardNumber -> {
                if (etIdCardNumber != null && idCardNumber != null && !idCardNumber.equals(etIdCardNumber.getText().toString())) {
                    etIdCardNumber.setText(idCardNumber);
                }
            });

            // 监听农户姓名输入变化
            if (etFarmerName != null) {
                etFarmerName.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (viewModel != null) {
                            viewModel.setFarmerName(s.toString());
                        }
                    }
                });
            }

            // 监听身份证号输入变化
            if (etIdCardNumber != null) {
                etIdCardNumber.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (viewModel != null) {
                            viewModel.setIdCardNumber(s.toString());
                        }
                    }
                });
            }
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
     * 处理身份证事件
     */
    private void handleIdCardEvent(WeightingViewModel.IdCardEvent event) {
        if (event == null)
            return;

        switch (event.getType()) {
            case ID_CARD_SUCCESS:
                com.tobacco.weight.hardware.idcard.IdCardData idCardData = event.getIdCardData();
                if (idCardData != null) {
                    showIdCardSuccessDialog(
                            idCardData.getName(),
                            idCardData.getIdNumber(),
                            idCardData.getGender(),
                            idCardData.getNationality());
                }
                break;
            case ID_CARD_FAILURE:
                showIdCardFailureDialog(
                        "身份证读取错误",
                        event.getMessage(),
                        event.getDetails());
                break;
            case CONNECTION_SUCCESS:
                Toast.makeText(getContext(), "✅ " + event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case CONNECTION_FAILED:
                Toast.makeText(getContext(), "❌ " + event.getMessage(), Toast.LENGTH_LONG).show();
                break;
            case STATUS_UPDATE:
                // Update UI status if needed
                break;
        }
    }

    /**
     * 初始化基本视图
     */
    private void initializeBasicViews(View view) {
        // 初始化基本UI组件
        etFarmerName = view.findViewById(R.id.et_farmer_name);
        etIdCardNumber = view.findViewById(R.id.et_id_card_number); // Initialize the new field
        tvCurrentWeight = view.findViewById(R.id.tv_current_weight);
        btnUpperLevel = view.findViewById(R.id.btn_upper_level);
        btnMiddleLevel = view.findViewById(R.id.btn_middle_level);
        btnLowerLevel = view.findViewById(R.id.btn_lower_level);
        btnPrint = view.findViewById(R.id.btn_print);
        btnReadIdCard = view.findViewById(R.id.btn_read_id_card);
        btnOpenAdminInterface = view.findViewById(R.id.btn_open_admin_interface);
        
        // 初始化会话相关UI组件
        etPrecheckNumber = view.findViewById(R.id.et_precheck_number);
        etBundleCount = view.findViewById(R.id.et_bundle_count);
        tvSessionStatus = view.findViewById(R.id.tv_session_status);
        btnStartSession = view.findViewById(R.id.btn_start_session);
        btnResetSession = view.findViewById(R.id.btn_reset_session);
        btnConfirmSave = view.findViewById(R.id.btn_confirm_save);
        btnCancelSession = view.findViewById(R.id.btn_cancel_session);

        // 这些组件已经被移除，不再需要初始化
        // etPrecheckRatio, etUpperRatio, etMiddleRatio, etLowerRatio 
        // tvPrecheckId, tvPrecheckDate 已不存在于新布局中

        // 数据库状态控件已移除，相关功能可在管理员界面查看
        // tvDatabaseCount = view.findViewById(R.id.tv_database_count);
        
        // 初始化右侧统计显示控件
        tvSessionDisplayStatus = view.findViewById(R.id.tv_session_display_status);
        tvSessionPrecheckNumber = view.findViewById(R.id.tv_session_precheck_number);
        tvTotalBundles = view.findViewById(R.id.tv_total_bundles);
        tvTotalWeight = view.findViewById(R.id.tv_total_weight);
        tvUpperLeafStats = view.findViewById(R.id.tv_upper_leaf_stats);
        tvMiddleLeafStats = view.findViewById(R.id.tv_middle_leaf_stats);
        tvLowerLeafStats = view.findViewById(R.id.tv_lower_leaf_stats);
        tvCurrentScaleWeight = view.findViewById(R.id.tv_current_scale_weight);
        tvFarmerNameDisplay = view.findViewById(R.id.tv_farmer_name_display);
        tvIdCardNumberDisplay = view.findViewById(R.id.tv_id_card_number_display);  // Use the new ID

        // 设置一些测试数据（会被ViewModel数据覆盖）
        if (etFarmerName != null) {
            etFarmerName.setText("张三");
        }
        if (etIdCardNumber != null) { // Set test data for the new field
            etIdCardNumber.setText("110101199001011234");
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
                addBundleWithLevel("上部叶");
            });
        }

        if (btnMiddleLevel != null) {
            btnMiddleLevel.setOnClickListener(v -> {
                addBundleWithLevel("中部叶");
            });
        }

        if (btnLowerLevel != null) {
            btnLowerLevel.setOnClickListener(v -> {
                addBundleWithLevel("下部叶");
            });
        }

        // 会话按钮监听器
        if (btnStartSession != null) {
            btnStartSession.setOnClickListener(v -> {
                String precheckNumber = etPrecheckNumber != null ? etPrecheckNumber.getText().toString().trim() : "";
                if (precheckNumber.isEmpty()) {
                    Toast.makeText(getContext(), "请输入预检编号", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (viewModel != null) {
                    viewModel.startWeighingSession(precheckNumber);
                }
            });
        }
        
        if (btnResetSession != null) {
            btnResetSession.setOnClickListener(v -> {
                // 重置当前会话数据
                if (viewModel != null && viewModel.getCurrentSession() != null && 
                    viewModel.getCurrentSessionState() != WeightingViewModel.SessionState.INACTIVE) {
                    // 显示确认对话框
                    new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("重置确认")
                        .setMessage("确定要重置当前会话的所有数据吗？此操作不可撤销。")
                        .setPositiveButton("确定重置", (dialog, which) -> {
                            // 执行重置操作
                            viewModel.resetSession();
                            // 清空输入框
                            if (etBundleCount != null) {
                                etBundleCount.setText("");
                            }
                            if (etPrecheckNumber != null) {
                                etPrecheckNumber.setText("");
                            }
                            // 更新统计显示
                            updateSessionStatisticsDisplay();
                            Toast.makeText(getContext(), "✅ 会话数据已重置", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                } else {
                    Toast.makeText(getContext(), "当前无活动会话", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (btnConfirmSave != null) {
            btnConfirmSave.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.confirmAndSaveSession();
                }
            });
        }
        
        if (btnCancelSession != null) {
            btnCancelSession.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.cancelWeighingSession();
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
            // 添加双击功能来切换开发模拟模式，单击执行正常读卡功能
            final long[] lastClickTime = {0};
            btnReadIdCard.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime[0] < 500) {
                    // 双击检测到，切换开发模拟模式
                    toggleDevelopmentSimulation();
                    lastClickTime[0] = 0; // 重置以避免连续触发
                } else {
                    // 单击，执行正常的读卡功能
                    lastClickTime[0] = currentTime;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (currentTime == lastClickTime[0]) {
                            // 500ms后仍是最后一次点击，执行单击动作
                            readIdCardWithFeedback();
                        }
                    }, 500);
                }
            });
            
            // 添加长按功能来显示连接状态（调试用）
            btnReadIdCard.setOnLongClickListener(v -> {
                showIdCardConnectionStatus();
                return true;
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
     * 通过选择的等级添加捆包到会话
     */
    private void addBundleWithLevel(String level) {
        // 检查是否有活动会话
        if (viewModel == null || viewModel.getCurrentSession() == null) {
            Toast.makeText(getContext(), "请先开始会话", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取捆数输入
        String bundleCountStr = etBundleCount != null ? etBundleCount.getText().toString().trim() : "";
        if (bundleCountStr.isEmpty()) {
            Toast.makeText(getContext(), "请输入捆数", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int bundleCount = Integer.parseInt(bundleCountStr);
            if (bundleCount <= 0) {
                Toast.makeText(getContext(), "捆数必须大于0", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 添加到会话
            viewModel.addToWeighingSession(level, bundleCount);
            
            // 显示添加成功的反馈
            Toast.makeText(getContext(), String.format("已添加 %d 捆 %s", bundleCount, level), Toast.LENGTH_SHORT).show();
            
            // 清空捆数输入，准备下一次输入
            if (etBundleCount != null) {
                etBundleCount.setText("");
            }
            
            // 更新右侧统计显示
            updateSessionStatisticsDisplay();
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "请输入有效的捆数", Toast.LENGTH_SHORT).show();
        }
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
            // 同步测试模式设置到打印机管理器
            Boolean isTestMode = viewModel.getIsTestMode().getValue();
            if (isTestMode != null) {
                printerManager.setTestMode(isTestMode);
            }
            
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
                            // Print completed successfully - notify success with print data
                            viewModel.notifyPrintSuccess(printData);
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
     * 显示身份证读取成功界面
     */
    private void showIdCardSuccessDialog(String farmerName, String idNumber, String gender, String nationality) {
        if (getContext() == null)
            return;

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(
                getContext());

        // 设置标题和图标
        builder.setTitle("✅ 身份证读取成功");
        builder.setIcon(android.R.drawable.ic_dialog_info);

        // 构建详细信息
        StringBuilder message = new StringBuilder();
        message.append("身份证信息已成功读取并验证！\n\n");
        message.append("📋 身份证详情:\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        message.append("👤 姓名: ").append(farmerName != null ? farmerName : "未读取").append("\n");
        message.append("🆔 身份证号: ").append(maskIdCardNumber(idNumber)).append("\n");
        message.append("⚧ 性别: ").append(gender != null ? gender : "未知").append("\n");
        message.append("🏛️ 民族: ").append(nationality != null ? nationality : "未知").append("\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        message.append("✨ 农户信息已自动填入称重界面，可以开始称重操作。");

        builder.setMessage(message.toString());

        // 设置按钮
        builder.setPositiveButton("开始称重", (dialog, which) -> {
            dialog.dismiss();
            // 可选：自动获取焦点到预检编号输入框
            if (etPrecheckNumber != null) {
                etPrecheckNumber.requestFocus();
            }
        });

        builder.setNeutralButton("重新读取", (dialog, which) -> {
            dialog.dismiss();
            // 重新调用身份证读取功能
            if (btnReadIdCard != null) {
                btnReadIdCard.performClick();
            }
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
     * 显示身份证读取失败界面
     */
    private void showIdCardFailureDialog(String errorType, String errorMessage, String errorDetails) {
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
        message.append(errorDetails != null ? errorDetails : "读卡器连接失败或身份证读取异常").append("\n");
        message.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        message.append("💡 建议解决方案:\n");
        message.append("• 检查身份证读卡器电源和连接\n");
        message.append("• 确认身份证放置正确且清洁\n");
        message.append("• 检查USB连接是否稳定\n");
        message.append("• 重新插拔读卡器设备\n");
        message.append("• 尝试手动输入农户姓名");

        builder.setMessage(message.toString());

        // 设置按钮
        builder.setPositiveButton("重试读取", (dialog, which) -> {
            dialog.dismiss();
            // 重新尝试读取身份证
            if (btnReadIdCard != null) {
                btnReadIdCard.performClick();
            }
        });

        builder.setNeutralButton("手动输入", (dialog, which) -> {
            dialog.dismiss();
            // 将焦点转移到农户姓名输入框
            if (etFarmerName != null) {
                etFarmerName.requestFocus();
                Toast.makeText(getContext(), "请手动输入农户姓名", Toast.LENGTH_SHORT).show();
            }
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
     * 屏蔽身份证号中间位数
     */
    private String maskIdCardNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 8) {
            return "****";
        }
        
        return idNumber.substring(0, 6) + "********" + idNumber.substring(idNumber.length() - 4);
    }

    /**
     * 读取身份证并提供反馈
     */
    private void readIdCardWithFeedback() {
        if (viewModel == null || mainViewModel == null) {
            Toast.makeText(getContext(), "系统未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示读取开始状态
        viewModel.notifyIdCardStatusUpdate("正在连接身份证读卡器...");
        Toast.makeText(getContext(), "正在连接身份证读卡器...", Toast.LENGTH_SHORT).show();

        // 记录开始时间，用于超时检测
        final long startTime = System.currentTimeMillis();
        final long TIMEOUT_MS = 10000; // 10秒超时

        try {
            // 使用真实IdCardManager连接身份证读卡器
            if (idCardManager != null) {
                idCardManager.connectReader();
            } else {
                // 备用：使用MainViewModel的模拟器方法
                mainViewModel.connectIdCardReader();
            }
            
            // 生成新的合同号
            viewModel.generateNewContractNumber();
            
            // 设置状态监听和超时检测
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                // 检查身份证读卡器连接状态（从ViewModel获取实际状态）
                boolean connected = isIdCardReaderConnected();
                boolean simulatorMode = isHardwareSimulatorMode();
                
                // 调试信息
                android.util.Log.d("WeightingFragment", 
                    "ID Card Reader Status - Connected: " + connected + 
                    ", Simulator Mode: " + simulatorMode);
                
                if (connected) {
                    // 设备已连接（无论是模拟器还是真实硬件）
                    String deviceType = simulatorMode ? "硬件模拟器" : "身份证读卡器设备";
                    viewModel.notifyIdCardConnectionSuccess(deviceType);
                    
                    if (simulatorMode) {
                        viewModel.notifyIdCardStatusUpdate("模拟器模式：正在生成测试数据...");
                        // 模拟器通常会快速生成数据，但仍设置超时保护
                        setupIdCardReadTimeout(startTime, TIMEOUT_MS);
                    } else {
                        viewModel.notifyIdCardStatusUpdate("请将身份证放置在读卡器上...");
                        // 真实硬件需要用户操作，设置超时检测
                        setupIdCardReadTimeout(startTime, TIMEOUT_MS);
                    }
                } else {
                    // 设备未连接
                    String errorMessage = simulatorMode 
                        ? "硬件模拟器初始化失败"
                        : "无法连接到身份证读卡器设备";
                    viewModel.notifyIdCardConnectionFailed(errorMessage);
                }
            }, 1000);

        } catch (Exception e) {
            viewModel.notifyIdCardFailure("连接错误", "身份证读卡器连接失败", e.getMessage());
        }
    }

    /**
     * 设置身份证读取超时检测
     */
    private void setupIdCardReadTimeout(long startTime, long timeoutMs) {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // 检查是否已经超时
            if (System.currentTimeMillis() - startTime >= timeoutMs) {
                // 检查是否已经收到数据（通过检查农户姓名是否已更新）
                if (viewModel != null && etFarmerName != null) {
                    String currentFarmerName = etFarmerName.getText().toString().trim();
                    // 如果农户姓名还是空或者是默认值，说明没有收到身份证数据
                    if (currentFarmerName.isEmpty() || currentFarmerName.equals("未读取")) {
                        String errorDetails = isHardwareSimulatorMode() 
                            ? "模拟器未能在规定时间内生成测试数据，可能系统负载过高"
                            : "读卡器连接正常但未能读取到身份证数据，请检查身份证是否正确放置";
                        viewModel.notifyIdCardFailure("读取超时", "身份证读取超时", errorDetails);
                    }
                }
            }
        }, timeoutMs);
    }

    /**
     * 检查是否为硬件模拟器模式
     */
    private boolean isHardwareSimulatorMode() {
        // 检查IdCardManager是否使用原生库
        // 如果原生库不可用，则使用模拟模式
        return !com.tobacco.weight.hardware.idcard.IdCardManager.isNativeLibraryAvailable();
    }

    /**
     * 检查身份证读卡器是否连接 - 使用真实IdCardManager状态
     */
    private boolean isIdCardReaderConnected() {
        // 优先使用真实IdCardManager的状态
        if (idCardManager != null) {
            try {
                // 从IdCardManager的连接流获取最新状态
                Boolean connected = idCardManager.connectionStream().blockingFirst();
                android.util.Log.d("WeightingFragment", "IdCardManager连接状态: " + connected);
                return Boolean.TRUE.equals(connected);
            } catch (Exception e) {
                android.util.Log.w("WeightingFragment", "无法获取IdCardManager连接状态: " + e.getMessage());
            }
        }
        
        // 备用：从ViewModel获取连接状态
        if (viewModel != null) {
            Boolean connected = viewModel.getIdCardReaderConnected().getValue();
            if (connected != null) {
                return connected;
            }
        }
        
        // 备用：检查MainViewModel的连接状态
        if (mainViewModel != null) {
            Boolean connected = mainViewModel.getIdCardConnected().getValue();
            if (connected != null) {
                return connected;
            }
        }
        
        return false;
    }

    /**
     * 显示身份证连接状态（调试用）
     */
    private void showIdCardConnectionStatus() {
        boolean connected = isIdCardReaderConnected();
        boolean simulatorMode = isHardwareSimulatorMode();
        boolean nativeLibAvailable = com.tobacco.weight.hardware.idcard.IdCardManager.isNativeLibraryAvailable();
        boolean devSimulationEnabled = (idCardManager != null) ? idCardManager.isDevelopmentSimulationEnabled() : false;
        
        // 从IdCardManager获取详细状态
        String idCardManagerStatus = "未知";
        if (idCardManager != null) {
            try {
                Boolean managerConnected = idCardManager.connectionStream().blockingFirst();
                idCardManagerStatus = managerConnected != null ? managerConnected.toString() : "null";
            } catch (Exception e) {
                idCardManagerStatus = "获取失败: " + e.getMessage();
            }
        }
        
        // 从ViewModel获取详细状态
        String viewModelStatus = "未知";
        String mainViewModelStatus = "未知";
        
        if (viewModel != null) {
            Boolean vmConnected = viewModel.getIdCardReaderConnected().getValue();
            viewModelStatus = vmConnected != null ? vmConnected.toString() : "null";
        }
        
        if (mainViewModel != null) {
            Boolean mvmConnected = mainViewModel.getIdCardConnected().getValue();
            mainViewModelStatus = mvmConnected != null ? mvmConnected.toString() : "null";
        }
        
        String modeDescription;
        if (nativeLibAvailable) {
            modeDescription = "真实硬件模式 🔧";
        } else if (devSimulationEnabled) {
            modeDescription = "开发模拟模式 🧪";
        } else {
            modeDescription = "生产模式(无硬件) 🏭";
        }
        
        String statusMessage = String.format(
            "📊 身份证读卡器状态\n\n" +
            "📚 原生库: %s\n" +
            "🔧 运行模式: %s\n" +
            "🧪 开发模拟: %s\n" +
            "🔌 连接状态: %s\n\n" +
            "📱 IdCardManager: %s\n" +
            "📱 WeightingViewModel: %s\n" +
            "📱 MainViewModel: %s\n\n" +
            "💡 双击读卡按钮切换开发模拟模式\n" +
            "💡 长按读卡按钮查看此状态",
            nativeLibAvailable ? "可用 ✅" : "不可用 ❌",
            modeDescription,
            devSimulationEnabled ? "已启用 ✅" : "已禁用 ❌",
            connected ? "已连接 ✅" : "未连接 ❌",
            idCardManagerStatus,
            viewModelStatus,
            mainViewModelStatus
        );
        
        if (getContext() != null) {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("🔍 设备状态检测")
                .setMessage(statusMessage)
                .setPositiveButton("刷新状态", (dialog, which) -> {
                    dialog.dismiss();
                    // 延迟一点再重新显示，以便观察状态变化
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        showIdCardConnectionStatus();
                    }, 500);
                })
                .setNegativeButton("关闭", null)
                .show();
        }
        
        // 同时在日志中输出详细信息
        android.util.Log.i("WeightingFragment", 
            "ID Card Connection Status: connected=" + connected + 
            ", simulator=" + simulatorMode + 
            ", viewModel=" + viewModelStatus + 
            ", mainViewModel=" + mainViewModelStatus);
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

    /**
     * 切换开发模拟模式（调试用）
     */
    private void toggleDevelopmentSimulation() {
        if (idCardManager != null) {
            boolean currentlyEnabled = idCardManager.isDevelopmentSimulationEnabled();
            
            if (currentlyEnabled) {
                idCardManager.disableDevelopmentSimulation();
                Toast.makeText(getContext(), "🏭 已切换到生产模式 - 需要真实硬件", Toast.LENGTH_LONG).show();
                android.util.Log.d("WeightingFragment", "🏭 开发模拟模式已禁用");
            } else {
                idCardManager.enableDevelopmentSimulation();
                Toast.makeText(getContext(), "🧪 已切换到开发模拟模式", Toast.LENGTH_LONG).show();
                android.util.Log.d("WeightingFragment", "🧪 开发模拟模式已启用");
            }
        } else {
            Toast.makeText(getContext(), "❌ IdCardManager未初始化", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 清理RxJava订阅
        if (disposables != null && !disposables.isDisposed()) {
            disposables.dispose();
            android.util.Log.d("WeightingFragment", "已清理IdCardManager订阅");
        }
        
        // 释放IdCardManager资源
        if (idCardManager != null) {
            try {
                idCardManager.release();
                android.util.Log.d("WeightingFragment", "已释放IdCardManager资源");
            } catch (Exception e) {
                android.util.Log.w("WeightingFragment", "释放IdCardManager失败", e);
            }
        }
    }

    /**
     * 根据会话状态更新按钮状态
     */
    private void updateSessionButtonStates(WeightingViewModel.SessionState sessionState) {
        if (sessionState == null) sessionState = WeightingViewModel.SessionState.INACTIVE;
        
        boolean isActive = sessionState == WeightingViewModel.SessionState.ACTIVE;
        boolean isSaved = sessionState == WeightingViewModel.SessionState.SAVED;
        boolean isInactive = sessionState == WeightingViewModel.SessionState.INACTIVE;
        
        if (btnStartSession != null) {
            btnStartSession.setEnabled(isInactive);
        }
        
        if (btnResetSession != null) {
            btnResetSession.setEnabled(isActive || isSaved);
        }
        
        if (btnConfirmSave != null) {
            btnConfirmSave.setEnabled(isActive);
        }
        
        if (btnCancelSession != null) {
            btnCancelSession.setEnabled(isActive || isSaved);
        }
        
        if (etBundleCount != null) {
            etBundleCount.setEnabled(isActive);
        }
        
        if (etPrecheckNumber != null) {
            etPrecheckNumber.setEnabled(isInactive);
        }
        
        // Enable/disable leaf level buttons based on session state (only active)
        if (btnUpperLevel != null) {
            btnUpperLevel.setEnabled(isActive);
        }
        
        if (btnMiddleLevel != null) {
            btnMiddleLevel.setEnabled(isActive);
        }
        
        if (btnLowerLevel != null) {
            btnLowerLevel.setEnabled(isActive);
        }
        
        // Print button should be enabled when there's a saved session
        if (btnPrint != null) {
            btnPrint.setEnabled(isSaved);
        }
    }
    
    /**
     * 更新会话统计显示
     */
    private void updateSessionStatisticsDisplay() {
        if (viewModel == null) return;
        
        WeightingViewModel.WeighingSession currentSession = viewModel.getCurrentSession();
        WeightingViewModel.SessionState sessionState = viewModel.getCurrentSessionState();
        
        if (currentSession == null || sessionState == WeightingViewModel.SessionState.INACTIVE) {
            // 没有活动会话时显示默认状态
            if (tvSessionDisplayStatus != null) {
                tvSessionDisplayStatus.setText("无活动会话");
            }
            if (tvSessionPrecheckNumber != null) {
                tvSessionPrecheckNumber.setText("--");
            }
            if (tvTotalBundles != null) {
                tvTotalBundles.setText("0 捆");
            }
            if (tvTotalWeight != null) {
                tvTotalWeight.setText("0.0 kg");
            }
            if (tvUpperLeafStats != null) {
                tvUpperLeafStats.setText("0捆 (0.0kg) - 0%");
            }
            if (tvMiddleLeafStats != null) {
                tvMiddleLeafStats.setText("0捆 (0.0kg) - 0%");
            }
            if (tvLowerLeafStats != null) {
                tvLowerLeafStats.setText("0捆 (0.0kg) - 0%");
            }
        } else {
            // 有会话时显示实际数据（活动中或已保存）
            String statusText = sessionState == WeightingViewModel.SessionState.ACTIVE ? "进行中" : "✅ 已保存";
            if (tvSessionDisplayStatus != null) {
                tvSessionDisplayStatus.setText(statusText);
            }
            if (tvSessionPrecheckNumber != null) {
                tvSessionPrecheckNumber.setText(currentSession.getPrecheckNumber());
            }
            if (tvTotalBundles != null) {
                tvTotalBundles.setText(currentSession.getTotalBundles() + " 捆");
            }
            if (tvTotalWeight != null) {
                tvTotalWeight.setText(String.format("%.1f kg", currentSession.getTotalWeight()));
            }
            
            // 计算各等级统计
            int upperBundles = 0, middleBundles = 0, lowerBundles = 0;
            double upperWeight = 0.0, middleWeight = 0.0, lowerWeight = 0.0;
            
            for (WeightingViewModel.WeighingSession.SessionEntry entry : currentSession.getEntries()) {
                switch (entry.getTobaccoGrade()) {
                    case "上部叶":
                        upperBundles += entry.getBundleCount();
                        upperWeight += entry.getWeight();
                        break;
                    case "中部叶":
                        middleBundles += entry.getBundleCount();
                        middleWeight += entry.getWeight();
                        break;
                    case "下部叶":
                        lowerBundles += entry.getBundleCount();
                        lowerWeight += entry.getWeight();
                        break;
                }
            }
            
            int totalBundles = currentSession.getTotalBundles();
            
            // 计算百分比
            double upperPercent = totalBundles > 0 ? (upperBundles * 100.0 / totalBundles) : 0.0;
            double middlePercent = totalBundles > 0 ? (middleBundles * 100.0 / totalBundles) : 0.0;
            double lowerPercent = totalBundles > 0 ? (lowerBundles * 100.0 / totalBundles) : 0.0;
            
            // 更新各等级显示
            if (tvUpperLeafStats != null) {
                tvUpperLeafStats.setText(String.format("%d捆 (%.1fkg) - %.1f%%", 
                    upperBundles, upperWeight, upperPercent));
            }
            if (tvMiddleLeafStats != null) {
                tvMiddleLeafStats.setText(String.format("%d捆 (%.1fkg) - %.1f%%", 
                    middleBundles, middleWeight, middlePercent));
            }
            if (tvLowerLeafStats != null) {
                tvLowerLeafStats.setText(String.format("%d捆 (%.1fkg) - %.1f%%", 
                    lowerBundles, lowerWeight, lowerPercent));
            }
        }
    }
}