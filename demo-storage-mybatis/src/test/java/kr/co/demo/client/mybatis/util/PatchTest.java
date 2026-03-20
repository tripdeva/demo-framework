package kr.co.demo.client.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.co.demo.domain.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PatchTest {

	@Test
	@DisplayName("Patch 생성 성공")
	void createPatch() {
		Patch<Order> patch = Patch.create(
				Order.class, 1L,
				PatchValue.of("orderNumber", "NEW-001"),
				PatchValue.of("customerName", "test"));

		assertThat(patch.domainType()).isEqualTo(Order.class);
		assertThat(patch.id()).isEqualTo(1L);
		assertThat(patch.values()).hasSize(2);
	}

	@Test
	@DisplayName("ID가 null이면 예외")
	void createWithNullId() {
		assertThatThrownBy(() -> Patch.create(Order.class, null,
				PatchValue.of("orderNumber", "X")))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("PatchValue가 없으면 예외")
	void createWithNoValues() {
		assertThatThrownBy(() -> Patch.create(Order.class, 1L))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("PatchValue null 값 허용")
	void patchValueWithNull() {
		PatchValue<Object> pv = PatchValue.of("field", null);
		assertThat(pv.fieldName()).isEqualTo("field");
		assertThat(pv.value()).isNull();
		assertThat(pv.isNull()).isTrue();
	}

	@Test
	@DisplayName("PatchValue 일반 값")
	void patchValueWithValue() {
		PatchValue<String> pv = PatchValue.of("name", "hello");
		assertThat(pv.fieldName()).isEqualTo("name");
		assertThat(pv.value()).isEqualTo("hello");
		assertThat(pv.isNull()).isFalse();
	}
}
