package kr.co.demo.client.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PatchSqlAdvancedTest {

	@Nested
	@DisplayName("SET 절 검증")
	class SetClause {

		@Test
		@DisplayName("단일 필드 SET")
		void singleField() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("customerName", "New"));
			String sql = PatchSqlProvider.build(p);
			assertThat(sql).contains("customer_name = #{values[0].value}");
			assertThat(sql).doesNotContain("order_no");
		}

		@Test
		@DisplayName("3개 필드 SET")
		void threeFields() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("orderNumber", "X"),
					PatchValue.of("customerName", "Y"),
					PatchValue.of("totalAmount", new BigDecimal("99")));
			String sql = PatchSqlProvider.build(p);
			assertThat(sql).contains("order_no");
			assertThat(sql).contains("customer_name");
			assertThat(sql).contains("total_amount");
		}

		@Test
		@DisplayName("status enum 값 patch")
		void enumPatch() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("status", OrderStatus.SHIPPED));
			String sql = PatchSqlProvider.build(p);
			assertThat(sql).contains("status");
		}
	}

	@Nested
	@DisplayName("WHERE 절 검증")
	class WhereClause {

		@Test
		@DisplayName("WHERE id = #{id}")
		void whereClause() {
			Patch<Order> p = Patch.create(Order.class, 42L,
					PatchValue.of("customerName", "X"));
			String sql = PatchSqlProvider.build(p);
			assertThat(sql).contains("WHERE id = #{id}");
		}
	}

	@Nested
	@DisplayName("nullable 검증")
	class NullableValidation {

		@Test
		@DisplayName("nullable 필드에 null → columnName = NULL")
		void nullableFieldSetNull() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("status", null));
			String sql = PatchSqlProvider.build(p);
			assertThat(sql).contains("status = NULL");
		}

		@Test
		@DisplayName("nullable=false 필드에 null → 예외")
		void notNullableFieldSetNull() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("customerName", null));
			assertThatThrownBy(() -> PatchSqlProvider.build(p))
					.isInstanceOf(IllegalStateException.class);
		}

		@Test
		@DisplayName("orderedAt(암시적 nullable) null → NULL 리터럴")
		void implicitNullable() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("orderedAt", null));
			String sql = PatchSqlProvider.build(p);
			assertThat(sql).contains("ordered_at = NULL");
		}
	}

	@Nested
	@DisplayName("테이블/컬럼 해석")
	class TableColumn {

		@Test
		@DisplayName("UPDATE orders")
		void tableName() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("customerName", "X"));
			assertThat(PatchSqlProvider.build(p)).startsWith("UPDATE orders");
		}

		@Test
		@DisplayName("명시적 컬럼명 order_no 사용")
		void explicitColumn() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("orderNumber", "NEW"));
			assertThat(PatchSqlProvider.build(p)).contains("order_no");
		}

		@Test
		@DisplayName("암시적 컬럼명 total_amount 사용")
		void implicitColumn() {
			Patch<Order> p = Patch.create(Order.class, 1L,
					PatchValue.of("totalAmount", BigDecimal.TEN));
			assertThat(PatchSqlProvider.build(p)).contains("total_amount");
		}
	}
}
