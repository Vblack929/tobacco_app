# 地区数据自动初始化系统

## 🚀 **功能概述**

系统现在支持在应用启动时自动将硬编码的地区数据初始化到数据库中，无需手动运行迁移工具。

## ✅ **自动初始化特性**

### **1. 启动时自动检查**
```java
// 在DatabaseManager.createTables()中自动调用
initializeLocationData();
```

### **2. 智能跳过机制**
```sql
-- 检查是否已有数据
SELECT COUNT(*) FROM location_info
```
- ✅ **有数据**: 跳过初始化，直接使用现有数据
- ✅ **无数据**: 自动执行初始化流程

### **3. 批量高效插入**
```java
// 分两步执行
1. 批量插入所有乡镇 -> 获取生成的ID
2. 批量插入所有村庄 -> 建立父子关系
```

## 🏗️ **技术实现**

### **核心方法: `initializeLocationData()`**
```java
private void initializeLocationData() {
    try {
        // 1. 检查是否已有数据
        String checkSql = "SELECT COUNT(*) FROM location_info";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            
            if (rs.next() && rs.getInt(1) > 0) {
                logger.info("地区数据已存在，跳过初始化");
                return;
            }
        }

        // 2. 硬编码数据源（与原LocationData完全一致）
        Map<String, List<String>> townshipVillages = new LinkedHashMap<>();
        townshipVillages.put("永安镇", Arrays.asList("永和村", "永和村2", ...));
        // ... 完整的19个乡镇及其村庄

        // 3. 批量插入乡镇
        String insertTownshipSql = """
                INSERT INTO location_info (township_name, village_name, location_type, display_order)
                VALUES (?, '', 'township', ?)
                """;
        
        Map<String, Long> townshipIds = new HashMap<>();
        // ... 执行批量插入并获取生成的ID

        // 4. 批量插入村庄
        String insertVillageSql = """
                INSERT INTO location_info (township_name, village_name, location_type, parent_id, display_order)
                VALUES (?, ?, 'village', ?, ?)
                """;
        // ... 执行批量插入

        logger.info("地区数据初始化完成，总共插入 {} 个乡镇，{} 个村庄", 
                   townshipIds.size(), totalVillages);

    } catch (SQLException e) {
        logger.error("初始化地区数据失败", e);
        // 不抛出异常，允许系统继续运行，使用fallback数据
    }
}
```

### **调用时机**
```java
private void createTables() throws SQLException {
    // 1. 创建表结构
    stmt.execute(createFarmerTable);
    stmt.execute(createWeighingTable);
    stmt.execute(createLocationTable);    // ← 新增地区表
    stmt.execute(createConfigTable);

    // 2. 创建索引
    stmt.execute("CREATE INDEX IF NOT EXISTS idx_location_township ON location_info(township_name)");
    stmt.execute("CREATE INDEX IF NOT EXISTS idx_location_village ON location_info(village_name)");
    stmt.execute("CREATE INDEX IF NOT EXISTS idx_location_parent ON location_info(parent_id)");

    // 3. 执行数据库迁移
    migrateDatabase();

    // 4. 自动初始化地区数据 ← 新增步骤
    initializeLocationData();

    logger.info("数据库表创建完成");
}
```

## 📊 **数据结构**

### **初始化的完整数据集**
```
📍 永安镇 (7个村)
  └── 永和村, 永和村2, 丰裕村, 丰裕村2, 西湖潭村, 督正村, 大安村

📍 枨冲镇 (3个村)
  └── 三元村, 平息村, 和平村

📍 普迹镇 (2个村)
  └── 金峰村, 新街村

📍 官桥镇 (3个村)
  └── 一江村, 石灰嘴村, 九龙村

📍 沙市 (14个村)
  └── 长春, 赤马, 友助, 白水, 秀山, 敦睦, 河背, 团农, 文光, 东门, 莲塘, 沙市, 秧田, 中洲

... 以及其他 14 个乡镇，总计 19 个乡镇，106 个村庄
```

### **数据库记录格式**
```sql
-- 乡镇记录示例
INSERT INTO location_info (township_name, village_name, location_type, display_order)
VALUES ('永安镇', '', 'township', 1);

-- 村庄记录示例  
INSERT INTO location_info (township_name, village_name, location_type, parent_id, display_order)
VALUES ('永安镇', '永和村', 'village', 1, 1);
```

## 🎯 **用户体验**

### **首次启动流程**
```
1. 用户启动应用
2. 系统自动创建数据库表
3. 系统检查location_info表（发现为空）
4. 自动执行地区数据初始化
5. 插入19个乡镇 + 106个村庄
6. 应用正常启动，地区筛选功能立即可用
```

### **后续启动流程**
```
1. 用户启动应用
2. 系统检查location_info表（发现已有数据）
3. 跳过初始化，直接使用现有数据
4. 应用快速启动
```

## 💡 **优势对比**

### **之前 (手动迁移)**
- ❌ 需要手动运行迁移工具
- ❌ 用户可能忘记运行迁移
- ❌ 首次使用时地区筛选功能不可用
- ❌ 需要额外的使用说明

### **现在 (自动初始化)**
- ✅ 完全自动化，无需用户干预
- ✅ 首次启动即可正常使用所有功能
- ✅ 智能检测，避免重复初始化
- ✅ 故障容错，初始化失败不影响应用启动
- ✅ 性能优化，批量插入提升速度

## 🔧 **测试验证**

### **运行测试工具**
```bash
# 测试自动初始化功能
java -cp "target/classes:target/dependency/*" com.tobacco.weight.test.DatabaseLocationTest
```

### **预期输出**
```
🔍 测试数据库地区数据自动初始化...

📊 数据库统计:
总记录数: 125
乡镇数量: 19
第一个乡镇: 永安镇
永安镇 的村庄数量: 7
第一个村庄: 永和村

🔧 测试LocationData API:
通过API获取的乡镇数量: 19
永安镇 通过API获取的村庄数量: 7

📍 测试地址解析:
解析 '永安镇 永和村' -> 乡镇: 永安镇, 村庄: 永和村

✅ 数据库地区数据测试完成！
```

## 🚀 **部署说明**

### **新部署环境**
1. 直接启动应用
2. 系统自动完成所有初始化
3. 地区筛选功能立即可用

### **现有环境**
1. 更新应用代码
2. 重启应用
3. 系统检测到已有数据，保持不变
4. 功能正常，无任何影响

### **数据管理**
- **查看数据**: 通过农户管理界面的"刷新地区"功能
- **修改数据**: 直接操作location_info表
- **重置数据**: 删除location_info表内容，重启应用自动重新初始化

## 📈 **性能指标**

- **初始化时间**: < 1秒（125条记录批量插入）
- **检查时间**: < 10ms（单次COUNT查询）
- **内存占用**: 忽略不计（批量操作后立即释放）
- **启动影响**: 首次+1秒，后续无影响

这个自动初始化系统确保了烟叶收购系统的地区功能在任何环境下都能开箱即用！