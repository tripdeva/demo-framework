package kr.co.demo.client.jpa.adapter;

import kr.co.demo.core.mapper.DomainMapper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * JPA Repository와 DomainMapper를 수동으로 주입받는 Adapter 베이스 클래스
 *
 * <p>Repository와 Mapper를 직접 생성자에서 주입받아 사용합니다.
 * 타입을 명시적으로 지정하므로 컴파일 타임에 타입 안전성이 보장됩니다.
 *
 * <p>DomainMapper를 사용하는 방식과 Function을 직접 전달하는 방식 모두 지원합니다.
 *
 * <p>사용 예시 (DomainMapper 방식):
 * <pre>{@code
 * @Adapter
 * public class OrderJpaAdapter extends ManualJpaAdapter<Order, OrderEntity, Long, OrderJpaRepository>
 *         implements OrderPort {
 *
 *     public OrderJpaAdapter(OrderJpaRepository repository, OrderDomainMapper mapper) {
 *         super(repository, mapper);
 *     }
 *
 *     @Override
 *     public Order save(Order order) {
 *         return save(order);
 *     }
 * }
 * }</pre>
 *
 * <p>사용 예시 (Function 방식):
 * <pre>{@code
 * @Adapter
 * public class OrderJpaAdapter extends ManualJpaAdapter<Order, OrderEntity, Long, OrderJpaRepository>
 *         implements OrderPort {
 *
 *     public OrderJpaAdapter(OrderJpaRepository repository) {
 *         super(repository);
 *     }
 *
 *     @Override
 *     public Order save(Order order) {
 *         return save(order, OrderEntity::from, OrderEntity::toDomain);
 *     }
 * }
 * }</pre>
 *
 * @param <D>  도메인 객체 타입
 * @param <E>  Entity 타입
 * @param <ID> Entity의 ID 타입
 * @param <R>  JpaRepository 타입
 * @see AutoJpaAdapter
 * @see DomainMapper
 */
public abstract class ManualJpaAdapter<D, E, ID, R extends JpaRepository<E, ID>> {

	/**
	 * JPA Repository 인스턴스
	 */
	protected final R repository;

	/**
	 * 도메인 ↔ Entity 변환 매퍼 (nullable)
	 */
	protected final DomainMapper<D, E> mapper;

	/**
	 * DomainMapper를 사용하는 생성자
	 *
	 * @param repository JPA Repository 인스턴스
	 * @param mapper     도메인 ↔ Entity 변환 매퍼
	 */
	protected ManualJpaAdapter(R repository, DomainMapper<D, E> mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	// ==================== DomainMapper 사용 방식 ====================

	/**
	 * 도메인 객체를 저장합니다. (DomainMapper 사용)
	 *
	 * @param domain 저장할 도메인 객체
	 * @return 저장된 도메인 객체
	 * @throws IllegalStateException DomainMapper가 설정되지 않은 경우
	 */
	protected D save(D domain) {
		requireMapper();
		E entity = mapper.toStorage(domain);
		E saved = repository.save(entity);
		return mapper.toDomain(saved);
	}

	/**
	 * 여러 도메인 객체를 일괄 저장합니다. (DomainMapper 사용)
	 *
	 * @param domains 저장할 도메인 객체 목록
	 * @return 저장된 도메인 객체 목록
	 * @throws IllegalStateException DomainMapper가 설정되지 않은 경우
	 */
	protected List<D> saveAll(List<D> domains) {
		requireMapper();
		List<E> entities = domains.stream()
				.map(mapper::toStorage)
				.toList();
		List<E> savedEntities = repository.saveAll(entities);
		return savedEntities.stream()
				.map(mapper::toDomain)
				.toList();
	}

	/**
	 * ID로 도메인 객체를 조회합니다. (DomainMapper 사용)
	 *
	 * @param id 조회할 Entity의 ID
	 * @return 도메인 객체를 담은 Optional (없으면 empty)
	 * @throws IllegalStateException DomainMapper가 설정되지 않은 경우
	 */
	protected Optional<D> findById(ID id) {
		requireMapper();
		return repository.findById(id).map(mapper::toDomain);
	}

	/**
	 * 모든 도메인 객체를 조회합니다. (DomainMapper 사용)
	 *
	 * @return 도메인 객체 목록
	 * @throws IllegalStateException DomainMapper가 설정되지 않은 경우
	 */
	protected List<D> findAll() {
		requireMapper();
		return repository.findAll().stream()
				.map(mapper::toDomain)
				.toList();
	}

	/**
	 * 도메인 객체를 삭제합니다. (DomainMapper 사용)
	 *
	 * @param domain 삭제할 도메인 객체
	 * @throws IllegalStateException DomainMapper가 설정되지 않은 경우
	 */
	protected void delete(D domain) {
		requireMapper();
		E entity = mapper.toStorage(domain);
		repository.delete(entity);
	}

	// ==================== Function 전달 방식 ====================

	/**
	 * 도메인 객체를 저장합니다. (Function 전달 방식)
	 *
	 * @param domain   저장할 도메인 객체
	 * @param toEntity 도메인 → Entity 변환 함수
	 * @param toDomain Entity → 도메인 변환 함수
	 * @return 저장된 도메인 객체
	 */
	protected D save(D domain, Function<D, E> toEntity, Function<E, D> toDomain) {
		E entity = toEntity.apply(domain);
		E saved = repository.save(entity);
		return toDomain.apply(saved);
	}

	/**
	 * 여러 도메인 객체를 일괄 저장합니다. (Function 전달 방식)
	 *
	 * @param domains  저장할 도메인 객체 목록
	 * @param toEntity 도메인 → Entity 변환 함수
	 * @param toDomain Entity → 도메인 변환 함수
	 * @return 저장된 도메인 객체 목록
	 */
	protected List<D> saveAll(List<D> domains, Function<D, E> toEntity, Function<E, D> toDomain) {
		List<E> entities = domains.stream()
				.map(toEntity)
				.toList();
		List<E> savedEntities = repository.saveAll(entities);
		return savedEntities.stream()
				.map(toDomain)
				.toList();
	}

	/**
	 * ID로 도메인 객체를 조회합니다. (Function 전달 방식)
	 *
	 * @param id       조회할 Entity의 ID
	 * @param toDomain Entity → 도메인 변환 함수
	 * @return 도메인 객체를 담은 Optional (없으면 empty)
	 */
	protected Optional<D> findById(ID id, Function<E, D> toDomain) {
		return repository.findById(id).map(toDomain);
	}

	/**
	 * 모든 도메인 객체를 조회합니다. (Function 전달 방식)
	 *
	 * @param toDomain Entity → 도메인 변환 함수
	 * @return 도메인 객체 목록
	 */
	protected List<D> findAll(Function<E, D> toDomain) {
		return repository.findAll().stream()
				.map(toDomain)
				.toList();
	}

	/**
	 * 도메인 객체를 삭제합니다. (Function 전달 방식)
	 *
	 * @param domain   삭제할 도메인 객체
	 * @param toEntity 도메인 → Entity 변환 함수
	 */
	protected void delete(D domain, Function<D, E> toEntity) {
		E entity = toEntity.apply(domain);
		repository.delete(entity);
	}

	// ==================== 공통 메서드 ====================

	/**
	 * ID로 Entity를 삭제합니다.
	 *
	 * @param id 삭제할 Entity의 ID
	 */
	protected void deleteById(ID id) {
		repository.deleteById(id);
	}

	/**
	 * 해당 ID의 Entity 존재 여부를 확인합니다.
	 *
	 * @param id 확인할 Entity의 ID
	 * @return 존재하면 true, 없으면 false
	 */
	protected boolean existsById(ID id) {
		return repository.existsById(id);
	}

	/**
	 * 전체 Entity 개수를 반환합니다.
	 *
	 * @return Entity 개수
	 */
	protected long count() {
		return repository.count();
	}

	/**
	 * DomainMapper가 설정되어 있는지 확인합니다.
	 *
	 * @throws IllegalStateException DomainMapper가 설정되지 않은 경우
	 */
	private void requireMapper() {
		if (mapper == null) {
			throw new IllegalStateException(
					"DomainMapper is not configured. " +
							"Use constructor with DomainMapper or use methods with Function parameters."
			);
		}
	}
}
