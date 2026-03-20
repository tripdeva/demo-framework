package kr.co.demo.core.storage.annotation;

import java.lang.annotation.*;

/**
 * 해당 필드를 영속화 대상에서 제외합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("orders")
 * public class Order {
 *
 *     @StorageId
 *     private Long id;
 *
 *     @StorageTransient
 *     private String tempData;  // DB에 저장되지 않음
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageTransient {
}
