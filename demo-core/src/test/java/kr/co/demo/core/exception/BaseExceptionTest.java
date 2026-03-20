package kr.co.demo.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseExceptionTest {

	@Test
	@DisplayName("ErrorCode로 생성")
	void createWithErrorCode() {
		BaseException ex = new BaseException(CommonErrorCode.NOT_FOUND);
		assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
		assertThat(ex.getMessage()).isEqualTo("리소스를 찾을 수 없습니다.");
		assertThat(ex.getCustomMessage()).isNull();
	}

	@Test
	@DisplayName("ErrorCode + 커스텀 메시지로 생성")
	void createWithCustomMessage() {
		BaseException ex = new BaseException(CommonErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다.");
		assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.NOT_FOUND);
		assertThat(ex.getMessage()).isEqualTo("주문을 찾을 수 없습니다.");
		assertThat(ex.getCustomMessage()).isEqualTo("주문을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("ErrorCode + cause로 생성")
	void createWithCause() {
		RuntimeException cause = new RuntimeException("DB error");
		BaseException ex = new BaseException(CommonErrorCode.INTERNAL_SERVER_ERROR, cause);
		assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
		assertThat(ex.getCause()).isEqualTo(cause);
		assertThat(ex.getCustomMessage()).isNull();
	}

	@Test
	@DisplayName("RuntimeException을 상속")
	void isRuntimeException() {
		BaseException ex = new BaseException(CommonErrorCode.UNAUTHORIZED);
		assertThat(ex).isInstanceOf(RuntimeException.class);
	}
}
