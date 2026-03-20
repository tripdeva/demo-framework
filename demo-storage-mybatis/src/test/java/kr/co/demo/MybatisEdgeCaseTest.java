package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import kr.co.demo.client.mybatis.util.Patch;
import kr.co.demo.client.mybatis.util.PatchValue;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.mapper.OrderMapper;
import kr.co.demo.domain.mapper.OrderSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class MybatisEdgeCaseTest {

	@Autowired private OrderMapper orderMapper;
	@Autowired private JdbcTemplate jdbc;

	private Order createOrder(String num, String name, OrderStatus st, BigDecimal amt) {
		Order o = new Order();
		o.setOrderNumber(num);
		o.setCustomerName(name);
		o.setStatus(st);
		o.setTotalAmount(amt);
		o.setOrderedAt(LocalDateTime.now());
		return o;
	}

	private Order insertOrder(String num, String name, OrderStatus st, BigDecimal amt) {
		Order o = createOrder(num, name, st, amt);
		orderMapper.insert(o);
		return o;
	}

	// ==================== BaseMapper 경계값 ====================

	@Nested
	@DisplayName("BaseMapper 경계값")
	class BaseMapperBoundary {

		@Test
		@DisplayName("금액 0원")
		void zeroAmount() {
			Order o = insertOrder("MBE-Z", "A", OrderStatus.PENDING, BigDecimal.ZERO);
			assertThat(orderMapper.findById(o.getId()).getTotalAmount())
					.isEqualByComparingTo(BigDecimal.ZERO);
		}

		@Test
		@DisplayName("특수문자 customerName")
		void specialChars() {
			String name = "O'Brien & Co. <\"test\"> 한글";
			Order o = insertOrder("MBE-SC", name, OrderStatus.PENDING, BigDecimal.ONE);
			assertThat(orderMapper.findById(o.getId()).getCustomerName()).isEqualTo(name);
		}

		@ParameterizedTest
		@EnumSource(OrderStatus.class)
		@DisplayName("모든 enum 값 저장/조회")
		void allEnums(OrderStatus status) {
			Order o = insertOrder("MBE-E-" + status, "A", status, BigDecimal.ONE);
			Order found = orderMapper.findById(o.getId());
			assertThat(found.getStatus()).isEqualTo(status);

			String dbVal = jdbc.queryForObject(
					"SELECT status FROM orders WHERE id = ?", String.class, o.getId());
			assertThat(dbVal).isEqualTo(status.name());
		}

		@Test
		@DisplayName("orderedAt null 저장/조회")
		void nullDateTime() {
			Order o = createOrder("MBE-NDT", "A", OrderStatus.PENDING, BigDecimal.ONE);
			o.setOrderedAt(null);
			orderMapper.insert(o);
			assertThat(orderMapper.findById(o.getId()).getOrderedAt()).isNull();
		}

		@Test
		@DisplayName("status null 저장/조회")
		void nullStatus() {
			Order o = createOrder("MBE-NST", "A", null, BigDecimal.ONE);
			orderMapper.insert(o);
			assertThat(orderMapper.findById(o.getId()).getStatus()).isNull();
		}
	}

	// ==================== 대량 데이터 ====================

	@Nested
	@DisplayName("대량 데이터")
	class BulkData {

		@Test
		@DisplayName("100건 벌크 insert + findAll")
		void bulk100() {
			IntStream.range(0, 100).forEach(i ->
					insertOrder("MBULK-" + i, "U" + i, OrderStatus.PENDING,
							new BigDecimal(i * 100)));
			assertThat(orderMapper.findAll().size()).isGreaterThanOrEqualTo(100);
			assertThat(orderMapper.count()).isGreaterThanOrEqualTo(100);
		}
	}

	// ==================== Patch 극한 ====================

	@Nested
	@DisplayName("Patch 극한 테스트")
	class PatchExtreme {

		@Test
		@DisplayName("단일 필드만 patch")
		void singleFieldPatch() {
			Order o = insertOrder("MPE-1", "Before", OrderStatus.PENDING, BigDecimal.ONE);
			Patch<Order> patch = Patch.create(Order.class, o.getId(),
					PatchValue.of("customerName", "After"));
			orderMapper.patch(patch);
			assertThat(orderMapper.findById(o.getId()).getCustomerName()).isEqualTo("After");
			// 다른 필드는 변경되지 않아야 함
			assertThat(orderMapper.findById(o.getId()).getOrderNumber()).isEqualTo("MPE-1");
		}

		@Test
		@DisplayName("여러 필드 동시 patch")
		void multiFieldPatch() {
			Order o = insertOrder("MPE-2", "Before", OrderStatus.PENDING,
					new BigDecimal("1000"));
			Patch<Order> patch = Patch.create(Order.class, o.getId(),
					PatchValue.of("customerName", "After"),
					PatchValue.of("orderNumber", "MPE-2-MOD"));
			orderMapper.patch(patch);
			Order found = orderMapper.findById(o.getId());
			assertThat(found.getCustomerName()).isEqualTo("After");
			assertThat(found.getOrderNumber()).isEqualTo("MPE-2-MOD");
		}

		@Test
		@DisplayName("nullable 필드를 NULL로 patch")
		void patchToNull() {
			Order o = insertOrder("MPE-3", "A", OrderStatus.PENDING, BigDecimal.ONE);
			Patch<Order> patch = Patch.create(Order.class, o.getId(),
					PatchValue.of("orderedAt", null));
			orderMapper.patch(patch);
			assertThat(orderMapper.findById(o.getId()).getOrderedAt()).isNull();
		}
	}

	// ==================== Upsert 극한 ====================

	@Nested
	@DisplayName("Upsert(save) 극한")
	class UpsertExtreme {

		@Test
		@DisplayName("기존 ID로 upsert → 업데이트 동작")
		void upsertWithExistingId() {
			Order o = insertOrder("MUP-1", "Before", OrderStatus.PENDING, BigDecimal.ONE);
			// ID가 있는 상태에서 save (MERGE)
			o.setCustomerName("After Upsert");
			orderMapper.save(o);
			assertThat(orderMapper.findById(o.getId()).getCustomerName())
					.isEqualTo("After Upsert");
		}

		@Test
		@DisplayName("기존 레코드 upsert → update 동작")
		void upsertUpdate() {
			Order o = insertOrder("MUP-2", "Before", OrderStatus.PENDING, BigDecimal.ONE);
			o.setCustomerName("After");
			orderMapper.save(o);
			assertThat(orderMapper.findById(o.getId()).getCustomerName()).isEqualTo("After");
		}
	}

	// ==================== 동적 쿼리 극한 (XML) ====================

	@Nested
	@DisplayName("동적 쿼리 극한")
	class DynamicQueryExtreme {

		@BeforeEach
		void setUpData() {
			insertOrder("DQ-1", "Kim", OrderStatus.PENDING, new BigDecimal("10000"));
			insertOrder("DQ-2", "Lee Kim", OrderStatus.CONFIRMED, new BigDecimal("20000"));
			insertOrder("DQ-3", "Park", OrderStatus.SHIPPED, new BigDecimal("30000"));
			insertOrder("DQ-4", "Kim Jr", OrderStatus.DELIVERED, new BigDecimal("40000"));
			insertOrder("DQ-5", "Choi", OrderStatus.CANCELLED, new BigDecimal("50000"));
		}

		@Test
		@DisplayName("모든 조건 null → 전체 반환")
		void allConditionsNull() {
			OrderSearchCondition cond = new OrderSearchCondition();
			assertThat(orderMapper.findByCondition(cond).size()).isGreaterThanOrEqualTo(5);
		}

		@Test
		@DisplayName("status만 지정")
		void statusOnly() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setStatus(OrderStatus.PENDING);
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).allSatisfy(o ->
					assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING));
		}

		@Test
		@DisplayName("customerName LIKE 검색")
		void nameSearch() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setCustomerName("Kim");
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).hasSizeGreaterThanOrEqualTo(2); // Kim, Lee Kim, Kim Jr
		}

		@Test
		@DisplayName("minAmount만 지정")
		void minAmountOnly() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setMinAmount(new BigDecimal("25000"));
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).allSatisfy(o ->
					assertThat(o.getTotalAmount()).isGreaterThanOrEqualTo(new BigDecimal("25000")));
		}

		@Test
		@DisplayName("maxAmount만 지정")
		void maxAmountOnly() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setMaxAmount(new BigDecimal("25000"));
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).allSatisfy(o ->
					assertThat(o.getTotalAmount()).isLessThanOrEqualTo(new BigDecimal("25000")));
		}

		@Test
		@DisplayName("minAmount + maxAmount 범위 검색")
		void amountRange() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setMinAmount(new BigDecimal("15000"));
			cond.setMaxAmount(new BigDecimal("35000"));
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).allSatisfy(o -> {
				assertThat(o.getTotalAmount()).isGreaterThanOrEqualTo(new BigDecimal("15000"));
				assertThat(o.getTotalAmount()).isLessThanOrEqualTo(new BigDecimal("35000"));
			});
		}

		@Test
		@DisplayName("status + customerName 복합 조건")
		void statusAndName() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setStatus(OrderStatus.CONFIRMED);
			cond.setCustomerName("Kim");
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).allSatisfy(o -> {
				assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
				assertThat(o.getCustomerName()).containsIgnoringCase("Kim");
			});
		}

		@Test
		@DisplayName("status + minAmount + maxAmount 3중 조건")
		void tripleCondition() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setStatus(OrderStatus.SHIPPED);
			cond.setMinAmount(new BigDecimal("20000"));
			cond.setMaxAmount(new BigDecimal("40000"));
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).allSatisfy(o -> {
				assertThat(o.getStatus()).isEqualTo(OrderStatus.SHIPPED);
				assertThat(o.getTotalAmount()).isBetween(
						new BigDecimal("20000"), new BigDecimal("40000"));
			});
		}

		@Test
		@DisplayName("4중 조건 (전부 지정)")
		void allConditions() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setStatus(OrderStatus.DELIVERED);
			cond.setCustomerName("Kim");
			cond.setMinAmount(new BigDecimal("30000"));
			cond.setMaxAmount(new BigDecimal("50000"));
			List<Order> result = orderMapper.findByCondition(cond);
			assertThat(result).allSatisfy(o -> {
				assertThat(o.getStatus()).isEqualTo(OrderStatus.DELIVERED);
				assertThat(o.getCustomerName()).containsIgnoringCase("Kim");
			});
		}

		@Test
		@DisplayName("매칭 결과 없는 조건")
		void noMatch() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setStatus(OrderStatus.PENDING);
			cond.setMinAmount(new BigDecimal("99999999"));
			assertThat(orderMapper.findByCondition(cond)).isEmpty();
		}

		@Test
		@DisplayName("@Select 어노테이션 쿼리 - findByStatus")
		void annotationQueryByStatus() {
			List<Order> confirmed = orderMapper.findByStatus(OrderStatus.CONFIRMED);
			assertThat(confirmed).allSatisfy(o ->
					assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED));
		}

		@Test
		@DisplayName("@Select 어노테이션 쿼리 - findByCustomerName")
		void annotationQueryByName() {
			List<Order> kims = orderMapper.findByCustomerName("Kim");
			assertThat(kims).isNotEmpty();
			assertThat(kims).allSatisfy(o ->
					assertThat(o.getCustomerName()).isEqualTo("Kim"));
		}
	}

	// ==================== existsById 엣지 ====================

	@Nested
	@DisplayName("existsById 엣지")
	class ExistsByIdEdge {

		@Test
		@DisplayName("삭제된 레코드 → false")
		void afterDelete() {
			Order o = insertOrder("EX-DEL", "A", OrderStatus.PENDING, BigDecimal.ONE);
			assertThat(orderMapper.existsById(o.getId())).isTrue();
			orderMapper.deleteById(o.getId());
			assertThat(orderMapper.existsById(o.getId())).isFalse();
		}

		@Test
		@DisplayName("음수 ID → false")
		void negativeId() {
			assertThat(orderMapper.existsById(-1L)).isFalse();
		}

		@Test
		@DisplayName("0 ID → false")
		void zeroId() {
			assertThat(orderMapper.existsById(0L)).isFalse();
		}
	}

	// ==================== Update 전후 검증 ====================

	@Nested
	@DisplayName("Update 정합성")
	class UpdateIntegrity {

		@Test
		@DisplayName("update 후 변경되지 않은 필드 유지")
		void unchangedFieldsPreserved() {
			Order o = insertOrder("UPD-1", "Original", OrderStatus.PENDING,
					new BigDecimal("5000"));
			LocalDateTime origDt = o.getOrderedAt();

			o.setCustomerName("Modified");
			orderMapper.update(o);

			Order found = orderMapper.findById(o.getId());
			assertThat(found.getCustomerName()).isEqualTo("Modified");
			assertThat(found.getOrderNumber()).isEqualTo("UPD-1");
			assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(found.getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000"));
		}

		@Test
		@DisplayName("status 변경")
		void updateStatus() {
			Order o = insertOrder("UPD-2", "A", OrderStatus.PENDING, BigDecimal.ONE);
			o.setStatus(OrderStatus.SHIPPED);
			orderMapper.update(o);
			assertThat(orderMapper.findById(o.getId()).getStatus())
					.isEqualTo(OrderStatus.SHIPPED);
		}
	}
}
