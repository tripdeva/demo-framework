package kr.co.demo.security.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 보안 설정 프로퍼티.
 *
 * <pre>{@code
 * security:
 *   public-urls:
 *     - /public/**
 *     - /health
 *   cors:
 *     allowed-origins:
 *       - http://localhost:3000
 *     allowed-methods:
 *       - GET
 *       - POST
 *     allowed-headers:
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
	 * CORS 허용 설정.
	 */
	public record CorsProperties(
			List<String> allowedOrigins,
			List<String> allowedMethods,
			List<String> allowedHeaders,
			Boolean allowCredentials,
			Long maxAge) {
	}
}
