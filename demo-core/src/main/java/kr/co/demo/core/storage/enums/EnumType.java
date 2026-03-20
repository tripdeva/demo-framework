package kr.co.demo.core.storage.enums;

/**
 * Enum 저장 방식
 */
public enum EnumType {

	/**
	 * Enum의 name()을 문자열로 저장
	 * <p>
	 * 예: OrderStatus.PENDING → "PENDING"
	 */
	STRING,

	/**
	 * Enum의 ordinal()을 숫자로 저장
	 * <p>
	 * 예: OrderStatus.PENDING → 0
	 */
	ORDINAL
}
