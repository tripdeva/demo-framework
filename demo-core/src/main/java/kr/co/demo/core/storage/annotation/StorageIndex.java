package kr.co.demo.core.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 데이터베이스 인덱스를 정의합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageTable("orders")
 * @StorageIndex(name = "idx_status", columns = {"status"})
 * @StorageIndex(name = "idx_customer_status", columns = {"customer_name", "status"}, unique = true)
 * public class Order { ... }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(StorageIndexes.class)
public @interface StorageIndex {

	/**
	 * 인덱스 이름 (생략 시 자동 생성).
	 *
	 * @return 인덱스 이름
	 */
	String name() default "";

	/**
	 * 인덱스에 포함할 컬럼명 목록 (snake_case).
	 *
	 * @return 컬럼명 배열
	 */
	String[] columns();

	/**
	 * UNIQUE 인덱스 여부.
	 *
	 * @return unique 여부 (기본: false)
	 */
	boolean unique() default false;
}
