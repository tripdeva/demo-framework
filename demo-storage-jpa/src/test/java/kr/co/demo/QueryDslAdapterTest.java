package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kr.co.demo.client.jpa.adapter.QueryDslAdapter;
import kr.co.demo.core.mapper.DomainMapper;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.QOrderEntity;
import kr.co.demo.domain.mapper.OrderStorageMapper;
import kr.co.demo.domain.repository.OrderEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class QueryDslAdapterTest {

	@Autowired
	private JPAQueryFactory queryFactory;

	@Autowired
	private OrderEntityRepository repository;

	@Autowired
	private OrderStorageMapper storageMapper;

	private TestQueryAdapter adapter;

	static class TestQueryAdapter extends QueryDslAdapter {
		private final DomainMapper<Order, OrderEntity> mapper;

		public TestQueryAdapter(JPAQueryFactory qf, DomainMapper<Order, OrderEntity> mapper) {
			super(qf);
			this.mapper = mapper;
		}

		public List<Order> findByStatus(OrderStatus status) {
			QOrderEntity o = QOrderEntity.orderEntity;
			List<OrderEntity> entities = queryFactory
					.selectFrom(o)
					.where(o.status.eq(status))
					.fetch();
			return entities.stream().map(mapper::toDomain).toList();
		}

		public List<Order> findByCustomerNameContaining(String keyword) {
			QOrderEntity o = QOrderEntity.orderEntity;
			List<OrderEntity> entities = queryFactory
					.selectFrom(o)
					.where(o.customerName.containsIgnoreCase(keyword))
					.fetch();
			return entities.stream().map(mapper::toDomain).toList();
		}

		public List<Order> findWithPaging(long offset, long limit) {
			QOrderEntity o = QOrderEntity.orderEntity;
			List<OrderEntity> entities = fetchWithPaging(
					queryFactory.selectFrom(o).orderBy(o.id.asc()),
					offset, limit);
			return entities.stream().map(mapper::toDomain).toList();
		}

		public List<Order> findByStatusAndMinAmount(OrderStatus status, BigDecimal minAmount) {
			QOrderEntity o = QOrderEntity.orderEntity;
			List<OrderEntity> entities = queryFactory
					.selectFrom(o)
					.where(
							o.status.eq(status),
							o.totalAmount.goe(minAmount))
					.orderBy(o.totalAmount.desc())
					.fetch();
			return entities.stream().map(mapper::toDomain).toList();
		}
	}

	private Order createAndSave(String num, String customer, OrderStatus status, BigDecimal amount) {
		Order o = new Order();
		o.setOrderNumber(num);
		o.setCustomerName(customer);
		o.setStatus(status);
		o.setTotalAmount(amount);
		o.setOrderedAt(LocalDateTime.now());
		OrderEntity entity = storageMapper.toStorage(o);
		OrderEntity saved = repository.save(entity);
		return storageMapper.toDomain(saved);
	}

	@BeforeEach
	void setUp() {
		adapter = new TestQueryAdapter(queryFactory, storageMapper);

		createAndSave("Q-001", "Kim", OrderStatus.PENDING, new BigDecimal("10000"));
		createAndSave("Q-002", "Lee", OrderStatus.CONFIRMED, new BigDecimal("20000"));
		createAndSave("Q-003", "Park", OrderStatus.PENDING, new BigDecimal("30000"));
		createAndSave("Q-004", "Kim Jr", OrderStatus.SHIPPED, new BigDecimal("5000"));
		createAndSave("Q-005", "Choi", OrderStatus.CONFIRMED, new BigDecimal("50000"));
	}

	@Test
	@DisplayName("status 조건 동적 쿼리")
	void findByStatus() {
		List<Order> pending = adapter.findByStatus(OrderStatus.PENDING);
		assertThat(pending).hasSize(2);
		assertThat(pending).allSatisfy(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING));
	}

	@Test
	@DisplayName("customerName LIKE 검색")
	void findByCustomerName() {
		List<Order> kims = adapter.findByCustomerNameContaining("kim");
		assertThat(kims).hasSize(2); // Kim, Kim Jr
	}

	@Test
	@DisplayName("페이징 적용")
	void findWithPaging() {
		List<Order> page1 = adapter.findWithPaging(0, 2);
		assertThat(page1).hasSize(2);

		List<Order> page2 = adapter.findWithPaging(2, 2);
		assertThat(page2).hasSize(2);

		List<Order> page3 = adapter.findWithPaging(4, 2);
		assertThat(page3).hasSize(1);
	}

	@Test
	@DisplayName("복합 조건 + 정렬")
	void findByStatusAndMinAmount() {
		List<Order> results = adapter.findByStatusAndMinAmount(
				OrderStatus.CONFIRMED, new BigDecimal("15000"));
		assertThat(results).hasSize(2); // Lee(20000), Choi(50000)
		// 금액 내림차순
		assertThat(results.get(0).getTotalAmount())
				.isGreaterThan(results.get(1).getTotalAmount());
	}
}
