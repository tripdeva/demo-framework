package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class MybatisCompleteTest {

	@Autowired private OrderMapper mapper;
	@Autowired private JdbcTemplate jdbc;

	private Order order(String num, String name, OrderStatus st, BigDecimal amt) {
		Order o = new Order();
		o.setOrderNumber(num);
		o.setCustomerName(name);
		o.setStatus(st);
		o.setTotalAmount(amt);
		o.setOrderedAt(LocalDateTime.now());
		return o;
	}

	private Order insert(String num, String name, OrderStatus st, BigDecimal amt) {
		Order o = order(num, name, st, amt);
		mapper.insert(o);
		return o;
	}

	@Nested
	@DisplayName("insert 상세")
	class InsertTests {

		@Test
		@DisplayName("insert 후 ID 자동 채번")
		void autoId() {
			Order o = insert("INS-01", "A", OrderStatus.PENDING, BigDecimal.ONE);
			assertThat(o.getId()).isNotNull().isGreaterThan(0);
		}

		@Test
		@DisplayName("연속 insert 시 ID 증가")
		void sequentialId() {
			Order o1 = insert("INS-02", "A", OrderStatus.PENDING, BigDecimal.ONE);
			Order o2 = insert("INS-03", "B", OrderStatus.PENDING, BigDecimal.ONE);
			assertThat(o2.getId()).isGreaterThan(o1.getId());
		}

		@Test
		@DisplayName("특수문자 customerName insert")
		void specialChars() {
			String name = "O'Brien & Co. <\"test\"> 한글 🎉";
			Order o = insert("INS-04", name, OrderStatus.PENDING, BigDecimal.ONE);
			assertThat(mapper.findById(o.getId()).getCustomerName()).isEqualTo(name);
		}

		@Test
		@DisplayName("BigDecimal 정밀도 유지")
		void decimalPrecision() {
			Order o = insert("INS-05", "A", OrderStatus.PENDING,
					new BigDecimal("99999999999999999.99"));
			assertThat(mapper.findById(o.getId()).getTotalAmount())
					.isEqualByComparingTo(new BigDecimal("99999999999999999.99"));
		}

		@ParameterizedTest
		@EnumSource(OrderStatus.class)
		@DisplayName("모든 status 값 insert/조회")
		void allStatuses(OrderStatus st) {
			Order o = insert("INS-ST-" + st, "A", st, BigDecimal.ONE);
			assertThat(mapper.findById(o.getId()).getStatus()).isEqualTo(st);
		}

		@Test
		@DisplayName("null status insert")
		void nullStatus() {
			Order o = insert("INS-NS", "A", null, BigDecimal.ONE);
			assertThat(mapper.findById(o.getId()).getStatus()).isNull();
		}

		@Test
		@DisplayName("null orderedAt insert")
		void nullDate() {
			Order o = order("INS-ND", "A", OrderStatus.PENDING, BigDecimal.ONE);
			o.setOrderedAt(null);
			mapper.insert(o);
			assertThat(mapper.findById(o.getId()).getOrderedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("findAll 상세")
	class FindAllTests {

		@BeforeEach
		void data() {
			IntStream.range(0, 15).forEach(i ->
					insert("FA-" + i, "U" + i, OrderStatus.values()[i % 5],
							new BigDecimal(i * 1000)));
		}

		@Test
		@DisplayName("전체 건수 일치")
		void totalCount() {
			assertThat(mapper.findAll()).hasSize(15);
			assertThat(mapper.count()).isEqualTo(15);
		}

		@Test
		@DisplayName("findAll 결과에 모든 필드 존재")
		void allFieldsPresent() {
			mapper.findAll().forEach(o -> {
				assertThat(o.getId()).isNotNull();
				assertThat(o.getOrderNumber()).isNotNull();
				assertThat(o.getCustomerName()).isNotNull();
				assertThat(o.getTotalAmount()).isNotNull();
			});
		}
	}

	@Nested
	@DisplayName("update 상세")
	class UpdateTests {

		@Test
		@DisplayName("customerName 변경")
		void updateName() {
			Order o = insert("UPD-01", "Before", OrderStatus.PENDING, BigDecimal.ONE);
			o.setCustomerName("After");
			mapper.update(o);
			assertThat(mapper.findById(o.getId()).getCustomerName()).isEqualTo("After");
		}

		@Test
		@DisplayName("status 변경")
		void updateStatus() {
			Order o = insert("UPD-02", "A", OrderStatus.PENDING, BigDecimal.ONE);
			o.setStatus(OrderStatus.SHIPPED);
			mapper.update(o);
			assertThat(mapper.findById(o.getId()).getStatus()).isEqualTo(OrderStatus.SHIPPED);
		}

		@Test
		@DisplayName("totalAmount 변경")
		void updateAmount() {
			Order o = insert("UPD-03", "A", OrderStatus.PENDING, BigDecimal.ONE);
			o.setTotalAmount(new BigDecimal("99999"));
			mapper.update(o);
			assertThat(mapper.findById(o.getId()).getTotalAmount())
					.isEqualByComparingTo(new BigDecimal("99999"));
		}

		@Test
		@DisplayName("전체 필드 동시 변경")
		void updateAllFields() {
			Order o = insert("UPD-04", "A", OrderStatus.PENDING, BigDecimal.ONE);
			o.setOrderNumber("UPD-04-MOD");
			o.setCustomerName("Modified");
			o.setStatus(OrderStatus.DELIVERED);
			o.setTotalAmount(new BigDecimal("55555"));
			mapper.update(o);

			Order found = mapper.findById(o.getId());
			assertThat(found.getOrderNumber()).isEqualTo("UPD-04-MOD");
			assertThat(found.getCustomerName()).isEqualTo("Modified");
			assertThat(found.getStatus()).isEqualTo(OrderStatus.DELIVERED);
			assertThat(found.getTotalAmount()).isEqualByComparingTo(new BigDecimal("55555"));
		}
	}

	@Nested
	@DisplayName("deleteById 상세")
	class DeleteTests {

		@Test
		@DisplayName("삭제 후 findById null")
		void deleteAndFind() {
			Order o = insert("DEL-01", "A", OrderStatus.PENDING, BigDecimal.ONE);
			mapper.deleteById(o.getId());
			assertThat(mapper.findById(o.getId())).isNull();
		}

		@Test
		@DisplayName("삭제 후 count 감소")
		void deleteAndCount() {
			Order o = insert("DEL-02", "A", OrderStatus.PENDING, BigDecimal.ONE);
			long before = mapper.count();
			mapper.deleteById(o.getId());
			assertThat(mapper.count()).isEqualTo(before - 1);
		}

		@Test
		@DisplayName("삭제 후 existsById false")
		void deleteAndExists() {
			Order o = insert("DEL-03", "A", OrderStatus.PENDING, BigDecimal.ONE);
			mapper.deleteById(o.getId());
			assertThat(mapper.existsById(o.getId())).isFalse();
		}
	}

	@Nested
	@DisplayName("patch 상세")
	class PatchTests {

		@Test
		@DisplayName("1개 필드 patch - 나머지 유지")
		void singleField() {
			Order o = insert("PAT-01", "Before", OrderStatus.PENDING,
					new BigDecimal("5000"));
			mapper.patch(Patch.create(Order.class, o.getId(),
					PatchValue.of("customerName", "After")));
			Order found = mapper.findById(o.getId());
			assertThat(found.getCustomerName()).isEqualTo("After");
			assertThat(found.getOrderNumber()).isEqualTo("PAT-01");
			assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(found.getTotalAmount()).isEqualByComparingTo(new BigDecimal("5000"));
		}

		@Test
		@DisplayName("2개 필드 동시 patch")
		void twoFields() {
			Order o = insert("PAT-02", "A", OrderStatus.PENDING, BigDecimal.ONE);
			mapper.patch(Patch.create(Order.class, o.getId(),
					PatchValue.of("orderNumber", "PAT-02-MOD"),
					PatchValue.of("customerName", "B")));
			Order found = mapper.findById(o.getId());
			assertThat(found.getOrderNumber()).isEqualTo("PAT-02-MOD");
			assertThat(found.getCustomerName()).isEqualTo("B");
		}

		@Test
		@DisplayName("nullable 필드 NULL로 patch")
		void nullPatch() {
			Order o = insert("PAT-03", "A", OrderStatus.PENDING, BigDecimal.ONE);
			mapper.patch(Patch.create(Order.class, o.getId(),
					PatchValue.of("orderedAt", null)));
			assertThat(mapper.findById(o.getId()).getOrderedAt()).isNull();
		}

		@Test
		@DisplayName("status enum patch")
		void enumPatch() {
			Order o = insert("PAT-04", "A", OrderStatus.PENDING, BigDecimal.ONE);
			mapper.patch(Patch.create(Order.class, o.getId(),
					PatchValue.of("status", OrderStatus.CANCELLED)));
			assertThat(mapper.findById(o.getId()).getStatus())
					.isEqualTo(OrderStatus.CANCELLED);
		}
	}

	@Nested
	@DisplayName("upsert(save) 상세")
	class UpsertTests {

		@Test
		@DisplayName("기존 ID → update 동작")
		void upsertUpdate() {
			Order o = insert("UPS-01", "Before", OrderStatus.PENDING, BigDecimal.ONE);
			o.setCustomerName("After");
			mapper.save(o);
			assertThat(mapper.findById(o.getId()).getCustomerName()).isEqualTo("After");
		}

		@Test
		@DisplayName("upsert 후 count 안 늘어남 (update)")
		void upsertCountStable() {
			Order o = insert("UPS-02", "A", OrderStatus.PENDING, BigDecimal.ONE);
			long before = mapper.count();
			o.setCustomerName("Modified");
			mapper.save(o);
			assertThat(mapper.count()).isEqualTo(before);
		}
	}

	@Nested
	@DisplayName("동적 쿼리 조합 극한")
	class DynamicQueryCombinations {

		@BeforeEach
		void data() {
			insert("DQ-1", "Alpha", OrderStatus.PENDING, new BigDecimal("10000"));
			insert("DQ-2", "Beta", OrderStatus.CONFIRMED, new BigDecimal("20000"));
			insert("DQ-3", "Alpha Beta", OrderStatus.SHIPPED, new BigDecimal("30000"));
			insert("DQ-4", "Gamma", OrderStatus.DELIVERED, new BigDecimal("40000"));
			insert("DQ-5", "Delta", OrderStatus.CANCELLED, new BigDecimal("50000"));
			insert("DQ-6", "Alpha", OrderStatus.CONFIRMED, new BigDecimal("15000"));
		}

		@Test
		void 빈조건_전체반환() {
			assertThat(mapper.findByCondition(new OrderSearchCondition())).hasSize(6);
		}

		@Test
		void status_PENDING() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setStatus(OrderStatus.PENDING);
			assertThat(mapper.findByCondition(c)).hasSize(1);
		}

		@Test
		void status_CONFIRMED() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setStatus(OrderStatus.CONFIRMED);
			assertThat(mapper.findByCondition(c)).hasSize(2);
		}

		@Test
		void name_Alpha() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setCustomerName("Alpha");
			assertThat(mapper.findByCondition(c)).hasSize(3); // Alpha, Alpha Beta, Alpha
		}

		@Test
		void name_Beta() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setCustomerName("Beta");
			assertThat(mapper.findByCondition(c)).hasSize(2); // Beta, Alpha Beta
		}

		@Test
		void minAmount_25000() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setMinAmount(new BigDecimal("25000"));
			List<Order> result = mapper.findByCondition(c);
			assertThat(result).hasSize(3);
			result.forEach(o -> assertThat(o.getTotalAmount())
					.isGreaterThanOrEqualTo(new BigDecimal("25000")));
		}

		@Test
		void maxAmount_20000() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setMaxAmount(new BigDecimal("20000"));
			List<Order> result = mapper.findByCondition(c);
			assertThat(result).hasSize(3);
		}

		@Test
		void range_15000_35000() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setMinAmount(new BigDecimal("15000"));
			c.setMaxAmount(new BigDecimal("35000"));
			assertThat(mapper.findByCondition(c)).hasSize(3); // 20000, 30000, 15000
		}

		@Test
		void status_AND_name() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setStatus(OrderStatus.CONFIRMED);
			c.setCustomerName("Alpha");
			assertThat(mapper.findByCondition(c)).hasSize(1); // Alpha, CONFIRMED, 15000
		}

		@Test
		void status_AND_min() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setStatus(OrderStatus.CONFIRMED);
			c.setMinAmount(new BigDecimal("18000"));
			assertThat(mapper.findByCondition(c)).hasSize(1); // Beta, 20000
		}

		@Test
		void name_AND_range() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setCustomerName("Alpha");
			c.setMinAmount(new BigDecimal("12000"));
			c.setMaxAmount(new BigDecimal("35000"));
			assertThat(mapper.findByCondition(c)).hasSize(2); // Alpha Beta 30000, Alpha 15000
		}

		@Test
		void all_four_conditions() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setStatus(OrderStatus.SHIPPED);
			c.setCustomerName("Alpha");
			c.setMinAmount(new BigDecimal("25000"));
			c.setMaxAmount(new BigDecimal("35000"));
			assertThat(mapper.findByCondition(c)).hasSize(1); // Alpha Beta, SHIPPED, 30000
		}

		@Test
		void no_match() {
			OrderSearchCondition c = new OrderSearchCondition();
			c.setStatus(OrderStatus.PENDING);
			c.setMinAmount(new BigDecimal("99999"));
			assertThat(mapper.findByCondition(c)).isEmpty();
		}

		@ParameterizedTest
		@EnumSource(OrderStatus.class)
		void findByStatus_all(OrderStatus st) {
			mapper.findByStatus(st).forEach(o ->
					assertThat(o.getStatus()).isEqualTo(st));
		}
	}
}
