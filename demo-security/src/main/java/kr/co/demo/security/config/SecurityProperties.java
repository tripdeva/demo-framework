package kr.co.demo.security.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 보안 설정 프로퍼티.
 *
 * <p>소비자 프로젝트에서 반드시 설정해야 합니다. 기본값 없음.
 *
 * <pre>{@code
 * security:
 *   public-urls:
 *     - /public/**
 *     - /health
 *   cors:
 *     origins:
 *       - http://localhost:3000
 *     methods:
 *       - GET
 *       - POST
 *     headers:
 *       - Authorization
 *       - Content-Type
 *     allow-credentials: true
 *     max-age: 3600
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
		List<String> publicUrls,
		CorsProperties cors) {

	/**
	 * CORS 설정.
	 */
	public record CorsProperties(
			List<String> origins,
			List<String> methods,
			List<String> headers,
			Boolean allowCredentials,
			Long maxAge) {
	}
}
