package kr.co.demo.client.mybatis.util;

public final class PatchValue<T> {

    private final String fieldName;
    private final T value;

    private PatchValue(String fieldName, T value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    public static <T> PatchValue<T> of(String fieldName, T value) {
        return new PatchValue<>(fieldName, value);
    }

    public String fieldName() {
        return fieldName;
    }

    public T value() {
        return value;
    }

    public boolean isNull() {
        return value == null;
    }
}
