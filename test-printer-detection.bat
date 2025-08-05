@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   打印机自动检测测试程序
echo ========================================
echo.

REM 检查Java环境
echo 正在检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Java环境，请安装Java 17或更高版本
    pause
    exit /b 1
)
echo [成功] Java环境正常

REM 检查Maven环境
echo 正在检查Maven环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Maven环境，请安装Maven
    pause
    exit /b 1
)
echo [成功] Maven环境正常

echo.
echo 正在编译项目...
mvn compile -q
if errorlevel 1 (
    echo [错误] 项目编译失败
    pause
    exit /b 1
)
echo [成功] 项目编译完成

echo.
echo 正在准备依赖...
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
if errorlevel 1 (
    echo [错误] 依赖准备失败
    pause
    exit /b 1
)
echo [成功] 依赖准备完成

echo.
echo ========================================
echo   启动打印机自动检测测试
echo ========================================
echo.

REM 运行测试程序
java -cp "target/classes;target/dependency/*" com.tobacco.weight.test.PrinterAutoDetectionTest

echo.
echo ========================================
echo   测试完成
echo ========================================
pause 