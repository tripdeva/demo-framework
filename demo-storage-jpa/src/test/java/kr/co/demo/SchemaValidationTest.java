package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderItem;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.OrderItemEntity;
import kr.co.demo.domain.mapper.OrderStorageMapper;
import kr.co.demo.domain.repository.OrderEntityRepository;
import kr.co.demo.domain.repository.OrderItemEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class SchemaValidationTest {

	@Autowired
	private OrderEntityRepository orderRepo;

	@Autowired
	private OrderItemEntityRepository orderItemRepo;

	@Autowired
	private OrderStorageMapper mapper;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager em;

	private OrderEntity createEntity(String num, String customer, OrderStatus status) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName(customer);
		e.setStatus(status);
		e.setTotalAmount(new BigDecimal("10000"));
		e.setOrderedAt(LocalDateTime.now());
		return e;
	}

	@Nested
	@DisplayName("테이블/컬럼 매핑 검증")
	class TableColumnMapping {

		@Test
		@DisplayName("@StorageTable('orders') → 테이블명 orders로 생성됨")
		void tableNameMapping() throws Exception {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet tables = meta.getTables(null, null, "ORDERS", null);
				assertThat(tables.next()).isTrue();
			}
		}

		@Test
		@DisplayName("@StorageColumn(value='order_no') → 컬럼명 order_no로 생성됨")
		void columnNameMapping() throws Exception {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet cols = meta.getColumns(null, null, "ORDERS", "ORDER_NO");
				assertThat(cols.next()).isTrue();
			}
		}

		@Test
		@DisplayName("명시적 컬럼명 없는 필드는 snake_case로 변환됨 (orderedAt → ordered_at)")
		void implicitSnakeCaseColumn() throws Exception {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet cols = meta.getColumns(null, null, "ORDERS", "ORDERED_AT");
				assertThat(cols.next()).isTrue();
			}
		}

		@Test
		@DisplayName("order_items 테이블도 생성됨")
		void orderItemsTable() throws Exception {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet tables = meta.getTables(null, null, "ORDER_ITEMS", null);
				assertThat(tables.next()).isTrue();
			}
		}

		@Test
		@DisplayName("전체 컬럼 목록 확인")
		void allColumns() throws Exception {
			List<String> columns = new ArrayList<>();
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet cols = meta.getColumns(null, null, "ORDERS", null);
				while (cols.next()) {
					columns.add(cols.getString("COLUMN_NAME"));
				}
			}
			assertThat(columns).contains("ID", "ORDER_NO", "CUSTOMER_NAME",
					"STATUS", "TOTAL_AMOUNT", "ORDERED_AT");
			// @StorageTransient 필드 tempCalculation은 없어야 함
			assertThat(columns).doesNotContain("TEMP_CALCULATION");
		}
	}

	@Nested
	@DisplayName("@StorageIndex 검증")
	class IndexValidation {

		@Test
		@DisplayName("idx_status 인덱스가 DB에 생성됨")
		void statusIndexExists() throws Exception {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet indexes = meta.getIndexInfo(null, null, "ORDERS", false, false);
				List<String> indexNames = new ArrayList<>();
				while (indexes.next()) {
					String name = indexes.getString("INDEX_NAME");
					if (name != null) indexNames.add(name.toUpperCase());
				}
				assertThat(indexNames).anyMatch(n -> n.contains("IDX_STATUS"));
			}
		}

		@Test
		@DisplayName("idx_customer_status 복합 인덱스가 DB에 생성됨")
		void compositeIndexExists() throws Exception {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet indexes = meta.getIndexInfo(null, null, "ORDERS", false, false);
				List<String> indexNames = new ArrayList<>();
				while (indexes.next()) {
					String name = indexes.getString("INDEX_NAME");
					if (name != null) indexNames.add(name.toUpperCase());
				}
				assertThat(indexNames).anyMatch(n -> n.contains("IDX_CUSTOMER_STATUS"));
			}
		}
	}

	@Nested
	@DisplayName("nullable/unique 제약조건 검증")
	class ConstraintValidation {

		@Test
		@DisplayName("nullable=false 컬럼에 NULL 입력 시 예외 발생")
		@Rollback
		void nullableConstraint() {
			OrderEntity entity = new OrderEntity();
			entity.setOrderNumber(null); // nullable=false
			entity.setCustomerName("Test");
			entity.setTotalAmount(new BigDecimal("100"));

			assertThatThrownBy(() -> {
				orderRepo.save(entity);
				orderRepo.flush();
			}).isInstanceOf(DataIntegrityViolationException.class);
		}

		@Test
		@DisplayName("unique=true 컬럼에 중복 입력 시 예외 발생")
		@Rollback
		void uniqueConstraint() {
			orderRepo.save(createEntity("UNIQUE-001", "Kim", OrderStatus.PENDING));
			orderRepo.flush();

			OrderEntity dup = createEntity("UNIQUE-001", "Lee", OrderStatus.CONFIRMED);

			assertThatThrownBy(() -> {
				orderRepo.save(dup);
				orderRepo.flush();
			}).isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	@Nested
	@DisplayName("@StorageEnum 검증")
	class EnumValidation {

		@Test
		@DisplayName("EnumType.STRING → DB에 문자열로 저장됨")
		void enumStoredAsString() {
			OrderEntity entity = createEntity("ENUM-001", "Kim", OrderStatus.CONFIRMED);
			orderRepo.save(entity);
			orderRepo.flush();

			String dbValue = jdbcTemplate.queryForObject(
					"SELECT STATUS FROM ORDERS WHERE ORDER_NO = 'ENUM-001'",
					String.class);
			assertThat(dbValue).isEqualTo("CONFIRMED");
		}

		@Test
		@DisplayName("모든 enum 값이 정상 저장됨")
		void allEnumValues() {
			for (OrderStatus status : OrderStatus.values()) {
				OrderEntity e = createEntity("ENUM-" + status.name(), "Test", status);
				orderRepo.save(e);
			}
			orderRepo.flush();

			for (OrderStatus status : OrderStatus.values()) {
				String dbValue = jdbcTemplate.queryForObject(
						"SELECT STATUS FROM ORDERS WHERE ORDER_NO = 'ENUM-" + status.name() + "'",
						String.class);
				assertThat(dbValue).isEqualTo(status.name());
			}
		}
	}

	@Nested
	@DisplayName("@StorageTransient 검증")
	class TransientValidation {

		@Test
		@DisplayName("@StorageTransient 필드는 DB 컬럼에 없음")
		void transientFieldNotInDb() throws Exception {
			List<String> columns = new ArrayList<>();
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet cols = meta.getColumns(null, null, "ORDERS", null);
				while (cols.next()) {
					columns.add(cols.getString("COLUMN_NAME"));
				}
			}
			assertThat(columns).doesNotContain("TEMP_CALCULATION");
		}

		@Test
		@DisplayName("@StorageTransient 필드가 Mapper에서 제외됨")
		void transientFieldExcludedFromMapper() {
			Order order = new Order();
			order.setOrderNumber("TRANS-001");
			order.setCustomerName("Test");
			order.setStatus(OrderStatus.PENDING);
			order.setTotalAmount(new BigDecimal("1000"));
			order.setTempCalculation("should-not-persist");

			OrderEntity entity = mapper.toStorage(order);
			// entity에 tempCalculation 필드 자체가 없으므로 정상

			Order back = mapper.toDomain(entity);
			// toDomain에서도 tempCalculation은 매핑 안 됨
			assertThat(back.getTempCalculation()).isNull();
		}
	}

	@Nested
	@DisplayName("@StorageId 검증")
	class IdValidation {

		@Test
		@DisplayName("autoGenerated=true → ID 자동 채번")
		void autoGeneratedId() {
			OrderEntity saved = orderRepo.save(
					createEntity("ID-001", "Kim", OrderStatus.PENDING));
			orderRepo.flush();
			assertThat(saved.getId()).isNotNull();
			assertThat(saved.getId()).isGreaterThan(0);
		}

		@Test
		@DisplayName("저장 후 ID로 조회 가능")
		void findByGeneratedId() {
			OrderEntity saved = orderRepo.save(
					createEntity("ID-002", "Lee", OrderStatus.CONFIRMED));
			orderRepo.flush();

			OrderEntity found = orderRepo.findById(saved.getId()).orElse(null);
			assertThat(found).isNotNull();
			assertThat(found.getOrderNumber()).isEqualTo("ID-002");
		}
	}

	@Nested
	@DisplayName("@StorageRelation 검증")
	class RelationValidation {

		@Test
		@DisplayName("OneToMany 관계 - Order에서 OrderItem 접근")
		void oneToManyRelation() {
			OrderEntity order = createEntity("REL-001", "Kim", OrderStatus.PENDING);
			orderRepo.save(order);
			orderRepo.flush();

			OrderItemEntity item1 = new OrderItemEntity();
			item1.setProductName("Item A");
			item1.setQuantity(2);
			item1.setUnitPrice(new BigDecimal("5000"));
			item1.setOrder(order);

			OrderItemEntity item2 = new OrderItemEntity();
			item2.setProductName("Item B");
			item2.setQuantity(1);
			item2.setUnitPrice(new BigDecimal("3000"));
			item2.setOrder(order);

			orderItemRepo.save(item1);
			orderItemRepo.save(item2);
			orderItemRepo.flush();
			em.clear();

			OrderEntity found = orderRepo.findById(order.getId()).orElse(null);
			assertThat(found).isNotNull();
			assertThat(found.getItems()).hasSize(2);
		}

		@Test
		@DisplayName("ManyToOne 관계 - OrderItem에서 Order 접근")
		void manyToOneRelation() {
			OrderEntity order = createEntity("REL-002", "Lee", OrderStatus.CONFIRMED);
			orderRepo.save(order);
			orderRepo.flush();

			OrderItemEntity item = new OrderItemEntity();
			item.setProductName("Item C");
			item.setQuantity(3);
			item.setUnitPrice(new BigDecimal("7000"));
			item.setOrder(order);
			orderItemRepo.save(item);
			orderItemRepo.flush();

			OrderItemEntity found = orderItemRepo.findById(item.getId()).orElse(null);
			assertThat(found).isNotNull();
			assertThat(found.getOrder()).isNotNull();
			assertThat(found.getOrder().getOrderNumber()).isEqualTo("REL-002");
		}
	}

	@Nested
	@DisplayName("@StorageVersion 검증")
	class VersionValidation {

		@Test
		@DisplayName("version 컬럼이 DB에 존재")
		void versionColumnExists() throws Exception {
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet cols = meta.getColumns(null, null, "ORDERS", "VERSION");
				assertThat(cols.next()).isTrue();
			}
		}

		@Test
		@DisplayName("최초 저장 시 version = 0")
		void initialVersion() {
			OrderEntity saved = orderRepo.save(
					createEntity("VER-001", "Kim", OrderStatus.PENDING));
			orderRepo.flush();
			assertThat(saved.getVersion()).isEqualTo(0L);
		}

		@Test
		@DisplayName("수정 시 version 자동 증가")
		void versionIncrement() {
			OrderEntity saved = orderRepo.save(
					createEntity("VER-002", "Lee", OrderStatus.PENDING));
			orderRepo.flush();
			Long v0 = saved.getVersion();

			saved.setCustomerName("Modified");
			orderRepo.flush();
			assertThat(saved.getVersion()).isGreaterThan(v0);
		}
	}

	@Nested
	@DisplayName("Auditing 검증")
	class AuditingValidation {

		@Test
		@DisplayName("createdAt 자동 설정됨")
		void createdAtAutoSet() {
			OrderEntity saved = orderRepo.save(
					createEntity("AUD-001", "Kim", OrderStatus.PENDING));
			orderRepo.flush();
			assertThat(saved.getCreatedAt()).isNotNull();
		}

		@Test
		@DisplayName("updatedAt 자동 설정됨")
		void updatedAtAutoSet() {
			OrderEntity saved = orderRepo.save(
					createEntity("AUD-002", "Lee", OrderStatus.PENDING));
			orderRepo.flush();
			assertThat(saved.getUpdatedAt()).isNotNull();
		}

		@Test
		@DisplayName("createdAt/updatedAt 컬럼 존재")
		void auditColumnsExist() throws Exception {
			List<String> columns = new ArrayList<>();
			try (Connection conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet cols = meta.getColumns(null, null, "ORDERS", null);
				while (cols.next()) {
					columns.add(cols.getString("COLUMN_NAME"));
				}
			}
			assertThat(columns).contains("CREATED_AT", "UPDATED_AT");
		}
	}

	@Nested
	@DisplayName("DomainMapper 양방향 변환 검증")
	class MapperValidation {

		@Test
		@DisplayName("Domain → Entity → Domain 왕복 변환 정합성")
		void roundTripConversion() {
			Order original = new Order();
			original.setId(99L);
			original.setOrderNumber("MAP-001");
			original.setCustomerName("RoundTrip");
			original.setStatus(OrderStatus.SHIPPED);
			original.setTotalAmount(new BigDecimal("25000"));
			original.setOrderedAt(LocalDateTime.of(2026, 3, 20, 12, 0));

			OrderEntity entity = mapper.toStorage(original);
			Order restored = mapper.toDomain(entity);

			assertThat(restored.getId()).isEqualTo(original.getId());
			assertThat(restored.getOrderNumber()).isEqualTo(original.getOrderNumber());
			assertThat(restored.getCustomerName()).isEqualTo(original.getCustomerName());
			assertThat(restored.getStatus()).isEqualTo(original.getStatus());
			assertThat(restored.getTotalAmount()).isEqualByComparingTo(original.getTotalAmount());
			assertThat(restored.getOrderedAt()).isEqualTo(original.getOrderedAt());
		}

		@Test
		@DisplayName("null 입력 시 null 반환")
		void nullHandling() {
			assertThat(mapper.toStorage(null)).isNull();
			assertThat(mapper.toDomain(null)).isNull();
		}
	}
}
