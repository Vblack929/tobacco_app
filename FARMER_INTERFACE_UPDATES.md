# 农户注册管理界面 - 数据共享更新

## 🔄 **更新说明**

已成功修改农户注册管理界面，现在与现有数据库完全共享数据，并移除了民族/种族信息显示。

## ✅ **主要修改内容**

### 1. **数据共享实现**
- **数据源改变**: 农户界面现在从 `weighing_records` 表获取农户信息
- **智能关联**: 自动关联 `weighing_records` 和 `farmer_info` 表数据
- **实时同步**: 所有在称重系统中出现的农户都会自动在农户管理界面中显示

### 2. **移除民族信息**
- **UI界面**: 从农户列表表格中移除"民族"列
- **详情对话框**: 从农户详情中移除民族信息显示
- **数据模型**: 简化显示模型，不再包含民族字段

### 3. **数据查询优化**
```sql
-- 新的查询逻辑：从称重记录获取农户，关联农户详细信息
SELECT DISTINCT 
    wr.farmer_name,
    wr.contract_number,
    wr.id_card_number,
    fi.gender,
    fi.birth_date,
    fi.address,
    fi.department,
    fi.start_date,
    fi.end_date,
    fi.photo
FROM weighing_records wr
LEFT JOIN farmer_info fi ON wr.id_card_number = fi.id_card_number 
    OR (wr.farmer_name = fi.farmer_name AND wr.contract_number = fi.contract_number)
WHERE wr.farmer_name IS NOT NULL AND wr.farmer_name != ''
ORDER BY wr.farmer_name
```

## 📊 **现在的界面显示**

农户列表现在包含以下列：
- ✅ 农户姓名
- ✅ 身份证号（脱敏显示）
- ✅ 合同号
- ✅ 地址
- ✅ 性别
- ✅ 状态
- ❌ ~~民族~~ (已移除)

## 🔗 **数据流程**

```
称重操作 → weighing_records 表 → 农户注册管理界面
    ↓
自动显示新农户 (无需手动添加)
    ↓
关联 farmer_info 表获取详细信息 (如果存在)
```

## 🚀 **如何使用**

1. **启动应用**: 运行主程序
2. **进行称重操作**: 输入农户信息并完成称重
3. **查看农户列表**: 点击"农户注册管理"按钮
4. **自动显示**: 刚才称重的农户自动出现在列表中

## 🧪 **测试数据**

可以使用以下命令加载测试数据：

### 加载称重记录（推荐）
```bash
java -cp "target/classes;target/dependency/*" com.tobacco.weight.test.SampleWeighingDataLoader
```
这将创建15条称重记录，涉及多个农户，农户管理界面将自动显示这些农户。

### 加载农户信息（可选）
```bash
java -cp "target/classes;target/dependency/*" com.tobacco.weight.test.SampleFarmerDataLoader
```
这将在farmer_info表中添加详细的农户信息，提供更完整的农户详情。

## 📈 **优势**

1. **数据一致性**: 农户界面和称重系统使用相同的数据源
2. **自动同步**: 新的称重记录自动创建农户条目
3. **简化界面**: 移除不必要的民族信息显示
4. **高效查询**: 优化的SQL查询提高性能
5. **智能关联**: 自动匹配农户详细信息

## 🔍 **搜索和筛选功能**

- **搜索**: 按农户姓名或合同号搜索（基于称重记录）
- **地址筛选**: 按地址筛选（基于farmer_info表中的地址信息）
- **实时过滤**: 搜索和筛选可以组合使用

现在农户注册管理界面完全与现有的称重数据库共享数据，确保了数据的一致性和完整性！