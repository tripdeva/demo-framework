package kr.co.demo.core.storage.annotation;

import kr.co.demo.core.storage.enums.CascadeType;
import kr.co.demo.core.storage.enums.FetchType;
import kr.co.demo.core.storage.enums.RelationType;

import java.lang.annotation.*;

/**
 * 도메인 객체 간의 연관관계를 정의합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @StorageRelation(
 *     type = RelationType.ONE_TO_MANY,
 *     mappedBy = "order",
 *     cascade = CascadeType.ALL,
 *     fetch = FetchType.LAZY
 * )
 * private List<OrderItem> items;
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageRelation {

	/**
	 * 연관관계 타입.
	 *
	 * @return 관계 유형
	 */
	RelationType type();

	/**
	 * 양방향 관계에서 반대편 필드명.
	 *
	 * @return mappedBy 필드명
	 */
	String mappedBy() default "";

	/**
	 * 대상 도메인 객체에서 참조할 필드명.
	 *
	 * @return 대상 필드명
	 */
	String targetField() default "";

	/**
	 * 영속성 전이 전략.
	 *
	 * @return cascade 타입 배열 (기본: 없음)
	 */
	CascadeType[] cascade() default {};

	/**
	 * 페치 전략.
	 *
	 * @return fetch 타입 (기본: DEFAULT → JPA 기본값 사용)
	 */
	FetchType fetch() default FetchType.DEFAULT;
}
