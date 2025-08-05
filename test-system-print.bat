@echo off
setlocal enabledelayedexpansion

echo ========================================
echo   系统打印功能测试
echo ========================================
echo.

REM 编译项目
echo 正在编译项目...
mvn compile -q
if errorlevel 1 (
    echo [错误] 项目编译失败
    pause
    exit /b 1
)

REM 准备依赖
echo 正在准备依赖...
mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q

echo.
echo ========================================
echo   启动系统打印测试
echo ========================================
echo.

REM 运行测试
java -cp "target/classes;target/dependency/*" com.tobacco.weight.test.SystemPrintTest

echo.
echo ========================================
echo   测试完成
echo ========================================
pause 