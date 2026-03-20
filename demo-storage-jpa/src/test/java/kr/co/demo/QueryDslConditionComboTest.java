package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import kr.co.demo.domain.OrderStatus;
import kr.co.demo.domain.entity.OrderEntity;
import kr.co.demo.domain.entity.QOrderEntity;
import kr.co.demo.domain.repository.OrderEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = TestApplication.class)
@Transactional
class QueryDslConditionComboTest {

	@Autowired private JPAQueryFactory qf;
	@Autowired private OrderEntityRepository repo;
	@Autowired private EntityManager em;

	private final QOrderEntity o = QOrderEntity.orderEntity;

	@BeforeEach
	void setUp() {
		String[] names = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon",
				"Zeta", "Eta", "Theta", "Iota", "Kappa"};
		IntStream.range(0, 40).forEach(i -> {
			OrderEntity e = new OrderEntity();
			e.setOrderNumber("CC-" + String.format("%03d", i));
			e.setCustomerName(names[i % 10]);
			e.setStatus(OrderStatus.values()[i % 5]);
			e.setTotalAmount(new BigDecimal((i + 1) * 1000));
			e.setOrderedAt(LocalDateTime.of(2026, 1 + (i % 12), 1 + (i % 28), 0, 0));
			repo.save(e);
		});
		em.flush();
		em.clear();
	}

	@Nested
	@DisplayName("BooleanBuilder 동적 쿼리")
	class BooleanBuilderTests {

		@Test
		@DisplayName("빈 조건 → 전체 반환")
		void emptyBuilder() {
			BooleanBuilder builder = new BooleanBuilder();
			assertThat(qf.selectFrom(o).where(builder).fetch()).hasSize(40);
		}

		@Test
		@DisplayName("status만 추가")
		void statusOnly() {
			BooleanBuilder builder = new BooleanBuilder();
			builder.and(o.status.eq(OrderStatus.PENDING));
			assertThat(qf.selectFrom(o).where(builder).fetch()).hasSize(8);
		}

		@Test
		@DisplayName("status + name 추가")
		void statusAndName() {
			BooleanBuilder builder = new BooleanBuilder();
			builder.and(o.status.eq(OrderStatus.PENDING));
			builder.and(o.customerName.eq("Alpha"));
			assertThat(qf.selectFrom(o).where(builder).fetch()).hasSizeGreaterThanOrEqualTo(1);
		}

		@Test
		@DisplayName("3중 AND 조건")
		void tripleAnd() {
			BooleanBuilder builder = new BooleanBuilder();
			builder.and(o.status.eq(OrderStatus.CONFIRMED));
			builder.and(o.totalAmount.goe(new BigDecimal("5000")));
			builder.and(o.customerName.ne("Alpha"));
			List<OrderEntity> result = qf.selectFrom(o).where(builder).fetch();
			result.forEach(e -> {
				assertThat(e.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
				assertThat(e.getTotalAmount()).isGreaterThanOrEqualTo(new BigDecimal("5000"));
				assertThat(e.getCustomerName()).isNotEqualTo("Alpha");
			});
		}

		@Test
		@DisplayName("OR 조건 빌더")
		void orBuilder() {
			BooleanBuilder builder = new BooleanBuilder();
			builder.or(o.customerName.eq("Alpha"));
			builder.or(o.customerName.eq("Beta"));
			assertThat(qf.selectFrom(o).where(builder).fetch()).hasSize(8);
		}

		@Test
		@DisplayName("AND + OR 혼합")
		void andOr() {
			BooleanBuilder statusBuilder = new BooleanBuilder();
			statusBuilder.or(o.status.eq(OrderStatus.PENDING));
			statusBuilder.or(o.status.eq(OrderStatus.CONFIRMED));

			BooleanBuilder main = new BooleanBuilder();
			main.and(statusBuilder);
			main.and(o.totalAmount.goe(new BigDecimal("10000")));

			List<OrderEntity> result = qf.selectFrom(o).where(main).fetch();
			result.forEach(e -> {
				assertThat(e.getStatus()).isIn(OrderStatus.PENDING, OrderStatus.CONFIRMED);
				assertThat(e.getTotalAmount()).isGreaterThanOrEqualTo(new BigDecimal("10000"));
			});
		}
	}

	@Nested
	@DisplayName("파라미터화 조건 테스트")
	class ParameterizedConditions {

		@ParameterizedTest
		@EnumSource(OrderStatus.class)
		@DisplayName("각 status별 정확히 8건")
		void eachStatus(OrderStatus status) {
			assertThat(qf.selectFrom(o).where(o.status.eq(status)).fetch()).hasSize(8);
		}

		@ParameterizedTest
		@ValueSource(strings = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon",
				"Zeta", "Eta", "Theta", "Iota", "Kappa"})
		@DisplayName("각 이름별 4건")
		void eachName(String name) {
			assertThat(qf.selectFrom(o).where(o.customerName.eq(name)).fetch()).hasSize(4);
		}

		@ParameterizedTest
		@CsvSource({
				"1000, 40", "10000, 31", "20000, 21", "30000, 11", "40000, 1"
		})
		@DisplayName("최소 금액별 건수")
		void minAmountCounts(String min, int expected) {
			long count = qf.selectFrom(o)
					.where(o.totalAmount.goe(new BigDecimal(min)))
					.stream().count();
			assertThat(count).isEqualTo(expected);
		}
	}

	@Nested
	@DisplayName("LIKE 패턴 매칭")
	class LikePatterns {

		@Test
		@DisplayName("startsWith 'A'")
		void startsA() {
			assertThat(qf.selectFrom(o).where(o.customerName.startsWith("A")).fetch())
					.hasSize(4); // Alpha
		}

		@Test
		@DisplayName("endsWith 'a'")
		void endsA() {
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.customerName.endsWithIgnoreCase("a")).fetch();
			// Alpha, Beta, Gamma, Delta, Epsilon, Zeta, Eta, Theta, Iota, Kappa
			assertThat(result).hasSizeGreaterThanOrEqualTo(4);
		}

		@Test
		@DisplayName("contains 'eta' (case insensitive)")
		void containsEta() {
			assertThat(qf.selectFrom(o)
					.where(o.customerName.containsIgnoreCase("eta")).fetch())
					.hasSizeGreaterThanOrEqualTo(8); // Beta(4) + Zeta(4) + Eta(4) + Theta(4)
		}

		@Test
		@DisplayName("NOT LIKE")
		void notLike() {
			assertThat(qf.selectFrom(o)
					.where(o.customerName.notLike("Alpha")).fetch())
					.hasSize(36); // 40 - 4
		}
	}

	@Nested
	@DisplayName("날짜 조건 쿼리")
	class DateConditions {

		@Test
		@DisplayName("특정 월 이후 주문")
		void afterMonth() {
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.orderedAt.after(LocalDateTime.of(2026, 6, 1, 0, 0)))
					.fetch();
			result.forEach(e ->
					assertThat(e.getOrderedAt()).isAfter(LocalDateTime.of(2026, 6, 1, 0, 0)));
		}

		@Test
		@DisplayName("특정 기간 내 주문")
		void betweenDates() {
			List<OrderEntity> result = qf.selectFrom(o)
					.where(o.orderedAt.between(
							LocalDateTime.of(2026, 3, 1, 0, 0),
							LocalDateTime.of(2026, 5, 31, 23, 59)))
					.fetch();
			assertThat(result).isNotEmpty();
		}
	}

	@Nested
	@DisplayName("복합 정렬 + 페이징")
	class SortPaging {

		@Test
		@DisplayName("status ASC, amount DESC, name ASC 3중 정렬")
		void tripleSort() {
			List<OrderEntity> result = qf.selectFrom(o)
					.orderBy(o.status.asc(), o.totalAmount.desc(), o.customerName.asc())
					.fetch();
			assertThat(result).hasSize(40);
		}

		@Test
		@DisplayName("정렬 + 페이징 조합")
		void sortWithPaging() {
			List<OrderEntity> page = qf.selectFrom(o)
					.orderBy(o.totalAmount.desc())
					.offset(10).limit(5)
					.fetch();
			assertThat(page).hasSize(5);
			for (int i = 0; i < page.size() - 1; i++) {
				assertThat(page.get(i).getTotalAmount())
						.isGreaterThanOrEqualTo(page.get(i + 1).getTotalAmount());
			}
		}

		@ParameterizedTest
		@ValueSource(ints = {1, 5, 10, 20, 40})
		@DisplayName("다양한 페이지 사이즈")
		void variousPageSize(int size) {
			List<OrderEntity> page = qf.selectFrom(o)
					.orderBy(o.id.asc())
					.offset(0).limit(size)
					.fetch();
			assertThat(page).hasSize(Math.min(size, 40));
		}
	}
}
