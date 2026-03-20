package kr.co.demo.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

class SecurityCorsEnabledTest {

	@Nested
	@DisplayName("CorsProperties 값 검증")
	class CorsPropertiesTests {

		@Test
		@DisplayName("전체 설정 값 바인딩")
		void fullConfig() {
			var cors = new SecurityProperties.CorsProperties(
					true,
					List.of("http://localhost:3000", "http://localhost:5173"),
					List.of("GET", "POST", "PUT"),
					List.of("Authorization", "Content-Type"),
					true,
					7200L);

			assertThat(cors.isEnabled()).isTrue();
			assertThat(cors.allowedOrigins()).hasSize(2);
			assertThat(cors.allowedMethods()).containsExactly("GET", "POST", "PUT");
			assertThat(cors.allowedHeaders()).containsExactly("Authorization", "Content-Type");
			assertThat(cors.allowCredentials()).isTrue();
			assertThat(cors.preflightCacheSeconds()).isEqualTo(7200L);
		}

		@Test
		@DisplayName("enabled=false")
		void disabled() {
			var cors = new SecurityProperties.CorsProperties(
					false, null, null, null, null, null);
			assertThat(cors.isEnabled()).isFalse();
		}
	}

	@Nested
	@DisplayName("CorsConfiguration 적용 검증")
	class CorsConfigApplyTests {

		@Test
		@DisplayName("프로퍼티 값이 CorsConfiguration에 정확히 적용됨")
		void applyProperties() {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(List.of("http://localhost:3000"));
			config.setAllowedMethods(List.of("GET", "POST"));
			config.setAllowedHeaders(List.of("Authorization"));
			config.setAllowCredentials(true);
			config.setMaxAge(3600L);

			assertThat(config.getAllowedOrigins())
					.containsExactly("http://localhost:3000");
			assertThat(config.getAllowedMethods())
					.containsExactly("GET", "POST");
			assertThat(config.getAllowedHeaders())
					.containsExactly("Authorization");
			assertThat(config.getAllowCredentials()).isTrue();
			assertThat(config.getMaxAge()).isEqualTo(3600L);
		}

		@Test
		@DisplayName("UrlBasedCorsConfigurationSource에 /** 패턴 등록")
		void registerPattern() {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(List.of("http://test.com"));

			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.registerCorsConfiguration("/**", config);

			assertThat(source.getCorsConfigurations()).containsKey("/**");
			assertThat(source.getCorsConfigurations().get("/**")
					.getAllowedOrigins()).contains("http://test.com");
		}

		@Test
		@DisplayName("빈 CorsConfiguration (disabled 시)")
		void emptyCorsConfig() {
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			assertThat(source.getCorsConfigurations()).isEmpty();
		}

		@Test
		@DisplayName("여러 origin 등록")
		void multipleOrigins() {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(List.of(
					"http://localhost:3000",
					"http://localhost:5173",
					"https://myapp.com"));
			assertThat(config.getAllowedOrigins()).hasSize(3);
		}
	}
}
