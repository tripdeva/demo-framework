package kr.co.demo.core.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 엔티티 수정 시각을 자동 기록하는 필드를 지정합니다.
 *
 * <p>JPA의 {@code @LastModifiedDate}로 변환됩니다.
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageUpdatedAt {
}
