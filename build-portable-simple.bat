@echo off
chcp 65001
echo ========================================
echo 生成便携式程序包 - 简化版
echo ========================================

REM 编译项目（跳过清理）
echo [1/4] 编译项目...
call mvn package -Dmaven.clean.skip=true
if errorlevel 1 (
    echo 错误：编译失败！
    pause
    exit /b 1
)

REM 检查JAR文件
if not exist "target\tobacco-weight-windows-1.0.0.jar" (
    echo 错误：JAR文件未生成！
    pause
    exit /b 1
)

REM 准备JavaFX依赖
echo [2/4] 准备JavaFX依赖...
if not exist "target\lib" mkdir "target\lib"
call mvn dependency:copy-dependencies -DoutputDirectory=target/lib -DincludeArtifactIds=javafx-controls,javafx-fxml,javafx-graphics,javafx-base

REM 创建便携式包
echo [3/4] 创建便携式包...
if not exist "target\烟叶称重系统-便携版" mkdir "target\烟叶称重系统-便携版"
copy "target\tobacco-weight-windows-1.0.0.jar" "target\烟叶称重系统-便携版\"
xcopy "target\lib" "target\烟叶称重系统-便携版\lib\" /E /I /Y

REM 创建启动脚本
echo [4/4] 创建启动脚本...
echo @echo off > "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo chcp 65001 >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo echo 正在启动烟叶称重系统... >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo cd /d "%%~dp0" >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo java --module-path "lib\javafx-controls-17.0.2-win.jar;lib\javafx-fxml-17.0.2-win.jar;lib\javafx-graphics-17.0.2-win.jar;lib\javafx-base-17.0.2-win.jar" --add-modules javafx.controls,javafx.fxml,javafx.graphics -Dfile.encoding=UTF-8 -jar tobacco-weight-windows-1.0.0.jar >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo if errorlevel 1 ( >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo     echo 程序启动失败！ >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo     echo 请检查Java版本是否为17或更高版本 >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo     pause >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"
echo ) >> "target\烟叶称重系统-便携版\启动烟叶称重系统.bat"

echo.
echo ========================================
echo 便携式程序包创建成功！
echo ========================================
echo.
echo 文件位置：target\烟叶称重系统-便携版\
echo.
echo 使用方法：
echo 1. 将整个"烟叶称重系统-便携版"文件夹复制到目标机器
echo 2. 确保目标机器安装了Java 17+
echo 3. 双击运行：启动烟叶称重系统.bat
echo.

pause 