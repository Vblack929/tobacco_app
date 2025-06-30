package com.tobacco.weight.data.database;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Room数据库日期类型转换器
 * 用于在Date和Long之间进行转换
 */
public class DateConverter {
    
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
} 