package com.tobacco.weight.database;

import com.tobacco.weight.data.LocationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 地区信息数据访问对象
 * 提供地区信息的数据库操作方法
 */
public class LocationInfoDao {

    private static final Logger logger = LoggerFactory.getLogger(LocationInfoDao.class);
    private final DatabaseManager databaseManager;

    public LocationInfoDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * 插入新的地区信息
     */
    public long insert(LocationInfo location) throws SQLException {
        String sql = """
                INSERT INTO location_info
                (township_name, village_name, location_type, parent_id, display_order, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, location.getTownshipName());
            pstmt.setString(2, location.getVillageName());
            pstmt.setString(3, location.getLocationType());
            if (location.getParentId() != null) {
                pstmt.setLong(4, location.getParentId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setInt(5, location.getDisplayOrder());
            pstmt.setBoolean(6, location.isActive());

            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("插入地区信息失败，没有行被影响");
            }

            // 获取生成的ID
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    location.setId(id);
                    logger.info("地区信息插入成功，ID: {}, 乡镇: {}, 村庄: {}", 
                               id, location.getTownshipName(), location.getVillageName());
                    return id;
                } else {
                    throw new SQLException("插入地区信息失败，无法获取生成的ID");
                }
            }
        }
    }

    /**
     * 批量插入地区信息
     */
    public void batchInsert(List<LocationInfo> locations) throws SQLException {
        String sql = """
                INSERT INTO location_info
                (township_name, village_name, location_type, parent_id, display_order, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (LocationInfo location : locations) {
                pstmt.setString(1, location.getTownshipName());
                pstmt.setString(2, location.getVillageName());
                pstmt.setString(3, location.getLocationType());
                if (location.getParentId() != null) {
                    pstmt.setLong(4, location.getParentId());
                } else {
                    pstmt.setNull(4, Types.INTEGER);
                }
                pstmt.setInt(5, location.getDisplayOrder());
                pstmt.setBoolean(6, location.isActive());
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            conn.commit();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }

            logger.info("批量插入地区信息完成，总数: {}, 成功: {}", locations.size(), successCount);

        } catch (SQLException e) {
            logger.error("批量插入地区信息失败", e);
            throw e;
        }
    }

    /**
     * 查询所有乡镇
     */
    public List<LocationInfo> findAllTownships() throws SQLException {
        String sql = """
                SELECT * FROM location_info 
                WHERE location_type = 'township' AND is_active = 1 
                ORDER BY display_order, township_name
                """;

        List<LocationInfo> townships = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                townships.add(mapResultSetToLocationInfo(rs));
            }
        }

        logger.info("查询到 {} 个乡镇", townships.size());
        return townships;
    }

    /**
     * 根据乡镇查询村庄
     */
    public List<LocationInfo> findVillagesByTownship(String townshipName) throws SQLException {
        String sql = """
                SELECT * FROM location_info 
                WHERE township_name = ? AND location_type = 'village' AND is_active = 1 
                ORDER BY display_order, village_name
                """;

        List<LocationInfo> villages = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, townshipName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    villages.add(mapResultSetToLocationInfo(rs));
                }
            }
        }

        logger.info("查询到乡镇 {} 下的 {} 个村庄", townshipName, villages.size());
        return villages;
    }

    /**
     * 根据乡镇ID查询村庄
     */
    public List<LocationInfo> findVillagesByTownshipId(Long townshipId) throws SQLException {
        String sql = """
                SELECT * FROM location_info 
                WHERE parent_id = ? AND location_type = 'village' AND is_active = 1 
                ORDER BY display_order, village_name
                """;

        List<LocationInfo> villages = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, townshipId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    villages.add(mapResultSetToLocationInfo(rs));
                }
            }
        }

        logger.info("查询到乡镇ID {} 下的 {} 个村庄", townshipId, villages.size());
        return villages;
    }

    /**
     * 根据ID查询地区信息
     */
    public LocationInfo findById(Long id) throws SQLException {
        String sql = "SELECT * FROM location_info WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLocationInfo(rs);
                }
            }
        }

        return null;
    }

    /**
     * 根据乡镇和村庄名称查询
     */
    public LocationInfo findByTownshipAndVillage(String townshipName, String villageName) throws SQLException {
        String sql = "SELECT * FROM location_info WHERE township_name = ? AND village_name = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, townshipName);
            pstmt.setString(2, villageName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLocationInfo(rs);
                }
            }
        }

        return null;
    }

    /**
     * 查询所有地区信息（按层级排序）
     */
    public List<LocationInfo> findAll() throws SQLException {
        String sql = """
                SELECT * FROM location_info 
                WHERE is_active = 1 
                ORDER BY township_name, location_type DESC, display_order, village_name
                """;

        List<LocationInfo> locations = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                locations.add(mapResultSetToLocationInfo(rs));
            }
        }

        logger.info("查询到 {} 条地区信息", locations.size());
        return locations;
    }

    /**
     * 更新地区信息
     */
    public int update(LocationInfo location) throws SQLException {
        String sql = """
                UPDATE location_info 
                SET township_name = ?, village_name = ?, location_type = ?, 
                    parent_id = ?, display_order = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, location.getTownshipName());
            pstmt.setString(2, location.getVillageName());
            pstmt.setString(3, location.getLocationType());
            if (location.getParentId() != null) {
                pstmt.setLong(4, location.getParentId());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setInt(5, location.getDisplayOrder());
            pstmt.setBoolean(6, location.isActive());
            pstmt.setLong(7, location.getId());

            int affectedRows = pstmt.executeUpdate();
            logger.info("地区信息更新成功，影响行数: {}", affectedRows);
            return affectedRows;
        }
    }

    /**
     * 删除地区信息（软删除，设置is_active为false）
     */
    public int softDelete(Long id) throws SQLException {
        String sql = "UPDATE location_info SET is_active = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            int affectedRows = pstmt.executeUpdate();
            logger.info("地区信息软删除成功，影响行数: {}", affectedRows);
            return affectedRows;
        }
    }

    /**
     * 物理删除地区信息
     */
    public int delete(Long id) throws SQLException {
        String sql = "DELETE FROM location_info WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            int affectedRows = pstmt.executeUpdate();
            logger.info("地区信息删除成功，影响行数: {}", affectedRows);
            return affectedRows;
        }
    }

    /**
     * 检查地区信息是否存在
     */
    public boolean exists(String townshipName, String villageName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM location_info WHERE township_name = ? AND village_name = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, townshipName);
            pstmt.setString(2, villageName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    /**
     * 获取记录总数
     */
    public int getCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM location_info WHERE is_active = 1";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    /**
     * 将ResultSet映射为LocationInfo对象
     */
    private LocationInfo mapResultSetToLocationInfo(ResultSet rs) throws SQLException {
        LocationInfo location = new LocationInfo();
        location.setId(rs.getLong("id"));
        location.setTownshipName(rs.getString("township_name"));
        location.setVillageName(rs.getString("village_name"));
        location.setLocationType(rs.getString("location_type"));
        
        Long parentId = rs.getLong("parent_id");
        if (!rs.wasNull()) {
            location.setParentId(parentId);
        }
        
        location.setDisplayOrder(rs.getInt("display_order"));
        location.setActive(rs.getBoolean("is_active"));
        location.setCreatedAt(rs.getTimestamp("created_at"));
        location.setUpdatedAt(rs.getTimestamp("updated_at"));
        
        return location;
    }
    
    /**
     * 检查地区是否存在
     */
    public boolean existsLocation(String township, String village) throws SQLException {
        String sql = "SELECT COUNT(*) FROM location_info WHERE township_name = ? AND village_name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, township);
            stmt.setString(2, village);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    /**
     * 获取所有地区名称列表
     */
    public List<String> getAllLocationNames() throws SQLException {
        String sql = """
            SELECT DISTINCT (township_name || village_name) as full_name 
            FROM location_info 
            WHERE is_active = 1 
            ORDER BY township_name, village_name
            """;
        
        List<String> locations = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                locations.add(rs.getString("full_name"));
            }
        }
        
        return locations;
    }
    
    /**
     * 插入新地区（简化版本）
     */
    public void insertLocation(String township, String village) throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO location_info (township_name, village_name, location_type, is_active)
            VALUES (?, ?, 'village', 1)
            """;
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, township);
            stmt.setString(2, village);
            stmt.executeUpdate();
        }
    }
}