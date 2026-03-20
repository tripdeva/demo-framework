package kr.co.demo.client.mybatis.util;

import kr.co.demo.core.storage.annotation.StorageColumn;
import kr.co.demo.core.storage.annotation.StorageId;
import kr.co.demo.core.storage.annotation.StorageTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Update 문 생성해주는 클래스
 *
 * - 값이 있는 컬럼만 Set 생성
 */
public class PatchSqlProvider {

    private static final Logger log = LoggerFactory.getLogger(PatchSqlProvider.class);

    /**
     * UPDATE 쿼리 생성 메서드
     *
     * @param patch
     * @return
     */
    public static String build(Patch<?> patch) {
        Class<?> type = patch.domainType();

        String table = Resolver.resolveTable(type);
        Field idField = Resolver.resolveIdField(type);

        List<String> sets = new ArrayList<>();

        for (PatchValue<?> pv : patch.values()) {
            Field field = Resolver.resolveField(type, pv.fieldName());
            StorageColumn column = field.getAnnotation(StorageColumn.class);

            if (field.isAnnotationPresent(StorageId.class)) continue;

            boolean nullable = column == null || column.nullable();
            String columnName = Resolver.resolveColumn(field);

            if (pv.isNull() && !nullable) {
                throw new IllegalStateException(
                        "컬럼 '" + columnName + "' 은 nullable=false 이므로 NULL 로 업데이트할 수 없습니다"
                );
            }

            if (pv.isNull()) {
                sets.add(columnName + " = NULL");
            } else {
                sets.add(columnName + " = #{values[" +
                        patch.values().indexOf(pv) + "].value}");
            }
        }

        if (sets.isEmpty()) {
            throw new IllegalStateException("PATCH 대상 필드 없음");
        }

        String sql = "UPDATE " + table +
                " SET " + String.join(", ", sets) +
                " WHERE " + idField.getName() + " = #{id}";
        log.debug(sql);

        return "UPDATE " + table +
                " SET " + String.join(", ", sets) +
                " WHERE " + idField.getName() + " = #{id}";
    }
}
