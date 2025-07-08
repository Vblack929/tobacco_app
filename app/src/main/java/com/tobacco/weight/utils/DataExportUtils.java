package com.tobacco.weight.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import com.tobacco.weight.data.model.WeightRecord;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 数据导出工具类
 * 支持导出CSV格式的烟叶称重记录
 */
public class DataExportUtils {

    private static final String EXPORT_FOLDER = "TobaccoWeightExports";
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss",
            Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault());

    /**
     * 导出所有称重记录到CSV文件
     */
    public static void exportAllRecordsToCSV(Context context, List<WeightRecord> records, ExportCallback callback) {
        if (records == null || records.isEmpty()) {
            callback.onError("没有数据可导出");
            return;
        }

        String filename = "所有烟农预检记录_" + FILE_DATE_FORMAT.format(new Date()) + ".csv";
        exportRecordsToCSV(context, records, filename, "所有烟农预检记录", callback);
    }

    /**
     * 导出指定烟农的称重记录到CSV文件
     */
    public static void exportFarmerRecordsToCSV(Context context, List<WeightRecord> records, String farmerName,
            ExportCallback callback) {
        if (records == null || records.isEmpty()) {
            callback.onError("该烟农没有预检记录");
            return;
        }

        String filename = farmerName + "_预检记录_" + FILE_DATE_FORMAT.format(new Date()) + ".csv";
        exportRecordsToCSV(context, records, filename, farmerName + "预检记录", callback);
    }

    /**
     * 通用CSV导出方法
     */
    private static void exportRecordsToCSV(Context context, List<WeightRecord> records, String filename, String title,
            ExportCallback callback) {
        try {
            // 创建导出目录
            File exportDir = getExportDirectory();
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                callback.onError("无法创建导出目录");
                return;
            }

            // 创建CSV文件
            File csvFile = new File(exportDir, filename);
            FileWriter writer = new FileWriter(csvFile, false);

            // 写入BOM头（支持Excel正确显示中文）
            writer.write('\uFEFF');

            // 写入CSV头部
            writer.append("记录编号,烟农姓名,身份证号,烟叶部位,捆数,重量(kg),预检编号,操作员,仓库编号,创建时间,状态\n");

            // 写入数据行
            for (WeightRecord record : records) {
                writer.append(escapeCsvField(record.getRecordNumber())).append(",");
                writer.append(escapeCsvField(record.getFarmerName())).append(",");
                writer.append(escapeCsvField(record.getIdCardNumber())).append(",");
                writer.append(escapeCsvField(record.getTobaccoPart())).append(",");
                writer.append(String.valueOf(record.getTobaccoBundles())).append(",");
                writer.append(String.valueOf(record.getWeight())).append(",");
                writer.append(escapeCsvField(record.getPreCheckNumber())).append(",");
                writer.append(escapeCsvField(record.getOperatorName())).append(",");
                writer.append(escapeCsvField(record.getWarehouseNumber())).append(",");
                writer.append(escapeCsvField(
                        record.getCreateTime() != null ? DISPLAY_DATE_FORMAT.format(record.getCreateTime()) : ""))
                        .append(",");
                writer.append(escapeCsvField(record.getStatus())).append("\n");
            }

            writer.close();

            String successMessage = String.format("导出成功！\n文件：%s\n位置：%s\n记录数：%d条",
                    filename, csvFile.getAbsolutePath(), records.size());
            callback.onSuccess(successMessage, csvFile.getAbsolutePath(), csvFile);

        } catch (IOException e) {
            callback.onError("导出失败：" + e.getMessage());
        }
    }

    /**
     * 转义CSV字段，处理包含逗号、引号、换行符的情况
     */
    private static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }

        // 如果字段包含逗号、引号或换行符，需要用引号包围并转义内部引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    /**
     * 获取导出目录
     */
    private static File getExportDirectory() {
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        return new File(documentsDir, EXPORT_FOLDER);
    }

    /**
     * 检查存储权限并获取可用的导出路径
     */
    public static String getExportPath() {
        File exportDir = getExportDirectory();
        return exportDir.getAbsolutePath();
    }

    /**
     * 打开导出文件夹
     */
    public static void openExportFolder(Context context) {
        try {
            File exportDir = getExportDirectory();
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", exportDir);
            intent.setDataAndType(uri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "选择文件管理器"));
        } catch (Exception e) {
            Toast.makeText(context, "无法打开文件夹: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 打开CSV文件
     */
    public static void openCsvFile(Context context, File csvFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", csvFile);
            intent.setDataAndType(uri, "text/csv");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "选择应用打开CSV文件"));
        } catch (Exception e) {
            Toast.makeText(context, "无法打开文件: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 获取导出文件夹的友好路径显示
     */
    public static String getExportPathDisplay() {
        return "内部存储/Documents/TobaccoWeightExports/";
    }

    /**
     * 导出结果回调接口
     */
    public interface ExportCallback {
        void onSuccess(String message, String filePath, File file);

        void onError(String error);
    }
}