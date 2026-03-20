package kr.co.demo.client.jpa.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA Storage 모듈 자동 설정 클래스
 * <p>
 * JPA 및 QueryDSL 관련 공통 Bean을 자동으로 등록합니다.
 * Spring Boot의 자동 설정 메커니즘에 의해 로드됩니다.
 *
 * <p>제공하는 Bean:
 * <ul>
 *     <li>{@link JPAQueryFactory} - QueryDSL 사용 시 자동 등록</li>
 * </ul>
 *
 * @author demo-framework
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(JpaRepository.class)
public class JpaStorageAutoConfiguration {

	/**
	 * QueryDSL용 JPAQueryFactory Bean을 등록합니다.
	 * <p>
	 * QueryDSL 의존성이 classpath에 있을 때만 Bean이 생성됩니다.
	 * 이미 JPAQueryFactory Bean이 등록되어 있으면 생성하지 않습니다.
	 *
	 * @param entityManager JPA EntityManager
	 * @return JPAQueryFactory 인스턴스
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = "com.querydsl.jpa.impl.JPAQueryFactory")
	public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
		return new JPAQueryFactory(entityManager);
	}

}
