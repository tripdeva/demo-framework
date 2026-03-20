package kr.co.demo.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainExceptionTest {

	@Test
	@DisplayName("코드와 메시지로 생성")
	void createWithCodeAndMessage() {
		DomainException ex = new DomainException("ERR001", "test error");
		assertThat(ex.getCode()).isEqualTo("ERR001");
		assertThat(ex.getMessage()).isEqualTo("test error");
		assertThat(ex.getCause()).isNull();
	}

	@Test
	@DisplayName("코드, 메시지, 원인으로 생성")
	void createWithCause() {
		RuntimeException cause = new RuntimeException("root cause");
		DomainException ex = new DomainException("ERR002", "wrapped", cause);
		assertThat(ex.getCode()).isEqualTo("ERR002");
		assertThat(ex.getMessage()).isEqualTo("wrapped");
		assertThat(ex.getCause()).isEqualTo(cause);
	}
}
