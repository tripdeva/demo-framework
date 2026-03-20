package kr.co.demo.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SecurityPropertiesTest {

	@Nested
	@DisplayName("enabled 테스트")
	class EnabledTests {

		@Test
		@DisplayName("null → true (기본값)")
		void defaultEnabled() {
			SecurityProperties props = new SecurityProperties(null, null, null, null);
			assertThat(props.isEnabled()).isTrue();
		}

		@Test
		@DisplayName("true → true")
		void explicitTrue() {
			SecurityProperties props = new SecurityProperties(true, null, null, null);
			assertThat(props.isEnabled()).isTrue();
		}

		@Test
		@DisplayName("false → false")
		void explicitFalse() {
			SecurityProperties props = new SecurityProperties(false, null, null, null);
			assertThat(props.isEnabled()).isFalse();
		}
	}

	@Nested
	@DisplayName("sessionPolicy 테스트")
	class SessionPolicyTests {

		@Test
		@DisplayName("null → stateless (기본값)")
		void defaultStateless() {
			SecurityProperties props = new SecurityProperties(null, null, null, null);
			assertThat(props.resolveSessionPolicy()).isEqualTo("stateless");
		}

		@Test
		@DisplayName("stateful 설정")
		void stateful() {
			SecurityProperties props = new SecurityProperties(null, "stateful", null, null);
			assertThat(props.resolveSessionPolicy()).isEqualTo("stateful");
		}

		@Test
		@DisplayName("none 설정")
		void none() {
			SecurityProperties props = new SecurityProperties(null, "none", null, null);
			assertThat(props.resolveSessionPolicy()).isEqualTo("none");
		}
	}

	@Nested
	@DisplayName("publicUrls 테스트")
	class PublicUrlsTests {

		@Test
		@DisplayName("null")
		void nullUrls() {
			SecurityProperties props = new SecurityProperties(null, null, null, null);
			assertThat(props.publicUrls()).isNull();
		}

		@Test
		@DisplayName("설정된 URL 목록")
		void withUrls() {
			SecurityProperties props = new SecurityProperties(
					null, null, List.of("/public/**", "/health"), null);
			assertThat(props.publicUrls()).containsExactly("/public/**", "/health");
		}
	}

	@Nested
	@DisplayName("CorsProperties 테스트")
	class CorsTests {

		@Test
		@DisplayName("cors null")
		void nullCors() {
			SecurityProperties props = new SecurityProperties(null, null, null, null);
			assertThat(props.cors()).isNull();
		}

		@Test
		@DisplayName("cors enabled 기본값 true")
		void corsDefaultEnabled() {
			var cors = new SecurityProperties.CorsProperties(
					null, null, null, null, null, null);
			assertThat(cors.isEnabled()).isTrue();
		}

		@Test
		@DisplayName("cors enabled false")
		void corsDisabled() {
			var cors = new SecurityProperties.CorsProperties(
					false, null, null, null, null, null);
			assertThat(cors.isEnabled()).isFalse();
		}

		@Test
		@DisplayName("cors 전체 설정")
		void corsFullConfig() {
			var cors = new SecurityProperties.CorsProperties(
					true,
					List.of("http://localhost:3000"),
					List.of("GET", "POST"),
					List.of("Authorization"),
					true,
					3600L);
			assertThat(cors.isEnabled()).isTrue();
			assertThat(cors.allowedOrigins()).containsExactly("http://localhost:3000");
			assertThat(cors.allowedMethods()).containsExactly("GET", "POST");
			assertThat(cors.allowedHeaders()).containsExactly("Authorization");
			assertThat(cors.allowCredentials()).isTrue();
			assertThat(cors.preflightCacheSeconds()).isEqualTo(3600L);
		}
	}
}
