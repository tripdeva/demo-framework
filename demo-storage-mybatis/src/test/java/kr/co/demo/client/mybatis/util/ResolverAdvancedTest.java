package kr.co.demo.client.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import kr.co.demo.core.storage.annotation.StorageColumn;
import kr.co.demo.core.storage.annotation.StorageId;
import kr.co.demo.core.storage.annotation.StorageTable;
import kr.co.demo.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ResolverAdvancedTest {

	@Nested
	@DisplayName("resolveTable 상세")
	class ResolveTableTests {

		@Test
		@DisplayName("Order → 'orders'")
		void explicitTableName() {
			assertThat(Resolver.resolveTable(Order.class)).isEqualTo("orders");
		}

		@StorageTable
		static class NoNameDomain {}

		@Test
		@DisplayName("@StorageTable 값 비어있으면 빈 문자열 반환")
		void emptyTableName() {
			assertThat(Resolver.resolveTable(NoNameDomain.class)).isEmpty();
		}

		@Test
		@DisplayName("@StorageTable 없는 클래스 → 예외")
		void noAnnotation() {
			assertThatThrownBy(() -> Resolver.resolveTable(String.class))
					.isInstanceOf(Exception.class);
		}
	}

	@Nested
	@DisplayName("resolveIdField 상세")
	class ResolveIdTests {

		@Test
		@DisplayName("Order의 ID 필드 = id")
		void orderId() {
			Field f = Resolver.resolveIdField(Order.class);
			assertThat(f.getName()).isEqualTo("id");
		}

		@Test
		@DisplayName("ID 필드에 @StorageId 존재")
		void hasAnnotation() {
			Field f = Resolver.resolveIdField(Order.class);
			assertThat(f.getAnnotation(StorageId.class)).isNotNull();
		}

		static class NoIdDomain {
			private String name;
		}

		@Test
		@DisplayName("@StorageId 없는 클래스 → 예외")
		void noIdField() {
			assertThatThrownBy(() -> Resolver.resolveIdField(NoIdDomain.class))
					.isInstanceOf(IllegalStateException.class);
		}
	}

	@Nested
	@DisplayName("resolveColumn 상세")
	class ResolveColumnTests {

		@Test
		@DisplayName("명시적 컬럼 order_no")
		void explicitColumn() throws Exception {
			Field f = Order.class.getDeclaredField("orderNumber");
			assertThat(Resolver.resolveColumn(f)).isEqualTo("order_no");
		}

		@Test
		@DisplayName("@StorageColumn nullable 속성")
		void nullableAttr() throws Exception {
			Field f = Order.class.getDeclaredField("orderNumber");
			StorageColumn col = f.getAnnotation(StorageColumn.class);
			assertThat(col.nullable()).isFalse();
		}

		@Test
		@DisplayName("@StorageColumn 없으면 snake_case")
		void noAnnotation() throws Exception {
			Field f = Order.class.getDeclaredField("orderedAt");
			assertThat(Resolver.resolveColumn(f)).isEqualTo("ordered_at");
		}
	}

	@Nested
	@DisplayName("camelToSnake 상세")
	class CamelToSnakeTests {

		@ParameterizedTest
		@CsvSource({
				"id, id",
				"orderNumber, order_number",
				"customerName, customer_name",
				"totalAmount, total_amount",
				"orderedAt, ordered_at",
				"a, a",
				"aB, a_b",
				"abCdEf, ab_cd_ef"
		})
		@DisplayName("camelCase → snake_case 변환")
		void convert(String input, String expected) {
			assertThat(Resolver.camelToSnake(input)).isEqualTo(expected);
		}
	}

	@Nested
	@DisplayName("resolveField 상세")
	class ResolveFieldTests {

		@Test
		@DisplayName("존재하는 필드 조회")
		void existingField() {
			Field f = Resolver.resolveField(Order.class, "customerName");
			assertThat(f).isNotNull();
			assertThat(f.getType()).isEqualTo(String.class);
		}

		@Test
		@DisplayName("존재하지 않는 필드 → 예외")
		void nonExistingField() {
			assertThatThrownBy(() -> Resolver.resolveField(Order.class, "nonExistent"))
					.isInstanceOf(Exception.class);
		}
	}
}
