package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import kr.co.demo.client.jpa.adapter.ManualJpaAdapter;
import kr.co.demo.core.mapper.DomainMapper;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.mapper.OrderStorageMapper;
import kr.co.demo.domain.repository.OrderEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class ManualJpaAdapterTest {

	@Autowired
	private OrderEntityRepository repository;

	@Autowired
	private OrderStorageMapper storageMapper;

	private Order createOrder(String num, String customer) {
		Order o = new Order();
		o.setOrderNumber(num);
		o.setCustomerName(customer);
		o.setStatus(OrderStatus.PENDING);
		o.setTotalAmount(new BigDecimal("5000"));
		o.setOrderedAt(LocalDateTime.now());
		return o;
	}

	// DomainMapper 모드 어댑터
	static class MapperModeAdapter
			extends ManualJpaAdapter<Order, OrderEntity, Long, OrderEntityRepository> {
		public MapperModeAdapter(OrderEntityRepository repo, DomainMapper<Order, OrderEntity> mapper) {
			super(repo, mapper);
		}

		public Order doSave(Order o) { return save(o); }
		public List<Order> doSaveAll(List<Order> list) { return saveAll(list); }
		public Optional<Order> doFindById(Long id) { return findById(id); }
		public List<Order> doFindAll() { return findAll(); }
		public void doDelete(Order o) { delete(o); }
		public void doDeleteById(Long id) { deleteById(id); }
		public boolean doExistsById(Long id) { return existsById(id); }
		public long doCount() { return count(); }
	}

	// Function 모드 어댑터
	static class FunctionModeAdapter
			extends ManualJpaAdapter<Order, OrderEntity, Long, OrderEntityRepository> {
		public FunctionModeAdapter(OrderEntityRepository repo) {
			super(repo, null);
		}

		private final Function<Order, OrderEntity> toEntity = d -> {
			OrderEntity e = new OrderEntity();
			e.setId(d.getId());
			e.setOrderNumber(d.getOrderNumber());
			e.setCustomerName(d.getCustomerName());
			e.setStatus(d.getStatus());
			e.setTotalAmount(d.getTotalAmount());
			e.setOrderedAt(d.getOrderedAt());
			return e;
		};

		private final Function<OrderEntity, Order> toDomain = e -> {
			Order d = new Order();
			d.setId(e.getId());
			d.setOrderNumber(e.getOrderNumber());
			d.setCustomerName(e.getCustomerName());
			d.setStatus(e.getStatus());
			d.setTotalAmount(e.getTotalAmount());
			d.setOrderedAt(e.getOrderedAt());
			return d;
		};

		public Order doSave(Order o) { return save(o, toEntity, toDomain); }
		public Optional<Order> doFindById(Long id) { return findById(id, toDomain); }
		public List<Order> doFindAll() { return findAll(toDomain); }
		public void doDelete(Order o) { delete(o, toEntity); }
		public void doMapperSaveExpectFail(Order o) { save(o); }
	}

	@Nested
	@DisplayName("DomainMapper 모드")
	class MapperMode {
		@Test
		@DisplayName("CRUD 전체 동작")
		void fullCrud() {
			MapperModeAdapter adapter = new MapperModeAdapter(repository, storageMapper);

			// save
			Order saved = adapter.doSave(createOrder("M-001", "Kim"));
			assertThat(saved.getId()).isNotNull();

			// findById
			assertThat(adapter.doFindById(saved.getId())).isPresent();

			// findAll
			assertThat(adapter.doFindAll()).isNotEmpty();

			// count
			assertThat(adapter.doCount()).isGreaterThanOrEqualTo(1);

			// existsById
			assertThat(adapter.doExistsById(saved.getId())).isTrue();

			// delete
			adapter.doDelete(saved);
			assertThat(adapter.doFindById(saved.getId())).isEmpty();
		}

		@Test
		@DisplayName("saveAll 벌크 저장")
		void saveAll() {
			MapperModeAdapter adapter = new MapperModeAdapter(repository, storageMapper);
			List<Order> saved = adapter.doSaveAll(List.of(
					createOrder("M-B01", "A"),
					createOrder("M-B02", "B")));
			assertThat(saved).hasSize(2);
		}

		@Test
		@DisplayName("deleteById")
		void deleteById() {
			MapperModeAdapter adapter = new MapperModeAdapter(repository, storageMapper);
			Order saved = adapter.doSave(createOrder("M-D01", "Del"));
			adapter.doDeleteById(saved.getId());
			assertThat(adapter.doExistsById(saved.getId())).isFalse();
		}
	}

	@Nested
	@DisplayName("Function 모드")
	class FunctionMode {
		@Test
		@DisplayName("Function 기반 CRUD")
		void fullCrud() {
			FunctionModeAdapter adapter = new FunctionModeAdapter(repository);

			Order saved = adapter.doSave(createOrder("F-001", "Lee"));
			assertThat(saved.getId()).isNotNull();

			assertThat(adapter.doFindById(saved.getId())).isPresent();
			assertThat(adapter.doFindAll()).isNotEmpty();

			adapter.doDelete(saved);
			assertThat(adapter.doFindById(saved.getId())).isEmpty();
		}

		@Test
		@DisplayName("mapper 없이 mapper 메서드 호출 시 예외")
		void mapperNotConfigured() {
			FunctionModeAdapter adapter = new FunctionModeAdapter(repository);
			assertThatThrownBy(() -> adapter.doMapperSaveExpectFail(createOrder("F-ERR", "X")))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("DomainMapper is not configured");
		}
	}
}
