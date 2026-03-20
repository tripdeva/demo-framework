package kr.co.demo.client.jpa.adapter;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;

/**
 * QueryDSL을 사용하는 Adapter 베이스 클래스
 * <p>
 * 복잡한 동적 쿼리가 필요한 OutboundPort 구현체에서 상속받아 사용합니다.
 * JPAQueryFactory를 통해 타입 안전한 쿼리를 작성할 수 있습니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Adapter
 * public class OrderQueryAdapter extends QueryDslAdapter implements OrderQueryPort {
 *
 *     public OrderQueryAdapter(JPAQueryFactory queryFactory) {
 *         super(queryFactory);
 *     }
 *
 *     @Override
 *     public List<Order> searchByCondition(OrderSearchCondition cond) {
 *         return queryFactory
 *                 .selectFrom(order)
 *                 .where(statusEq(cond.getStatus()))
 *                 .fetch()
 *                 .stream()
 *                 .map(OrderEntity::toDomain)
 *                 .toList();
 *     }
 *
 *     private BooleanExpression statusEq(OrderStatus status) {
 *         return status != null ? order.status.eq(status) : null;
 *     }
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 */
public abstract class QueryDslAdapter {

	/**
	 * QueryDSL 쿼리 생성을 위한 Factory
	 */
	protected final JPAQueryFactory queryFactory;

	/**
	 * QueryDslAdapter 생성자
	 *
	 * @param queryFactory JPAQueryFactory 인스턴스
	 */
	protected QueryDslAdapter(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	/**
	 * 쿼리에 페이징을 적용하여 결과를 조회합니다.
	 *
	 * @param <T>    조회 결과 타입
	 * @param query  JPAQuery 인스턴스
	 * @param offset 시작 위치 (0부터 시작)
	 * @param limit  조회할 최대 개수
	 * @return 페이징이 적용된 결과 목록
	 */
	protected <T> List<T> fetchWithPaging(JPAQuery<T> query, long offset, long limit) {
		return query
				.offset(offset)
				.limit(limit)
				.fetch();
	}
}