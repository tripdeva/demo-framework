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
 *   cors-urls:
 *     - http://localhost:3000
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@ConfigurationProperties(prefix = "security")
public record SecurityProperties(
		List<String> publicUrls,
		List<String> corsUrls) {
}
