package kr.co.demo.spring.boot.starter.web;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.demo.core.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

	@Nested
	@DisplayName("success 응답")
	class SuccessTests {

		@Test
		@DisplayName("데이터 포함 성공 응답")
		void successWithData() {
			ApiResponse<String> res = ApiResponse.success("hello");
			assertThat(res.isSuccess()).isTrue();
			assertThat(res.getData()).isEqualTo("hello");
			assertThat(res.getError()).isNull();
		}

		@Test
		@DisplayName("데이터 없는 성공 응답")
		void successNoData() {
			ApiResponse<Void> res = ApiResponse.success();
			assertThat(res.isSuccess()).isTrue();
			assertThat(res.getData()).isNull();
			assertThat(res.getError()).isNull();
		}

		@Test
		@DisplayName("null 데이터 성공 응답")
		void successNullData() {
			ApiResponse<Object> res = ApiResponse.success(null);
			assertThat(res.isSuccess()).isTrue();
			assertThat(res.getData()).isNull();
		}

		@Test
		@DisplayName("복합 타입 데이터")
		void successComplexData() {
			record TestDto(Long id, String name) {}
			ApiResponse<TestDto> res = ApiResponse.success(new TestDto(1L, "test"));
			assertThat(res.getData().id()).isEqualTo(1L);
			assertThat(res.getData().name()).isEqualTo("test");
		}
	}

	@Nested
	@DisplayName("fail 응답")
	class FailTests {

		@Test
		@DisplayName("ErrorCode로 실패 응답")
		void failWithErrorCode() {
			ApiResponse<Void> res = ApiResponse.fail(CommonErrorCode.NOT_FOUND);
			assertThat(res.isSuccess()).isFalse();
			assertThat(res.getData()).isNull();
			assertThat(res.getError()).isNotNull();
			assertThat(res.getError().getCode()).isEqualTo("C003");
			assertThat(res.getError().getMessage()).isEqualTo("리소스를 찾을 수 없습니다.");
		}

		@Test
		@DisplayName("ErrorCode + 커스텀 메시지로 실패 응답")
		void failWithCustomMessage() {
			ApiResponse<Void> res = ApiResponse.fail(
					CommonErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다.");
			assertThat(res.getError().getCode()).isEqualTo("C003");
			assertThat(res.getError().getMessage()).isEqualTo("주문을 찾을 수 없습니다.");
		}

		@Test
		@DisplayName("커스텀 메시지 null이면 ErrorCode 메시지 사용")
		void failNullCustomMessage() {
			ApiResponse<Void> res = ApiResponse.fail(CommonErrorCode.NOT_FOUND, null);
			assertThat(res.getError().getMessage()).isEqualTo("리소스를 찾을 수 없습니다.");
		}

		@Test
		@DisplayName("직접 코드/메시지 지정")
		void failWithCodeAndMessage() {
			ApiResponse<Void> res = ApiResponse.fail("CUSTOM_001", "커스텀 에러");
			assertThat(res.getError().getCode()).isEqualTo("CUSTOM_001");
			assertThat(res.getError().getMessage()).isEqualTo("커스텀 에러");
		}

		@Test
		@DisplayName("모든 CommonErrorCode로 실패 응답 생성")
		void failAllErrorCodes() {
			for (CommonErrorCode code : CommonErrorCode.values()) {
				ApiResponse<Void> res = ApiResponse.fail(code);
				assertThat(res.isSuccess()).isFalse();
				assertThat(res.getError().getCode()).isEqualTo(code.getCode());
			}
		}
	}
}
