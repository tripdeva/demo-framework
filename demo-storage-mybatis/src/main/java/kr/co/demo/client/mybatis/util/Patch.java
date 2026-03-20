package kr.co.demo.client.mybatis.util;

import java.util.List;

public final class Patch<T> {

    private final Class<T> domainType;
    private final Object id;
    private final List<PatchValue<?>> values;

    private Patch(Class<T> domainType, Object id, List<PatchValue<?>> values) {
        this.domainType = domainType;
        this.id = id;
        this.values = values;
    }

    @SafeVarargs
    public static <T> Patch<T> create(
            Class<T> type,
            Object id,
            PatchValue<?>... values
    ) {
        if (id == null) {
            throw new IllegalStateException("PATCH id는 필수입니다");
        }
        if (values == null || values.length == 0) {
            throw new IllegalStateException("PatchValue가 최소 1개 이상 필요합니다");
        }
        return new Patch<>(type, id, List.of(values));
    }

    public Class<T> domainType() {
        return domainType;
    }

    public Object id() {
        return id;
    }

    public List<PatchValue<?>> values() {
        return values;
    }
}

