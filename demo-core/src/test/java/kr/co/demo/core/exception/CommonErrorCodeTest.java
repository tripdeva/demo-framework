package kr.co.demo.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CommonErrorCodeTest {

	@ParameterizedTest
	@EnumSource(CommonErrorCode.class)
	@DisplayName("모든 ErrorCode가 ErrorCode 인터페이스 구현")
	void implementsInterface(CommonErrorCode code) {
		assertThat(code).isInstanceOf(ErrorCode.class);
	}

	@ParameterizedTest
	@EnumSource(CommonErrorCode.class)
	@DisplayName("모든 ErrorCode에 코드/메시지/상태 존재")
	void hasAllFields(CommonErrorCode code) {
		assertThat(code.getCode()).isNotBlank();
		assertThat(code.getMessage()).isNotBlank();
		assertThat(code.getStatusCode()).isGreaterThan(0);
	}

	@Test
	@DisplayName("INVALID_INPUT_VALUE = 400")
	void invalidInput() {
		assertThat(CommonErrorCode.INVALID_INPUT_VALUE.getStatusCode()).isEqualTo(400);
		assertThat(CommonErrorCode.INVALID_INPUT_VALUE.getCode()).isEqualTo("C001");
	}

	@Test
	@DisplayName("NOT_FOUND = 404")
	void notFound() {
		assertThat(CommonErrorCode.NOT_FOUND.getStatusCode()).isEqualTo(404);
		assertThat(CommonErrorCode.NOT_FOUND.getCode()).isEqualTo("C003");
	}

	@Test
	@DisplayName("UNAUTHORIZED = 401")
	void unauthorized() {
		assertThat(CommonErrorCode.UNAUTHORIZED.getStatusCode()).isEqualTo(401);
		assertThat(CommonErrorCode.UNAUTHORIZED.getCode()).isEqualTo("C006");
	}

	@Test
	@DisplayName("ACCESS_DENIED = 403")
	void accessDenied() {
		assertThat(CommonErrorCode.ACCESS_DENIED.getStatusCode()).isEqualTo(403);
		assertThat(CommonErrorCode.ACCESS_DENIED.getCode()).isEqualTo("C007");
	}

	@Test
	@DisplayName("INTERNAL_SERVER_ERROR = 500")
	void internalError() {
		assertThat(CommonErrorCode.INTERNAL_SERVER_ERROR.getStatusCode()).isEqualTo(500);
		assertThat(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode()).isEqualTo("C004");
	}

	@Test
	@DisplayName("총 7개 에러 코드")
	void totalCount() {
		assertThat(CommonErrorCode.values()).hasSize(7);
	}
}
