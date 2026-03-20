package kr.co.demo.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 프로퍼티.
 *
 * <pre>{@code
 * jwt:
 *   secret: my-secret-key-at-least-32-characters-long
 *   access-token-expiration: 3600000
 *   refresh-token-expiration: 86400000
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
		String secret,
		long accessTokenExpiration,
		long refreshTokenExpiration) {
}
