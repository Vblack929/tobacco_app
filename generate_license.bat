@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ========================================
echo 烟叶称重系统 - 动态绑定许可证生成工具
echo ========================================
echo.

:: 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误：未找到Java运行环境，请确保已安装Java 8或更高版本
    pause
    exit /b 1
)

:: 设置类路径
set CLASSPATH=target\classes;target\lib\*

:: 检查编译后的类文件是否存在
if not exist "target\classes\com\tobacco\weight\license\LicenseIdGenerator.class" (
    echo 正在编译许可证生成器...
    call mvn compile -q
    if errorlevel 1 (
        echo 错误：编译失败，请检查项目配置
        pause
        exit /b 1
    )
)

echo 请输入许可证信息：
echo.

:: 获取客户名称
set /p CUSTOMER_NAME="客户名称（可选，直接回车跳过）: "
if "!CUSTOMER_NAME!"=="" set CUSTOMER_NAME=默认客户

:: 获取最大设备数
set /p MAX_DEVICES="最大设备数（默认2台）: "
if "!MAX_DEVICES!"=="" set MAX_DEVICES=2

:: 验证设备数是否为数字
echo !MAX_DEVICES! | findstr /r "^[1-9][0-9]*$" >nul
if errorlevel 1 (
    echo 错误：设备数必须是正整数
    pause
    exit /b 1
)

:: 获取有效天数
set /p VALID_DAYS="有效天数（默认365天）: "
if "!VALID_DAYS!"=="" set VALID_DAYS=365

:: 验证天数是否为数字
echo !VALID_DAYS! | findstr /r "^[1-9][0-9]*$" >nul
if errorlevel 1 (
    echo 错误：有效天数必须是正整数
    pause
    exit /b 1
)

echo.
echo 正在生成许可证...
echo.

:: 调用Java程序生成许可证
java -cp "%CLASSPATH%" com.tobacco.weight.license.LicenseIdGenerator "!CUSTOMER_NAME!" !MAX_DEVICES! !VALID_DAYS!

if errorlevel 1 (
    echo.
    echo 错误：许可证生成失败
    pause
    exit /b 1
)

echo.
echo 许可证生成完成！
echo.
echo 使用说明：
echo 1. 将生成的许可证ID提供给客户
echo 2. 客户在软件中输入许可证ID进行激活
echo 3. 软件会自动绑定客户的设备
echo 4. 同一许可证可在指定数量的设备上使用
echo.

pause