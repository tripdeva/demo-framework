package kr.co.demo.security.config;

import kr.co.demo.security.filter.JwtAuthenticationEntryPoint;
import kr.co.demo.security.filter.JwtAuthenticationFilter;
import kr.co.demo.security.jwt.JwtTokenProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 보안 자동 설정.
 *
 * <p>{@code jwt.secret} 프로퍼티가 있으면 JWT 인증을 활성화합니다.
 *
 * @author demo-framework
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.security.config.annotation.web.configuration.EnableWebSecurity")
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
public class DemoSecurityAutoConfiguration {

	/**
	 * JwtTokenProvider 빈.
	 *
	 * @param properties JWT 설정
	 * @return JwtTokenProvider
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "jwt", name = "secret")
	public JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
		return new JwtTokenProvider(properties);
	}

	/**
	 * JwtAuthenticationFilter 빈.
	 *
	 * @param provider JwtTokenProvider
	 * @return JwtAuthenticationFilter
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "jwt", name = "secret")
	public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider provider) {
		return new JwtAuthenticationFilter(provider);
	}

	/**
	 * JwtAuthenticationEntryPoint 빈.
	 *
	 * @return JwtAuthenticationEntryPoint
	 */
	@Bean
	@ConditionalOnMissingBean
	public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
		return new JwtAuthenticationEntryPoint();
	}

	/**
	 * PasswordEncoder 빈 (BCrypt).
	 *
	 * @return PasswordEncoder
	 */
	@Bean
	@ConditionalOnMissingBean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
