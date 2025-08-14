package com.tobacco.weight.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导入报告模型
 * 包含导入操作的完整统计信息和详细记录
 */
public class ImportReport {
    
    // 导入统计
    private final LocalDateTime importTime;
    private final String fileName;
    private final boolean dryRun;
    
    private int totalRows;
    private int insertedFarmers;
    private int updatedFarmers;
    private int insertedContracts;
    private int updatedContracts;
    private int errorCount;
    private int warningCount;
    
    // 详细记录
    private final List<ImportRecord> allRecords;
    private final List<ImportRecord> errorRecords;
    private final List<ImportRecord> warningRecords;
    private final List<ImportRecord> successRecords;
    
    public ImportReport(String fileName, boolean dryRun) {
        this.fileName = fileName;
        this.dryRun = dryRun;
        this.importTime = LocalDateTime.now();
        this.allRecords = new ArrayList<>();
        this.errorRecords = new ArrayList<>();
        this.warningRecords = new ArrayList<>();
        this.successRecords = new ArrayList<>();
    }
    
    /**
     * 添加导入记录
     */
    public void addRecord(ImportRecord record) {
        allRecords.add(record);
        
        switch (record.getStatus()) {
            case ERROR:
                errorRecords.add(record);
                errorCount++;
                break;
            case WARNING:
                warningRecords.add(record);
                warningCount++;
                break;
            case OK:
                successRecords.add(record);
                break;
        }
        
        totalRows = allRecords.size();
    }
    
    /**
     * 增加统计计数
     */
    public void incrementInsertedFarmers() { insertedFarmers++; }
    public void incrementUpdatedFarmers() { updatedFarmers++; }
    public void incrementInsertedContracts() { insertedContracts++; }
    public void incrementUpdatedContracts() { updatedContracts++; }
    
    /**
     * 生成摘要报告
     */
    public String generateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== 导入报告摘要 ===\n");
        summary.append("文件名: ").append(fileName).append("\n");
        summary.append("导入时间: ").append(importTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        summary.append("模式: ").append(dryRun ? "预览模式" : "正式导入").append("\n\n");
        
        summary.append("处理统计:\n");
        summary.append("- 总记录数: ").append(totalRows).append("\n");
        summary.append("- 成功处理: ").append(successRecords.size()).append("\n");
        summary.append("- 警告记录: ").append(warningCount).append("\n");
        summary.append("- 错误记录: ").append(errorCount).append("\n\n");
        
        if (!dryRun) {
            summary.append("数据库操作:\n");
            summary.append("- 新增农户: ").append(insertedFarmers).append("\n");
            summary.append("- 更新农户: ").append(updatedFarmers).append("\n");
            summary.append("- 新增合同: ").append(insertedContracts).append("\n");
            summary.append("- 更新合同: ").append(updatedContracts).append("\n");
        }
        
        return summary.toString();
    }
    
    /**
     * 生成详细报告
     */
    public String generateDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append(generateSummary()).append("\n");
        
        if (!errorRecords.isEmpty()) {
            report.append("=== 错误记录 ===\n");
            for (ImportRecord record : errorRecords) {
                report.append("第").append(record.getRowNumber()).append("行: ")
                      .append(record.getFarmerName()).append(" (").append(record.getMaskedNationalId()).append(") - ")
                      .append(record.getMessage()).append("\n");
            }
            report.append("\n");
        }
        
        if (!warningRecords.isEmpty()) {
            report.append("=== 警告记录 ===\n");
            for (ImportRecord record : warningRecords) {
                report.append("第").append(record.getRowNumber()).append("行: ")
                      .append(record.getFarmerName()).append(" (").append(record.getMaskedNationalId()).append(") - ")
                      .append(record.getMessage()).append("\n");
            }
            report.append("\n");
        }
        
        if (!successRecords.isEmpty()) {
            report.append("=== 成功记录概览 ===\n");
            Map<String, Long> stationStats = successRecords.stream()
                .collect(Collectors.groupingBy(ImportRecord::getStation, Collectors.counting()));
            
            for (Map.Entry<String, Long> entry : stationStats.entrySet()) {
                report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("条\n");
            }
        }
        
        return report.toString();
    }
    
    /**
     * 生成CSV格式的错误报告
     */
    public String generateErrorCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("行号,站点,姓名,合同号,身份证号,行政区划,合同量,状态,错误信息\n");
        
        List<ImportRecord> problemRecords = new ArrayList<>();
        problemRecords.addAll(errorRecords);
        problemRecords.addAll(warningRecords);
        
        for (ImportRecord record : problemRecords) {
            csv.append(record.getRowNumber()).append(",");
            csv.append("\"").append(record.getStation()).append("\",");
            csv.append("\"").append(record.getFarmerName()).append("\",");
            csv.append("\"").append(record.getContractNo()).append("\",");
            csv.append("\"").append(record.getMaskedNationalId()).append("\",");
            csv.append("\"").append(record.getAdminDivision()).append("\",");
            csv.append(record.getContractAmount()).append(",");
            csv.append(record.getStatus()).append(",");
            csv.append("\"").append(record.getMessage()).append("\"\n");
        }
        
        return csv.toString();
    }
    
    // Getters
    public LocalDateTime getImportTime() { return importTime; }
    public String getFileName() { return fileName; }
    public boolean isDryRun() { return dryRun; }
    public int getTotalRows() { return totalRows; }
    public int getInsertedFarmers() { return insertedFarmers; }
    public int getUpdatedFarmers() { return updatedFarmers; }
    public int getInsertedContracts() { return insertedContracts; }
    public int getUpdatedContracts() { return updatedContracts; }
    public int getErrorCount() { return errorCount; }
    public int getWarningCount() { return warningCount; }
    public List<ImportRecord> getAllRecords() { return new ArrayList<>(allRecords); }
    public List<ImportRecord> getErrorRecords() { return new ArrayList<>(errorRecords); }
    public List<ImportRecord> getWarningRecords() { return new ArrayList<>(warningRecords); }
    public List<ImportRecord> getSuccessRecords() { return new ArrayList<>(successRecords); }
    
    /**
     * 检查导入是否成功
     */
    public boolean isSuccessful() {
        return errorCount == 0;
    }
    
    /**
     * 检查是否有问题需要注意
     */
    public boolean hasIssues() {
        return errorCount > 0 || warningCount > 0;
    }
}
