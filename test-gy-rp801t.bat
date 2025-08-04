@echo off
chcp 65001 > nul
echo ========================================
echo   GY-RP801T打印机测试程序启动器
echo ========================================
echo.

echo 正在检查环境...

:: 检查Java环境
java -version > nul 2>&1
if errorlevel 1 (
    echo ❌ Java环境未安装或未配置PATH
    echo 请确保已安装Java 17+并配置环境变量
    pause
    exit /b 1
)
echo ✅ Java环境正常

:: 检查Maven环境
mvn -version > nul 2>&1
if errorlevel 1 (
    echo ❌ Maven环境未安装或未配置PATH
    echo 请确保已安装Maven并配置环境变量
    pause
    exit /b 1
)
echo ✅ Maven环境正常

:: 编译项目
echo.
echo 正在编译项目...
call mvn clean compile -q
if errorlevel 1 (
    echo ❌ 项目编译失败
    echo 请检查代码是否有语法错误
    pause
    exit /b 1
)
echo ✅ 项目编译成功

:: 复制依赖
echo.
echo 正在准备依赖...
if not exist "target\dependency" (
    echo 正在下载依赖文件...
    call mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q
    if errorlevel 1 (
        echo ❌ 依赖下载失败
        pause
        exit /b 1
    )
)
echo ✅ 依赖准备完成

:: 检查类文件
if not exist "target\classes\com\tobacco\weight\hardware\printer\GY_RP801T_PrinterTest.class" (
    echo ❌ 测试程序类文件不存在
    echo 请确保编译成功
    pause
    exit /b 1
)

:: 启动程序
echo.
echo ========================================
echo   启动GY-RP801T打印机测试程序
echo ========================================
echo.
echo 💡 使用说明:
echo   1. 如果有实物打印机，选择"连接串口打印机"
echo   2. 如果要模拟测试，先选择"启动打印机模拟器"
echo   3. 安装com0com可以创建虚拟串口用于测试
echo.
echo 正在启动程序...
echo.

java -cp "target/classes;target/dependency/*" com.tobacco.weight.hardware.printer.GY_RP801T_PrinterTest

echo.
echo 程序已退出
pause 