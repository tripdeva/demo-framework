package kr.co.demo.security.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 보안 설정 프로퍼티.
 *
 * <pre>{@code
 * # 전체 비활성화 (인증 없는 프로젝트)
 * security:
 *   enabled: false
 *
 * # JWT + Stateless (기본)
 * security:
 *   enabled: true
 *   session-policy: stateless
 *   public-urls:
 *     - /public/**
 *   cors:
 *     enabled: true
 *     allowed-origins:
 *       - http://localhost:3000
 *     allowed-methods:
 *       - GET
 *       - POST
 *     allowed-headers:
 *       - Authorization
 *     allow-credentials: true
 *     preflight-cache-seconds: 3600
 *
 * # 세션 방식
 * security:
 *   session-policy: stateful
 *
 * # CORS 비활성화
 * security:
 *   cors:
 *     enabled: false
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
		/** Security 전체 활성화 여부 (기본: true). */
		Boolean enabled,
		/** 세션 정책: stateless / stateful / none (기본: stateless). */
		String sessionPolicy,
		/** 인증 면제 URL 패턴 목록. */
		List<String> publicUrls,
		/** CORS 설정. */
		CorsProperties cors) {

	/**
	 * Security 활성화 여부 (null이면 true).
	 *
	 * @return 활성화 여부
	 */
	public boolean isEnabled() {
		return enabled == null || enabled;
	}

	/**
	 * 세션 정책 (null이면 stateless).
	 *
	 * @return 세션 정책 문자열
	 */
	public String resolveSessionPolicy() {
		return sessionPolicy != null ? sessionPolicy : "stateless";
	}

	/**
	 * CORS 허용 설정.
	 */
	public record CorsProperties(
			/** CORS 활성화 여부 (기본: true). */
			Boolean enabled,
			List<String> allowedOrigins,
			List<String> allowedMethods,
			List<String> allowedHeaders,
			Boolean allowCredentials,
			Long preflightCacheSeconds) {

		/**
		 * CORS 활성화 여부 (null이면 true).
		 *
		 * @return 활성화 여부
		 */
		public boolean isEnabled() {
			return enabled == null || enabled;
		}
	}
}
