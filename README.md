# 烟叶称重管理系统 (Tobacco Weight Management System)

## 项目概述
一款运行在Android 13平板上的烟叶称重管理应用，通过串口通信读取电子秤数据，USB读取身份证信息，实现称重数据记录、标签打印和历史查询功能。

## 技术栈
- **开发语言**: Java (主要) + Kotlin (部分组件)
- **架构模式**: MVVM (Model-View-ViewModel)
- **UI框架**: Android原生 + Material Design
- **数据库**: Room (SQLite)
- **串口通信**: android-serialport-api
- **依赖注入**: Hilt
- **响应式编程**: LiveData + RxJava3
- **导航**: Navigation Component

## 核心功能模块
1. **设备管理**: 电子秤、身份证读卡器、热敏打印机的连接检测与管理
2. **实时称重**: 电子秤数据实时显示，身份证信息自动读取
3. **数据录入**: 烟叶信息手动录入（捆数、部位、预检编号等）
4. **数据存储**: 本地数据库存储，支持数据备份导出
5. **标签打印**: 自定义模板，70×70mm热敏标签打印
6. **历史查询**: 记录浏览、搜索、统计分析
7. **系统设置**: 设备配置、模板编辑、参数设置

## 项目架构
采用MVVM架构模式，结合Android Jetpack组件：
- **View**: Activity/Fragment + Data Binding
- **ViewModel**: 处理业务逻辑，管理UI状态
- **Model**: Repository模式，统一数据访问接口
- **数据层**: Room数据库 + 串口通信服务

## 开发环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11 或更高版本
- Android SDK API 33 (Android 13)
- 最低支持 API 26 (Android 8.0)

## 硬件要求
- Android 13平板设备
- RS232串口转USB适配器
- 电子秤（支持RS232通信）
- 身份证读卡器（USB接口）
- 热敏打印机（70×70mm，支持串口/USB） 