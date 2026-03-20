package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.OrderItemEntity;
import kr.co.demo.domain.entity.QOrderEntity;
import kr.co.demo.domain.entity.QOrderItemEntity;
import kr.co.demo.domain.repository.OrderEntityRepository;
import kr.co.demo.domain.repository.OrderItemEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class QueryDslAdvancedTest {

	@Autowired private JPAQueryFactory qf;
	@Autowired private OrderEntityRepository orderRepo;
	@Autowired private OrderItemEntityRepository itemRepo;

	private final QOrderEntity o = QOrderEntity.orderEntity;
	private final QOrderItemEntity oi = QOrderItemEntity.orderItemEntity;

	private OrderEntity saveOrder(String num, String name, OrderStatus st, BigDecimal amt) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName(name);
		e.setStatus(st);
		e.setTotalAmount(amt);
		e.setOrderedAt(LocalDateTime.now());
		return orderRepo.saveAndFlush(e);
	}

	private void saveItem(OrderEntity order, String prod, int qty, BigDecimal price) {
		OrderItemEntity item = new OrderItemEntity();
		item.setProductName(prod);
		item.setQuantity(qty);
		item.setUnitPrice(price);
		item.setOrder(order);
		itemRepo.saveAndFlush(item);
	}

	@BeforeEach
	void setUp() {
		OrderEntity o1 = saveOrder("QA-1", "Kim", OrderStatus.PENDING, new BigDecimal("10000"));
		OrderEntity o2 = saveOrder("QA-2", "Lee", OrderStatus.CONFIRMED, new BigDecimal("20000"));
		OrderEntity o3 = saveOrder("QA-3", "Park", OrderStatus.SHIPPED, new BigDecimal("30000"));
		OrderEntity o4 = saveOrder("QA-4", "Kim", OrderStatus.DELIVERED, new BigDecimal("5000"));
		OrderEntity o5 = saveOrder("QA-5", "Choi", OrderStatus.CANCELLED, new BigDecimal("50000"));

		saveItem(o1, "Apple", 3, new BigDecimal("1000"));
		saveItem(o1, "Banana", 5, new BigDecimal("500"));
		saveItem(o2, "Cherry", 2, new BigDecimal("3000"));
		saveItem(o3, "Date", 10, new BigDecimal("200"));
		saveItem(o3, "Elderberry", 1, new BigDecimal("8000"));
		saveItem(o3, "Fig", 4, new BigDecimal("1500"));
	}

	@Nested
	@DisplayName("조건 조합 테스트")
	class ConditionCombinations {

		@Test
		@DisplayName("NULL 안전 동적 조건 (BooleanExpression)")
		void nullSafeCondition() {
			BooleanExpression statusCond = null;
			BooleanExpression nameCond = o.customerName.eq("Kim");
			List<OrderEntity> result = qf.selectFrom(o)
					.where(statusCond, nameCond)
					.fetch();
			assertThat(result).hasSize(2);
		}

		@Test
		@DisplayName("OR 조건")
		void orCondition() {
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.status.eq(OrderStatus.PENDING)
							.or(o.status.eq(OrderStatus.CONFIRMED)))
					.fetch();
			assertThat(result).hasSize(2);
		}

		@Test
		@DisplayName("NOT 조건")
		void notCondition() {
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.status.ne(OrderStatus.CANCELLED))
					.fetch();
			assertThat(result).hasSize(4);
		}

		@Test
		@DisplayName("IN 조건")
		void inCondition() {
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.status.in(OrderStatus.PENDING, OrderStatus.SHIPPED))
					.fetch();
			assertThat(result).hasSize(2);
		}

		@Test
		@DisplayName("BETWEEN 조건")
		void betweenCondition() {
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.totalAmount.between(
							new BigDecimal("10000"), new BigDecimal("30000")))
					.fetch();
			assertThat(result).hasSize(3); // 10000, 20000, 30000
		}

		@Test
		@DisplayName("LIKE 시작/끝/포함")
		void likeVariants() {
			assertThat(qf.selectFrom(o)
					.where(o.customerName.startsWith("K")).fetch()).hasSize(2);
			assertThat(qf.selectFrom(o)
					.where(o.customerName.endsWith("i")).fetch()).hasSize(1); // Choi
			assertThat(qf.selectFrom(o)
					.where(o.customerName.contains("ar")).fetch()).hasSize(1); // Park
		}

		@Test
		@DisplayName("IS NULL / IS NOT NULL")
		void nullChecks() {
			saveOrder("QA-NULL", "NullTest", null, BigDecimal.ONE);
			assertThat(qf.selectFrom(o).where(o.status.isNull()).fetch())
					.hasSizeGreaterThanOrEqualTo(1);
			assertThat(qf.selectFrom(o).where(o.status.isNotNull()).fetch())
					.hasSizeGreaterThanOrEqualTo(5);
		}
	}

	@Nested
	@DisplayName("정렬 테스트")
	class SortTests {

		@Test
		@DisplayName("금액 내림차순")
		void orderByAmountDesc() {
			List<OrderEntity> result = qf.selectFrom(o)
					.orderBy(o.totalAmount.desc())
					.fetch();
			for (int i = 0; i < result.size() - 1; i++) {
				assertThat(result.get(i).getTotalAmount())
						.isGreaterThanOrEqualTo(result.get(i + 1).getTotalAmount());
			}
		}

		@Test
		@DisplayName("복합 정렬 (status ASC, amount DESC)")
		void multiSort() {
			List<OrderEntity> result = qf.selectFrom(o)
					.orderBy(o.status.asc(), o.totalAmount.desc())
					.fetch();
			assertThat(result).isNotEmpty();
		}
	}

	@Nested
	@DisplayName("집계 테스트")
	class AggregationTests {

		@Test
		@DisplayName("COUNT")
		void count() {
			Long count = qf.select(o.count()).from(o).fetchOne();
			assertThat(count).isGreaterThanOrEqualTo(5);
		}

		@Test
		@DisplayName("GROUP BY status + COUNT")
		void groupByStatus() {
			List<Tuple> result = qf.select(o.status, o.count())
					.from(o)
					.where(o.status.isNotNull())
					.groupBy(o.status)
					.fetch();
			assertThat(result).hasSizeGreaterThanOrEqualTo(4);
		}

		@Test
		@DisplayName("GROUP BY + HAVING (COUNT > 0)")
		void groupByHaving() {
			List<Tuple> result = qf.select(o.status, o.count())
					.from(o)
					.where(o.status.isNotNull())
					.groupBy(o.status)
					.having(o.count().gt(0))
					.fetch();
			assertThat(result).isNotEmpty();
		}

		@Test
		@DisplayName("MAX 금액 조회")
		void maxAmount() {
			OrderEntity maxOrder = qf.selectFrom(o)
					.orderBy(o.totalAmount.desc())
					.limit(1)
					.fetchOne();
			assertThat(maxOrder).isNotNull();
			assertThat(maxOrder.getTotalAmount())
					.isGreaterThanOrEqualTo(new BigDecimal("50000"));
		}

		@Test
		@DisplayName("MIN 금액 조회")
		void minAmount() {
			OrderEntity minOrder = qf.selectFrom(o)
					.orderBy(o.totalAmount.asc())
					.limit(1)
					.fetchOne();
			assertThat(minOrder).isNotNull();
			assertThat(minOrder.getTotalAmount())
					.isLessThanOrEqualTo(new BigDecimal("5000"));
		}
	}

	@Nested
	@DisplayName("JOIN 테스트")
	class JoinTests {

		@Test
		@DisplayName("INNER JOIN - Order + OrderItem")
		void innerJoin() {
			List<OrderEntity> result = qf.selectFrom(o)
					.innerJoin(o.items, oi)
					.distinct()
					.fetch();
			// o1(2 items), o2(1 item), o3(3 items) = 3 orders
			assertThat(result).hasSize(3);
		}

		@Test
		@DisplayName("LEFT JOIN - 아이템 없는 주문도 포함")
		void leftJoin() {
			List<OrderEntity> result = qf.selectFrom(o)
					.leftJoin(o.items, oi)
					.distinct()
					.fetch();
			assertThat(result).hasSize(5); // 전체 5개 주문
		}

		@Test
		@DisplayName("JOIN + WHERE 조건")
		void joinWithWhere() {
			List<OrderEntity> result = qf.selectFrom(o)
					.innerJoin(o.items, oi)
					.where(oi.quantity.gt(3))
					.distinct()
					.fetch();
			// Apple(3 X), Banana(5 ✓), Cherry(2 X), Date(10 ✓), Fig(4 ✓)
			assertThat(result).hasSizeGreaterThanOrEqualTo(2);
		}

		@Test
		@DisplayName("JOIN + GROUP BY (주문별 아이템 개수)")
		void joinGroupBy() {
			List<Tuple> result = qf.select(o.orderNumber, oi.count())
					.from(o)
					.innerJoin(o.items, oi)
					.groupBy(o.id, o.orderNumber)
					.fetch();
			assertThat(result).hasSize(3);
		}
	}

	@Nested
	@DisplayName("조건부 조회 테스트")
	class ConditionalTests {

		@Test
		@DisplayName("최고 금액 주문 조회")
		void topOrder() {
			OrderEntity top = qf.selectFrom(o)
					.orderBy(o.totalAmount.desc())
					.limit(1)
					.fetchOne();
			assertThat(top).isNotNull();
			// 다른 모든 주문 금액보다 크거나 같아야 함
			List<OrderEntity> all = qf.selectFrom(o).fetch();
			assertThat(all).allSatisfy(order ->
					assertThat(top.getTotalAmount())
							.isGreaterThanOrEqualTo(order.getTotalAmount()));
		}

		@Test
		@DisplayName("특정 금액 이상 주문만 조회")
		void minAmountFilter() {
			BigDecimal threshold = new BigDecimal("20000");
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.totalAmount.goe(threshold))
					.fetch();
			assertThat(result).allSatisfy(order ->
					assertThat(order.getTotalAmount()).isGreaterThanOrEqualTo(threshold));
		}
	}

	@Nested
	@DisplayName("페이징 테스트")
	class PagingTests {

		@Test
		@DisplayName("offset/limit 페이징")
		void paging() {
			List<OrderEntity> page1 = qf.selectFrom(o)
					.orderBy(o.id.asc())
					.offset(0).limit(2).fetch();
			List<OrderEntity> page2 = qf.selectFrom(o)
					.orderBy(o.id.asc())
					.offset(2).limit(2).fetch();
			assertThat(page1).hasSize(2);
			assertThat(page2).hasSize(2);
			// 페이지 간 겹침 없음
			assertThat(page1.get(0).getId()).isNotEqualTo(page2.get(0).getId());
		}

		@Test
		@DisplayName("마지막 페이지 (남은 것보다 limit이 큰 경우)")
		void lastPage() {
			List<OrderEntity> lastPage = qf.selectFrom(o)
					.orderBy(o.id.asc())
					.offset(4).limit(10).fetch();
			assertThat(lastPage).hasSize(1); // 5개 중 4 skip → 1개
		}

		@Test
		@DisplayName("빈 페이지")
		void emptyPage() {
			List<OrderEntity> empty = qf.selectFrom(o)
					.orderBy(o.id.asc())
					.offset(100).limit(10).fetch();
			assertThat(empty).isEmpty();
		}

		@Test
		@DisplayName("fetchCount (total count)")
		void fetchCount() {
			long total = qf.selectFrom(o).stream().count();
			assertThat(total).isGreaterThanOrEqualTo(5);
		}
	}

	@Nested
	@DisplayName("프로젝션 테스트")
	class ProjectionTests {

		@Test
		@DisplayName("단일 컬럼 프로젝션")
		void singleColumn() {
			List<String> names = qf.select(o.customerName)
					.from(o)
					.distinct()
					.fetch();
			assertThat(names).contains("Kim", "Lee", "Park", "Choi");
		}

		@Test
		@DisplayName("Tuple 프로젝션")
		void tupleProjection() {
			List<Tuple> result = qf.select(o.orderNumber, o.customerName, o.totalAmount)
					.from(o)
					.fetch();
			assertThat(result).hasSize(5);
			for (Tuple t : result) {
				assertThat(t.get(o.orderNumber)).isNotNull();
				assertThat(t.get(o.customerName)).isNotNull();
				assertThat(t.get(o.totalAmount)).isNotNull();
			}
		}
	}
}
