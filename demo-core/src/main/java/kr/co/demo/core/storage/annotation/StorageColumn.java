package kr.co.demo.core.storage.annotation;

import java.lang.annotation.*;

/**
 * 도메인 객체의 필드와 데이터베이스 컬럼 간의 매핑을 정의합니다.
 * <p>
 * 어노테이션을 생략하면 필드명을 스네이크 케이스로 변환하여 사용합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("orders")
 * public class Order {
 *
 *     @StorageColumn(value = "order_no", nullable = false)
 *     private String orderNumber;
 *
 *     private LocalDateTime createdAt;  // → created_at 자동 매핑
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageColumn {

	/**
	 * 컬럼 이름
	 * <p>
	 * 미지정 시 필드명을 스네이크 케이스로 변환합니다.
	 * (예: orderNumber → order_number)
	 *
	 * @return 컬럼 이름
	 */
	String value() default "";

	/**
	 * NULL 허용 여부
	 *
	 * @return NULL 허용 여부 (기본값: true)
	 */
	boolean nullable() default true;

	/**
	 * UNIQUE 제약 조건
	 */
	boolean unique() default false;
}
