package kr.co.demo.client.processor.util;

/**
 * 네이밍 변환 유틸리티
 *
 * @author demo-framework
 * @since 1.0.0
 */
public final class NamingUtils {

    private NamingUtils() {
    }

    /**
     * camelCase를 snake_case로 변환합니다.
     */
    public static String toSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * 첫 글자를 대문자로 변환합니다.
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 첫 글자를 소문자로 변환합니다.
     */
    public static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
