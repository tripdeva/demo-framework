package kr.co.demo.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kr.co.demo.core.exception.CommonErrorCode;
import kr.co.demo.spring.boot.starter.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * JWT 인증 실패 시 401 응답.
 *
 * @author demo-framework
 * @since 1.1.0
 */
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void commence(HttpServletRequest request,
	                      HttpServletResponse response,
	                      AuthenticationException authException) throws IOException {
		log.warn("인증 실패: {}", authException.getMessage());
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ApiResponse<Void> body = ApiResponse.fail(CommonErrorCode.UNAUTHORIZED);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
