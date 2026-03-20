package kr.co.demo.core.storage.annotation;

import java.lang.annotation.*;

/**
 * 도메인 객체와 데이터베이스 테이블 간의 매핑을 정의합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("orders")
 * public class Order {
 *     // ...
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageTable {


	/**
	 * 테이블 이름
	 * <p>
	 * 미지정 시 클래스명을 스네이크 케이스로 변환합니다.
	 * (예: OrderItem → order_item)
	 *
	 * @return 테이블 이름
	 */
	String value() default "";
}
