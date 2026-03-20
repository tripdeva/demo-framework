package kr.co.demo.client.jpa.processor.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NamingUtilsTest {

	@ParameterizedTest
	@CsvSource({
			"orderNumber, order_number",
			"OrderItem, order_item",
			"id, id",
			"totalAmount, total_amount",
			"customerName, customer_name",
			"orderedAt, ordered_at"
	})
	@DisplayName("camelCase -> snake_case 변환")
	void toSnakeCase(String input, String expected) {
		assertThat(NamingUtils.toSnakeCase(input)).isEqualTo(expected);
	}

	@Test
	@DisplayName("toSnakeCase null/empty 처리")
	void toSnakeCaseEdge() {
		assertThat(NamingUtils.toSnakeCase(null)).isNull();
		assertThat(NamingUtils.toSnakeCase("")).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
			"order, Order",
			"Order, Order",
			"a, A"
	})
	@DisplayName("capitalize")
	void capitalize(String input, String expected) {
		assertThat(NamingUtils.capitalize(input)).isEqualTo(expected);
	}

	@Test
	@DisplayName("capitalize null/empty 처리")
	void capitalizeEdge() {
		assertThat(NamingUtils.capitalize(null)).isNull();
		assertThat(NamingUtils.capitalize("")).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
			"Order, order",
			"order, order",
			"A, a"
	})
	@DisplayName("uncapitalize")
	void uncapitalize(String input, String expected) {
		assertThat(NamingUtils.uncapitalize(input)).isEqualTo(expected);
	}

	@Test
	@DisplayName("uncapitalize null/empty 처리")
	void uncapitalizeEdge() {
		assertThat(NamingUtils.uncapitalize(null)).isNull();
		assertThat(NamingUtils.uncapitalize("")).isEmpty();
	}
}
