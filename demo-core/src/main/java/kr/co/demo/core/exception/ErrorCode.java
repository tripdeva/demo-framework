package kr.co.demo.core.exception;

/**
 * 에러 코드 인터페이스.
 *
 * <p>프로젝트별로 이 인터페이스를 구현하여 에러 코드를 정의합니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * public enum MyErrorCode implements ErrorCode {
 *     NOT_FOUND(404, "C003", "리소스를 찾을 수 없습니다.");
 *
 *     private final int status;
 *     private final String code;
 *     private final String message;
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
public interface ErrorCode {

	/**
	 * HTTP 상태 코드를 반환한다.
	 *
	 * @return HTTP 상태 코드
	 */
	int getStatusCode();

	/**
	 * 에러 코드 문자열을 반환한다.
	 *
	 * @return 에러 코드
	 */
	String getCode();

	/**
	 * 에러 메시지를 반환한다.
	 *
	 * @return 에러 메시지
	 */
	String getMessage();
}
