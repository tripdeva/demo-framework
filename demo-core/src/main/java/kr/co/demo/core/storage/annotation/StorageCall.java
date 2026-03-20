package kr.co.demo.core.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 저장 프로시저(Stored Procedure) 호출을 정의합니다.
 *
 * <p>MyBatis에서는 {@code @Select("{CALL procedure_name(...)}")}로,
 * JPA에서는 {@code @NamedStoredProcedureQuery}로 변환됩니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageCall(name = "sp_get_order_summary", resultType = OrderSummary.class)
 * OrderSummary getOrderSummary(@Param("orderId") Long orderId);
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageCall {

	/**
	 * 프로시저 이름.
	 *
	 * @return 프로시저명
	 */
	String name();

	/**
	 * 결과 타입.
	 *
	 * @return 결과 클래스 (기본: void)
	 */
	Class<?> resultType() default void.class;
}
