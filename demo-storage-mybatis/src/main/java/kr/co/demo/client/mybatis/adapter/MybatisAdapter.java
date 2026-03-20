package kr.co.demo.client.mybatis.adapter;

import kr.co.demo.core.exception.StorageException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis Mapper를 감싸는 Adapter 베이스 클래스
 *
 * <p>OutboundPort 구현체에서 상속받아 사용하며,
 * 공통 CRUD 작업과 예외 변환을 지원합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Adapter
 * public class OrderMybatisAdapter extends MybatisAdapter<Order, Long, OrderMapper>
 *         implements OrderPort {
 *
 *     public OrderMybatisAdapter(OrderMapper mapper) {
 *         super(Order.class, mapper);
 *     }
 *
 *     @Override
 *     public Order save(Order order) {
 *         return insertWithException(order);
 *     }
 *
 *     @Override
 *     public List<Order> findByStatus(OrderStatus status) {
 *         return mapper.findByStatus(status);  // 커스텀 메서드
 *     }
 * }
 * }</pre>
 *
 * @param <D>  도메인 객체 타입
 * @param <ID> ID 타입
 * @param <M>  MyBatis Mapper 타입
 * @author demo-framework
 * @since 1.0.0
 */
public abstract class MybatisAdapter<D, ID, M> {

    /**
     * MyBatis Mapper 인스턴스
     */
    protected final M mapper;

    /**
     * 도메인 클래스 (예외 메시지용)
     */
    protected final Class<D> domainClass;

    /**
     * MybatisAdapter 생성자
     *
     * @param domainClass 도메인 클래스
     * @param mapper      MyBatis Mapper 인스턴스
     */
    protected MybatisAdapter(Class<D> domainClass, M mapper) {
        this.domainClass = domainClass;
        this.mapper = mapper;
    }

    // ==================== 추상 메서드 (BaseMapper 위임) ====================

    /**
     * ID로 도메인 객체를 조회합니다.
     * <p>서브클래스에서 BaseMapper 의 findById를 호출해야 합니다.
     */
    protected abstract D doFindById(ID id);

    /**
     * 전체 도메인 객체를 조회합니다.
     * <p>서브클래스에서 BaseMapper 의 findAll 을 호출해야 합니다.
     */
    protected abstract List<D> doFindAll();

    /**
     * 도메인 객체를 삽입합니다.
     * <p>서브클래스에서 BaseMapper 의 insert 를 호출해야 합니다.
     */
    protected abstract int doInsert(D domain);

    /**
     * 도메인 객체를 수정합니다.
     * <p>서브클래스에서 BaseMapper 의 update 를 호출해야 합니다.
     */
    protected abstract int doUpdate(D domain);

    /**
     * ID로 도메인 객체를 삭제합니다.
     * <p>서브클래스에서 BaseMapper 의 deleteById 를 호출해야 합니다.
     */
    protected abstract int doDeleteById(ID id);

    /**
     * 전체 개수를 조회합니다.
     * <p>서브클래스에서 BaseMapper 의 count 를 호출해야 합니다.
     */
    protected abstract long doCount();

    /**
     * ID로 존재 여부를 확인합니다.
     * <p>서브클래스에서 BaseMapper 의 existsById 를 호출해야 합니다.
     */
    protected abstract boolean doExistsById(ID id);

    // ==================== 예외 변환 포함 메서드 ====================

    /**
     * ID로 도메인 객체를 조회합니다. (예외 변환 포함)
     *
     * @param id 조회할 ID
     * @return 도메인 객체를 담은 Optional
     * @throws StorageException 조회 실패 시
     */
    protected Optional<D> findByIdWithException(ID id) {
        try {
            return Optional.ofNullable(doFindById(id));
        } catch (DataAccessException e) {
            throw StorageException.of("조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * ID로 도메인 객체를 조회합니다. 없으면 예외를 던집니다.
     *
     * @param id 조회할 ID
     * @return 도메인 객체
     * @throws StorageException Entity 가 없는 경우
     */
    protected D findByIdOrThrow(ID id) {
        return findByIdWithException(id)
                .orElseThrow(() -> StorageException.notFound(domainClass.getSimpleName(), id));
    }

    /**
     * 전체 도메인 객체를 조회합니다. (예외 변환 포함)
     *
     * @return 도메인 객체 목록
     * @throws StorageException 조회 실패 시
     */
    protected List<D> findAllWithException() {
        try {
            return doFindAll();
        } catch (DataAccessException e) {
            throw StorageException.of("조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 도메인 객체를 삽입합니다. (예외 변환 포함)
     *
     * @param domain 삽입할 도메인 객체
     * @return 삽입된 도메인 객체
     * @throws StorageException 삽입 실패 시
     */
    protected D insertWithException(D domain) {
        try {
            doInsert(domain);
            return domain;
        } catch (DataAccessException e) {
            throw StorageException.saveFailed(domainClass.getSimpleName(), e);
        }
    }

    /**
     * 도메인 객체를 수정합니다. (예외 변환 포함)
     *
     * @param domain 수정할 도메인 객체
     * @return 수정된 도메인 객체
     * @throws StorageException 수정 실패 시
     */
    protected D updateWithException(D domain) {
        try {
            doUpdate(domain);
            return domain;
        } catch (DataAccessException e) {
            throw StorageException.saveFailed(domainClass.getSimpleName(), e);
        }
    }

    /**
     * ID로 도메인 객체를 삭제합니다. (예외 변환 포함)
     *
     * @param id 삭제할 ID
     * @throws StorageException 삭제 실패 시
     */
    protected void deleteByIdWithException(ID id) {
        try {
            doDeleteById(id);
        } catch (DataAccessException e) {
            throw StorageException.deleteFailed(domainClass.getSimpleName(), e);
        }
    }

    // ==================== 기본 메서드 (예외 변환 없음) ====================

    /**
     * ID로 도메인 객체를 조회합니다.
     *
     * @param id 조회할 ID
     * @return 도메인 객체를 담은 Optional
     */
    protected Optional<D> findById(ID id) {
        return Optional.ofNullable(doFindById(id));
    }

    /**
     * 전체 도메인 객체를 조회합니다.
     *
     * @return 도메인 객체 목록
     */
    protected List<D> findAll() {
        return doFindAll();
    }

    /**
     * 도메인 객체를 삽입합니다.
     *
     * @param domain 삽입할 도메인 객체
     * @return 삽입된 도메인 객체
     */
    protected D insert(D domain) {
        doInsert(domain);
        return domain;
    }

    /**
     * 도메인 객체를 수정합니다.
     *
     * @param domain 수정할 도메인 객체
     * @return 수정된 도메인 객체
     */
    protected D update(D domain) {
        doUpdate(domain);
        return domain;
    }

    /**
     * ID로 도메인 객체를 삭제합니다.
     *
     * @param id 삭제할 ID
     */
    protected void deleteById(ID id) {
        doDeleteById(id);
    }

    /**
     * 전체 개수를 조회합니다.
     *
     * @return 개수
     */
    protected long count() {
        return doCount();
    }

    /**
     * ID로 존재 여부를 확인합니다.
     *
     * @param id 확인할 ID
     * @return 존재하면 true
     */
    protected boolean existsById(ID id) {
        return doExistsById(id);
    }
}
