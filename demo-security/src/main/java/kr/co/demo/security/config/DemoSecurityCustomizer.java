package kr.co.demo.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * SecurityFilterChain 커스터마이저.
 *
 * <p>소비자 프로젝트에서 빈으로 등록하면 기본 SecurityFilterChain에
 * 추가 설정을 적용할 수 있습니다.
 *
 * <p>사용 예시:
 * <pre>{@code
 * @Bean
 * public DemoSecurityCustomizer securityCustomizer() {
 *     return http -> http
 *         .authorizeHttpRequests(auth -> auth
 *             .requestMatchers("/admin/**").hasRole("ADMIN")
 *             .requestMatchers("/api/**").hasRole("USER"));
 * }
 * }</pre>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@FunctionalInterface
public interface DemoSecurityCustomizer {

	/**
	 * HttpSecurity에 추가 설정을 적용한다.
	 *
	 * @param http HttpSecurity
	 * @throws Exception 설정 실패 시
	 */
	void customize(HttpSecurity http) throws Exception;
}
