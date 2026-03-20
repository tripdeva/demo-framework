package kr.co.demo.core.mapper;

/**
 * 도메인 객체와 영속성 모델 간의 변환을 담당하는 인터페이스
 * <p>
 * 도메인 객체가 특정 저장소 기술(JPA, MyBatis 등)에 종속되지 않도록
 * 변환 로직을 분리합니다.
 *
 * @param <D> 도메인 객체 타입
 * @param <P> 영속성 모델 타입 (JPA Entity, MyBatis DTO 등)
 */
public interface DomainMapper<D, P> {

	/**
	 * 도메인 객체를 영속성 모델로 변환합니다.
	 *
	 * @param domain 도메인 객체
	 * @return 영속성 모델
	 */
	P toStorage(D domain);

	/**
	 * 영속성 모델을 도메인 객체로 변환합니다.
	 *
	 * @param persistence 영속성 모델
	 * @return 도메인 객체
	 */
	D toDomain(P persistence);
}
