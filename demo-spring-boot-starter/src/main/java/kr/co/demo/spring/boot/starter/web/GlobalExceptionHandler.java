package kr.co.demo.spring.boot.starter.web;

import kr.co.demo.core.exception.BaseException;
import kr.co.demo.core.exception.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 전역 예외 처리 핸들러.
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 404 Not Found 처리.
	 */
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
		log.warn("NoHandlerFoundException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiResponse.fail(CommonErrorCode.NOT_FOUND));
	}

	/**
	 * 요청 바디 파싱 실패.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
		log.warn("HttpMessageNotReadableException: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE,
						"요청 바디의 형식이 잘못되었습니다."));
	}

	/**
	 * HTTP 메서드 불일치.
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
			HttpRequestMethodNotSupportedException e) {
		log.warn("MethodNotAllowed: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(ApiResponse.fail(CommonErrorCode.METHOD_NOT_ALLOWED));
	}

	/**
	 * BaseException 처리.
	 */
	@ExceptionHandler(BaseException.class)
	public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
		log.warn("BaseException: {}", e.getMessage());
		return ResponseEntity.status(e.getErrorCode().getStatusCode())
				.body(ApiResponse.fail(e.getErrorCode(), e.getCustomMessage()));
	}

	/**
	 * 타입 변환 실패.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
			MethodArgumentTypeMismatchException e) {
		log.warn("타입 변환 실패: param={}, value={}", e.getPropertyName(), e.getValue());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(CommonErrorCode.INVALID_TYPE_VALUE));
	}

	/**
	 * Validation 실패.
	 */
	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ApiResponse<Void>> handleValidation(BindException e) {
		log.warn("ValidationException: {}", e.getMessage());
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.findFirst().orElse("유효성 검증 실패");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE, message));
	}

	/**
	 * 필수 헤더 누락.
	 */
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingHeader(
			MissingRequestHeaderException e) {
		log.warn("MissingHeader: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE,
						"필수 헤더가 누락되었습니다."));
	}

	/**
	 * 필수 파라미터 누락.
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingParam(
			MissingServletRequestParameterException e) {
		log.warn("MissingParam: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE,
						"필수 파라미터가 누락되었습니다."));
	}

	/**
	 * 미지원 미디어 타입.
	 */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
			HttpMediaTypeNotSupportedException e) {
		log.warn("UnsupportedMediaType: {}", e.getMessage());
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.body(ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE,
						"지원하지 않는 미디어 타입입니다."));
	}

	/**
	 * 예상치 못한 예외.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
		log.error("예상치 못한 서버 장애가 발생했습니다.", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.fail(CommonErrorCode.INTERNAL_SERVER_ERROR));
	}
}
