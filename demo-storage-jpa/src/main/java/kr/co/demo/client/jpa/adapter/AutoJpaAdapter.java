package kr.co.demo.client.jpa.adapter;

import kr.co.demo.core.exception.StorageException;
import kr.co.demo.core.mapper.DomainMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository와 Mapper를 자동으로 연결하는 JPA Adapter 베이스 클래스
 *
 * <p>Annotation Processor가 생성한 Repository와 Mapper를
 * ApplicationContext에서 자동으로 조회하여 사용합니다.
 *
 * <p>예외 변환 기능이 내장되어 있어, DataAccessException 발생 시
 * StorageException으로 자동 변환됩니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Adapter
 * public class OrderAdapter extends AutoJpaAdapter<Order, Long>
 *         implements OrderPort {
 *
 *     public OrderAdapter(ApplicationContext context) {
 *         super(Order.class, context);
 *     }
 *
 *     @Override
 *     public Order save(Order order) {
 *         return saveWithException(order);
 *     }
 *
 *     @Override
 *     public Optional<Order> findById(Long id) {
 *         return findByIdWithException(id);
 *     }
 * }
 * }</pre>
 *
 * <p>Bean 조회 규칙:
 * <ul>
 *     <li>Repository: {@code {도메인명}EntityRepository}</li>
 *     <li>Mapper: {@code {도메인명}StorageMapper}</li>
 * </ul>
 *
 * @param <D>  도메인 객체 타입
 * @param <ID> Entity의 ID 타입
 * @see ManualJpaAdapter
 * @see StorageException
 */
@SuppressWarnings("unchecked")
public abstract class AutoJpaAdapter<D, ID> {

	/**
	 * JPA Repository 인스턴스
	 */
	protected final JpaRepository<Object, ID> repository;

	/**
	 * 도메인 ↔ Entity 변환 매퍼
	 */
	protected final DomainMapper<D, Object> mapper;

	/**
	 * 도메인 클래스 (예외 메시지용)
	 */
	protected final Class<D> domainClass;

	/**
	 * AutoJpaAdapter 생성자
	 *
	 * <p>ApplicationContext에서 Repository와 Mapper를 자동으로 조회합니다.
	 *
	 * @param domainClass 도메인 클래스
	 * @param context     Spring ApplicationContext
	 * @throws IllegalStateException Repository 또는 Mapper Bean을 찾을 수 없는 경우
	 */
	protected AutoJpaAdapter(Class<D> domainClass, ApplicationContext context) {
		this.domainClass = domainClass;
		String domainName = domainClass.getSimpleName();

		// Repository Bean 조회: OrderEntityRepository
		String repositoryBeanName = uncapitalize(domainName) + "EntityRepository";
		this.repository = (JpaRepository<Object, ID>) context.getBean(repositoryBeanName);

		// Mapper Bean 조회: OrderStorageMapper
		String mapperBeanName = uncapitalize(domainName) + "StorageMapper";
		this.mapper = (DomainMapper<D, Object>) context.getBean(mapperBeanName);
	}

	/**
	 * Repository와 Mapper를 직접 주입받는 생성자
	 *
	 * <p>테스트나 특수한 경우에 직접 주입할 때 사용합니다.
	 *
	 * @param domainClass 도메인 클래스
	 * @param repository  JPA Repository
	 * @param mapper      DomainMapper
	 */
	protected AutoJpaAdapter(Class<D> domainClass,
	                         JpaRepository<?, ID> repository,
	                         DomainMapper<D, ?> mapper) {
		this.domainClass = domainClass;
		this.repository = (JpaRepository<Object, ID>) repository;
		this.mapper = (DomainMapper<D, Object>) mapper;
	}

	// ==================== 예외 변환 포함 메서드 ====================

	/**
	 * 도메인 객체를 저장합니다. (예외 변환 포함)
	 *
	 * @param domain 저장할 도메인 객체
	 * @return 저장된 도메인 객체
	 * @throws StorageException 저장 실패 시
	 */
	protected D saveWithException(D domain) {
		try {
			Object entity = mapper.toStorage(domain);
			Object saved = repository.save(entity);
			return mapper.toDomain(saved);
		} catch (DataAccessException e) {
			throw StorageException.saveFailed(domainClass.getSimpleName(), e);
		}
	}

	/**
	 * 여러 도메인 객체를 일괄 저장합니다. (예외 변환 포함)
	 *
	 * @param domains 저장할 도메인 객체 목록
	 * @return 저장된 도메인 객체 목록
	 * @throws StorageException 저장 실패 시
	 */
	protected List<D> saveAllWithException(List<D> domains) {
		try {
			List<Object> entities = domains.stream()
					.map(mapper::toStorage)
					.toList();
			List<Object> savedEntities = repository.saveAll(entities);
			return savedEntities.stream()
					.map(mapper::toDomain)
					.toList();
		} catch (DataAccessException e) {
			throw StorageException.saveFailed(domainClass.getSimpleName(), e);
		}
	}

	/**
	 * ID로 도메인 객체를 조회합니다. (예외 변환 포함)
	 *
	 * @param id 조회할 Entity의 ID
	 * @return 도메인 객체를 담은 Optional (없으면 empty)
	 * @throws StorageException 조회 실패 시
	 */
	protected Optional<D> findByIdWithException(ID id) {
		try {
			return repository.findById(id).map(mapper::toDomain);
		} catch (DataAccessException e) {
			throw StorageException.of("조회 중 오류가 발생했습니다.", e);
		}
	}

	/**
	 * ID로 도메인 객체를 조회합니다. 없으면 예외를 던집니다.
	 *
	 * @param id 조회할 Entity의 ID
	 * @return 도메인 객체
	 * @throws StorageException 조회 실패 또는 Entity가 없는 경우
	 */
	protected D findByIdOrThrow(ID id) {
		return findByIdWithException(id)
				.orElseThrow(() -> StorageException.notFound(domainClass.getSimpleName(), id));
	}

	/**
	 * 모든 도메인 객체를 조회합니다. (예외 변환 포함)
	 *
	 * @return 도메인 객체 목록
	 * @throws StorageException 조회 실패 시
	 */
	protected List<D> findAllWithException() {
		try {
			return repository.findAll().stream()
					.map(mapper::toDomain)
					.toList();
		} catch (DataAccessException e) {
			throw StorageException.of("조회 중 오류가 발생했습니다.", e);
		}
	}

	/**
	 * 도메인 객체를 삭제합니다. (예외 변환 포함)
	 *
	 * @param domain 삭제할 도메인 객체
	 * @throws StorageException 삭제 실패 시
	 */
	protected void deleteWithException(D domain) {
		try {
			Object entity = mapper.toStorage(domain);
			repository.delete(entity);
		} catch (DataAccessException e) {
			throw StorageException.deleteFailed(domainClass.getSimpleName(), e);
		}
	}

	/**
	 * ID로 Entity를 삭제합니다. (예외 변환 포함)
	 *
	 * @param id 삭제할 Entity의 ID
	 * @throws StorageException 삭제 실패 시
	 */
	protected void deleteByIdWithException(ID id) {
		try {
			repository.deleteById(id);
		} catch (DataAccessException e) {
			throw StorageException.deleteFailed(domainClass.getSimpleName(), e);
		}
	}

	// ==================== 예외 변환 없는 기본 메서드 ====================

	/**
	 * 도메인 객체를 저장합니다.
	 *
	 * @param domain 저장할 도메인 객체
	 * @return 저장된 도메인 객체
	 */
	protected D save(D domain) {
		Object entity = mapper.toStorage(domain);
		Object saved = repository.save(entity);
		return mapper.toDomain(saved);
	}

	/**
	 * 여러 도메인 객체를 일괄 저장합니다.
	 *
	 * @param domains 저장할 도메인 객체 목록
	 * @return 저장된 도메인 객체 목록
	 */
	protected List<D> saveAll(List<D> domains) {
		List<Object> entities = domains.stream()
				.map(mapper::toStorage)
				.toList();
		List<Object> savedEntities = repository.saveAll(entities);
		return savedEntities.stream()
				.map(mapper::toDomain)
				.toList();
	}

	/**
	 * ID로 도메인 객체를 조회합니다.
	 *
	 * @param id 조회할 Entity의 ID
	 * @return 도메인 객체를 담은 Optional (없으면 empty)
	 */
	protected Optional<D> findById(ID id) {
		return repository.findById(id).map(mapper::toDomain);
	}

	/**
	 * 모든 도메인 객체를 조회합니다.
	 *
	 * @return 도메인 객체 목록
	 */
	protected List<D> findAll() {
		return repository.findAll().stream()
				.map(mapper::toDomain)
				.toList();
	}

	/**
	 * 도메인 객체를 삭제합니다.
	 *
	 * @param domain 삭제할 도메인 객체
	 */
	protected void delete(D domain) {
		Object entity = mapper.toStorage(domain);
		repository.delete(entity);
	}

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

	// ==================== 유틸리티 ====================

	/**
	 * 첫 글자를 소문자로 변환합니다.
	 *
	 * @param str 변환할 문자열
	 * @return 첫 글자가 소문자인 문자열
	 */
	private String uncapitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}
}
