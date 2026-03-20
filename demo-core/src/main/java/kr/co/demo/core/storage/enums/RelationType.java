package kr.co.demo.core.storage.enums;

/**
 * 연관관계 타입
 */
public enum RelationType {

	/**
	 * 다대일 관계
	 * <p>
	 * 예: OrderItem(다) → Order(일)
	 */
	MANY_TO_ONE,

	/**
	 * 일대다 관계
	 * <p>
	 * 예: Order(일) → OrderItem(다)
	 */
	ONE_TO_MANY,

	/**
	 * 일대일 관계
	 * <p>
	 * 예: User(일) ↔ UserProfile(일)
	 */
	ONE_TO_ONE,

	/**
	 * 다대다 관계
	 * <p>
	 * 예: Student(다) ↔ Course(다)
	 */
	MANY_TO_MANY
}
