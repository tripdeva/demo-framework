package kr.co.demo.spring.boot.starter.web;

import kr.co.demo.core.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 API 응답 포맷.
 *
 * @param <T> 응답 데이터 타입
 * @author demo-framework
 * @since 1.1.0
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

	private boolean success;
	private T data;
	private ErrorResponse error;

	/**
	 * 성공 응답 (데이터 포함).
	 *
	 * @param data 응답 데이터
	 * @param <T>  데이터 타입
	 * @return 성공 응답
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data, null);
	}

	/**
	 * 성공 응답 (데이터 없음).
	 *
	 * @param <T> 데이터 타입
	 * @return 성공 응답
	 */
	public static <T> ApiResponse<T> success() {
		return new ApiResponse<>(true, null, null);
	}

	/**
	 * 실패 응답 (ErrorCode).
	 *
	 * @param errorCode 에러 코드
	 * @param <T>       데이터 타입
	 * @return 실패 응답
	 */
	public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
		return new ApiResponse<>(false, null,
				new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
	}

	/**
	 * 실패 응답 (ErrorCode + 커스텀 메시지).
	 *
	 * @param errorCode     에러 코드
	 * @param customMessage 커스텀 메시지
	 * @param <T>           데이터 타입
	 * @return 실패 응답
	 */
	public static <T> ApiResponse<T> fail(ErrorCode errorCode, String customMessage) {
		String msg = customMessage != null ? customMessage : errorCode.getMessage();
		return new ApiResponse<>(false, null,
				new ErrorResponse(errorCode.getCode(), msg));
	}

	/**
	 * 실패 응답 (직접 코드/메시지 지정).
	 *
	 * @param code    에러 코드 문자열
	 * @param message 에러 메시지
	 * @param <T>     데이터 타입
	 * @return 실패 응답
	 */
	public static <T> ApiResponse<T> fail(String code, String message) {
		return new ApiResponse<>(false, null, new ErrorResponse(code, message));
	}

	/**
	 * 에러 응답 상세.
	 */
	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	public static class ErrorResponse {

		private String code;
		private String message;
	}
}
