package kr.co.demo.core.storage.annotation;

import kr.co.demo.core.storage.enums.EnumType;

import java.lang.annotation.*;

/**
 * Enum 필드의 데이터베이스 저장 방식을 지정합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("orders")
 * public class Order {
 *
 *     @StorageId
 *     private Long id;
 *
 *     @StorageEnum(EnumType.STRING)
 *     private OrderStatus status;  // "PENDING", "COMPLETED" 로 저장
 * }
 * }</pre>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageEnum {


	/**
	 * Enum 저장 방식
	 *
	 * @return 저장 방식 (기본값: STRING)
	 */
	EnumType value() default EnumType.STRING;
}
