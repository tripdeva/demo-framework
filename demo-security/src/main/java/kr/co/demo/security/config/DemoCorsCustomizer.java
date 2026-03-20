package kr.co.demo.security.config;

import org.springframework.web.cors.CorsConfiguration;

/**
 * CORS 설정 커스터마이저.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Bean
 * public DemoCorsCustomizer corsCustomizer() {
 *     return config -> {
 *         config.addAllowedHeader("X-Custom-Header");
 *         config.setMaxAge(7200L);
 *     };
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@FunctionalInterface
public interface DemoCorsCustomizer {

	/**
	 * CorsConfiguration에 추가 설정을 적용한다.
	 *
	 * @param config CorsConfiguration
	 */
	void customize(CorsConfiguration config);
}
