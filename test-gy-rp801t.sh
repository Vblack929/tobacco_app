#!/bin/bash

echo "========================================"
echo "  GY-RP801T打印机测试程序启动器 (Git Bash版)"
echo "========================================"
echo

echo "正在检查环境..."

# 检查Java环境
if java -version >/dev/null 2>&1; then
    echo "✅ Java环境正常"
else
    echo "❌ Java环境未安装或未配置PATH"
    echo "请确保已安装Java 17+并配置环境变量"
    read -p "按回车键退出..."
    exit 1
fi

# 检查Maven环境
if mvn -version >/dev/null 2>&1; then
    echo "✅ Maven环境正常"
else
    echo "❌ Maven环境未安装或未配置PATH"
    echo "请确保已安装Maven并配置环境变量"
    read -p "按回车键退出..."
    exit 1
fi

# 编译项目
echo
echo "正在编译项目..."
if mvn clean compile -q; then
    echo "✅ 项目编译成功"
else
    echo "❌ 项目编译失败"
    echo "请检查代码是否有语法错误"
    read -p "按回车键退出..."
    exit 1
fi

# 复制依赖
echo
echo "正在准备依赖..."
if [ ! -d "target/dependency" ]; then
    echo "正在下载依赖文件..."
    if mvn dependency:copy-dependencies -DoutputDirectory=target/dependency -q; then
        echo "✅ 依赖下载成功"
    else
        echo "❌ 依赖下载失败"
        read -p "按回车键退出..."
        exit 1
    fi
else
    echo "✅ 依赖已存在"
fi

# 检查类文件
if [ ! -f "target/classes/com/tobacco/weight/hardware/printer/GY_RP801T_PrinterTest.class" ]; then
    echo "❌ 测试程序类文件不存在"
    echo "请确保编译成功"
    read -p "按回车键退出..."
    exit 1
fi

# 启动程序
echo
echo "========================================"
echo "  启动GY-RP801T打印机测试程序"
echo "========================================"
echo
echo "💡 使用说明:"
echo "  1. 如果有实物打印机，选择\"连接串口打印机\""
echo "  2. 如果要模拟测试，先选择\"启动打印机模拟器\""
echo "  3. 安装com0com可以创建虚拟串口用于测试"
echo
echo "正在启动程序..."
echo

# 使用冒号作为classpath分隔符（Git Bash兼容）
java -cp "target/classes:target/dependency/*" com.tobacco.weight.hardware.printer.GY_RP801T_PrinterTest

echo
echo "程序已退出"
read -p "按回车键退出..." 