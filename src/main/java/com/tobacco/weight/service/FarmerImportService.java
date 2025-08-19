package com.tobacco.weight.service;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.database.DatabaseManager;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.Decryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 农户Excel导入服务
 * 固定列顺序导入（第一行可能为空，第二行为表头，之后为数据）
 * 列顺序：站点名称 | 姓名 | 种植者编号（合同号） | 身份证号 | 行政区划 | 合同量
 */
public class FarmerImportService {

    private static final Logger logger = LoggerFactory.getLogger(FarmerImportService.class);
    private final DatabaseManager databaseManager;

    // 固定列索引（0-based）
    private static final int COL_STATION = 0;      // 站点名称
    private static final int COL_NAME = 1;         // 姓名
    private static final int COL_CONTRACT_NO = 2;  // 种植者编号（合同号）
    private static final int COL_ID = 3;           // 身份证号
    private static final int COL_ADMIN = 4;        // 行政区划（如：永安镇督正村）
    private static final int COL_AMOUNT = 5;       // 合同量

    public FarmerImportService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * 导入结果
     */
    public static class ImportResult {
        private int totalRows;
        private int successCount;
        private int failureCount;
        private int duplicateCount;
        private final List<String> errorMessages;

        public ImportResult() {
            this.errorMessages = new ArrayList<>();
        }

        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public int getDuplicateCount() { return duplicateCount; }
        public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
        public List<String> getErrorMessages() { return errorMessages; }
        public void addErrorMessage(String message) { this.errorMessages.add(message); }

        public String getSummary() {
            return String.format("导入完成：总计 %d 条，成功 %d 条，失败 %d 条，重复 %d 条",
                    totalRows, successCount, failureCount, duplicateCount);
        }
    }

    /**
     * 从Excel文件导入农户信息（固定格式，按列顺序读取）
     * 第一行可为空；第二行为表头；数据自表头下一行开始
     */
    public ImportResult importFromExcel(File excelFile, String sheetName) throws IOException {
        return importFromExcel(excelFile, sheetName, null);
    }

    /**
     * 从Excel文件导入农户信息（支持加密文件）
     * @param excelFile Excel文件
     * @param sheetName 工作表名称（可为null使用第一个工作表）
     * @param password 密码（可为null表示无密码）
     */
    public ImportResult importFromExcel(File excelFile, String sheetName, String password) throws IOException {
        ImportResult result = new ImportResult();

        try (Workbook workbook = createWorkbook(excelFile, password)) {
            Sheet sheet = getSheet(workbook, sheetName);
            if (sheet == null) {
                result.addErrorMessage("找不到工作表: " + sheetName);
                return result;
            }

            // 找到表头行（跳过前置空行后的第一行）
            final int headerRow = findHeaderRow(sheet); // 期望为索引1（第二行），但做了健壮处理
            final int dataStartRow = (headerRow >= 0) ? headerRow + 1 : 2;

            int lastRowNum = sheet.getLastRowNum();
            result.setTotalRows(Math.max(0, lastRowNum - dataStartRow + 1));

            logger.info("固定格式导入：数据起始行={}, 结束行={}", dataStartRow + 1, lastRowNum + 1);

            for (int i = dataStartRow; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String station = getCellStringValue(row.getCell(COL_STATION)).trim();
                    String farmerName = getCellStringValue(row.getCell(COL_NAME)).trim();
                    String contractNo = getCellStringValue(row.getCell(COL_CONTRACT_NO)).trim();
                    String idCardNumber = getCellStringValue(row.getCell(COL_ID)).trim();
                    String adminDivision = getCellStringValue(row.getCell(COL_ADMIN)).trim();
                    String amountStr = getCellStringValue(row.getCell(COL_AMOUNT)).trim();

                    // 预处理
                    double contractAmount = 0.0;
                    if (!amountStr.isEmpty()) {
                        amountStr = amountStr.replace(",", "");
                        contractAmount = Double.parseDouble(amountStr);
                        if (contractAmount < 0) throw new IllegalArgumentException("合同量不能为负数");
                    }

                    // 基础校验
                    if (farmerName.isEmpty() || idCardNumber.isEmpty() || adminDivision.isEmpty()) {
                        result.addErrorMessage("第" + (i + 1) + "行数据不完整");
                        result.setFailureCount(result.getFailureCount() + 1);
                        continue;
                    }
                    if (!isValidIdCard(idCardNumber)) {
                        result.addErrorMessage("第" + (i + 1) + "行身份证号格式错误: " + idCardNumber);
                        result.setFailureCount(result.getFailureCount() + 1);
                        continue;
                    }

                    // 行政区划解析：永安镇督正村 → (永安镇, 督正村)
                    String[] tv = parseAdminDivision(adminDivision);
                    String township = tv[0];
                    String village = tv[1];

                    // 重复检查（按身份证号）
                    if (farmerExists(idCardNumber)) {
                        result.setDuplicateCount(result.getDuplicateCount() + 1);
                        continue;
                    }

                    // 使用提供的合同号（不再生成）
                    FarmerInfo farmerInfo = new FarmerInfo(farmerName, contractNo, idCardNumber);

                    // 保存农户基本信息
                    if (saveFarmerInfo(farmerInfo, township, village)) {
                        // 保存合同信息到farmer_contracts表
                        if (saveContractInfo(idCardNumber, contractNo, contractAmount, station)) {
                            result.setSuccessCount(result.getSuccessCount() + 1);
                        } else {
                            result.addErrorMessage("第" + (i + 1) + "行合同信息保存失败: " + farmerName);
                            result.setFailureCount(result.getFailureCount() + 1);
                        }
                    } else {
                        result.addErrorMessage("第" + (i + 1) + "行保存失败: " + farmerName);
                        result.setFailureCount(result.getFailureCount() + 1);
                    }

                } catch (Exception e) {
                    result.addErrorMessage("第" + (i + 1) + "行处理异常: " + e.getMessage());
                    result.setFailureCount(result.getFailureCount() + 1);
                    logger.error("处理第{}行数据时出错", i + 1, e);
                }
            }

            logger.info("Excel导入完成: {}, 总记录数: {}, 成功: {}, 失败: {}, 重复: {}", 
                       excelFile.getName(), result.getTotalRows(), result.getSuccessCount(), 
                       result.getFailureCount(), result.getDuplicateCount());

        } catch (Exception e) {
            // 对于加密相关的异常，直接抛出以便上层处理密码逻辑
            String errorMsg = e.getMessage();
            if (errorMsg != null && (
                errorMsg.contains("OLE2") || 
                errorMsg.contains("encrypted") || 
                errorMsg.contains("Encrypted") ||
                errorMsg.contains("password") || 
                errorMsg.contains("decrypted") ||
                errorMsg.contains("需要提供密码") ||
                (errorMsg.contains("XSSF") && errorMsg.contains("HSSF")))) {
                logger.warn("检测到加密Excel文件，抛出异常供上层处理: {}", errorMsg);
                throw new RuntimeException("检测到加密Excel文件: " + errorMsg, e);
            }
            
            result.addErrorMessage("读取Excel文件失败: " + e.getMessage());
            logger.error("导入Excel文件失败", e);
        }

        return result;
    }

    /**
     * 创建工作簿对象（支持加密文件）
     */
    private Workbook createWorkbook(File excelFile, String password) throws IOException {
        String fileName = excelFile.getName().toLowerCase();
        
        // 如果提供了密码，尝试解密
        if (password != null && !password.trim().isEmpty()) {
            try {
                return createEncryptedWorkbook(excelFile, password);
            } catch (Exception e) {
                logger.warn("使用密码解密失败: {}", e.getMessage());
                throw new IOException("密码解密失败: " + e.getMessage(), e);
            }
        }
        
        // 无密码方式
        try (FileInputStream fis = new FileInputStream(excelFile)) {
            if (fileName.endsWith(".xlsx")) {
                return new XSSFWorkbook(fis);
            } else if (fileName.endsWith(".xls")) {
                return new HSSFWorkbook(fis);
            } else {
                throw new IOException("不支持的文件格式，请使用.xls或.xlsx文件");
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            
            // 如果是OLE2格式错误（加密的xlsx文件），直接抛出加密异常
            if (errorMsg != null && errorMsg.contains("OLE2") && fileName.endsWith(".xlsx")) {
                throw new IOException("检测到加密的XLSX文件，需要提供密码", e);
            }
            
            // 其他加密相关错误
            if (errorMsg != null && (
                errorMsg.contains("Encrypted") || 
                errorMsg.contains("encrypted") || 
                errorMsg.contains("decrypted") ||
                (errorMsg.contains("XSSF") && errorMsg.contains("HSSF")))) {
                throw new IOException("检测到加密文件，需要提供密码: " + errorMsg, e);
            }
            
            throw new IOException("读取Excel文件失败: " + errorMsg, e);
        }
    }

    /**
     * 创建加密的Workbook
     */
    private Workbook createEncryptedWorkbook(File file, String password) throws Exception {
        logger.info("尝试使用密码解密文件: {}", file.getName());
        
        // 对于加密文件，都需要通过POIFSFileSystem处理
        try (FileInputStream fis = new FileInputStream(file);
             POIFSFileSystem fs = new POIFSFileSystem(fis)) {
            
            EncryptionInfo info = new EncryptionInfo(fs);
            Decryptor decryptor = Decryptor.getInstance(info);
            
            if (!decryptor.verifyPassword(password)) {
                logger.warn("密码验证失败: [{}]", password);
                throw new Exception("密码验证失败，密码可能不正确: " + password);
            }
            
            logger.info("密码验证成功，开始解密文件");
            
            try (java.io.InputStream dataStream = decryptor.getDataStream(fs)) {
                // 根据原始文件格式决定使用哪个Workbook
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".xls")) {
                    logger.info("创建HSSFWorkbook用于.xls格式");
                    return new HSSFWorkbook(dataStream);
                } else {
                    // 默认使用XSSFWorkbook，包括.xlsx和其他情况
                    logger.info("创建XSSFWorkbook用于.xlsx格式");
                    return new XSSFWorkbook(dataStream);
                }
            }
        } catch (Exception e) {
            logger.error("解密文件失败: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().contains("password")) {
                throw new Exception("密码验证失败，请检查密码是否正确", e);
            }
            throw new Exception("解密文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取工作表
     */
    private Sheet getSheet(Workbook workbook, String sheetName) {
        logger.info("工作簿中共有 {} 个工作表", workbook.getNumberOfSheets());
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            logger.info("工作表 {}: {}", i, workbook.getSheetName(i));
        }

        if (sheetName != null && !sheetName.trim().isEmpty()) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet != null) {
                logger.info("找到指定工作表: {}", sheetName);
                return sheet;
            } else {
                logger.warn("未找到指定工作表: {}，将使用第一个工作表", sheetName);
            }
        }
        if (workbook.getNumberOfSheets() > 0) {
            Sheet sheet = workbook.getSheetAt(0);
            logger.info("使用第一个工作表: {}", sheet.getSheetName());
            return sheet;
        }
        return null;
    }

    /**
     * 获取单元格字符串值（避免科学计数法，保留身份证/合同号格式）
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    // 先尝试作为字符串获取
                    return cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    // 回退为数值
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            default:
                return "";
        }
    }

    /**
     * 验证身份证号格式（18位，最后一位可为X/x）
     */
    private boolean isValidIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) return false;
        return idCard.matches("\\d{17}[\\dXx]");
    }

    /**
     * 检查农户是否已存在（按身份证号）
     */
    private boolean farmerExists(String idCardNumber) {
        String sql = "SELECT COUNT(*) FROM farmer_info WHERE id_card_number = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idCardNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            logger.error("检查农户是否存在时出错", e);
            return false;
        }
    }

    /**
     * 保存农户信息到数据库
     * 注意：此方法沿用原有签名以保持兼容
     */
    private boolean saveFarmerInfo(FarmerInfo farmerInfo, String township, String village) {
        String sql = """
                INSERT INTO farmer_info (farmer_name, contract_number, id_card_number, address, created_at, updated_at)
                VALUES (?, ?, ?, ?, datetime('now'), datetime('now'))
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, farmerInfo.getFarmerName());
            stmt.setString(2, farmerInfo.getContractNumber()); // 使用Excel提供的合同号
            stmt.setString(3, farmerInfo.getIdCardNumber());
            stmt.setString(4, township + " " + village);

            int affected = stmt.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            logger.error("保存农户信息失败: {}", farmerInfo.getFarmerName(), e);
            return false;
        }
    }

    /**
     * 保存合同信息到farmer_contracts表
     */
    private boolean saveContractInfo(String nationalId, String contractNo, double contractAmount, String station) {
        String sql = """
                INSERT OR REPLACE INTO farmer_contracts (national_id, contract_no, contract_amount, station, created_at, updated_at)
                VALUES (?, ?, ?, ?, datetime('now'), datetime('now'))
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nationalId);
            stmt.setString(2, contractNo);
            stmt.setDouble(3, contractAmount);
            stmt.setString(4, station);

            int affected = stmt.executeUpdate();
            // logger.debug("保存合同信息: 身份证={}, 合同号={}, 合同量={}, 站点={}", 
            //             nationalId, contractNo, contractAmount, station);
            return affected > 0;

        } catch (SQLException e) {
            logger.error("保存合同信息失败: 身份证={}, 合同号={}", nationalId, contractNo, e);
            return false;
        }
    }

    /**
     * 生成合同号（保留原方法以兼容；当前导入流程默认使用Excel提供的合同号）
     */
    private String generateContractNumber(String township, String village) {
        String year = String.valueOf(java.time.Year.now().getValue());
        String townshipCode = township != null && township.length() >= 2 ? township.substring(0, 2) : (township == null ? "" : township);
        String villageCode = village != null && village.length() >= 2 ? village.substring(0, 2) : (village == null ? "" : village);
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000);
        return "HT" + year + townshipCode + villageCode + timestamp;
    }

    /**
     * 解析行政区划为 [乡镇, 村]；支持“镇/乡/街道/社区/办事处”等关键字
     */
    private String[] parseAdminDivision(String admin) {
        if (admin == null) return new String[]{"", ""};
        admin = admin.replaceAll("\\s+", ""); // 去除所有空白
        String[] keys = {"镇", "乡", "街道", "社区", "办事处"};
        int cut = -1;
        for (String k : keys) {
            int idx = admin.indexOf(k);
            if (idx >= 0) { cut = idx + k.length(); break; }
        }
        if (cut == -1 || cut >= admin.length()) {
            int vIdx = Math.max(admin.lastIndexOf("村"), admin.lastIndexOf("社区"));
            if (vIdx > 0) {
                String township = admin.substring(0, Math.min(vIdx, admin.length()));
                String village = admin.substring(Math.min(vIdx, admin.length()));
                return new String[]{township, village};
            }
            return new String[]{admin, ""};
        }
        String township = admin.substring(0, cut);
        String village = admin.substring(cut);
        return new String[]{township, village};
    }

    /**
     * 找到表头行（跳过开头空行后第一行），若未找到返回1（默认第二行是表头）
     */
    private int findHeaderRow(Sheet sheet) {
        int last = sheet.getLastRowNum();
        for (int i = 0; i <= Math.min(last, 10); i++) {
            Row r = sheet.getRow(i);
            if (r == null || isRowEmpty(r)) continue;
            return i; // 第一行非空即视为表头
        }
        return 1;
    }

    /**
     * 判断整行是否为空（所有单元格为空或仅空白）
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        int first = row.getFirstCellNum();
        int last = row.getLastCellNum();
        if (first < 0 || last < 0) return true;
        for (int c = first; c < last; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !getCellStringValue(cell).trim().isEmpty()) return false;
        }
        return true;
        }
}