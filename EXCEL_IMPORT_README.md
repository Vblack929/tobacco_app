# 农户合同Excel导入功能

## 🎯 **功能概述**

新增的Excel导入功能允许批量导入农户信息和合同数据，支持数据验证、地区映射、幂等操作和详细报告。

## 📋 **支持的Excel格式**

### **必需列（列头名称）**
- `站点名称` - 收购站点
- `姓名` - 农户姓名  
- `种植者编号（合同号）` 或 `种植者编号` - 合同编号
- `身份证号` - 18位身份证号码
- `行政区划` - 地区信息，如"永安镇督正村"
- `合同量` - 合同金额（支持千分位格式如"6,130.00"）

### **Excel文件要求**
- **格式支持**：`.xlsx` (Office 2007+) 和 `.xls` (Office 97-2003)
- **加密支持**：支持密码保护的Excel文件（系统会自动尝试密码0807）
- **自动检测**：系统会根据文件扩展名和内容自动选择合适的解析器
- **文件结构**：第一行可以为空（会自动跳过），第二行为列头，数据从第三行开始

### **示例数据格式**
```
站点名称    | 姓名   | 种植者编号      | 身份证号           | 行政区划     | 合同量
永安站中心点 | 伍丰虎  | 4301811565000434| 430181197910160317| 永安镇督正村  | 6,130.00
```

## 🔧 **核心功能特性**

### **1. 数据预处理**
- ✅ 自动跳过空行
- ✅ 智能识别列头位置
- ✅ 清理隐藏字符和多余空格
- ✅ 规范化数值格式

### **2. 数据验证**
- ✅ **身份证号**：格式检查 + 校验位验证
- ✅ **合同金额**：非负数验证 + 合理性检查
- ✅ **地区解析**：行政区划自动映射到乡镇+村庄
- ✅ **必填字段**：完整性验证

### **3. 幂等操作**
- ✅ **农户主键**：身份证号（更新已存在，插入新农户）
- ✅ **合同主键**：身份证号+合同号（更新金额，插入新合同）
- ✅ **冲突处理**：姓名变化警告，金额冲突保留最新值

### **4. 导入模式**
- 🔍 **预览模式**：验证数据但不写入数据库
- 💾 **正式导入**：验证通过后写入数据库
- 📊 **详细报告**：成功/警告/错误统计及详情

## 📊 **数据库表结构**

### **新增表：farmer_contracts**
```sql
CREATE TABLE farmer_contracts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    national_id TEXT NOT NULL,           -- 身份证号
    contract_no TEXT NOT NULL,           -- 合同号  
    contract_amount REAL NOT NULL,       -- 合同金额
    station TEXT,                        -- 站点名称
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(national_id, contract_no)     -- 复合主键防重复
);
```

### **更新表：farmer_info**
- 现有结构保持不变
- 通过身份证号关联合同信息

## 🚀 **使用方法**

### **1. 通过UI界面**
1. 打开主程序
2. 点击"农户注册管理"按钮
3. 点击"导入Excel"按钮
4. 选择"预览模式"或"正式导入"
5. 选择Excel文件
6. 查看导入报告

### **2. 程序化调用**
```java
// 初始化服务
DatabaseManager dbManager = DatabaseManager.getInstance();
FarmerContractImportService importService = new FarmerContractImportService(dbManager);

// 执行导入（预览模式）
File excelFile = new File("农户合同信息.xlsx");
ImportReport report = importService.importExcel(excelFile, true);

// 查看结果
System.out.println(report.generateSummary());
if (report.hasIssues()) {
    System.out.println(report.generateDetailedReport());
}
```

### **3. 命令行测试**
```bash
# 编译并运行测试类
javac -cp "lib/*:." src/main/java/com/tobacco/weight/test/ExcelImportTest.java
java -cp "lib/*:." com.tobacco.weight.test.ExcelImportTest
```

## 📋 **导入报告示例**

### **成功导入报告**
```
=== 导入报告摘要 ===
文件名: 农户合同信息.xlsx
导入时间: 2024-01-15 14:30:22
模式: 正式导入

处理统计:
- 总记录数: 150
- 成功处理: 148
- 警告记录: 2
- 错误记录: 0

数据库操作:
- 新增农户: 95
- 更新农户: 53
- 新增合同: 142
- 更新合同: 6
```

### **错误记录格式**
```csv
行号,站点,姓名,合同号,身份证号,行政区划,合同量,状态,错误信息
15,永安站,张三,HT001,******1234,永安镇xxx村,5000.0,ERROR,无法解析行政区划 '永安镇xxx村'
27,中心站,李四,HT002,******5678,督正镇李村,0,WARNING,合同金额为0，请确认是否正确
```

## ⚠️ **注意事项**

### **数据质量要求**
1. **身份证号**必须为有效的18位格式
2. **行政区划**必须在系统地区注册表中存在
3. **合同金额**不能为负数
4. **姓名变化**会产生警告但允许更新

### **性能考虑**
- 大文件建议先用预览模式验证
- 每行采用独立事务，失败行不影响其他行
- 地区映射结果会缓存以提升性能

### **安全措施**
- 身份证号在日志中自动脱敏显示（******1234）
- 预览模式不会修改任何数据库数据
- 错误记录可导出CSV用于问题排查

## 🔗 **相关文件**

### **核心类文件**
- `FarmerContractImportService.java` - 主导入服务
- `ImportRecord.java` - 单行记录模型
- `ImportReport.java` - 导入报告模型
- `ValidationService.java` - 数据验证服务
- `LocationMappingService.java` - 地区映射服务

### **数据库相关**
- `DatabaseManager.java` - 新增farmer_contracts表
- `LocationInfoDao.java` - 地区信息查询

### **UI集成**
- `FarmerRegistrationWindow.java` - Excel导入界面

## 🛠️ **故障排除**

### **常见错误及解决方案**

| 错误信息 | 原因 | 解决方案 |
|---------|------|---------|
| "The supplied data appears to be in the OLE2 Format" | Excel格式不匹配 | ✅ 已修复：自动支持.xls和.xlsx格式 |
| "Encrypted .xlsx file. It must be decrypted" | Excel文件加密 | ✅ 已修复：自动尝试密码0807 |
| 文件打开失败/密码错误 | Excel文件加密 | ✅ 已修复：自动尝试密码0807 |
| "未找到有效的列头行" | Excel列头不匹配 | 检查列头名称是否正确 |
| "身份证号格式不正确" | 身份证号格式错误 | 确保18位有效身份证号 |
| "无法解析行政区划" | 地区不在注册表中 | 先在地区管理中添加该地区 |
| "合同金额不能为负数" | 数值格式问题 | 检查Excel中的数值格式 |

### **调试技巧**
1. 使用预览模式先验证数据
2. 查看详细报告了解具体错误
3. 导出错误CSV文件逐行检查
4. 检查数据库中的location_info表确认地区数据

这个Excel导入功能提供了完整的企业级数据导入解决方案，具备强大的验证、报告和错误处理能力。
