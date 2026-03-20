package kr.co.demo.client.mybatis.util;

import kr.co.demo.core.storage.annotation.StorageColumn;
import kr.co.demo.core.storage.annotation.StorageId;
import kr.co.demo.core.storage.annotation.StorageTable;

import java.lang.reflect.Field;

public class Resolver {

    // ----------------- resolve helpers -----------------

    static String resolveTable(Class<?> type) {
        StorageTable table = type.getAnnotation(StorageTable.class);
        if (table == null) {
            throw new IllegalStateException("@StorageTable 없음: " + type.getName());
        }
        return table.value();
    }

    static Field resolveIdField(Class<?> type) {
        for (Field f : type.getDeclaredFields()) {
            if (f.isAnnotationPresent(StorageId.class)) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new IllegalStateException("@StorageId 필드 없음");
    }

    static Field resolveField(Class<?> type, String name) {
        try {
            Field f = type.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("존재하지 않는 필드: " + name);
        }
    }

    static String resolveColumn(Field field) {
        StorageColumn column = field.getAnnotation(StorageColumn.class);
        return column == null || column.value().equals("") ? camelToSnake(field.getName()) : column.value();
    }

    static String camelToSnake(String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
