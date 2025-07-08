package com.tobacco.weight.data.database;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * Room数据库日期类型转换器
 */
public class DateConverter {

    /**
     * 时间戳转日期
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * 日期转时间戳
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}