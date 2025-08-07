package com.tobacco.weight.service;

import com.tobacco.weight.data.FarmerInfo;
import com.tobacco.weight.database.DatabaseManager;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 农户Excel导入服务
 * 负责从Excel文件中读取农户信息并导入到数据库
 */
public class FarmerImportService {

    private static final Logger logger = LoggerFactory.getLogger(FarmerImportService.class);
    private final DatabaseManager databaseManager;

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
        private List<String> errorMessages;

        public ImportResult() {
            this.errorMessages = new ArrayList<>();
        }

        // Getters and setters
        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public void setFailureCount(int failureCount) {
            this.failureCount = failureCount;
        }

        public int getDuplicateCount() {
            return duplicateCount;
        }

        public void setDuplicateCount(int duplicateCount) {
            this.duplicateCount = duplicateCount;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }

        public void addErrorMessage(String message) {
            this.errorMessages.add(message);
        }

        public String getSummary() {
            return String.format("导入完成：总计 %d 条，成功 %d 条，失败 %d 条，重复 %d 条",
                    totalRows, successCount, failureCount, duplicateCount);
        }
    }

    /**
     * 从Excel文件导入农户信息
     */
    public ImportResult importFromExcel(File excelFile, String sheetName) throws IOException {
        ImportResult result = new ImportResult();

        try (FileInputStream fis = new FileInputStream(excelFile)) {
            Workbook workbook = createWorkbook(excelFile, fis);
            Sheet sheet = getSheet(workbook, sheetName);

            if (sheet == null) {
                result.addErrorMessage("找不到工作表: " + sheetName);
                return result;
            }

            // 查找列索引 - 智能查找标题行
            int townshipCol = -1, villageCol = -1, nameCol = -1, idCardCol = -1;
            int headerRowIndex = -1;

            // 在前10行中查找包含必需列的行
            for (int i = 0; i < Math.min(10, sheet.getLastRowNum() + 1); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                int tempTownship = -1, tempVillage = -1, tempName = -1, tempIdCard = -1;

                for (Cell cell : row) {
                    String headerValue = getCellStringValue(cell).trim();
                    if (headerValue.isEmpty())
                        continue;

                    switch (headerValue) {
                        case "乡镇":
                            tempTownship = cell.getColumnIndex();
                            break;
                        case "村":
                            tempVillage = cell.getColumnIndex();
                            break;
                        case "烟农姓名":
                        case "姓名":
                            tempName = cell.getColumnIndex();
                            break;
                        case "身份证号":
                            tempIdCard = cell.getColumnIndex();
                            break;
                    }
                }

                // 如果找到了所有必需的列，记录这一行
                if (tempTownship != -1 && tempVillage != -1 && tempName != -1 && tempIdCard != -1) {
                    headerRowIndex = i;
                    townshipCol = tempTownship;
                    villageCol = tempVillage;
                    nameCol = tempName;
                    idCardCol = tempIdCard;
                    logger.info("在第{}行找到完整的列标题", i + 1);
                    break;
                }
            }

            if (headerRowIndex != -1) {
                logger.info("找到列标题行: {}, 乡镇={}, 村={}, 姓名={}, 身份证号={}",
                        headerRowIndex + 1, townshipCol, villageCol, nameCol, idCardCol);
            }

            // 验证必需列是否存在
            StringBuilder missingCols = new StringBuilder();
            if (townshipCol == -1)
                missingCols.append("乡镇 ");
            if (villageCol == -1)
                missingCols.append("村 ");
            if (nameCol == -1)
                missingCols.append("烟农姓名/姓名 ");
            if (idCardCol == -1)
                missingCols.append("身份证号 ");

            if (townshipCol == -1 || villageCol == -1 || nameCol == -1 || idCardCol == -1) {
                result.addErrorMessage("Excel文件缺少必需的列：" + missingCols.toString().trim());
                logger.error("缺少列：{}，找到的列：乡镇={}, 村={}, 姓名={}, 身份证号={}",
                        missingCols.toString().trim(), townshipCol, villageCol, nameCol, idCardCol);
                return result;
            }

            logger.info("所有必需列都已找到：乡镇={}, 村={}, 姓名={}, 身份证号={}",
                    townshipCol, villageCol, nameCol, idCardCol);

            // 处理数据行
            int lastRowNum = sheet.getLastRowNum();
            int dataStartRow = headerRowIndex + 1; // 从标题行的下一行开始
            result.setTotalRows(lastRowNum - dataStartRow + 1); // 数据行数

            logger.info("开始处理数据，从第{}行到第{}行", dataStartRow + 1, lastRowNum + 1);

            for (int i = dataStartRow; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                try {
                    String township = getCellStringValue(row.getCell(townshipCol)).trim();
                    String village = getCellStringValue(row.getCell(villageCol)).trim();
                    String farmerName = getCellStringValue(row.getCell(nameCol)).trim();
                    String idCardNumber = getCellStringValue(row.getCell(idCardCol)).trim();

                    // 验证数据完整性
                    if (township.isEmpty() || village.isEmpty() || farmerName.isEmpty() || idCardNumber.isEmpty()) {
                        result.addErrorMessage("第" + (i + 1) + "行数据不完整");
                        result.setFailureCount(result.getFailureCount() + 1);
                        continue;
                    }

                    // 验证身份证号格式
                    if (!isValidIdCard(idCardNumber)) {
                        result.addErrorMessage("第" + (i + 1) + "行身份证号格式错误: " + idCardNumber);
                        result.setFailureCount(result.getFailureCount() + 1);
                        continue;
                    }

                    // 检查是否已存在
                    if (farmerExists(idCardNumber)) {
                        result.addErrorMessage("第" + (i + 1) + "行农户已存在: " + farmerName + " (" + idCardNumber + ")");
                        result.setDuplicateCount(result.getDuplicateCount() + 1);
                        continue;
                    }

                    // 创建农户信息
                    FarmerInfo farmerInfo = new FarmerInfo(farmerName, generateContractNumber(township, village),
                            idCardNumber);

                    // 保存到数据库
                    if (saveFarmerInfo(farmerInfo, township, village)) {
                        result.setSuccessCount(result.getSuccessCount() + 1);
                        logger.info("成功导入农户: {} ({})", farmerName, idCardNumber);
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

            workbook.close();

        } catch (Exception e) {
            result.addErrorMessage("读取Excel文件失败: " + e.getMessage());
            logger.error("导入Excel文件失败", e);
        }

        return result;
    }

    /**
     * 创建工作簿对象
     */
    private Workbook createWorkbook(File excelFile, FileInputStream fis) throws IOException {
        String fileName = excelFile.getName().toLowerCase();
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IOException("不支持的文件格式，请使用.xls或.xlsx文件");
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
        // 如果没有指定工作表名或指定的工作表不存在，返回第一个工作表
        if (workbook.getNumberOfSheets() > 0) {
            Sheet sheet = workbook.getSheetAt(0);
            logger.info("使用第一个工作表: {}", sheet.getSheetName());
            return sheet;
        }
        return null;
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 处理数字类型，避免科学计数法
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
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 验证身份证号格式
     */
    private boolean isValidIdCard(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }
        return idCard.matches("\\d{17}[\\dXx]");
    }

    /**
     * 检查农户是否已存在
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
     */
    private boolean saveFarmerInfo(FarmerInfo farmerInfo, String township, String village) {
        String sql = """
                INSERT INTO farmer_info (farmer_name, contract_number, id_card_number, address, created_at, updated_at)
                VALUES (?, ?, ?, ?, datetime('now'), datetime('now'))
                """;

        try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, farmerInfo.getFarmerName());
            stmt.setString(2, farmerInfo.getContractNumber());
            stmt.setString(3, farmerInfo.getIdCardNumber());
            stmt.setString(4, township + " " + village);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            logger.error("保存农户信息失败: {}", farmerInfo.getFarmerName(), e);
            return false;
        }
    }

    /**
     * 生成合同号
     */
    private String generateContractNumber(String township, String village) {
        // 生成格式：HT + 年份 + 乡镇代码 + 村代码 + 序号
        String year = String.valueOf(java.time.Year.now().getValue());
        String townshipCode = township.length() >= 2 ? township.substring(0, 2) : township;
        String villageCode = village.length() >= 2 ? village.substring(0, 2) : village;
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000);

        return "HT" + year + townshipCode + villageCode + timestamp;
    }
}