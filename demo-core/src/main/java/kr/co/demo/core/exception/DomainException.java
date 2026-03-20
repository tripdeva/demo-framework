package kr.co.demo.core.exception;

/**
 * 도메인 레이어 공통 예외
 */
public class DomainException extends RuntimeException {

	private final String code;

	/**
	 * 생성자
	 *
	 * @param code    에러 코드
	 * @param message 예외 메세지
	 */
	public DomainException(String code, String message) {
		super(message);
		this.code = code;
	}

	/**
	 * 생성자
	 *
	 * @param code    에러 코드
	 * @param message 예외 메세지
	 * @param cause   예외 trace
	 */
	public DomainException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * 에러 코드 getter
	 *
	 * @return 에러 코드
	 */
	public String getCode() {
		return this.code;
	}
}
