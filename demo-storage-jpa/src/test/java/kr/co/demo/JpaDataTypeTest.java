package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class JpaDataTypeTest {

	@Autowired private OrderEntityRepository repo;
	@Autowired private OrderItemEntityRepository itemRepo;
	@Autowired private OrderStorageMapper mapper;
	@Autowired private EntityManager em;
	@Autowired private JdbcTemplate jdbc;

	private OrderEntity order(String num, String name, OrderStatus st, BigDecimal amt) {
		OrderEntity e = new OrderEntity();
		e.setOrderNumber(num);
		e.setCustomerName(name);
		e.setStatus(st);
		e.setTotalAmount(amt);
		e.setOrderedAt(LocalDateTime.now());
		return e;
	}

	@Nested
	@DisplayName("BigDecimal 정밀도 테스트")
	class DecimalPrecision {

		@ParameterizedTest
		@CsvSource({
				"0.01", "0.99", "1.00", "100.50", "9999.99",
				"123456789.12", "99999999999999999.99", "0.00"
		})
		@DisplayName("다양한 BigDecimal 값 저장/조회")
		void variousDecimals(String value) {
			BigDecimal amt = new BigDecimal(value);
			OrderEntity saved = repo.save(order("DEC-" + value, "A", OrderStatus.PENDING, amt));
			em.flush();
			em.clear();
			assertThat(repo.findById(saved.getId()).get().getTotalAmount())
					.isEqualByComparingTo(amt);
		}
	}

	@Nested
	@DisplayName("LocalDateTime 테스트")
	class DateTimeTests {

		@Test
		@DisplayName("과거 날짜 저장/조회")
		void pastDate() {
			OrderEntity e = order("DT-1", "A", OrderStatus.PENDING, BigDecimal.ONE);
			e.setOrderedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
			repo.save(e);
			em.flush();
			em.clear();
			assertThat(repo.findById(e.getId()).get().getOrderedAt().getYear()).isEqualTo(2020);
		}

		@Test
		@DisplayName("미래 날짜 저장/조회")
		void futureDate() {
			OrderEntity e = order("DT-2", "A", OrderStatus.PENDING, BigDecimal.ONE);
			e.setOrderedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59));
			repo.save(e);
			em.flush();
			em.clear();
			assertThat(repo.findById(e.getId()).get().getOrderedAt().getYear()).isEqualTo(2099);
		}

		@Test
		@DisplayName("나노초 정밀도")
		void nanoPrecision() {
			LocalDateTime precise = LocalDateTime.of(2026, 3, 20, 12, 30, 45, 123456000);
			OrderEntity e = order("DT-3", "A", OrderStatus.PENDING, BigDecimal.ONE);
			e.setOrderedAt(precise);
			repo.save(e);
			em.flush();
			em.clear();
			assertThat(repo.findById(e.getId()).get().getOrderedAt())
					.isEqualTo(precise);
		}
	}

	@Nested
	@DisplayName("String 경계값 테스트")
	class StringBoundary {

		@ParameterizedTest
		@ValueSource(ints = {1, 5, 10, 50, 100, 200, 255})
		@DisplayName("다양한 길이의 문자열")
		void variousLengths(int len) {
			String name = "A".repeat(len);
			OrderEntity saved = repo.save(order("STR-" + len, name,
					OrderStatus.PENDING, BigDecimal.ONE));
			em.flush();
			em.clear();
			assertThat(repo.findById(saved.getId()).get().getCustomerName())
					.hasSize(len);
		}

		@Test
		@DisplayName("유니코드 문자열")
		void unicode() {
			String[] names = {"한글테스트", "日本語テスト", "العربية", "Ñoño", "Ünüm"};
			for (int i = 0; i < names.length; i++) {
				OrderEntity saved = repo.save(order("UNI-" + i, names[i],
						OrderStatus.PENDING, BigDecimal.ONE));
				em.flush();
				em.clear();
				assertThat(repo.findById(saved.getId()).get().getCustomerName())
						.isEqualTo(names[i]);
			}
		}

		@Test
		@DisplayName("공백만 있는 문자열")
		void whitespace() {
			OrderEntity saved = repo.save(order("WS-1", "   ",
					OrderStatus.PENDING, BigDecimal.ONE));
			em.flush();
			em.clear();
			assertThat(repo.findById(saved.getId()).get().getCustomerName())
					.isEqualTo("   ");
		}

		@Test
		@DisplayName("SQL injection 시도 문자열 (안전하게 이스케이프)")
		void sqlInjection() {
			String malicious = "'; DROP TABLE orders; --";
			OrderEntity saved = repo.save(order("INJ-1", malicious,
					OrderStatus.PENDING, BigDecimal.ONE));
			em.flush();
			em.clear();
			assertThat(repo.findById(saved.getId()).get().getCustomerName())
					.isEqualTo(malicious);
			// 테이블이 여전히 존재하는지 확인
			assertThat(repo.count()).isGreaterThanOrEqualTo(1);
		}
	}

	@Nested
	@DisplayName("OrderItem 데이터 타입")
	class ItemDataTypes {

		@Test
		@DisplayName("quantity 0")
		void zeroQuantity() {
			OrderEntity o = repo.save(order("ITEM-Q0", "A", OrderStatus.PENDING, BigDecimal.ONE));
			OrderItemEntity item = new OrderItemEntity();
			item.setProductName("Free");
			item.setQuantity(0);
			item.setUnitPrice(BigDecimal.ZERO);
			item.setOrder(o);
			itemRepo.save(item);
			em.flush();
			em.clear();
			assertThat(itemRepo.findById(item.getId()).get().getQuantity()).isZero();
		}

		@Test
		@DisplayName("대량 수량")
		void largeQuantity() {
			OrderEntity o = repo.save(order("ITEM-QL", "A", OrderStatus.PENDING, BigDecimal.ONE));
			OrderItemEntity item = new OrderItemEntity();
			item.setProductName("Bulk");
			item.setQuantity(Integer.MAX_VALUE);
			item.setUnitPrice(BigDecimal.ONE);
			item.setOrder(o);
			itemRepo.save(item);
			em.flush();
			em.clear();
			assertThat(itemRepo.findById(item.getId()).get().getQuantity())
					.isEqualTo(Integer.MAX_VALUE);
		}

		@Test
		@DisplayName("unitPrice 소수점 정밀도")
		void pricePrecision() {
			OrderEntity o = repo.save(order("ITEM-PR", "A", OrderStatus.PENDING, BigDecimal.ONE));
			OrderItemEntity item = new OrderItemEntity();
			item.setProductName("Precise");
			item.setQuantity(1);
			item.setUnitPrice(new BigDecimal("12345.67"));
			item.setOrder(o);
			itemRepo.save(item);
			em.flush();
			em.clear();
			assertThat(itemRepo.findById(item.getId()).get().getUnitPrice())
					.isEqualByComparingTo(new BigDecimal("12345.67"));
		}
	}

	@Nested
	@DisplayName("Mapper 데이터 타입 변환")
	class MapperDataTypes {

		@Test
		@DisplayName("모든 OrderStatus enum 변환")
		void allEnumConversion() {
			for (OrderStatus st : OrderStatus.values()) {
				kr.co.demo.domain.Order domain = new kr.co.demo.domain.Order();
				domain.setStatus(st);
				domain.setOrderNumber("MP-" + st);
				domain.setCustomerName("Test");
				domain.setTotalAmount(BigDecimal.ONE);

				OrderEntity entity = mapper.toStorage(domain);
				assertThat(entity.getStatus()).isEqualTo(st);

				kr.co.demo.domain.Order back = mapper.toDomain(entity);
				assertThat(back.getStatus()).isEqualTo(st);
			}
		}

		@Test
		@DisplayName("BigDecimal 왕복 변환 정밀도")
		void decimalRoundTrip() {
			BigDecimal precise = new BigDecimal("12345678901234567.89");
			kr.co.demo.domain.Order domain = new kr.co.demo.domain.Order();
			domain.setTotalAmount(precise);
			domain.setOrderNumber("MP-DEC");
			domain.setCustomerName("Test");

			OrderEntity entity = mapper.toStorage(domain);
			kr.co.demo.domain.Order back = mapper.toDomain(entity);
			assertThat(back.getTotalAmount()).isEqualByComparingTo(precise);
		}

		@Test
		@DisplayName("LocalDateTime 왕복 변환")
		void dateTimeRoundTrip() {
			LocalDateTime dt = LocalDateTime.of(2026, 6, 15, 14, 30, 45);
			kr.co.demo.domain.Order domain = new kr.co.demo.domain.Order();
			domain.setOrderedAt(dt);
			domain.setOrderNumber("MP-DT");
			domain.setCustomerName("Test");
			domain.setTotalAmount(BigDecimal.ONE);

			OrderEntity entity = mapper.toStorage(domain);
			kr.co.demo.domain.Order back = mapper.toDomain(entity);
			assertThat(back.getOrderedAt()).isEqualTo(dt);
		}
	}
}
