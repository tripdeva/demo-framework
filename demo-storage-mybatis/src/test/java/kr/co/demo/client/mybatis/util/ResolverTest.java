package kr.co.demo.client.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import kr.co.demo.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResolverTest {

	@Test
	@DisplayName("StorageTable에서 테이블명 해석")
	void resolveTable() {
		String table = Resolver.resolveTable(Order.class);
		assertThat(table).isEqualTo("orders");
	}

	@Test
	@DisplayName("StorageId 필드 탐색")
	void resolveIdField() {
		Field idField = Resolver.resolveIdField(Order.class);
		assertThat(idField).isNotNull();
		assertThat(idField.getName()).isEqualTo("id");
	}

	@Test
	@DisplayName("StorageColumn 명시적 컬럼명")
	void resolveExplicitColumn() throws Exception {
		Field field = Order.class.getDeclaredField("orderNumber");
		String column = Resolver.resolveColumn(field);
		assertThat(column).isEqualTo("order_no");
	}

	@Test
	@DisplayName("StorageColumn 없는 필드는 snake_case 변환")
	void resolveImplicitColumn() throws Exception {
		Field field = Order.class.getDeclaredField("orderedAt");
		String column = Resolver.resolveColumn(field);
		assertThat(column).isEqualTo("ordered_at");
	}

	@Test
	@DisplayName("camelToSnake 변환")
	void camelToSnake() {
		assertThat(Resolver.camelToSnake("orderNumber")).isEqualTo("order_number");
		assertThat(Resolver.camelToSnake("id")).isEqualTo("id");
		assertThat(Resolver.camelToSnake("totalAmount")).isEqualTo("total_amount");
	}

	@Test
	@DisplayName("resolveField로 필드 조회")
	void resolveField() {
		Field field = Resolver.resolveField(Order.class, "customerName");
		assertThat(field).isNotNull();
		assertThat(field.getName()).isEqualTo("customerName");
	}
}
