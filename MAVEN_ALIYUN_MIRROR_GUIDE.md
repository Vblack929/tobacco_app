# Maven 阿里云镜像源配置指南

## 概述

在中国境内使用Maven下载依赖时，由于网络原因可能会遇到下载缓慢或失败的问题。本项目已配置阿里云镜像源来解决这个问题。

## 配置说明

### 1. 项目级别配置（pom.xml）

项目的 `pom.xml` 文件中已经添加了阿里云镜像源配置：

- **阿里云中央仓库**: `https://maven.aliyun.com/repository/central`
- **阿里云公共仓库**: `https://maven.aliyun.com/repository/public`
- **阿里云Spring仓库**: `https://maven.aliyun.com/repository/spring`
- **备用Maven中央仓库**: `https://repo1.maven.org/maven2`

### 2. 全局级别配置（settings.xml）

项目根目录下提供了 `settings.xml` 文件，可以复制到以下位置之一：

#### Windows系统：
```
C:\Users\{用户名}\.m2\settings.xml
```

#### Linux/Mac系统：
```
~/.m2/settings.xml
```

### 3. 使用方法

#### 方法一：使用项目提供的settings.xml
```bash
# 复制settings.xml到用户目录
cp settings.xml %USERPROFILE%\.m2\settings.xml

# 或者使用Maven命令指定settings文件
mvn clean install -s settings.xml
```

#### 方法二：仅使用pom.xml配置
项目的pom.xml已经包含了镜像源配置，直接运行Maven命令即可：
```bash
mvn clean install
```

## 配置优先级

1. **settings.xml镜像配置** - 全局生效，优先级最高
2. **pom.xml仓库配置** - 项目级别，作为备用
3. **Maven默认中央仓库** - 最后备用

## 故障排除

### 如果依然无法下载依赖：

1. **检查网络连接**
   ```bash
   ping maven.aliyun.com
   ```

2. **清理本地仓库缓存**
   ```bash
   mvn dependency:purge-local-repository
   ```

3. **强制更新依赖**
   ```bash
   mvn clean install -U
   ```

4. **查看详细日志**
   ```bash
   mvn clean install -X
   ```

### 常见问题：

- **SSL证书问题**: 确保Java版本支持TLS 1.2+
- **代理设置**: 如果使用公司代理，需要在settings.xml中配置代理
- **防火墙**: 确保443端口（HTTPS）未被阻止

## 验证配置

运行以下命令验证镜像源是否生效：
```bash
mvn help:effective-settings
mvn dependency:resolve-sources
```

## 注意事项

1. 阿里云镜像源通常比官方仓库快3-10倍
2. 镜像源会定期同步，可能有几小时的延迟
3. 如果某个依赖在阿里云找不到，会自动回退到Maven中央仓库
4. 建议定期更新Maven版本以获得更好的性能

## 相关链接

- [阿里云Maven仓库](https://maven.aliyun.com/mvn/guide)
- [Maven官方文档](https://maven.apache.org/guides/)
- [Maven Settings参考](https://maven.apache.org/settings.html)