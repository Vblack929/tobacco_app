# 烟叶称重管理系统 - UI设计指南

## 项目概述

本UI设计针对Android 13平板（10英寸）设备，采用Material Design 3设计语言，为烟叶称重管理系统提供专业、易用的界面。整体设计遵循MVVM架构，所有控件预留ID供后续绑定ViewModel使用。

## 设计原则

### 1. 平板适配
- **字体大小**：最小16sp，主要内容18-24sp，数据显示24-56sp
- **按钮尺寸**：最小48dp高度，主要操作按钮56-64dp
- **间距设计**：使用16dp、24dp为基准间距，充分利用平板大屏空间
- **触摸目标**：所有可点击元素至少48dp×48dp

### 2. 视觉层次
- **卡片布局**：使用CardView和ConstraintLayout提供清晰的信息分组
- **颜色层次**：主色调#1976D2，强调色#FF5722，成功#4CAF50，警告#FF9800
- **阴影效果**：4-8dp elevation营造层次感

### 3. 交互反馈
- 所有按钮添加ripple效果
- 列表项支持点击反馈
- 加载状态和空状态提示

## 界面设计详情

### 界面1：称重界面 (`fragment_weighing.xml`)

#### 布局结构
```
ConstraintLayout (根布局)
├── 顶部信息栏 (LinearLayout)
│   ├── 农户姓名 (TextView)
│   ├── 合同编号 (TextView) 
│   └── 账户余额 (TextView)
├── 重量显示区域 (CardView + ConstraintLayout)
│   ├── 实时重量数值 (TextView - 56sp)
│   ├── 重量单位 (TextView)
│   └── 重量状态 (TextView)
├── 右侧状态面板 (LinearLayout)
│   ├── 设备状态显示 (TextView)
│   ├── 预检码状态 (TextView)
│   └── 预检码确认按钮 (Button)
├── 烟叶信息输入区域 (CardView)
│   ├── 部位选择 (RadioGroup)
│   └── 捆数输入 (EditText)
├── 底部操作按钮 (LinearLayout)
│   ├── 去皮重/清零按钮 (Button)
│   ├── 确认保存按钮 (Button)
│   └── 打印标签按钮 (Button)
└── 状态消息 (TextView)
```

#### 关键特性
- **实时重量显示**：56sp大字体，Roboto Mono字体，醒目的重量数值
- **状态指示**：设备连接状态实时显示，重量稳定性指示器
- **响应式布局**：重量显示区域占65%宽度，状态面板30%宽度
- **操作流程**：从上到下的自然操作流程

#### 数据绑定点
- `tv_farmer_name` → farmerName LiveData
- `tv_contract_number` → contractNumber LiveData  
- `tv_balance` → accountBalance LiveData
- `tv_current_weight` → currentWeight LiveData
- `tv_weight_status` → weightData.isStable
- `rg_tobacco_part` → tobaccoPart LiveData
- `et_bundle_count` → tobaccoBundles LiveData
- `btn_confirm` → saveWeightRecord()
- `btn_print` → printLabel()

### 界面2：预检码界面 (`fragment_precheck.xml`)

#### 布局结构  
```
ConstraintLayout (根布局)
├── 顶部标题栏 (LinearLayout)
├── 搜索区域 (CardView)
│   ├── 搜索输入框 (EditText)
│   ├── 搜索/清空按钮 (Button)
│   └── 搜索选项 (CheckBox)
├── 统计信息栏 (LinearLayout)
├── 预检码列表 (RecyclerView)
├── 空状态显示 (LinearLayout)
├── 底部操作区域 (LinearLayout)
└── 加载指示器 (ProgressBar)
```

#### 列表项设计 (`item_precheck.xml`)
```
CardView (列表项容器)
└── ConstraintLayout
    ├── 选择状态指示器 (View - 4dp宽度条)
    ├── 状态图标 (ImageView)
    ├── 主要信息区域 (LinearLayout)
    │   ├── 批次号和状态 (LinearLayout)
    │   ├── 调拨信息 (LinearLayout)
    │   ├── 预检码 (LinearLayout)
    │   └── 创建时间/有效期 (LinearLayout)
    ├── 选择按钮 (Button)
    └── 详情按钮 (Button)
```

#### 关键特性
- **高级搜索**：支持批次号、调拨信息、预检码多字段搜索
- **选择反馈**：选中项显示4dp蓝色左边框指示器
- **状态管理**：统计信息实时更新，选择状态提示
- **复制功能**：预检码可一键复制

#### 数据绑定点
- `et_search` → searchQuery LiveData
- `rv_precheck_list` → precheckList LiveData
- `btn_confirm_select` → selectPrecheckCode()

### 界面3：管理记录界面 (`fragment_records.xml`)

#### 布局结构
```
ConstraintLayout (根布局)
├── 顶部标题栏 (LinearLayout)
├── 搜索筛选区域 (CardView)
│   ├── 搜索框 (EditText)
│   ├── 日期范围选择 (Button×2)
│   ├── 部位筛选 (Spinner)
│   └── 排序选择 (Spinner)
├── 统计信息区域 (LinearLayout)
│   ├── 总记录数 (TextView)
│   ├── 总重量 (TextView)
│   ├── 总捆数 (TextView)
│   └── 平均单价 (TextView)
├── 记录列表 (RecyclerView)
├── 空状态显示 (LinearLayout)
└── 浮动操作按钮 (FAB)
```

#### 记录项设计 (`item_weight_record.xml`)
```
CardView (可展开列表项)
└── LinearLayout (垂直方向)
    ├── 主要内容区域 (ConstraintLayout)
    │   ├── 记录ID/时间 (TextView)
    │   ├── 农户信息 (LinearLayout)
    │   ├── 称重数据 (LinearLayout - 4列网格)
    │   └── 状态/展开指示器 (LinearLayout)
    └── 详细信息区域 (LinearLayout - 可展开)
        ├── 身份信息 (LinearLayout)
        ├── 预检码信息 (LinearLayout)
        ├── 备注信息 (TextView)
        └── 操作按钮 (LinearLayout)
```

#### 关键特性
- **统计面板**：4项关键数据的视觉化展示
- **高级筛选**：日期范围、部位类型、排序方式多维度筛选
- **展开详情**：点击记录可展开显示完整信息
- **数据导出**：支持记录数据导出功能

#### 数据绑定点
- `et_search_records` → searchQuery LiveData
- `spinner_tobacco_part` → filterTobaccoPart LiveData
- `rv_records_list` → weightRecords LiveData
- `tv_total_records` → statisticsData.totalRecords
- `tv_total_weight` → statisticsData.totalWeight

## 样式规范

### 颜色主题 (`colors.xml`)
```xml
<!-- 主色调 -->
<color name="primary">#1976D2</color>          <!-- 主按钮、选中状态 -->
<color name="accent">#FF5722</color>           <!-- 重量数值、强调内容 -->

<!-- 文字颜色 -->
<color name="text_primary">#212121</color>     <!-- 主要文字 -->
<color name="text_secondary">#757575</color>   <!-- 次要文字 -->
<color name="text_hint">#BDBDBD</color>        <!-- 提示文字 -->

<!-- 状态颜色 -->
<color name="success">#4CAF50</color>          <!-- 成功、连接状态 -->
<color name="warning">#FF9800</color>          <!-- 警告、价格 -->
<color name="error">#F44336</color>            <!-- 错误状态 -->
```

### 字体大小规范
- **数据显示**：56sp (重量)、24sp (统计数据)
- **标题文字**：24sp (页面标题)、20sp (卡片标题)、18sp (农户姓名)
- **正文内容**：16sp (标准文字)、14sp (次要信息)
- **辅助信息**：12sp (提示文字、时间戳)

### 间距规范
- **页面边距**：24dp
- **卡片间距**：16-20dp
- **内容边距**：16dp
- **元素间距**：8dp、12dp、16dp

## 开发接入指南

### 1. ViewModel绑定示例
```java
// 在Fragment中绑定ViewModel
private void observeViewModel() {
    viewModel.getCurrentWeight().observe(this, weight -> {
        binding.tvCurrentWeight.setText(String.format("%.3f", weight));
    });
    
    viewModel.getWeightRecords().observe(this, records -> {
        recordsAdapter.submitList(records);
    });
}
```

### 2. 事件监听设置
```java
// 按钮点击事件
binding.btnConfirm.setOnClickListener(v -> {
    viewModel.saveWeightRecord();
});

// RecyclerView项点击
recordsAdapter.setOnItemClickListener((record, position) -> {
    toggleRecordDetail(position);
});
```

### 3. 状态管理
```java
// 加载状态
viewModel.getLoadingState().observe(this, isLoading -> {
    binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
});

// 空状态
viewModel.getRecordsList().observe(this, records -> {
    binding.emptyStateLayout.setVisibility(
        records.isEmpty() ? View.VISIBLE : View.GONE
    );
});
```

## 资源文件补充

### 需要补充的Drawable资源
1. **图标资源**：`ic_search`, `ic_precheck`, `ic_history`, `ic_expand_more`, `ic_copy`
2. **背景资源**：`search_edittext_background`, `status_indicator_background`, `weight_display_background`
3. **状态资源**：`status_chip_background`, `part_chip_background`, `remarks_background`

### 字体资源
- `roboto_mono`：用于数值显示，确保等宽对齐

## 性能优化建议

### 1. RecyclerView优化
- 使用ViewBinding减少findViewById调用
- 实现DiffUtil.ItemCallback提升列表更新性能
- 设置setHasFixedSize(true)优化布局

### 2. 内存管理
- 大图片使用适当尺寸，避免OOM
- 及时释放不需要的View引用
- 使用WeakReference处理长生命周期回调

### 3. 响应性能
- 复杂布局使用ConstraintLayout减少嵌套
- 耗时操作在后台线程执行
- 使用LiveData避免内存泄漏

---

**设计完成时间**: 2024年3月
**适配设备**: Android 13平板 (10英寸)
**设计规范**: Material Design 3
**架构模式**: MVVM + Room + LiveData 