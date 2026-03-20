package kr.co.demo.security.config;

import kr.co.demo.security.filter.JwtAuthenticationEntryPoint;
import kr.co.demo.security.filter.JwtAuthenticationFilter;
import kr.co.demo.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 보안 자동 설정.
 *
 * <h2>시나리오별 설정</h2>
 *
 * <h3>인증 없는 프로젝트</h3>
 * <pre>{@code
 * security:
 *   enabled: false
 * }</pre>
 *
 * <h3>JWT + Stateless (기본)</h3>
 * <pre>{@code
 * security:
 *   session-policy: stateless
 * jwt:
 *   secret: my-key
 * }</pre>
 *
 * <h3>세션 방식</h3>
 * <pre>{@code
 * security:
 *   session-policy: stateful
 * }</pre>
 *
 * <h3>CORS 비활성화</h3>
 * <pre>{@code
 * security:
 *   cors:
 *     enabled: false
 * }</pre>
 *
 * <h2>커스터마이징</h2>
 * <ol>
 *   <li>application.yml 설정만 변경</li>
 *   <li>{@link DemoSecurityCustomizer} 빈 등록 (부분 확장)</li>
 *   <li>개별 빈 교체 ({@code @ConditionalOnMissingBean})</li>
 *   <li>{@code SecurityFilterChain} 직접 정의 (완전 오버라이드)</li>
 * </ol>
 *
 * @author demo-framework
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name =
		"org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
public class DemoSecurityAutoConfiguration {

	// ==================== JWT ====================

	/**
	 * JWT 토큰 제공자.
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "jwt", name = "secret")
	public JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
		return new JwtTokenProvider(properties);
	}

	/**
	 * JWT 인증 필터.
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "jwt", name = "secret")
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider) {
		return new JwtAuthenticationFilter(provider);
	}

	/**
	 * JWT 인증 실패 핸들러 (401 응답).
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "security", name = "enabled", matchIfMissing = true)
	public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
		return new JwtAuthenticationEntryPoint();
	}

	// ==================== 공통 ====================

	/**
	 * 비밀번호 인코더 (기본: BCrypt).
	 */
	@Bean
	@ConditionalOnMissingBean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// ==================== CORS ====================

	/**
	 * CORS 설정.
	 *
	 * <p>{@code security.cors.enabled: false}이면 빈 자체를 생성하지 않습니다.
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "security", name = "enabled", matchIfMissing = true)
	public CorsConfigurationSource corsConfigurationSource(
			SecurityProperties properties,
			ObjectProvider<DemoCorsCustomizer> customizers) {

		CorsConfiguration config = new CorsConfiguration();
		SecurityProperties.CorsProperties cors = properties.cors();

		// CORS가 비활성화되었으면 빈 설정 반환
		if (cors != null && !cors.isEnabled()) {
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			return source;
		}

		if (cors != null) {
			if (cors.allowedOrigins() != null) {
				config.setAllowedOrigins(cors.allowedOrigins());
			}
			if (cors.allowedMethods() != null) {
				config.setAllowedMethods(cors.allowedMethods());
			}
			if (cors.allowedHeaders() != null) {
				config.setAllowedHeaders(cors.allowedHeaders());
			}
			if (cors.allowCredentials() != null) {
				config.setAllowCredentials(cors.allowCredentials());
			}
			if (cors.preflightCacheSeconds() != null) {
				config.setMaxAge(cors.preflightCacheSeconds());
			}
		}

		customizers.orderedStream().forEach(c -> c.customize(config));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	// ==================== SecurityFilterChain ====================

	/**
	 * Security 비활성화 시 모든 요청 허용.
	 *
	 * <p>{@code security.enabled: false}이면 이 빈이 활성화됩니다.
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "false")
	public SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}

	/**
	 * 기본 SecurityFilterChain.
	 *
	 * <p>{@code security.enabled: true} (기본)일 때 활성화.
	 * 세션 정책은 {@code security.session-policy}로 제어:
	 * <ul>
	 *   <li>{@code stateless} (기본) — JWT용, 세션 미사용</li>
	 *   <li>{@code stateful} — 세션 기반 인증</li>
	 *   <li>{@code none} — 세션 정책 설정 안 함 (Spring 기본값)</li>
	 * </ul>
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "security", name = "enabled", matchIfMissing = true)
	public SecurityFilterChain demoSecurityFilterChain(
			HttpSecurity http,
			SecurityProperties properties,
			ObjectProvider<CorsConfigurationSource> corsSource,
			ObjectProvider<JwtAuthenticationEntryPoint> entryPoint,
			ObjectProvider<JwtAuthenticationFilter> jwtFilter,
			ObjectProvider<DemoSecurityCustomizer> customizers) throws Exception {

		// CSRF
		http.csrf(AbstractHttpConfigurer::disable);

		// CORS
		boolean corsEnabled = properties.cors() == null || properties.cors().isEnabled();
		if (corsEnabled) {
			CorsConfigurationSource source = corsSource.getIfAvailable();
			if (source != null) {
				http.cors(cors -> cors.configurationSource(source));
			}
		} else {
			http.cors(AbstractHttpConfigurer::disable);
		}

		// 세션 정책
		String sessionPolicy = properties.resolveSessionPolicy();
		switch (sessionPolicy) {
			case "stateless" -> http.sessionManagement(session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
			case "stateful" -> http.sessionManagement(session ->
					session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
			case "none" -> { /* Spring 기본값 사용 */ }
			default -> http.sessionManagement(session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		}

		// 인증 실패 핸들러
		JwtAuthenticationEntryPoint ep = entryPoint.getIfAvailable();
		if (ep != null) {
			http.exceptionHandling(ex -> ex.authenticationEntryPoint(ep));
		}

		// public URL
		String[] publicUrls = properties.publicUrls() != null
				? properties.publicUrls().toArray(new String[0])
				: new String[0];

		http.authorizeHttpRequests(auth -> {
			if (publicUrls.length > 0) {
				auth.requestMatchers(publicUrls).permitAll();
			}
			auth.anyRequest().authenticated();
		});

		// JWT 필터 (stateless + jwt.secret 설정 시에만)
		if ("stateless".equals(sessionPolicy)) {
			jwtFilter.ifAvailable(filter ->
					http.addFilterBefore(filter,
							UsernamePasswordAuthenticationFilter.class));
		}

		// 커스터마이저
		for (DemoSecurityCustomizer customizer :
				customizers.orderedStream().toList()) {
			customizer.customize(http);
		}

		return http.build();
	}
}
