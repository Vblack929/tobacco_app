# 烟叶称重管理系统项目结构

## 目录架构概览

```
tobacco_app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tobacco/weight/
│   │   │   │   ├── TobaccoApplication.java                    # 应用主类
│   │   │   │   │
│   │   │   │   ├── data/                                      # 数据层
│   │   │   │   │   ├── model/                                 # 数据模型
│   │   │   │   │   │   ├── WeightRecord.java                 # 称重记录实体
│   │   │   │   │   │   ├── SystemConfig.java                 # 系统配置实体
│   │   │   │   │   │   ├── PrintTemplate.java               # 打印模板实体
│   │   │   │   │   │   └── IdCardInfo.java                   # 身份证信息实体
│   │   │   │   │   │
│   │   │   │   │   ├── database/                              # 数据库
│   │   │   │   │   │   ├── TobaccoDatabase.java              # Room数据库
│   │   │   │   │   │   ├── WeightRecordDao.java              # 称重记录DAO
│   │   │   │   │   │   ├── SystemConfigDao.java              # 系统配置DAO
│   │   │   │   │   │   └── DateConverter.java                # 日期转换器
│   │   │   │   │   │
│   │   │   │   │   ├── repository/                            # 数据仓库
│   │   │   │   │   │   ├── WeightRepository.java             # 称重数据仓库
│   │   │   │   │   │   ├── ConfigRepository.java             # 配置数据仓库
│   │   │   │   │   │   └── PrintRepository.java              # 打印数据仓库
│   │   │   │   │   │
│   │   │   │   │   └── preferences/                           # 偏好设置
│   │   │   │   │       ├── AppPreferences.java               # 应用偏好设置
│   │   │   │   │       └── PreferenceKeys.java               # 偏好设置键名
│   │   │   │   │
│   │   │   │   ├── hardware/                                  # 硬件通信层
│   │   │   │   │   ├── serial/                                # 串口通信
│   │   │   │   │   │   ├── SerialPortManager.java            # 串口管理器
│   │   │   │   │   │   ├── SerialPortUtils.java              # 串口工具类
│   │   │   │   │   │   └── SerialPortConfig.java             # 串口配置
│   │   │   │   │   │
│   │   │   │   │   ├── scale/                                 # 电子秤
│   │   │   │   │   │   ├── ScaleManager.java                 # 电子秤管理器
│   │   │   │   │   │   ├── WeightData.java                   # 重量数据模型
│   │   │   │   │   │   ├── ScaleProtocol.java                # 电子秤协议
│   │   │   │   │   │   └── ScaleListener.java                # 电子秤监听器
│   │   │   │   │   │
│   │   │   │   │   ├── idcard/                                # 身份证读卡器
│   │   │   │   │   │   ├── IdCardManager.java                # 身份证读卡器管理
│   │   │   │   │   │   ├── IdCardReader.java                 # 身份证读取器
│   │   │   │   │   │   └── IdCardData.java                   # 身份证数据模型
│   │   │   │   │   │
│   │   │   │   │   └── printer/                               # 热敏打印机
│   │   │   │   │       ├── PrinterManager.java               # 打印机管理器
│   │   │   │   │       ├── LabelPrinter.java                 # 标签打印器
│   │   │   │   │       ├── PrintCommand.java                 # 打印命令
│   │   │   │   │       └── PrintTemplate.java               # 打印模板
│   │   │   │   │
│   │   │   │   ├── ui/                                        # UI层
│   │   │   │   │   ├── main/                                  # 主界面
│   │   │   │   │   │   ├── MainActivity.java                 # 主活动
│   │   │   │   │   │   ├── MainFragment.java                 # 主界面Fragment
│   │   │   │   │   │   ├── MainViewModel.java                # 主界面ViewModel
│   │   │   │   │   │   └── WeightingFragment.java            # 称重Fragment
│   │   │   │   │   │
│   │   │   │   │   ├── splash/                                # 启动画面
│   │   │   │   │   │   ├── SplashActivity.java               # 启动活动
│   │   │   │   │   │   └── SplashViewModel.java              # 启动ViewModel
│   │   │   │   │   │
│   │   │   │   │   ├── history/                               # 历史记录
│   │   │   │   │   │   ├── HistoryFragment.java              # 历史记录Fragment
│   │   │   │   │   │   ├── HistoryViewModel.java             # 历史记录ViewModel
│   │   │   │   │   │   ├── HistoryAdapter.java               # 历史记录适配器
│   │   │   │   │   │   └── SearchFragment.java               # 搜索Fragment
│   │   │   │   │   │
│   │   │   │   │   ├── settings/                              # 设置界面
│   │   │   │   │   │   ├── SettingsActivity.java             # 设置活动
│   │   │   │   │   │   ├── SettingsFragment.java             # 设置Fragment
│   │   │   │   │   │   ├── SettingsViewModel.java            # 设置ViewModel
│   │   │   │   │   │   └── DeviceSettingsFragment.java       # 设备设置Fragment
│   │   │   │   │   │
│   │   │   │   │   ├── export/                                # 导出功能
│   │   │   │   │   │   ├── ExportFragment.java               # 导出Fragment
│   │   │   │   │   │   ├── ExportViewModel.java              # 导出ViewModel
│   │   │   │   │   │   └── ExportAdapter.java                # 导出适配器
│   │   │   │   │   │
│   │   │   │   │   └── common/                                # 通用UI组件
│   │   │   │   │       ├── BaseActivity.java                 # 基础活动类
│   │   │   │   │       ├── BaseFragment.java                 # 基础Fragment类
│   │   │   │   │       ├── BaseViewModel.java                # 基础ViewModel类
│   │   │   │   │       ├── LoadingDialog.java                # 加载对话框
│   │   │   │   │       └── ConfirmDialog.java                # 确认对话框
│   │   │   │   │
│   │   │   │   ├── service/                                   # 后台服务
│   │   │   │   │   ├── WeightService.java                    # 称重服务
│   │   │   │   │   ├── PrintService.java                     # 打印服务
│   │   │   │   │   ├── DataSyncService.java                  # 数据同步服务
│   │   │   │   │   └── DeviceMonitorService.java             # 设备监控服务
│   │   │   │   │
│   │   │   │   ├── utils/                                     # 工具类
│   │   │   │   │   ├── DateUtils.java                        # 日期工具
│   │   │   │   │   ├── NumberUtils.java                      # 数字工具
│   │   │   │   │   ├── StringUtils.java                      # 字符串工具
│   │   │   │   │   ├── FileUtils.java                        # 文件工具
│   │   │   │   │   ├── ExportUtils.java                      # 导出工具
│   │   │   │   │   ├── QRCodeUtils.java                      # 二维码工具
│   │   │   │   │   ├── PermissionUtils.java                  # 权限工具
│   │   │   │   │   └── LogUtils.java                         # 日志工具
│   │   │   │   │
│   │   │   │   ├── di/                                        # 依赖注入
│   │   │   │   │   ├── DatabaseModule.java                   # 数据库模块
│   │   │   │   │   ├── RepositoryModule.java                 # 仓库模块
│   │   │   │   │   ├── HardwareModule.java                   # 硬件模块
│   │   │   │   │   └── ServiceModule.java                    # 服务模块
│   │   │   │   │
│   │   │   │   └── constants/                                 # 常量定义
│   │   │   │       ├── AppConstants.java                     # 应用常量
│   │   │   │       ├── DatabaseConstants.java               # 数据库常量
│   │   │   │       ├── SerialConstants.java                 # 串口常量
│   │   │   │       └── PrintConstants.java                  # 打印常量
│   │   │   │
│   │   │   ├── res/                                          # 资源文件
│   │   │   │   ├── layout/                                   # 布局文件
│   │   │   │   │   ├── activity_main.xml                    # 主活动布局
│   │   │   │   │   ├── fragment_main.xml                    # 主界面布局
│   │   │   │   │   ├── fragment_weighing.xml                # 称重界面布局
│   │   │   │   │   ├── fragment_history.xml                 # 历史记录布局
│   │   │   │   │   ├── fragment_settings.xml                # 设置界面布局
│   │   │   │   │   ├── item_weight_record.xml               # 记录项布局
│   │   │   │   │   └── dialog_loading.xml                   # 加载对话框布局
│   │   │   │   │
│   │   │   │   ├── values/                                   # 值资源
│   │   │   │   │   ├── strings.xml                          # 字符串资源
│   │   │   │   │   ├── colors.xml                           # 颜色资源
│   │   │   │   │   ├── dimens.xml                           # 尺寸资源
│   │   │   │   │   ├── styles.xml                           # 样式资源
│   │   │   │   │   └── arrays.xml                           # 数组资源
│   │   │   │   │
│   │   │   │   ├── drawable/                                 # 图形资源
│   │   │   │   │   ├── ic_launcher.xml                      # 应用图标
│   │   │   │   │   ├── ic_scale.xml                         # 电子秤图标
│   │   │   │   │   ├── ic_printer.xml                       # 打印机图标
│   │   │   │   │   └── selector_button.xml                  # 按钮选择器
│   │   │   │   │
│   │   │   │   ├── menu/                                     # 菜单资源
│   │   │   │   │   ├── main_menu.xml                        # 主菜单
│   │   │   │   │   └── history_menu.xml                     # 历史记录菜单
│   │   │   │   │
│   │   │   │   └── xml/                                      # XML配置
│   │   │   │       ├── device_filter.xml                    # 设备过滤器
│   │   │   │       ├── file_paths.xml                       # 文件路径
│   │   │   │       └── backup_rules.xml                     # 备份规则
│   │   │   │
│   │   │   ├── cpp/                                          # 原生代码
│   │   │   │   ├── CMakeLists.txt                           # CMake配置
│   │   │   │   ├── serialport.cpp                          # 串口原生实现
│   │   │   │   └── serialport.h                            # 串口头文件
│   │   │   │
│   │   │   └── AndroidManifest.xml                          # 应用清单
│   │   │
│   │   ├── androidTest/                                      # Android测试
│   │   │   └── java/com/tobacco/weight/
│   │   │       ├── DatabaseTest.java                        # 数据库测试
│   │   │       ├── RepositoryTest.java                      # 仓库测试
│   │   │       └── UITest.java                              # UI测试
│   │   │
│   │   └── test/                                             # 单元测试
│   │       └── java/com/tobacco/weight/
│   │           ├── UtilsTest.java                           # 工具类测试
│   │           ├── ModelTest.java                           # 模型测试
│   │           └── ViewModelTest.java                       # ViewModel测试
│   │
│   ├── build.gradle                                         # 应用级构建脚本
│   └── proguard-rules.pro                                   # ProGuard规则
│
├── build.gradle                                             # 项目级构建脚本
├── settings.gradle                                          # 项目设置
├── gradle.properties                                        # Gradle属性
├── local.properties                                         # 本地属性
├── README.md                                                # 项目说明
└── PROJECT_STRUCTURE.md                                     # 项目结构文档
```

## 模块详细说明

### 1. 数据层 (data/)

#### 数据模型 (model/)
- **WeightRecord**: 称重记录实体类，包含所有称重相关字段
- **SystemConfig**: 系统配置实体，存储应用设置
- **PrintTemplate**: 打印模板实体，定义标签格式
- **IdCardInfo**: 身份证信息实体，存储读取的身份证数据

#### 数据库 (database/)
- **TobaccoDatabase**: Room数据库主类，管理数据库版本和迁移
- **WeightRecordDao**: 称重记录数据访问对象，定义CRUD操作
- **SystemConfigDao**: 系统配置数据访问对象
- **DateConverter**: Room类型转换器，处理Date与Long的转换

#### 数据仓库 (repository/)
- **WeightRepository**: 称重数据仓库，统一数据访问接口
- **ConfigRepository**: 配置数据仓库，管理应用配置
- **PrintRepository**: 打印数据仓库，管理打印模板和记录

### 2. 硬件通信层 (hardware/)

#### 串口通信 (serial/)
- **SerialPortManager**: 串口管理器，封装串口的打开、关闭、读写操作
- **SerialPortUtils**: 串口工具类，提供串口查找、权限检查等功能
- **SerialPortConfig**: 串口配置类，定义波特率、数据位等参数

#### 电子秤 (scale/)
- **ScaleManager**: 电子秤管理器，处理数据解析和稳定性检测
- **WeightData**: 重量数据模型，封装重量值和状态信息
- **ScaleProtocol**: 电子秤协议解析器，支持多种厂商协议
- **ScaleListener**: 电子秤事件监听器

#### 身份证读卡器 (idcard/)
- **IdCardManager**: 身份证读卡器管理，处理USB通信
- **IdCardReader**: 身份证读取器，解析身份证数据
- **IdCardData**: 身份证数据模型，存储姓名、地址等信息

#### 热敏打印机 (printer/)
- **PrinterManager**: 打印机管理器，控制打印任务
- **LabelPrinter**: 标签打印器，生成70×70mm标签
- **PrintCommand**: 打印命令类，封装ESC/POS指令
- **PrintTemplate**: 打印模板管理，支持自定义模板

### 3. UI层 (ui/)

#### 主界面 (main/)
- **MainActivity**: 主活动，应用入口点
- **MainFragment**: 主界面Fragment，导航中心
- **MainViewModel**: 主界面ViewModel，管理界面状态
- **WeightingFragment**: 称重界面，实时显示重量和录入数据

#### 历史记录 (history/)
- **HistoryFragment**: 历史记录显示和管理
- **HistoryViewModel**: 历史记录业务逻辑
- **HistoryAdapter**: RecyclerView适配器
- **SearchFragment**: 高级搜索功能

#### 设置界面 (settings/)
- **SettingsActivity**: 设置活动
- **SettingsFragment**: 设置主界面
- **SettingsViewModel**: 设置业务逻辑
- **DeviceSettingsFragment**: 设备配置界面

### 4. 后台服务 (service/)
- **WeightService**: 称重后台服务，持续监听电子秤数据
- **PrintService**: 打印后台服务，管理打印队列
- **DataSyncService**: 数据同步服务，备份和恢复数据
- **DeviceMonitorService**: 设备监控服务，检测设备连接状态

### 5. 工具类 (utils/)
- **DateUtils**: 日期格式化和计算工具
- **NumberUtils**: 数字格式化和计算工具
- **FileUtils**: 文件读写和管理工具
- **ExportUtils**: 数据导出工具，支持Excel、CSV格式
- **QRCodeUtils**: 二维码生成和识别工具
- **PermissionUtils**: 权限请求和检查工具

### 6. 依赖注入 (di/)
使用Hilt进行依赖注入，模块化管理各层依赖关系

### 7. 常量定义 (constants/)
集中管理应用中使用的各种常量值

## 架构特点

1. **分层架构**: 清晰的分层设计，数据层、业务层、UI层职责分明
2. **MVVM模式**: 使用ViewModel管理UI状态，LiveData实现数据绑定
3. **依赖注入**: 使用Hilt简化依赖管理，提高代码可测试性
4. **响应式编程**: 使用RxJava处理异步操作和数据流
5. **模块化设计**: 各功能模块独立，便于维护和扩展
6. **硬件抽象**: 硬件通信层抽象，支持不同厂商设备
7. **数据持久化**: 使用Room数据库，支持复杂查询和事务

## 技术亮点

1. **串口通信**: 原生C++实现串口通信，稳定可靠
2. **实时数据流**: RxJava实现电子秤数据实时流处理
3. **重量稳定性检测**: 算法检测重量稳定性，提高数据准确性
4. **标签打印**: 支持自定义模板的热敏标签打印
5. **数据导出**: 多格式数据导出，支持Excel、CSV、PDF
6. **设备管理**: 自动检测和管理多种硬件设备
7. **权限管理**: 完善的Android权限管理机制 