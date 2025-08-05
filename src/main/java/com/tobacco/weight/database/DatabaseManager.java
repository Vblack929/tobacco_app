package com.tobacco.weight.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.List;

/**
 * 数据库管理器
 * 负责SQLite数据库的初始化、连接和基本操作
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String DB_NAME = "tobacco_weight.db";
    private static final String DB_URL_PREFIX = "jdbc:sqlite:";

    private static DatabaseManager instance;
    private String dbPath;
    private Connection connection;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * 初始化数据库
     */
    private void initializeDatabase() {
        try {
            // 创建数据目录
            File dataDir = new File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                logger.info("创建数据目录: {}", dataDir.getAbsolutePath());
            }

            // 设置数据库路径
            dbPath = "data/" + DB_NAME;
            logger.info("数据库路径: {}", dbPath);

            // 加载SQLite驱动
            Class.forName("org.sqlite.JDBC");

            // 创建数据库连接
            connection = DriverManager.getConnection(DB_URL_PREFIX + dbPath);
            logger.info("数据库连接成功");

            // 创建表结构
            createTables();

        } catch (Exception e) {
            logger.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // 创建烟农信息表
            String createFarmerTable = """
                    CREATE TABLE IF NOT EXISTS farmer_info (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        farmer_name TEXT NOT NULL,
                        contract_number TEXT NOT NULL,
                        id_card_number TEXT,
                        gender TEXT,
                        nationality TEXT,
                        birth_date TEXT,
                        address TEXT,
                        department TEXT,
                        start_date TEXT,
                        end_date TEXT,
                        photo BLOB,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            // 创建称重记录表
            String createWeighingTable = """
                    CREATE TABLE IF NOT EXISTS weighing_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        precheck_id TEXT NOT NULL,
                        farmer_name TEXT NOT NULL,
                        contract_number TEXT NOT NULL,
                        leaf_type TEXT NOT NULL,
                        weight REAL NOT NULL,
                        operator TEXT,
                        warehouse_number TEXT,
                        status TEXT DEFAULT '正常',
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        id_card_number TEXT
                    )
                    """;

            // 创建地区信息表
            String createLocationTable = """
                    CREATE TABLE IF NOT EXISTS location_info (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        township_name TEXT NOT NULL,
                        village_name TEXT NOT NULL,
                        location_type TEXT NOT NULL CHECK (location_type IN ('township', 'village')),
                        parent_id INTEGER,
                        display_order INTEGER DEFAULT 0,
                        is_active BOOLEAN DEFAULT 1,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (parent_id) REFERENCES location_info(id),
                        UNIQUE(township_name, village_name)
                    )
                    """;

            // 创建系统配置表
            String createConfigTable = """
                    CREATE TABLE IF NOT EXISTS system_config (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        config_key TEXT UNIQUE NOT NULL,
                        config_value TEXT,
                        description TEXT,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """;

            stmt.execute(createFarmerTable);
            stmt.execute(createWeighingTable);
            stmt.execute(createLocationTable);
            stmt.execute(createConfigTable);

            // 创建地区信息表索引以提升查询性能
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_location_township ON location_info(township_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_location_village ON location_info(village_name)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_location_parent ON location_info(parent_id)");

            // 执行数据库迁移
            migrateDatabase();

            // 初始化地区数据
            initializeLocationData();

            logger.info("数据库表创建完成");

        } catch (SQLException e) {
            logger.error("创建数据库表失败", e);
            throw e;
        }
    }

    /**
     * 数据库迁移
     */
    private void migrateDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            // 检查weighing_records表是否存在id_card_number列
            try {
                stmt.execute("SELECT id_card_number FROM weighing_records LIMIT 1");
                logger.info("数据库表结构已是最新版本");
            } catch (SQLException e) {
                // 如果列不存在，添加该列
                logger.info("检测到旧版本数据库，开始迁移...");

                // 添加id_card_number列
                stmt.execute("ALTER TABLE weighing_records ADD COLUMN id_card_number TEXT");
                logger.info("已添加id_card_number列到weighing_records表");

                // 更新系统配置，记录迁移版本
                stmt.execute("""
                        INSERT OR REPLACE INTO system_config (config_key, config_value, description)
                        VALUES ('db_version', '2', '数据库版本号')
                        """);
                logger.info("数据库迁移完成，版本更新为2");
            }

        } catch (SQLException e) {
            logger.error("数据库迁移失败", e);
            throw e;
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL_PREFIX + dbPath);
        }
        return connection;
    }

    /**
     * 初始化地区数据
     */
    private void initializeLocationData() {
        try {
            // 检查是否已有地区数据
            String checkSql = "SELECT COUNT(*) FROM location_info";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                
                if (rs.next() && rs.getInt(1) > 0) {
                    logger.info("地区数据已存在，跳过初始化");
                    return;
                }
            }

            logger.info("开始初始化地区数据...");
            
            // 硬编码的地区数据
            Map<String, List<String>> townshipVillages = new LinkedHashMap<>();
            townshipVillages.put("永安镇", Arrays.asList("永和村", "永和村2", "丰裕村", "丰裕村2", "西湖潭村", "督正村", "大安村"));
            townshipVillages.put("枨冲镇", Arrays.asList("三元村", "平息村", "和平村"));
            townshipVillages.put("普迹镇", Arrays.asList("金峰村", "新街村"));
            townshipVillages.put("官桥镇", Arrays.asList("一江村", "石灰嘴村", "九龙村"));
            townshipVillages.put("沙市", Arrays.asList("长春", "赤马", "友助", "白水", "秀山", "敦睦", "河背", "团农", "文光", "东门", "莲塘", "沙市", "秧田", "中洲"));
            townshipVillages.put("龙伏", Arrays.asList("坪上", "新开", "焦桥", "达峰", "黄桥", "泮春", "相市", "龙伏"));
            townshipVillages.put("社港", Arrays.asList("合盛", "淮洲", "清江", "源田", "社港", "新光", "永兴", "浏北"));
            townshipVillages.put("淳口", Arrays.asList("高田", "鹤源", "黄荆坪", "羊古滩", "农大", "同辉", "谢家", "南冲", "鸭头", "狮岩"));
            townshipVillages.put("北盛", Arrays.asList("拔茅", "百塘", "亚洲湖", "卓然", "燕舞洲", "窑金", "边洲", "乌龙", "泉水", "马战", "仓胜", "环园"));
            townshipVillages.put("洞阳", Arrays.asList("洞阳"));
            townshipVillages.put("金云", Arrays.asList("金云"));
            townshipVillages.put("大围山", Arrays.asList("中岳村", "北麓园村"));
            townshipVillages.put("达浒", Arrays.asList("麻洲社区", "金田村", "长丰村", "书香村", "象形村"));
            townshipVillages.put("沿溪", Arrays.asList("大光村", "金桔村", "礼花村", "沙龙村"));
            townshipVillages.put("官渡", Arrays.asList("观音塘村", "田郊村", "兵和村", "新云山", "竹山社区", "南岳社区", "竹联村"));
            townshipVillages.put("张坊", Arrays.asList("白石村", "茶林村", "陈桥村", "江口村", "人溪村", "张家坊社区"));
            townshipVillages.put("小河", Arrays.asList("皇碑村", "潭湾村", "田心村", "乌石村", "新河村"));
            townshipVillages.put("高坪", Arrays.asList("船仓村"));
            townshipVillages.put("古港", Arrays.asList("白露村", "宝盖寺村", "燕港村", "东盈村", "花城村", "华湘村", "金园村", "三口村"));
            townshipVillages.put("关口", Arrays.asList("金湖村"));

            // 先插入乡镇
            String insertTownshipSql = """
                    INSERT INTO location_info (township_name, village_name, location_type, display_order)
                    VALUES (?, '', 'township', ?)
                    """;

            Map<String, Long> townshipIds = new HashMap<>();
            int townshipOrder = 1;

            try (PreparedStatement pstmt = connection.prepareStatement(insertTownshipSql, Statement.RETURN_GENERATED_KEYS)) {
                for (String townshipName : townshipVillages.keySet()) {
                    pstmt.setString(1, townshipName);
                    pstmt.setInt(2, townshipOrder++);
                    pstmt.addBatch();
                }
                
                int[] results = pstmt.executeBatch();
                
                // 获取生成的乡镇ID
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    int index = 0;
                    for (String townshipName : townshipVillages.keySet()) {
                        if (rs.next()) {
                            townshipIds.put(townshipName, rs.getLong(1));
                        }
                        index++;
                    }
                }
                
                logger.info("插入了 {} 个乡镇", results.length);
            }

            // 再插入村庄
            String insertVillageSql = """
                    INSERT INTO location_info (township_name, village_name, location_type, parent_id, display_order)
                    VALUES (?, ?, 'village', ?, ?)
                    """;

            int totalVillages = 0;
            try (PreparedStatement pstmt = connection.prepareStatement(insertVillageSql)) {
                for (Map.Entry<String, List<String>> entry : townshipVillages.entrySet()) {
                    String townshipName = entry.getKey();
                    List<String> villages = entry.getValue();
                    Long townshipId = townshipIds.get(townshipName);
                    
                    if (townshipId == null) {
                        logger.error("找不到乡镇ID: {}", townshipName);
                        continue;
                    }
                    
                    int villageOrder = 1;
                    for (String villageName : villages) {
                        pstmt.setString(1, townshipName);
                        pstmt.setString(2, villageName);
                        pstmt.setLong(3, townshipId);
                        pstmt.setInt(4, villageOrder++);
                        pstmt.addBatch();
                        totalVillages++;
                    }
                }
                
                int[] results = pstmt.executeBatch();
                logger.info("插入了 {} 个村庄", results.length);
            }

            logger.info("地区数据初始化完成，总共插入 {} 个乡镇，{} 个村庄", 
                       townshipIds.size(), totalVillages);

        } catch (SQLException e) {
            logger.error("初始化地区数据失败", e);
            // 不抛出异常，允许系统继续运行，使用fallback数据
        }
    }

    /**
     * 关闭数据库连接
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("数据库连接已关闭");
            }
        } catch (SQLException e) {
            logger.error("关闭数据库连接失败", e);
        }
    }

    /**
     * 获取数据库路径
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * 检查数据库连接状态
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 重置数据库（删除所有数据）
     */
    public void resetDatabase() {
        try {
            closeConnection();

            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                dbFile.delete();
                logger.info("数据库文件已删除: {}", dbPath);
            }

            // 重新初始化
            initializeDatabase();
            logger.info("数据库重置完成");

        } catch (Exception e) {
            logger.error("重置数据库失败", e);
            throw new RuntimeException("重置数据库失败", e);
        }
    }

    /**
     * 强制重建数据库表结构
     */
    public void rebuildTables() {
        try {
            closeConnection();

            // 删除现有表
            try (Connection conn = getConnection();
                    Statement stmt = conn.createStatement()) {

                stmt.execute("DROP TABLE IF EXISTS weighing_records");
                stmt.execute("DROP TABLE IF EXISTS farmer_info");
                stmt.execute("DROP TABLE IF EXISTS system_config");
                logger.info("已删除现有表结构");
            }

            // 重新创建表
            createTables();
            logger.info("数据库表结构重建完成");

        } catch (Exception e) {
            logger.error("重建数据库表失败", e);
            throw new RuntimeException("重建数据库表失败", e);
        }
    }

    /**
     * 备份数据库
     */
    public void backupDatabase(String backupPath) {
        try {
            File sourceFile = new File(dbPath);
            File backupFile = new File(backupPath);

            if (sourceFile.exists()) {
                // 关闭连接以确保文件不被占用
                closeConnection();

                // 复制文件
                java.nio.file.Files.copy(sourceFile.toPath(), backupFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                logger.info("数据库备份完成: {}", backupPath);

                // 重新连接
                connection = DriverManager.getConnection(DB_URL_PREFIX + dbPath);
            }

        } catch (Exception e) {
            logger.error("备份数据库失败", e);
            throw new RuntimeException("备份数据库失败", e);
        }
    }
}