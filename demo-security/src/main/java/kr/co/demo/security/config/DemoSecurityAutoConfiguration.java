package kr.co.demo.security.config;

import java.util.List;
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
 * <p>모든 빈은 {@code @ConditionalOnMissingBean} — 소비자가 같은 타입 빈을
 * 정의하면 라이브러리 기본값은 비활성화됩니다.
 *
 * <h2>커스터마이징 단계</h2>
 *
 * <h3>1단계: 설정만 변경 (application.yml)</h3>
 * <pre>{@code
 * jwt:
 *   secret: my-key
 * security:
 *   public-urls: ["/public/**"]
 *   cors-urls: ["http://myapp.com"]
 * }</pre>
 *
 * <h3>2단계: 부분 확장 (Customizer 빈)</h3>
 * <pre>{@code
 * @Bean
 * public DemoSecurityCustomizer myCustomizer() {
 *     return http -> http.authorizeHttpRequests(auth -> auth
 *         .requestMatchers("/admin/**").hasRole("ADMIN"));
 * }
 * }</pre>
 *
 * <h3>3단계: 개별 빈 교체</h3>
 * <pre>{@code
 * @Bean // BCrypt 대신 Argon2
 * public PasswordEncoder passwordEncoder() {
 *     return new Argon2PasswordEncoder(...);
 * }
 * }</pre>
 *
 * <h3>4단계: 완전 오버라이드</h3>
 * <pre>{@code
 * @Bean // 라이브러리 SecurityFilterChain 비활성화
 * public SecurityFilterChain myFilterChain(HttpSecurity http) {
 *     // 처음부터 직접 구성
 * }
 * }</pre>
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
	 * <p>모든 값은 {@code security.cors.allowed-*} 프로퍼티에서 읽습니다.
	 * 설정하지 않으면 CORS가 비활성화되어 브라우저가 차단합니다.
	 *
	 * <p>{@link DemoCorsCustomizer} 빈으로 추가 설정 가능.
	 * 완전 교체: {@code CorsConfigurationSource} 빈 직접 정의.
	 */
	@Bean
	@ConditionalOnMissingBean
	public CorsConfigurationSource corsConfigurationSource(
			SecurityProperties properties,
			ObjectProvider<DemoCorsCustomizer> customizers) {

		CorsConfiguration config = new CorsConfiguration();
		SecurityProperties.CorsProperties cors = properties.cors();

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
			if (cors.maxAge() != null) {
				config.setMaxAge(cors.maxAge());
			}
		}

		customizers.orderedStream().forEach(c -> c.customize(config));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	// ==================== SecurityFilterChain ====================

	/**
	 * 기본 SecurityFilterChain.
	 *
	 * <p>기본 동작:
	 * <ul>
	 *   <li>CSRF 비활성화</li>
	 *   <li>CORS 활성화</li>
	 *   <li>Stateless 세션</li>
	 *   <li>{@code security.public-urls} 인증 면제</li>
	 *   <li>나머지 인증 필요</li>
	 *   <li>JWT 필터 자동 등록 (jwt.secret 설정 시)</li>
	 * </ul>
	 *
	 * <p>{@link DemoSecurityCustomizer}로 부분 확장 가능.
	 * 직접 {@code SecurityFilterChain} 빈을 정의하면 이 기본값은 비활성화.
	 */
	@Bean
	@ConditionalOnMissingBean
	public SecurityFilterChain demoSecurityFilterChain(
			HttpSecurity http,
			SecurityProperties properties,
			CorsConfigurationSource corsSource,
			JwtAuthenticationEntryPoint entryPoint,
			ObjectProvider<JwtAuthenticationFilter> jwtFilter,
			ObjectProvider<DemoSecurityCustomizer> customizers) throws Exception {

		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsSource))
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));

		String[] publicUrls = properties.publicUrls() != null
				? properties.publicUrls().toArray(new String[0])
				: new String[0];

		http.authorizeHttpRequests(auth -> {
			if (publicUrls.length > 0) {
				auth.requestMatchers(publicUrls).permitAll();
			}
			auth.anyRequest().authenticated();
		});

		jwtFilter.ifAvailable(filter ->
				http.addFilterBefore(filter,
						UsernamePasswordAuthenticationFilter.class));

		for (DemoSecurityCustomizer customizer :
				customizers.orderedStream().toList()) {
			customizer.customize(http);
		}

		return http.build();
	}
}
