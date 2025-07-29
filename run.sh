#!/bin/bash

echo "启动烟叶称重系统..."

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请安装JDK 11或更高版本"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven环境，请安装Maven 3.6或更高版本"
    exit 1
fi

echo "正在编译项目..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误信息"
    exit 1
fi

echo "正在启动应用程序..."
mvn javafx:run

if [ $? -ne 0 ]; then
    echo "启动失败，请检查错误信息"
    exit 1
fi 