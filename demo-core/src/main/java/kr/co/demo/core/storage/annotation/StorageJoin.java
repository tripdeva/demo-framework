package kr.co.demo.core.storage.annotation;

import kr.co.demo.core.storage.enums.JoinType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MyBatis/JDBC에서 JOIN 쿼리 힌트를 정의합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("orders")
 * @StorageJoin(
 *     target = OrderItem.class,
 *     type = JoinType.LEFT,
 *     on = "orders.id = order_items.order_id"
 * )
 * public class Order { ... }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(StorageJoins.class)
public @interface StorageJoin {

	/**
	 * JOIN 대상 도메인 클래스.
	 *
	 * @return 대상 클래스
	 */
	Class<?> target();

	/**
	 * JOIN 유형.
	 *
	 * @return JOIN 타입 (기본: INNER)
	 */
	JoinType type() default JoinType.INNER;

	/**
	 * JOIN 조건 (ON 절).
	 *
	 * @return ON 조건 SQL
	 */
	String on();

	/**
	 * JOIN 별칭 (optional).
	 *
	 * @return 별칭
	 */
	String alias() default "";
}
