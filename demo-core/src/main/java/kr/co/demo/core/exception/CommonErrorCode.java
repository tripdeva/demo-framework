package kr.co.demo.core.exception;

/**
 * 공통 에러 코드.
 *
 * @author demo-framework
 * @since 1.1.0
 */
public enum CommonErrorCode implements ErrorCode {

	INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
	METHOD_NOT_ALLOWED(405, "C002", "지원하지 않는 HTTP 메서드입니다."),
	NOT_FOUND(404, "C003", "리소스를 찾을 수 없습니다."),
	INTERNAL_SERVER_ERROR(500, "C004", "서버 오류가 발생했습니다."),
	INVALID_TYPE_VALUE(400, "C005", "잘못된 타입의 값입니다."),
	UNAUTHORIZED(401, "C006", "인증이 필요합니다."),
	ACCESS_DENIED(403, "C007", "접근 권한이 없습니다.");

	private final int statusCode;
	private final String code;
	private final String message;

	CommonErrorCode(int statusCode, String code, String message) {
		this.statusCode = statusCode;
		this.code = code;
		this.message = message;
	}

	@Override
	public int getStatusCode() {
		return statusCode;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
