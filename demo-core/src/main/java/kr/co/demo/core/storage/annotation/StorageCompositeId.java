package kr.co.demo.core.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 복합 기본키를 구성하는 필드를 지정합니다.
 *
 * <p>여러 필드에 이 어노테이션을 붙이면 자동으로 {@code @IdClass}가 생성됩니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("order_details")
 * public class OrderDetail {
 *     @StorageCompositeId
 *     private Long orderId;
 *
 *     @StorageCompositeId
 *     private Long productId;
 *
 *     private int quantity;
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageCompositeId {
}
