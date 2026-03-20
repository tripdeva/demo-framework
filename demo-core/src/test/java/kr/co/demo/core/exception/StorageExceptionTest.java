package kr.co.demo.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StorageExceptionTest {

	@Test
	@DisplayName("of(message) 팩토리")
	void ofMessage() {
		StorageException ex = StorageException.of("generic error");
		assertThat(ex.getCode()).isEqualTo(StorageException.GENERAL);
		assertThat(ex.getMessage()).isEqualTo("generic error");
	}

	@Test
	@DisplayName("of(message, cause) 팩토리")
	void ofMessageWithCause() {
		RuntimeException cause = new RuntimeException("db down");
		StorageException ex = StorageException.of("failed", cause);
		assertThat(ex.getCode()).isEqualTo(StorageException.GENERAL);
		assertThat(ex.getCause()).isEqualTo(cause);
	}

	@Test
	@DisplayName("saveFailed 팩토리")
	void saveFailed() {
		RuntimeException cause = new RuntimeException();
		StorageException ex = StorageException.saveFailed("Order", cause);
		assertThat(ex.getCode()).isEqualTo(StorageException.SAVE_FAILED);
		assertThat(ex.getMessage()).contains("Order");
		assertThat(ex.getCause()).isEqualTo(cause);
	}

	@Test
	@DisplayName("notFound 팩토리")
	void notFound() {
		StorageException ex = StorageException.notFound("Member", "user1");
		assertThat(ex.getCode()).isEqualTo(StorageException.NOT_FOUND);
		assertThat(ex.getMessage()).contains("Member");
		assertThat(ex.getMessage()).contains("user1");
	}

	@Test
	@DisplayName("deleteFailed 팩토리")
	void deleteFailed() {
		RuntimeException cause = new RuntimeException();
		StorageException ex = StorageException.deleteFailed("Order", cause);
		assertThat(ex.getCode()).isEqualTo(StorageException.DELETE_FAILED);
		assertThat(ex.getMessage()).contains("Order");
	}

	@Test
	@DisplayName("duplicate 팩토리")
	void duplicate() {
		StorageException ex = StorageException.duplicate("Member", "username", "admin");
		assertThat(ex.getCode()).isEqualTo(StorageException.DUPLICATE);
		assertThat(ex.getMessage()).contains("Member");
		assertThat(ex.getMessage()).contains("username");
		assertThat(ex.getMessage()).contains("admin");
	}

	@Test
	@DisplayName("상수 값 확인")
	void constants() {
		assertThat(StorageException.SAVE_FAILED).isEqualTo("STORAGE_SAVE_FAILED");
		assertThat(StorageException.NOT_FOUND).isEqualTo("STORAGE_NOT_FOUND");
		assertThat(StorageException.DELETE_FAILED).isEqualTo("STORAGE_DELETE_FAILED");
		assertThat(StorageException.DUPLICATE).isEqualTo("STORAGE_DUPLICATE");
		assertThat(StorageException.GENERAL).isEqualTo("STORAGE_ERROR");
	}

	@Test
	@DisplayName("DomainException을 상속")
	void inheritance() {
		StorageException ex = StorageException.of("test");
		assertThat(ex).isInstanceOf(DomainException.class);
		assertThat(ex).isInstanceOf(RuntimeException.class);
	}
}
