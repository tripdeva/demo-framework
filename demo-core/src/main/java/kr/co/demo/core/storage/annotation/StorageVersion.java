package kr.co.demo.core.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 낙관적 잠금(Optimistic Locking)을 위한 버전 필드를 지정합니다.
 *
 * <p>JPA의 {@code @Version}으로 변환되며, 동시 수정 충돌을 감지합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("orders")
 * public class Order {
 *     @StorageId
 *     private Long id;
 *
 *     @StorageVersion
 *     private Long version;
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageVersion {
}
