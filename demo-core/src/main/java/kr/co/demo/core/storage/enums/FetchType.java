package kr.co.demo.core.storage.enums;

/**
 * 페치 전략.
 *
 * @author demo-framework
 * @since 1.1.0
 */
public enum FetchType {
	/** JPA 기본값 사용 (OneToMany=LAZY, ManyToOne=EAGER) */
	DEFAULT,
	LAZY,
	EAGER
}
