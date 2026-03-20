package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.demo.client.mybatis.adapter.MybatisAdapter;
import kr.co.demo.core.exception.StorageException;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.mapper.OrderBaseMapper;
import kr.co.demo.domain.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class MybatisAdapterTest {

	@Autowired
	private OrderMapper baseMapper;

	private TestOrderAdapter adapter;

	static class TestOrderAdapter extends MybatisAdapter<Order, Long, OrderBaseMapper> {
		public TestOrderAdapter(OrderBaseMapper mapper) {
			super(Order.class, mapper);
		}

		@Override protected Order doFindById(Long id) { return mapper.findById(id); }
		@Override protected List<Order> doFindAll() { return mapper.findAll(); }
		@Override protected int doInsert(Order d) { return mapper.insert(d); }
		@Override protected int doUpdate(Order d) { return mapper.update(d); }
		@Override protected int doDeleteById(Long id) { return mapper.deleteById(id); }
		@Override protected long doCount() { return mapper.count(); }
		@Override protected boolean doExistsById(Long id) { return mapper.existsById(id); }

		// public 위임 메서드
		public Optional<Order> pubFindById(Long id) { return findById(id); }
		public Optional<Order> pubFindByIdWithEx(Long id) { return findByIdWithException(id); }
		public Order pubFindByIdOrThrow(Long id) { return findByIdOrThrow(id); }
		public List<Order> pubFindAll() { return findAll(); }
		public List<Order> pubFindAllWithEx() { return findAllWithException(); }
		public void pubInsert(Order o) { insert(o); }
		public void pubInsertWithEx(Order o) { insertWithException(o); }
		public void pubUpdate(Order o) { update(o); }
		public void pubUpdateWithEx(Order o) { updateWithException(o); }
		public void pubDeleteById(Long id) { deleteById(id); }
		public void pubDeleteByIdWithEx(Long id) { deleteByIdWithException(id); }
		public long pubCount() { return count(); }
		public boolean pubExistsById(Long id) { return existsById(id); }
	}

	private Order createOrder(String num, String customer) {
		Order o = new Order();
		o.setOrderNumber(num);
		o.setCustomerName(customer);
		o.setStatus(OrderStatus.PENDING);
		o.setTotalAmount(new BigDecimal("10000"));
		o.setOrderedAt(LocalDateTime.now());
		return o;
	}

	@BeforeEach
	void setUp() {
		adapter = new TestOrderAdapter(baseMapper);
	}

	@Test
	@DisplayName("insert + findById")
	void insertAndFind() {
		Order order = createOrder("MA-001", "Kim");
		adapter.pubInsert(order);
		assertThat(order.getId()).isNotNull();

		Optional<Order> found = adapter.pubFindById(order.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getOrderNumber()).isEqualTo("MA-001");
	}

	@Test
	@DisplayName("findByIdWithException")
	void findByIdWithException() {
		Order order = createOrder("MA-002", "Lee");
		adapter.pubInsert(order);

		Optional<Order> found = adapter.pubFindByIdWithEx(order.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getCustomerName()).isEqualTo("Lee");
	}

	@Test
	@DisplayName("findByIdOrThrow - 없으면 StorageException")
	void findByIdOrThrowNotFound() {
		assertThatThrownBy(() -> adapter.pubFindByIdOrThrow(999999L))
				.isInstanceOf(StorageException.class);
	}

	@Test
	@DisplayName("findAllWithException")
	void findAllWithException() {
		adapter.pubInsert(createOrder("MA-FA1", "A"));
		adapter.pubInsert(createOrder("MA-FA2", "B"));

		List<Order> all = adapter.pubFindAllWithEx();
		assertThat(all.size()).isGreaterThanOrEqualTo(2);
	}

	@Test
	@DisplayName("update + updateWithException")
	void update() {
		Order order = createOrder("MA-U01", "Original");
		adapter.pubInsert(order);
		order.setCustomerName("Updated");
		adapter.pubUpdate(order);
		assertThat(adapter.pubFindById(order.getId()).get().getCustomerName()).isEqualTo("Updated");

		order.setCustomerName("UpdatedEx");
		adapter.pubUpdateWithEx(order);
		assertThat(adapter.pubFindById(order.getId()).get().getCustomerName()).isEqualTo("UpdatedEx");
	}

	@Test
	@DisplayName("deleteById + deleteByIdWithException")
	void deleteById() {
		Order o1 = createOrder("MA-D01", "Del1");
		adapter.pubInsert(o1);
		adapter.pubDeleteById(o1.getId());
		assertThat(adapter.pubExistsById(o1.getId())).isFalse();

		Order o2 = createOrder("MA-D02", "Del2");
		adapter.pubInsert(o2);
		adapter.pubDeleteByIdWithEx(o2.getId());
		assertThat(adapter.pubExistsById(o2.getId())).isFalse();
	}

	@Test
	@DisplayName("count + existsById")
	void countAndExists() {
		Order order = createOrder("MA-CE1", "CE");
		adapter.pubInsert(order);
		assertThat(adapter.pubCount()).isGreaterThanOrEqualTo(1);
		assertThat(adapter.pubExistsById(order.getId())).isTrue();
		assertThat(adapter.pubExistsById(999999L)).isFalse();
	}

	@Test
	@DisplayName("insertWithException")
	void insertWithException() {
		Order order = createOrder("MA-IE1", "InsEx");
		adapter.pubInsertWithEx(order);
		assertThat(order.getId()).isNotNull();
	}

	@Test
	@DisplayName("findAll (비예외)")
	void findAllNoException() {
		adapter.pubInsert(createOrder("MA-FN1", "NoEx"));
		assertThat(adapter.pubFindAll()).isNotEmpty();
	}
}
