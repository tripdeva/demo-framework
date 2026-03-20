package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import kr.co.demo.client.jpa.adapter.AutoJpaAdapter;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.OrderItemEntity;
import kr.co.demo.domain.mapper.OrderStorageMapper;
import kr.co.demo.domain.repository.OrderEntityRepository;
import kr.co.demo.domain.repository.OrderItemEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class EdgeCaseTest {

	@Autowired private OrderEntityRepository orderRepo;
	@Autowired private OrderItemEntityRepository itemRepo;
	@Autowired private OrderStorageMapper mapper;
	@Autowired private ApplicationContext context;
	@Autowired private JdbcTemplate jdbc;

	private OrderEntity entity(String num, String name, OrderStatus st, BigDecimal amt) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName(name);
		e.setStatus(st);
		e.setTotalAmount(amt);
		e.setOrderedAt(LocalDateTime.now());
		return e;
	}

	// ==================== 경계값 ====================

	@Nested
	@DisplayName("경계값 테스트")
	class BoundaryTests {

		@Test
		@DisplayName("금액 0원 저장/조회")
		void zeroAmount() {
			OrderEntity saved = orderRepo.save(
					entity("ZERO-001", "A", OrderStatus.PENDING, BigDecimal.ZERO));
			orderRepo.flush();
			assertThat(orderRepo.findById(saved.getId()).get().getTotalAmount())
					.isEqualByComparingTo(BigDecimal.ZERO);
		}

		@Test
		@DisplayName("금액 소수점 정밀도 (19,2)")
		void decimalPrecision() {
			BigDecimal precise = new BigDecimal("99999999999999999.99");
			OrderEntity saved = orderRepo.save(
					entity("DEC-001", "A", OrderStatus.PENDING, precise));
			orderRepo.flush();
			assertThat(orderRepo.findById(saved.getId()).get().getTotalAmount())
					.isEqualByComparingTo(precise);
		}

		@Test
		@DisplayName("빈 문자열 customerName (nullable=false이지만 empty는 허용)")
		void emptyString() {
			OrderEntity saved = orderRepo.save(
					entity("EMPTY-001", "", OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();
			assertThat(orderRepo.findById(saved.getId()).get().getCustomerName()).isEmpty();
		}

		@Test
		@DisplayName("긴 orderNumber (VARCHAR 한계)")
		void longOrderNumber() {
			String longNum = "A".repeat(255);
			OrderEntity saved = orderRepo.save(
					entity(longNum, "A", OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();
			assertThat(orderRepo.findById(saved.getId()).get().getOrderNumber())
					.isEqualTo(longNum);
		}

		@Test
		@DisplayName("특수문자 포함 문자열")
		void specialChars() {
			String special = "O'Brien & Co. <\"test\"> 한글 日本語 🎉";
			OrderEntity saved = orderRepo.save(
					entity("SPEC-001", special, OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();
			assertThat(orderRepo.findById(saved.getId()).get().getCustomerName())
					.isEqualTo(special);
		}

		@Test
		@DisplayName("orderedAt null 허용")
		void nullOrderedAt() {
			OrderEntity e = entity("NULL-DT", "A", OrderStatus.PENDING, BigDecimal.ONE);
			e.setOrderedAt(null);
			OrderEntity saved = orderRepo.save(e);
			orderRepo.flush();
			assertThat(orderRepo.findById(saved.getId()).get().getOrderedAt()).isNull();
		}

		@Test
		@DisplayName("status null 허용")
		void nullStatus() {
			OrderEntity e = entity("NULL-ST", "A", null, BigDecimal.ONE);
			OrderEntity saved = orderRepo.save(e);
			orderRepo.flush();
			assertThat(orderRepo.findById(saved.getId()).get().getStatus()).isNull();
		}

		@ParameterizedTest
		@EnumSource(OrderStatus.class)
		@DisplayName("모든 OrderStatus 값 저장/조회")
		void allStatuses(OrderStatus status) {
			OrderEntity saved = orderRepo.save(
					entity("ST-" + status.name(), "A", status, BigDecimal.ONE));
			orderRepo.flush();
			OrderEntity found = orderRepo.findById(saved.getId()).get();
			assertThat(found.getStatus()).isEqualTo(status);
			String dbVal = jdbc.queryForObject(
					"SELECT STATUS FROM ORDERS WHERE ID = ?", String.class, saved.getId());
			assertThat(dbVal).isEqualTo(status.name());
		}
	}

	// ==================== 대량 데이터 ====================

	@Nested
	@DisplayName("대량 데이터 테스트")
	class BulkTests {

		@Test
		@DisplayName("100건 벌크 저장")
		void bulk100() {
			List<OrderEntity> entities = IntStream.range(0, 100)
					.mapToObj(i -> entity("BULK-" + i, "User" + i,
							OrderStatus.PENDING, new BigDecimal(i * 100)))
					.toList();
			orderRepo.saveAll(entities);
			orderRepo.flush();
			assertThat(orderRepo.count()).isGreaterThanOrEqualTo(100);
		}

		@Test
		@DisplayName("대량 findAll 후 스트림 처리")
		void bulkFindAll() {
			List<OrderEntity> entities = IntStream.range(0, 50)
					.mapToObj(i -> entity("BFA-" + i, "U" + i,
							OrderStatus.values()[i % 5], new BigDecimal(i * 1000)))
					.toList();
			orderRepo.saveAll(entities);
			orderRepo.flush();

			long confirmedCount = orderRepo.findAll().stream()
					.filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
					.count();
			assertThat(confirmedCount).isEqualTo(10); // 50/5 = 10
		}

		@Test
		@DisplayName("대량 삭제")
		void bulkDelete() {
			List<OrderEntity> entities = IntStream.range(0, 30)
					.mapToObj(i -> entity("BDEL-" + i, "U" + i,
							OrderStatus.PENDING, BigDecimal.ONE))
					.toList();
			List<OrderEntity> saved = orderRepo.saveAll(entities);
			orderRepo.flush();
			long before = orderRepo.count();

			orderRepo.deleteAll(saved);
			orderRepo.flush();
			assertThat(orderRepo.count()).isEqualTo(before - 30);
		}
	}

	// ==================== 관계 엣지케이스 ====================

	@Nested
	@DisplayName("관계 엣지케이스")
	class RelationEdgeCases {

		@Test
		@DisplayName("OrderItem 0건인 Order")
		void orderWithNoItems() {
			OrderEntity saved = orderRepo.save(
					entity("NOITEM-001", "A", OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();
			OrderEntity found = orderRepo.findById(saved.getId()).get();
			assertThat(found.getItems()).isNullOrEmpty();
		}

		@Test
		@DisplayName("OrderItem 다수(10건) 연결")
		void manyItems() {
			OrderEntity order = orderRepo.save(
					entity("MANY-001", "A", OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();

			for (int i = 0; i < 10; i++) {
				OrderItemEntity item = new OrderItemEntity();
				item.setProductName("Product " + i);
				item.setQuantity(i + 1);
				item.setUnitPrice(new BigDecimal(1000 * (i + 1)));
				item.setOrder(order);
				itemRepo.save(item);
			}
			itemRepo.flush();

			long count = itemRepo.count();
			assertThat(count).isGreaterThanOrEqualTo(10);
		}

		@Test
		@DisplayName("OrderItem의 order_id FK 무결성")
		void fkIntegrity() {
			OrderEntity order = orderRepo.save(
					entity("FK-001", "A", OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();

			OrderItemEntity item = new OrderItemEntity();
			item.setProductName("FK Item");
			item.setQuantity(1);
			item.setUnitPrice(BigDecimal.ONE);
			item.setOrder(order);
			itemRepo.save(item);
			itemRepo.flush();

			Long orderId = jdbc.queryForObject(
					"SELECT ORDER_ID FROM ORDER_ITEMS WHERE ID = ?",
					Long.class, item.getId());
			assertThat(orderId).isEqualTo(order.getId());
		}
	}

	// ==================== Mapper 엣지케이스 ====================

	@Nested
	@DisplayName("Mapper 엣지케이스")
	class MapperEdgeCases {

		@Test
		@DisplayName("모든 필드 null인 도메인 → Entity 변환")
		void allNullFields() {
			Order domain = new Order();
			OrderEntity entity = mapper.toStorage(domain);
			assertThat(entity).isNotNull();
			assertThat(entity.getId()).isNull();
			assertThat(entity.getOrderNumber()).isNull();
		}

		@Test
		@DisplayName("모든 필드 채워진 왕복 변환")
		void fullFieldRoundTrip() {
			Order orig = new Order();
			orig.setId(1L);
			orig.setOrderNumber("RT-001");
			orig.setCustomerName("Full");
			orig.setStatus(OrderStatus.DELIVERED);
			orig.setTotalAmount(new BigDecimal("12345.67"));
			orig.setOrderedAt(LocalDateTime.of(2026, 1, 15, 9, 30, 45));

			Order result = mapper.toDomain(mapper.toStorage(orig));
			assertThat(result.getId()).isEqualTo(orig.getId());
			assertThat(result.getOrderNumber()).isEqualTo(orig.getOrderNumber());
			assertThat(result.getCustomerName()).isEqualTo(orig.getCustomerName());
			assertThat(result.getStatus()).isEqualTo(orig.getStatus());
			assertThat(result.getTotalAmount()).isEqualByComparingTo(orig.getTotalAmount());
			assertThat(result.getOrderedAt()).isEqualTo(orig.getOrderedAt());
		}
	}

	// ==================== AutoJpaAdapter 빈 탐색 ====================

	@Nested
	@DisplayName("AutoJpaAdapter 빈 탐색 상세")
	class BeanDiscovery {

		@Test
		@DisplayName("OrderItem 관련 빈도 존재")
		void orderItemBeans() {
			assertThat(context.containsBean("orderItemEntityRepository")).isTrue();
			assertThat(context.containsBean("orderItemStorageMapper")).isTrue();
		}

		@Test
		@DisplayName("잘못된 도메인 타입으로 Adapter 생성 시 예외")
		void wrongDomainType() {
			assertThatThrownBy(() -> new AutoJpaAdapter<String, Long>(String.class, context) {})
					.isInstanceOf(Exception.class);
		}
	}

	// ==================== 트랜잭션 동작 ====================

	@Nested
	@DisplayName("트랜잭션/flush 동작")
	class TransactionTests {

		@Test
		@DisplayName("save 후 flush 전에 findById 가능 (영속성 컨텍스트)")
		void findBeforeFlush() {
			OrderEntity saved = orderRepo.save(
					entity("NOFLUSH", "A", OrderStatus.PENDING, BigDecimal.ONE));
			// flush 안 해도 1차 캐시에서 조회 가능
			Optional<OrderEntity> found = orderRepo.findById(saved.getId());
			assertThat(found).isPresent();
		}

		@Test
		@DisplayName("수정 후 flush하면 DB 반영됨")
		void modifyAndFlush() {
			OrderEntity saved = orderRepo.save(
					entity("MOD-001", "Before", OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();

			saved.setCustomerName("After");
			orderRepo.flush();

			String dbName = jdbc.queryForObject(
					"SELECT CUSTOMER_NAME FROM ORDERS WHERE ID = ?",
					String.class, saved.getId());
			assertThat(dbName).isEqualTo("After");
		}
	}

	// ==================== JDBC 직접 검증 ====================

	@Nested
	@DisplayName("JDBC 직접 검증")
	class JdbcVerification {

		@Test
		@DisplayName("JPA 저장 후 JDBC로 직접 조회 일치")
		void jpaToJdbc() {
			OrderEntity saved = orderRepo.save(
					entity("JDBC-001", "JdbcUser", OrderStatus.SHIPPED,
							new BigDecimal("77777.77")));
			orderRepo.flush();

			var row = jdbc.queryForMap(
					"SELECT * FROM ORDERS WHERE ID = ?", saved.getId());
			assertThat(row.get("ORDER_NO")).isEqualTo("JDBC-001");
			assertThat(row.get("CUSTOMER_NAME")).isEqualTo("JdbcUser");
			assertThat(row.get("STATUS")).isEqualTo("SHIPPED");
			assertThat(((java.math.BigDecimal) row.get("TOTAL_AMOUNT"))
					.compareTo(new BigDecimal("77777.77"))).isZero();
		}

		@Test
		@DisplayName("ID 자동 채번이 순차적")
		void sequentialIds() {
			OrderEntity e1 = orderRepo.save(
					entity("SEQ-1", "A", OrderStatus.PENDING, BigDecimal.ONE));
			OrderEntity e2 = orderRepo.save(
					entity("SEQ-2", "B", OrderStatus.PENDING, BigDecimal.ONE));
			orderRepo.flush();
			assertThat(e2.getId()).isGreaterThan(e1.getId());
		}
	}
}
