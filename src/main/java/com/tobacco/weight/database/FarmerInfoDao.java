package com.tobacco.weight.database;

import com.tobacco.weight.data.FarmerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 烟农信息数据访问对象
 * 负责farmer_info表的CRUD操作
 */
public class FarmerInfoDao {

    private static final Logger logger = LoggerFactory.getLogger(FarmerInfoDao.class);

    private final DatabaseManager databaseManager;

    public FarmerInfoDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * 插入新的烟农信息
     */
    public long insert(FarmerInfo farmerInfo) throws SQLException {
        String sql = """
                INSERT INTO farmer_info (
                    farmer_name, contract_number, id_card_number, gender, nationality,
                    birth_date, address, department, start_date, end_date, photo
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, farmerInfo.getFarmerName());
            stmt.setString(2, farmerInfo.getContractNumber());
            stmt.setString(3, farmerInfo.getIdCardNumber());
            stmt.setString(4, farmerInfo.getGender());
            stmt.setString(5, farmerInfo.getNationality());
            stmt.setString(6, farmerInfo.getBirthDate());
            stmt.setString(7, farmerInfo.getAddress());
            stmt.setString(8, farmerInfo.getDepartment());
            stmt.setString(9, farmerInfo.getStartDate());
            stmt.setString(10, farmerInfo.getEndDate());
            stmt.setBytes(11, farmerInfo.getPhoto());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("插入烟农信息失败，没有行被影响");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("插入烟农信息失败，无法获取ID");
                }
            }
        }
    }

    /**
     * 更新烟农信息
     */
    public int update(FarmerInfo farmerInfo, long id) throws SQLException {
        String sql = """
                UPDATE farmer_info SET
                    farmer_name = ?, contract_number = ?, id_card_number = ?, gender = ?, 
                    nationality = ?, birth_date = ?, address = ?, department = ?, 
                    start_date = ?, end_date = ?, photo = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, farmerInfo.getFarmerName());
            stmt.setString(2, farmerInfo.getContractNumber());
            stmt.setString(3, farmerInfo.getIdCardNumber());
            stmt.setString(4, farmerInfo.getGender());
            stmt.setString(5, farmerInfo.getNationality());
            stmt.setString(6, farmerInfo.getBirthDate());
            stmt.setString(7, farmerInfo.getAddress());
            stmt.setString(8, farmerInfo.getDepartment());
            stmt.setString(9, farmerInfo.getStartDate());
            stmt.setString(10, farmerInfo.getEndDate());
            stmt.setBytes(11, farmerInfo.getPhoto());
            stmt.setLong(12, id);

            return stmt.executeUpdate();
        }
    }

    /**
     * 删除烟农信息
     */
    public int delete(long id) throws SQLException {
        String sql = "DELETE FROM farmer_info WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate();
        }
    }

    /**
     * 根据ID查询烟农信息
     */
    public FarmerInfo findById(long id) throws SQLException {
        String sql = "SELECT * FROM farmer_info WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFarmerInfo(rs);
                }
                return null;
            }
        }
    }

    /**
     * 获取所有烟农信息 - 从称重记录和农户信息表合并获取
     */
    public List<FarmerInfo> findAll() throws SQLException {
        String sql = """
                SELECT DISTINCT 
                    wr.farmer_name,
                    wr.contract_number,
                    wr.id_card_number,
                    fi.gender,
                    fi.nationality,
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
                """;

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<FarmerInfo> farmers = new ArrayList<>();
            while (rs.next()) {
                farmers.add(mapWeighingRecordToFarmerInfo(rs));
            }
            return farmers;
        }
    }

    /**
     * 根据姓名或合同号搜索烟农 - 从称重记录搜索
     */
    public List<FarmerInfo> searchByNameOrContract(String searchTerm) throws SQLException {
        String sql = """
                SELECT DISTINCT 
                    wr.farmer_name,
                    wr.contract_number,
                    wr.id_card_number,
                    fi.gender,
                    fi.nationality,
                    fi.birth_date,
                    fi.address,
                    fi.department,
                    fi.start_date,
                    fi.end_date,
                    fi.photo
                FROM weighing_records wr
                LEFT JOIN farmer_info fi ON wr.id_card_number = fi.id_card_number 
                    OR (wr.farmer_name = fi.farmer_name AND wr.contract_number = fi.contract_number)
                WHERE (wr.farmer_name LIKE ? OR wr.contract_number LIKE ?)
                    AND wr.farmer_name IS NOT NULL AND wr.farmer_name != ''
                ORDER BY wr.farmer_name
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String likeTerm = "%" + searchTerm + "%";
            stmt.setString(1, likeTerm);
            stmt.setString(2, likeTerm);

            try (ResultSet rs = stmt.executeQuery()) {
                List<FarmerInfo> farmers = new ArrayList<>();
                while (rs.next()) {
                    farmers.add(mapWeighingRecordToFarmerInfo(rs));
                }
                return farmers;
            }
        }
    }

    /**
     * 根据地址筛选烟农（用于乡村筛选） - 从称重记录搜索
     */
    public List<FarmerInfo> findByLocation(String location) throws SQLException {
        String sql = """
                SELECT DISTINCT 
                    wr.farmer_name,
                    wr.contract_number,
                    wr.id_card_number,
                    fi.gender,
                    fi.nationality,
                    fi.birth_date,
                    fi.address,
                    fi.department,
                    fi.start_date,
                    fi.end_date,
                    fi.photo
                FROM weighing_records wr
                LEFT JOIN farmer_info fi ON wr.id_card_number = fi.id_card_number 
                    OR (wr.farmer_name = fi.farmer_name AND wr.contract_number = fi.contract_number)
                WHERE fi.address LIKE ?
                    AND wr.farmer_name IS NOT NULL AND wr.farmer_name != ''
                ORDER BY wr.farmer_name
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String likeTerm = "%" + location + "%";
            stmt.setString(1, likeTerm);

            try (ResultSet rs = stmt.executeQuery()) {
                List<FarmerInfo> farmers = new ArrayList<>();
                while (rs.next()) {
                    farmers.add(mapWeighingRecordToFarmerInfo(rs));
                }
                return farmers;
            }
        }
    }

    /**
     * 根据身份证号查找烟农
     */
    public FarmerInfo findByIdCardNumber(String idCardNumber) throws SQLException {
        String sql = "SELECT * FROM farmer_info WHERE id_card_number = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idCardNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFarmerInfo(rs);
                }
                return null;
            }
        }
    }

    /**
     * 获取所有不同的地址信息（用于筛选下拉框）- 从称重记录关联的农户地址获取
     */
    public List<String> getAllDistinctLocations() throws SQLException {
        String sql = """
                SELECT DISTINCT fi.address 
                FROM weighing_records wr
                LEFT JOIN farmer_info fi ON wr.id_card_number = fi.id_card_number 
                    OR (wr.farmer_name = fi.farmer_name AND wr.contract_number = fi.contract_number)
                WHERE fi.address IS NOT NULL AND fi.address != ''
                ORDER BY fi.address
                """;

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            List<String> locations = new ArrayList<>();
            while (rs.next()) {
                String address = rs.getString("address");
                if (address != null && !address.trim().isEmpty()) {
                    locations.add(address.trim());
                }
            }
            return locations;
        }
    }

    /**
     * 获取烟农总数
     */
    public long getCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM farmer_info";

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }

    /**
     * 将ResultSet映射为FarmerInfo对象
     */
    private FarmerInfo mapResultSetToFarmerInfo(ResultSet rs) throws SQLException {
        return FarmerInfo.createWithIdCard(
                rs.getString("farmer_name"),
                rs.getString("contract_number"),
                rs.getString("id_card_number"),
                rs.getString("gender"),
                rs.getString("nationality"),
                rs.getString("birth_date"),
                rs.getString("address"),
                rs.getString("department"),
                rs.getString("start_date"),
                rs.getString("end_date"),
                rs.getBytes("photo")
        );
    }

    /**
     * 将称重记录关联查询的ResultSet映射为FarmerInfo对象
     */
    private FarmerInfo mapWeighingRecordToFarmerInfo(ResultSet rs) throws SQLException {
        return FarmerInfo.createWithIdCard(
                rs.getString("farmer_name"),
                rs.getString("contract_number"),
                rs.getString("id_card_number"),
                rs.getString("gender") != null ? rs.getString("gender") : "",
                rs.getString("nationality") != null ? rs.getString("nationality") : "",
                rs.getString("birth_date") != null ? rs.getString("birth_date") : "",
                rs.getString("address") != null ? rs.getString("address") : "",
                rs.getString("department") != null ? rs.getString("department") : "",
                rs.getString("start_date") != null ? rs.getString("start_date") : "",
                rs.getString("end_date") != null ? rs.getString("end_date") : "",
                rs.getBytes("photo")
        );
    }
}