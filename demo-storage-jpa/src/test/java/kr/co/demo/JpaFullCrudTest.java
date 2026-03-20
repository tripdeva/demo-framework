package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.demo.client.jpa.adapter.AutoJpaAdapter;
import kr.co.demo.core.exception.StorageException;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class JpaFullCrudTest {

	@Autowired
	private ApplicationContext context;

	private OrderTestAdapter adapter;

	static class OrderTestAdapter extends AutoJpaAdapter<Order, Long> {
		public OrderTestAdapter(ApplicationContext ctx) {
			super(Order.class, ctx);
		}

		public Order doSave(Order o) { return saveWithException(o); }
		public List<Order> doSaveAll(List<Order> list) { return saveAllWithException(list); }
		public Optional<Order> doFindById(Long id) { return findByIdWithException(id); }
		public Order doFindByIdOrThrow(Long id) { return findByIdOrThrow(id); }
		public List<Order> doFindAll() { return findAllWithException(); }
		public void doDelete(Order o) { deleteWithException(o); }
		public void doDeleteById(Long id) { deleteByIdWithException(id); }
		public long doCount() { return count(); }
		public boolean doExistsById(Long id) { return existsById(id); }

		// 비예외 메서드
		public Order doSaveNoEx(Order o) { return save(o); }
		public Optional<Order> doFindByIdNoEx(Long id) { return findById(id); }
		public List<Order> doFindAllNoEx() { return findAll(); }
		public void doDeleteNoEx(Order o) { delete(o); }
	}

	@BeforeEach
	void setUp() {
		adapter = new OrderTestAdapter(context);
	}

	private Order createOrder(String orderNumber, String customer) {
		Order order = new Order();
		order.setOrderNumber(orderNumber);
		order.setCustomerName(customer);
		order.setStatus(OrderStatus.PENDING);
		order.setTotalAmount(new BigDecimal("10000"));
		order.setOrderedAt(LocalDateTime.now());
		return order;
	}

	@Nested
	@DisplayName("save 테스트")
	class SaveTests {
		@Test
		@DisplayName("단건 저장")
		void saveSingle() {
			Order saved = adapter.doSave(createOrder("ORD-001", "Kim"));
			assertThat(saved.getId()).isNotNull();
			assertThat(saved.getOrderNumber()).isEqualTo("ORD-001");
		}

		@Test
		@DisplayName("벌크 저장")
		void saveAll() {
			List<Order> saved = adapter.doSaveAll(List.of(
					createOrder("ORD-A01", "Kim"),
					createOrder("ORD-A02", "Lee"),
					createOrder("ORD-A03", "Park")));
			assertThat(saved).hasSize(3);
			assertThat(saved).allSatisfy(o -> assertThat(o.getId()).isNotNull());
		}
	}

	@Nested
	@DisplayName("findById 테스트")
	class FindByIdTests {
		@Test
		@DisplayName("존재하는 ID 조회")
		void findExisting() {
			Order saved = adapter.doSave(createOrder("ORD-F01", "Kim"));
			Optional<Order> found = adapter.doFindById(saved.getId());
			assertThat(found).isPresent();
			assertThat(found.get().getCustomerName()).isEqualTo("Kim");
		}

		@Test
		@DisplayName("존재하지 않는 ID 조회")
		void findNonExisting() {
			assertThat(adapter.doFindById(999999L)).isEmpty();
		}

		@Test
		@DisplayName("findByIdOrThrow - 존재")
		void findOrThrowExists() {
			Order saved = adapter.doSave(createOrder("ORD-F02", "Lee"));
			Order found = adapter.doFindByIdOrThrow(saved.getId());
			assertThat(found.getOrderNumber()).isEqualTo("ORD-F02");
		}

		@Test
		@DisplayName("findByIdOrThrow - 미존재시 예외")
		void findOrThrowNotExists() {
			assertThatThrownBy(() -> adapter.doFindByIdOrThrow(999999L))
					.isInstanceOf(StorageException.class);
		}
	}

	@Nested
	@DisplayName("findAll 테스트")
	class FindAllTests {
		@Test
		@DisplayName("전체 조회")
		void findAll() {
			adapter.doSave(createOrder("ORD-FA1", "A"));
			adapter.doSave(createOrder("ORD-FA2", "B"));
			assertThat(adapter.doFindAll().size()).isGreaterThanOrEqualTo(2);
		}
	}

	@Nested
	@DisplayName("delete 테스트")
	class DeleteTests {
		@Test
		@DisplayName("단건 삭제")
		void deleteEntity() {
			Order saved = adapter.doSave(createOrder("ORD-D01", "Del"));
			adapter.doDelete(saved);
			assertThat(adapter.doFindById(saved.getId())).isEmpty();
		}

		@Test
		@DisplayName("ID로 삭제")
		void deleteById() {
			Order saved = adapter.doSave(createOrder("ORD-D02", "Del2"));
			adapter.doDeleteById(saved.getId());
			assertThat(adapter.doFindById(saved.getId())).isEmpty();
		}
	}

	@Nested
	@DisplayName("count, existsById 테스트")
	class CountExistsTests {
		@Test
		@DisplayName("count")
		void count() {
			adapter.doSave(createOrder("ORD-C01", "Count"));
			assertThat(adapter.doCount()).isGreaterThanOrEqualTo(1);
		}

		@Test
		@DisplayName("existsById - 존재")
		void existsTrue() {
			Order saved = adapter.doSave(createOrder("ORD-E01", "Exists"));
			assertThat(adapter.doExistsById(saved.getId())).isTrue();
		}

		@Test
		@DisplayName("existsById - 미존재")
		void existsFalse() {
			assertThat(adapter.doExistsById(999999L)).isFalse();
		}
	}

	@Nested
	@DisplayName("비예외 메서드 테스트")
	class NoExceptionTests {
		@Test
		@DisplayName("save/findById/findAll/delete 비예외 버전")
		void nonExceptionMethods() {
			Order saved = adapter.doSaveNoEx(createOrder("ORD-NE1", "NoEx"));
			assertThat(saved.getId()).isNotNull();

			assertThat(adapter.doFindByIdNoEx(saved.getId())).isPresent();
			assertThat(adapter.doFindAllNoEx()).isNotEmpty();

			adapter.doDeleteNoEx(saved);
			assertThat(adapter.doFindByIdNoEx(saved.getId())).isEmpty();
		}
	}
}
