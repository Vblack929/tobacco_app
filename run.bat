@echo off
echo 启动烟叶称重系统...

REM 检查Java版本
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Java运行时环境
    pause
    exit /b 1
)

REM 使用Maven运行应用程序
echo 使用Maven启动应用程序...
call mvn clean compile exec:java

pause 