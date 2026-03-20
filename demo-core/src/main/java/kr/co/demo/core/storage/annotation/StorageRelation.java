package kr.co.demo.core.storage.annotation;

import kr.co.demo.core.storage.enums.RelationType;

import java.lang.annotation.*;

/**
 * 도메인 객체 간의 연관관계를 정의합니다.
 * <p>
 * JPA에서는 연관관계 매핑에 활용되고,
 * MyBatis/JDBC에서는 필요 시 조인 정보로 활용됩니다.
 *
 * <p>사용 예시 (다대일):
 * <pre>{@code
 * @StorageTable("order_items")
 * public class OrderItem {
 *
 *     @StorageId
 *     private Long id;
 *
 *     @StorageRelation(type = RelationType.MANY_TO_ONE)
 *     private Order order;
 * }
 * }</pre>
 *
 * <p>사용 예시 (일대다):
 * <pre>{@code
 * @StorageTable("orders")
 * public class Order {
 *
 *     @StorageId
 *     private Long id;
 *
 *     @StorageRelation(type = RelationType.ONE_TO_MANY, mappedBy = "order")
 *     private List<OrderItem> items;
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageRelation {


	/**
	 * 연관관계 타입
	 */
	RelationType type();

	/**
	 * 양방향 관계에서 반대편 필드명
	 */
	String mappedBy() default "";

	/**
	 * 대상 도메인 객체에서 참조할 필드명
	 * <p>
	 * 미지정 시 대상의 @StorageId 필드를 사용합니다.
	 * UNIQUE 필드를 참조할 경우 해당 필드명을 지정합니다.
	 *
	 * @return 대상 필드명
	 */
	String targetField() default "";
}
