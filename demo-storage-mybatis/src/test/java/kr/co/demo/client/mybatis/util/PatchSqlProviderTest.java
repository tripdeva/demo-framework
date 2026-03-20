package kr.co.demo.client.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.co.demo.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PatchSqlProviderTest {

	@Test
	@DisplayName("Patch SQL 생성 - 일반 값")
	void buildPatchSql() {
		Patch<Order> patch = Patch.create(Order.class, 1L,
				PatchValue.of("orderNumber", "NEW-001"),
				PatchValue.of("customerName", "Test User"));

		String sql = PatchSqlProvider.build(patch);

		assertThat(sql).contains("UPDATE orders");
		assertThat(sql).contains("SET");
		assertThat(sql).contains("order_no");
		assertThat(sql).contains("customer_name");
		assertThat(sql).contains("WHERE id = #{id}");
	}

	@Test
	@DisplayName("Patch SQL 생성 - NULL 값 (nullable 필드)")
	void buildPatchSqlWithNull() {
		Patch<Order> patch = Patch.create(Order.class, 1L,
				PatchValue.of("orderedAt", null));

		String sql = PatchSqlProvider.build(patch);

		assertThat(sql).contains("ordered_at = NULL");
	}

	@Test
	@DisplayName("not-null 필드에 NULL 전달 시 예외")
	void buildPatchSqlNullOnNotNull() {
		Patch<Order> patch = Patch.create(Order.class, 1L,
				PatchValue.of("orderNumber", null));

		assertThatThrownBy(() -> PatchSqlProvider.build(patch))
				.isInstanceOf(IllegalStateException.class);
	}
}
