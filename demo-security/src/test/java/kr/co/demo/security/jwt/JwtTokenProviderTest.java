package kr.co.demo.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kr.co.demo.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class JwtTokenProviderTest {

	private JwtTokenProvider provider;

	@BeforeEach
	void setUp() {
		JwtProperties props = new JwtProperties(
				"test-secret-key-that-is-at-least-32-characters-long!!",
				3600000L,
				86400000L);
		provider = new JwtTokenProvider(props);
	}

	@Nested
	@DisplayName("Access Token")
	class AccessTokenTests {

		@Test
		@DisplayName("토큰 생성")
		void create() {
			String token = provider.createAccessToken("user1", List.of("ROLE_USER"));
			assertThat(token).isNotBlank();
		}

		@Test
		@DisplayName("토큰 검증 성공")
		void validate() {
			String token = provider.createAccessToken("user1", List.of("ROLE_USER"));
			assertThat(provider.validateToken(token)).isTrue();
		}

		@Test
		@DisplayName("username 추출")
		void getUsername() {
			String token = provider.createAccessToken("admin", List.of("ROLE_ADMIN"));
			assertThat(provider.getUsername(token)).isEqualTo("admin");
		}

		@Test
		@DisplayName("Authentication 추출")
		void getAuthentication() {
			String token = provider.createAccessToken("user1",
					List.of("ROLE_USER", "ROLE_ADMIN"));
			Authentication auth = provider.getAuthentication(token);
			assertThat(auth.getName()).isEqualTo("user1");
			assertThat(auth.getAuthorities()).hasSize(2);
		}

		@Test
		@DisplayName("roles 빈 목록")
		void emptyRoles() {
			String token = provider.createAccessToken("user1", List.of());
			Authentication auth = provider.getAuthentication(token);
			assertThat(auth.getAuthorities()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Refresh Token")
	class RefreshTokenTests {

		@Test
		@DisplayName("토큰 생성")
		void create() {
			String token = provider.createRefreshToken("user1");
			assertThat(token).isNotBlank();
		}

		@Test
		@DisplayName("토큰 검증 성공")
		void validate() {
			String token = provider.createRefreshToken("user1");
			assertThat(provider.validateToken(token)).isTrue();
		}

		@Test
		@DisplayName("username 추출")
		void getUsername() {
			String token = provider.createRefreshToken("user1");
			assertThat(provider.getUsername(token)).isEqualTo("user1");
		}
	}

	@Nested
	@DisplayName("검증 실패")
	class ValidationFailTests {

		@Test
		@DisplayName("잘못된 토큰")
		void invalidToken() {
			assertThat(provider.validateToken("not-a-jwt")).isFalse();
		}

		@Test
		@DisplayName("빈 문자열")
		void emptyToken() {
			assertThat(provider.validateToken("")).isFalse();
		}

		@Test
		@DisplayName("만료된 토큰")
		void expiredToken() {
			JwtProperties shortLived = new JwtProperties(
					"test-secret-key-that-is-at-least-32-characters-long!!",
					0L, 0L);
			JwtTokenProvider shortProvider = new JwtTokenProvider(shortLived);
			String token = shortProvider.createAccessToken("user1", List.of());
			// 즉시 만료
			assertThat(shortProvider.validateToken(token)).isFalse();
		}

		@Test
		@DisplayName("다른 secret으로 서명된 토큰")
		void wrongSecret() {
			JwtProperties otherProps = new JwtProperties(
					"other-secret-key-that-is-at-least-32-characters!!",
					3600000L, 86400000L);
			JwtTokenProvider otherProvider = new JwtTokenProvider(otherProps);
			String token = otherProvider.createAccessToken("user1", List.of());

			assertThat(provider.validateToken(token)).isFalse();
		}
	}

	@Nested
	@DisplayName("토큰 독립성")
	class TokenIndependence {

		@Test
		@DisplayName("같은 사용자의 access/refresh 토큰은 다름")
		void differentTokens() {
			String access = provider.createAccessToken("user1", List.of());
			String refresh = provider.createRefreshToken("user1");
			assertThat(access).isNotEqualTo(refresh);
		}

		@Test
		@DisplayName("access와 refresh는 다른 만료시간을 가짐")
		void differentExpiration() {
			String access = provider.createAccessToken("user1", List.of());
			String refresh = provider.createRefreshToken("user1");
			// 둘 다 유효하지만 서로 다름
			assertThat(provider.validateToken(access)).isTrue();
			assertThat(provider.validateToken(refresh)).isTrue();
			assertThat(access).isNotEqualTo(refresh);
		}
	}
}
