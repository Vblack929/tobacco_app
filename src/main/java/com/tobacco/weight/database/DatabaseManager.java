package com.tobacco.weight.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
                        timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
            stmt.execute(createConfigTable);

            logger.info("数据库表创建完成");

        } catch (SQLException e) {
            logger.error("创建数据库表失败", e);
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