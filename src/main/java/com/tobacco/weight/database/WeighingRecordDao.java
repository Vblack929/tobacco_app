package com.tobacco.weight.database;

import com.tobacco.weight.data.WeighingRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 称重记录数据访问对象
 * 提供称重记录的数据库操作方法
 */
public class WeighingRecordDao {

    private static final Logger logger = LoggerFactory.getLogger(WeighingRecordDao.class);
    private final DatabaseManager databaseManager;

    public WeighingRecordDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * 插入新的称重记录
     */
    public long insert(WeighingRecord record) throws SQLException {
        String sql = """
                INSERT INTO weighing_records
                (precheck_id, farmer_name, contract_number, leaf_type, weight,
                 operator, warehouse_number, status, timestamp, id_card_number)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, record.getPrecheckId());
            pstmt.setString(2, record.getFarmerName());
            pstmt.setString(3, record.getContractNumber());
            pstmt.setString(4, record.getLeafType());
            pstmt.setDouble(5, record.getWeight());
            pstmt.setString(6, record.getOperator());
            pstmt.setString(7, record.getWarehouseNumber());
            pstmt.setString(8, record.getStatus());
            pstmt.setTimestamp(9, new Timestamp(record.getTimestamp().getTime()));
            pstmt.setString(10, record.getIdCardNumber());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("插入记录失败，没有行被影响");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    logger.info("称重记录插入成功，ID: {}", id);
                    return id;
                } else {
                    throw new SQLException("插入记录失败，无法获取生成的ID");
                }
            }
        }
    }

    /**
     * 更新称重记录
     */
    public int update(WeighingRecord record) throws SQLException {
        String sql = """
                UPDATE weighing_records
                SET precheck_id = ?, farmer_name = ?, contract_number = ?,
                    leaf_type = ?, weight = ?, operator = ?, warehouse_number = ?,
                    status = ?, timestamp = ?, id_card_number = ?
                WHERE id = ?
                """;

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, record.getPrecheckId());
            pstmt.setString(2, record.getFarmerName());
            pstmt.setString(3, record.getContractNumber());
            pstmt.setString(4, record.getLeafType());
            pstmt.setDouble(5, record.getWeight());
            pstmt.setString(6, record.getOperator());
            pstmt.setString(7, record.getWarehouseNumber());
            pstmt.setString(8, record.getStatus());
            pstmt.setTimestamp(9, new Timestamp(record.getTimestamp().getTime()));
            pstmt.setString(10, record.getIdCardNumber());
            pstmt.setLong(11, record.getId());

            int affectedRows = pstmt.executeUpdate();
            logger.info("称重记录更新成功，影响行数: {}", affectedRows);
            return affectedRows;
        }
    }

    /**
     * 删除称重记录
     */
    public int delete(long id) throws SQLException {
        String sql = "DELETE FROM weighing_records WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            int affectedRows = pstmt.executeUpdate();
            logger.info("称重记录删除成功，影响行数: {}", affectedRows);
            return affectedRows;
        }
    }

    /**
     * 根据ID查询记录
     */
    public WeighingRecord findById(long id) throws SQLException {
        String sql = "SELECT * FROM weighing_records WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRecord(rs);
                }
                return null;
            }
        }
    }

    /**
     * 获取所有记录（按时间倒序）
     */
    public List<WeighingRecord> findAll() throws SQLException {
        String sql = "SELECT * FROM weighing_records ORDER BY timestamp DESC";
        List<WeighingRecord> records = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                records.add(mapResultSetToRecord(rs));
            }
        }

        logger.info("查询到 {} 条称重记录", records.size());
        return records;
    }

    /**
     * 根据烟农姓名查询记录
     */
    public List<WeighingRecord> findByFarmerName(String farmerName) throws SQLException {
        String sql = "SELECT * FROM weighing_records WHERE farmer_name = ? ORDER BY timestamp DESC";
        List<WeighingRecord> records = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, farmerName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        }

        logger.info("查询到烟农 {} 的 {} 条称重记录", farmerName, records.size());
        return records;
    }

    /**
     * 根据身份证号查询记录
     */
    public List<WeighingRecord> findByIdCardNumber(String idCardNumber) throws SQLException {
        String sql = "SELECT * FROM weighing_records WHERE id_card_number = ? ORDER BY timestamp DESC";
        List<WeighingRecord> records = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, idCardNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        }

        logger.info("查询到身份证号 {} 的 {} 条称重记录", idCardNumber, records.size());
        return records;
    }

    /**
     * 根据预检编号查询记录
     */
    public List<WeighingRecord> findByPrecheckId(String precheckId) throws SQLException {
        String sql = "SELECT * FROM weighing_records WHERE precheck_id = ? ORDER BY timestamp DESC";
        List<WeighingRecord> records = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, precheckId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        }

        logger.info("查询到预检编号 {} 的 {} 条称重记录", precheckId, records.size());
        return records;
    }

    /**
     * 根据日期范围查询记录
     */
    public List<WeighingRecord> findByDateRange(Date startDate, Date endDate) throws SQLException {
        String sql = "SELECT * FROM weighing_records WHERE DATE(timestamp) BETWEEN ? AND ? ORDER BY timestamp DESC";
        List<WeighingRecord> records = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
            pstmt.setDate(2, new java.sql.Date(endDate.getTime()));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        }

        logger.info("查询到日期范围 {} 到 {} 的 {} 条称重记录", startDate, endDate, records.size());
        return records;
    }

    /**
     * 获取记录总数
     */
    public long getCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM weighing_records";

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    /**
     * 将ResultSet映射为WeighingRecord对象
     */
    private WeighingRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        WeighingRecord record = new WeighingRecord();
        record.setId(rs.getLong("id"));
        record.setPrecheckId(rs.getString("precheck_id"));
        record.setFarmerName(rs.getString("farmer_name"));
        record.setContractNumber(rs.getString("contract_number"));
        record.setLeafType(rs.getString("leaf_type"));
        record.setWeight(rs.getDouble("weight"));
        record.setOperator(rs.getString("operator"));
        record.setWarehouseNumber(rs.getString("warehouse_number"));
        record.setStatus(rs.getString("status"));
        record.setTimestamp(rs.getTimestamp("timestamp"));
        record.setIdCardNumber(rs.getString("id_card_number"));
        return record;
    }

    /**
     * 统计每个合同号的累计重量（用于在列表中展示“合同量”）
     * 注意：这里用称重累计重量近似“合同量”，若后续有独立合同量字段，可替换为对应查询
     */
    public java.util.Map<String, Double> getTotalWeightByContract() throws SQLException {
        String sql = "SELECT contract_number, SUM(weight) AS total_weight FROM weighing_records WHERE contract_number IS NOT NULL AND contract_number != '' GROUP BY contract_number";
        java.util.Map<String, Double> result = new java.util.HashMap<>();
        try (Connection conn = databaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String contract = rs.getString("contract_number");
                double total = rs.getDouble("total_weight");
                result.put(contract, total);
            }
        }
        return result;
    }
}