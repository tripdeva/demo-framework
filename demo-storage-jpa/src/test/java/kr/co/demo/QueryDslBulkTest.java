package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.QOrderEntity;
import kr.co.demo.domain.repository.OrderEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class QueryDslBulkTest {

	@Autowired private JPAQueryFactory qf;
	@Autowired private OrderEntityRepository repo;
	@Autowired private EntityManager em;

	private final QOrderEntity o = QOrderEntity.orderEntity;

	private OrderEntity order(String num, String name, OrderStatus st, BigDecimal amt) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName(name);
		e.setStatus(st);
		e.setTotalAmount(amt);
		e.setOrderedAt(LocalDateTime.now());
		return e;
	}

	@BeforeEach
	void setUp() {
		IntStream.range(0, 20).forEach(i ->
				repo.save(order("QB-" + i, "User" + (i % 5),
						OrderStatus.values()[i % 5],
						new BigDecimal((i + 1) * 1000))));
		em.flush();
		em.clear();
	}

	@Nested
	@DisplayName("벌크 UPDATE (QueryDSL)")
	class BulkUpdate {

		@Test
		@DisplayName("상태 일괄 변경")
		void bulkStatusUpdate() {
			long updated = qf.update(o)
					.set(o.status, OrderStatus.CANCELLED)
					.where(o.status.eq(OrderStatus.PENDING))
					.execute();
			em.flush();
			em.clear();
			assertThat(updated).isGreaterThan(0);
			assertThat(qf.selectFrom(o)
					.where(o.status.eq(OrderStatus.PENDING)).fetch()).isEmpty();
		}

		@Test
		@DisplayName("금액 일괄 증가")
		void bulkAmountIncrease() {
			long updated = qf.update(o)
					.set(o.totalAmount, o.totalAmount.add(new BigDecimal("500")))
					.where(o.status.eq(OrderStatus.CONFIRMED))
					.execute();
			em.clear();
			assertThat(updated).isGreaterThan(0);
		}

		@Test
		@DisplayName("customerName 일괄 변경")
		void bulkNameUpdate() {
			long updated = qf.update(o)
					.set(o.customerName, "Batch Updated")
					.where(o.customerName.eq("User0"))
					.execute();
			em.clear();
			assertThat(updated).isEqualTo(4); // 0,5,10,15
			assertThat(qf.selectFrom(o)
					.where(o.customerName.eq("Batch Updated")).fetch()).hasSize(4);
		}
	}

	@Nested
	@DisplayName("벌크 DELETE (QueryDSL)")
	class BulkDelete {

		@Test
		@DisplayName("상태별 일괄 삭제")
		void bulkDeleteByStatus() {
			long before = qf.selectFrom(o).stream().count();
			long deleted = qf.delete(o)
					.where(o.status.eq(OrderStatus.CANCELLED))
					.execute();
			em.clear();
			assertThat(deleted).isGreaterThan(0);
			long after = qf.selectFrom(o).stream().count();
			assertThat(after).isEqualTo(before - deleted);
		}

		@Test
		@DisplayName("금액 범위 일괄 삭제")
		void bulkDeleteByAmount() {
			long deleted = qf.delete(o)
					.where(o.totalAmount.lt(new BigDecimal("3000")))
					.execute();
			em.clear();
			assertThat(deleted).isGreaterThan(0);
			assertThat(qf.selectFrom(o)
					.where(o.totalAmount.lt(new BigDecimal("3000"))).fetch()).isEmpty();
		}
	}

	@Nested
	@DisplayName("복합 쿼리")
	class ComplexQueries {

		@Test
		@DisplayName("DISTINCT customerName")
		void distinctNames() {
			List<String> names = qf.select(o.customerName)
					.from(o).distinct().fetch();
			assertThat(names).hasSize(5);
		}

		@Test
		@DisplayName("여러 페이지 순회")
		void multiPageIteration() {
			int pageSize = 3;
			int totalPages = 0;
			int totalItems = 0;
			for (int page = 0; ; page++) {
				List<OrderEntity> results = qf.selectFrom(o)
						.orderBy(o.id.asc())
						.offset((long) page * pageSize)
						.limit(pageSize)
						.fetch();
				if (results.isEmpty()) break;
				totalPages++;
				totalItems += results.size();
			}
			assertThat(totalPages).isGreaterThanOrEqualTo(7); // 20/3 = 7
			assertThat(totalItems).isEqualTo(20);
		}

		@Test
		@DisplayName("status별 개수 집계 → 전부 4개")
		void statusDistribution() {
			var result = qf.select(o.status, o.count())
					.from(o)
					.groupBy(o.status)
					.fetch();
			assertThat(result).hasSize(5);
			result.forEach(t -> assertThat(t.get(o.count())).isEqualTo(4L));
		}

		@Test
		@DisplayName("금액 TOP 5")
		void top5ByAmount() {
			List<OrderEntity> top5 = qf.selectFrom(o)
					.orderBy(o.totalAmount.desc())
					.limit(5)
					.fetch();
			assertThat(top5).hasSize(5);
			for (int i = 0; i < 4; i++) {
				assertThat(top5.get(i).getTotalAmount())
						.isGreaterThanOrEqualTo(top5.get(i + 1).getTotalAmount());
			}
		}

		@Test
		@DisplayName("첫 번째 결과만 (fetchFirst)")
		void fetchFirst() {
			OrderEntity first = qf.selectFrom(o)
					.orderBy(o.id.asc())
					.fetchFirst();
			assertThat(first).isNotNull();
			assertThat(first.getOrderNumber()).isEqualTo("QB-0");
		}

		@Test
		@DisplayName("EXISTS 쿼리")
		void existsQuery() {
			boolean exists = qf.selectFrom(o)
					.where(o.status.eq(OrderStatus.PENDING))
					.fetchFirst() != null;
			assertThat(exists).isTrue();

			boolean notExists = qf.selectFrom(o)
					.where(o.orderNumber.eq("NONEXISTENT"))
					.fetchFirst() != null;
			assertThat(notExists).isFalse();
		}
	}
}
