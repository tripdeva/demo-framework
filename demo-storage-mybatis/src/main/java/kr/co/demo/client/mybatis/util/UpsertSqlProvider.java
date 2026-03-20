package kr.co.demo.client.mybatis.util;

import kr.co.demo.client.mybatis.config.DataSourceConfig;
import kr.co.demo.core.storage.annotation.StorageColumn;
import kr.co.demo.core.storage.annotation.StorageId;
import kr.co.demo.core.storage.enums.DialectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 연결 한 DB 종류에 따라 Upsert 생성
 */
public class UpsertSqlProvider {

    /**
     * DataSource 에 따라 분기
     *
     * @param param
     * @return
     */
    public static String build(Object param) {
        DialectType dialect =
                DataSourceConfig.resolve(DataSourceConfig.get());

        if (dialect == DialectType.MYSQL) {
            return buildMysql(param);
        }
        if (dialect == DialectType.POSTGRES) {
            return buildPostgres(param);
        }
        if (dialect == DialectType.H2) {
            return buildH2(param);
        }

        throw new IllegalStateException("지원하지 않는 Dialect");
    }

    /**
     * Create Upsert By Mysql
     * @param param
     * @return
     */
    public static String buildMysql(Object param) {
        Class<?> type = param.getClass();

        String table = Resolver.resolveTable(type);
        Field idField = Resolver.resolveIdField(type);
        String idColumn = Resolver.resolveColumn(idField);

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> updates = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);

            if (isId(field)) {
                columns.add(idColumn);
                values.add("#{" + field.getName() + "}");
                continue;
            }

            StorageColumn column = field.getAnnotation(StorageColumn.class);
            if (column == null) {
                continue; // 정책상 StorageColumn 없는 필드는 무시
            }

            String columnName = Resolver.resolveColumn(field);

            columns.add(columnName);
            values.add("#{" + field.getName() + "}");

            // UPSERT UPDATE 절 (ID 제외)
            updates.add(columnName + " = VALUES(" + columnName + ")");
        }

        if (columns.isEmpty()) {
            throw new IllegalStateException("UPSERT 대상 컬럼이 없습니다");
        }

        return "INSERT INTO " + table +
                " (" + String.join(", ", columns) + ")" +
                " VALUES (" + String.join(", ", values) + ")" +
                " ON DUPLICATE KEY UPDATE " +
                String.join(", ", updates);
    }


    /**
     * Create Upsert By Postgres
     * @param param
     * @return
     */
    public static String buildPostgres(Object param) {
        Class<?> type = param.getClass();

        String table = Resolver.resolveTable(type);
        Field idField = Resolver.resolveIdField(type);
        String idColumn = Resolver.resolveColumn(idField);

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> updates = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);

            StorageColumn column = field.getAnnotation(StorageColumn.class);
            if (column == null && !isId(field)) continue;

            String columnName = Resolver.resolveColumn(field);

            columns.add(columnName);
            values.add("#{" + field.getName() + "}");

            if (!isId(field)) {
                updates.add(columnName + " = EXCLUDED." + columnName);
            }
        }

        return "INSERT INTO " + table +
                " (" + String.join(", ", columns) + ")" +
                " VALUES (" + String.join(", ", values) + ")" +
                " ON CONFLICT (" + idColumn + ")" +
                " DO UPDATE SET " +
                String.join(", ", updates);
    }

    /**
     * Create Upsert By Oracle
     * @param param
     * @return
     */
    public static String buildOracle(Object param) {
        Class<?> type = param.getClass();

        String table = Resolver.resolveTable(type);
        Field idField = Resolver.resolveIdField(type);
        String idColumn = Resolver.resolveColumn(idField);

        List<String> updateSets = new ArrayList<>();
        List<String> insertColumns = new ArrayList<>();
        List<String> insertValues = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);

            StorageColumn column = field.getAnnotation(StorageColumn.class);
            if (column == null && !isId(field)) continue;

            String columnName = Resolver.resolveColumn(field);

            insertColumns.add(columnName);
            insertValues.add("#{" + field.getName() + "}");

            if (!isId(field)) {
                updateSets.add("t." + columnName + " = #{" + field.getName() + "}");
            }
        }

        return "MERGE INTO " + table + " t " +
                "USING (SELECT #{" + idField.getName() + "} AS " + idColumn + " FROM dual) s " +
                "ON (t." + idColumn + " = s." + idColumn + ") " +
                "WHEN MATCHED THEN UPDATE SET " +
                String.join(", ", updateSets) + " " +
                "WHEN NOT MATCHED THEN INSERT (" +
                String.join(", ", insertColumns) +
                ") VALUES (" +
                String.join(", ", insertValues) +
                ")";
    }

    public static String buildH2(Object param) {
        Class<?> type = param.getClass();

        String table = Resolver.resolveTable(type);
        Field idField = Resolver.resolveIdField(type);
        String idColumn = Resolver.resolveColumn(idField);

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> updates = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            field.setAccessible(true);

            StorageColumn column = field.getAnnotation(StorageColumn.class);
            if (column == null && !isId(field)) continue;

            String columnName = Resolver.resolveColumn(field);

            columns.add(columnName);
            values.add("#{" + field.getName() + "}");

            if (!isId(field)) {
                updates.add(columnName + " = #{" + field.getName() + "}");
            }
        }

        return "MERGE INTO " + table +
                " (" + String.join(", ", columns) + ")" +
                " KEY(" + idColumn + ")" +
                " VALUES (" + String.join(", ", values) + ")";
    }



    private static boolean isId(Field field) {
        return field.isAnnotationPresent(StorageId.class);
    }

}
