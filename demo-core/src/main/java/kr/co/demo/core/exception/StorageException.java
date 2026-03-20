package kr.co.demo.core.exception;

/**
 * 영속성 계층에서 발생하는 예외를 도메인 예외로 변환하는 클래스
 *
 * <p>JPA, MyBatis, JDBC 등 영속성 기술에서 발생하는 예외를
 * 도메인 계층에서 처리할 수 있는 형태로 변환합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * try {
 *     return repository.save(entity);
 * } catch (DataAccessException e) {
 *     throw StorageException.saveFailed("Order", e);
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.0.0
 * @see DomainException
 */
public class StorageException extends DomainException {

	/** 저장 실패 에러 코드 */
	public static final String SAVE_FAILED = "STORAGE_SAVE_FAILED";

	/** 조회 실패 에러 코드 */
	public static final String NOT_FOUND = "STORAGE_NOT_FOUND";

	/** 삭제 실패 에러 코드 */
	public static final String DELETE_FAILED = "STORAGE_DELETE_FAILED";

	/** 중복 에러 코드 */
	public static final String DUPLICATE = "STORAGE_DUPLICATE";

	/** 일반 에러 코드 */
	public static final String GENERAL = "STORAGE_ERROR";

	/**
	 * StorageException 생성자
	 *
	 * @param code    에러 코드
	 * @param message 예외 메시지
	 */
	public StorageException(String code, String message) {
		super(code, message);
	}

	/**
	 * StorageException 생성자 (원인 예외 포함)
	 *
	 * @param code    에러 코드
	 * @param message 예외 메시지
	 * @param cause   원인 예외
	 */
	public StorageException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}

	/**
	 * StorageException을 생성하는 정적 팩토리 메서드
	 *
	 * @param message 예외 메시지
	 * @return StorageException 인스턴스
	 */
	public static StorageException of(String message) {
		return new StorageException(GENERAL, message);
	}

	/**
	 * StorageException을 생성하는 정적 팩토리 메서드 (원인 예외 포함)
	 *
	 * @param message 예외 메시지
	 * @param cause   원인 예외
	 * @return StorageException 인스턴스
	 */
	public static StorageException of(String message, Throwable cause) {
		return new StorageException(GENERAL, message, cause);
	}

	/**
	 * 저장 실패 예외를 생성합니다.
	 *
	 * @param entityName 엔티티명
	 * @param cause      원인 예외
	 * @return StorageException 인스턴스
	 */
	public static StorageException saveFailed(String entityName, Throwable cause) {
		return new StorageException(SAVE_FAILED, entityName + " 저장에 실패했습니다.", cause);
	}

	/**
	 * 조회 실패 예외를 생성합니다.
	 *
	 * @param entityName 엔티티명
	 * @param id         조회 ID
	 * @return StorageException 인스턴스
	 */
	public static StorageException notFound(String entityName, Object id) {
		return new StorageException(NOT_FOUND, entityName + "을(를) 찾을 수 없습니다. ID: " + id);
	}

	/**
	 * 삭제 실패 예외를 생성합니다.
	 *
	 * @param entityName 엔티티명
	 * @param cause      원인 예외
	 * @return StorageException 인스턴스
	 */
	public static StorageException deleteFailed(String entityName, Throwable cause) {
		return new StorageException(DELETE_FAILED, entityName + " 삭제에 실패했습니다.", cause);
	}

	/**
	 * 중복 예외를 생성합니다.
	 *
	 * @param entityName 엔티티명
	 * @param fieldName  중복 필드명
	 * @param value      중복 값
	 * @return StorageException 인스턴스
	 */
	public static StorageException duplicate(String entityName, String fieldName, Object value) {
		return new StorageException(
				DUPLICATE,
				entityName + "의 " + fieldName + "이(가) 이미 존재합니다: " + value
		);
	}
}
