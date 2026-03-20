package kr.co.demo.client.processor.util;

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
			"customerName, customer_name"
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
			"a, A"
	})
	@DisplayName("capitalize")
	void capitalize(String input, String expected) {
		assertThat(NamingUtils.capitalize(input)).isEqualTo(expected);
	}

	@Test
	@DisplayName("capitalize null/empty")
	void capitalizeEdge() {
		assertThat(NamingUtils.capitalize(null)).isNull();
		assertThat(NamingUtils.capitalize("")).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
			"Order, order",
			"a, a"
	})
	@DisplayName("uncapitalize")
	void uncapitalize(String input, String expected) {
		assertThat(NamingUtils.uncapitalize(input)).isEqualTo(expected);
	}

	@Test
	@DisplayName("uncapitalize null/empty")
	void uncapitalizeEdge() {
		assertThat(NamingUtils.uncapitalize(null)).isNull();
		assertThat(NamingUtils.uncapitalize("")).isEmpty();
	}
}
