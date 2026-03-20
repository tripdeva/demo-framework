package kr.co.demo.core.storage.enums;

/**
 * 영속성 전이 전략.
 *
 * @author demo-framework
 * @since 1.1.0
 */
public enum CascadeType {
	ALL,
	PERSIST,
	MERGE,
	REMOVE,
	REFRESH,
	DETACH
}
