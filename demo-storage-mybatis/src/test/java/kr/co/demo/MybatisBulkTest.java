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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class MybatisBulkTest {

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

	@BeforeEach
	void setUp() {
		IntStream.range(0, 30).forEach(i ->
				mapper.insert(order("BLK-" + i, "User" + (i % 5),
						OrderStatus.values()[i % 5],
						new BigDecimal((i + 1) * 1000))));
	}

	@Nested
	@DisplayName("대량 조회")
	class BulkRead {

		@Test
		@DisplayName("findAll 30건")
		void findAll30() {
			assertThat(mapper.findAll()).hasSizeGreaterThanOrEqualTo(30);
		}

		@Test
		@DisplayName("count 30건 이상")
		void count30() {
			assertThat(mapper.count()).isGreaterThanOrEqualTo(30);
		}

		@Test
		@DisplayName("status별 findByStatus")
		void byStatus() {
			for (OrderStatus st : OrderStatus.values()) {
				List<Order> result = mapper.findByStatus(st);
				assertThat(result).allSatisfy(o ->
						assertThat(o.getStatus()).isEqualTo(st));
			}
		}

		@Test
		@DisplayName("customerName별 findByCustomerName")
		void byCustomerName() {
			for (int i = 0; i < 5; i++) {
				final String name = "User" + i;
				List<Order> result = mapper.findByCustomerName(name);
				assertThat(result).allSatisfy(o ->
						assertThat(o.getCustomerName()).isEqualTo(name));
			}
		}
	}

	@Nested
	@DisplayName("대량 수정")
	class BulkUpdate {

		@Test
		@DisplayName("30건 전부 update")
		void updateAll() {
			List<Order> all = mapper.findAll();
			int updated = 0;
			for (Order o : all) {
				o.setCustomerName("Updated-" + o.getId());
				mapper.update(o);
				updated++;
			}
			assertThat(updated).isGreaterThanOrEqualTo(30);

			mapper.findAll().forEach(o ->
					assertThat(o.getCustomerName()).startsWith("Updated-"));
		}

		@Test
		@DisplayName("10건 patch")
		void patchTen() {
			List<Order> all = mapper.findAll();
			int patched = 0;
			for (int i = 0; i < 10 && i < all.size(); i++) {
				Patch<Order> p = Patch.create(Order.class, all.get(i).getId(),
						PatchValue.of("customerName", "Patched-" + i));
				mapper.patch(p);
				patched++;
			}
			assertThat(patched).isEqualTo(10);
		}
	}

	@Nested
	@DisplayName("대량 삭제")
	class BulkDelete {

		@Test
		@DisplayName("전부 삭제")
		void deleteAll() {
			List<Order> all = mapper.findAll();
			for (Order o : all) {
				mapper.deleteById(o.getId());
			}
			assertThat(mapper.count()).isEqualTo(0);
		}
	}

	@Nested
	@DisplayName("복합 조건 동적 쿼리 (대량)")
	class DynamicQueryBulk {

		@Test
		@DisplayName("금액 범위 + status")
		void rangeAndStatus() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setStatus(OrderStatus.PENDING);
			cond.setMinAmount(new BigDecimal("5000"));
			cond.setMaxAmount(new BigDecimal("20000"));
			List<Order> result = mapper.findByCondition(cond);
			assertThat(result).allSatisfy(o -> {
				assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING);
				assertThat(o.getTotalAmount()).isBetween(
						new BigDecimal("5000"), new BigDecimal("20000"));
			});
		}

		@Test
		@DisplayName("customerName 검색 결과 수 정확성")
		void nameSearchCount() {
			OrderSearchCondition cond = new OrderSearchCondition();
			cond.setCustomerName("User0");
			List<Order> result = mapper.findByCondition(cond);
			assertThat(result).hasSize(6); // 0,5,10,15,20,25
		}
	}

	@Nested
	@DisplayName("JDBC 직접 검증")
	class JdbcVerify {

		@Test
		@DisplayName("MyBatis insert 후 JDBC 직접 조회 일치")
		void mybatisToJdbc() {
			Order o = order("JDBC-V1", "JdbcTest", OrderStatus.SHIPPED,
					new BigDecimal("12345.67"));
			mapper.insert(o);

			var row = jdbc.queryForMap("SELECT * FROM orders WHERE id = ?", o.getId());
			assertThat(row.get("ORDER_NO")).isEqualTo("JDBC-V1");
			assertThat(row.get("CUSTOMER_NAME")).isEqualTo("JdbcTest");
			assertThat(row.get("STATUS")).isEqualTo("SHIPPED");
		}

		@Test
		@DisplayName("patch 후 JDBC 검증")
		void patchJdbc() {
			Order o = order("JDBC-P1", "Before", OrderStatus.PENDING, BigDecimal.ONE);
			mapper.insert(o);

			Patch<Order> p = Patch.create(Order.class, o.getId(),
					PatchValue.of("customerName", "AfterPatch"));
			mapper.patch(p);

			String name = jdbc.queryForObject(
					"SELECT CUSTOMER_NAME FROM orders WHERE id = ?",
					String.class, o.getId());
			assertThat(name).isEqualTo("AfterPatch");
		}
	}
}
