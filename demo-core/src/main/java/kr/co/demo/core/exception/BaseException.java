package kr.co.demo.core.exception;

/**
 * 비즈니스 예외 기본 클래스.
 *
 * <p>{@link ErrorCode}를 기반으로 구조화된 예외를 제공합니다.
 *
 * @author demo-framework
 * @since 1.1.0
 */
public class BaseException extends RuntimeException {

	private final ErrorCode errorCode;
	private final String customMessage;

	/**
	 * ErrorCode로 생성한다.
	 *
	 * @param errorCode 에러 코드
	 */
	public BaseException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.customMessage = null;
	}

	/**
	 * ErrorCode + 커스텀 메시지로 생성한다.
	 *
	 * @param errorCode     에러 코드
	 * @param customMessage 커스텀 메시지
	 */
	public BaseException(ErrorCode errorCode, String customMessage) {
		super(customMessage);
		this.errorCode = errorCode;
		this.customMessage = customMessage;
	}

	/**
	 * ErrorCode + 원인 예외로 생성한다.
	 *
	 * @param errorCode 에러 코드
	 * @param cause     원인 예외
	 */
	public BaseException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
		this.customMessage = null;
	}

	/**
	 * 에러 코드를 반환한다.
	 *
	 * @return 에러 코드
	 */
	public ErrorCode getErrorCode() {
		return errorCode;
	}

	/**
	 * 커스텀 메시지를 반환한다.
	 *
	 * @return 커스텀 메시지 (없으면 null)
	 */
	public String getCustomMessage() {
		return customMessage;
	}
}
