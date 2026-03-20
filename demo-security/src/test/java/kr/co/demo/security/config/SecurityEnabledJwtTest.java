package kr.co.demo.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kr.co.demo.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = SecurityEnabledJwtTest.TestApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"jwt.secret=test-jwt-secret-key-minimum-32-characters!!",
		"jwt.access-token-expiration=3600000",
		"jwt.refresh-token-expiration=86400000",
		"security.public-urls=/public/**"
})
class SecurityEnabledJwtTest {

	@SpringBootApplication
	@RestController
	static class TestApp {
		@GetMapping("/public/health")
		String health() { return "ok"; }

		@GetMapping("/api/data")
		String data() { return "secret-data"; }
	}

	@Autowired private MockMvc mvc;
	@Autowired private JwtTokenProvider jwtProvider;

	@Nested
	@DisplayName("public URL")
	class PublicUrlTests {

		@Test
		@DisplayName("인증 없이 public URL 접근 → 200")
		void publicAccessible() throws Exception {
			mvc.perform(get("/public/health"))
					.andExpect(status().isOk());
		}
	}

	@Nested
	@DisplayName("인증 필요 URL")
	class ProtectedUrlTests {

		@Test
		@DisplayName("토큰 없이 접근 → 401")
		void noTokenUnauthorized() throws Exception {
			mvc.perform(get("/api/data"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("유효한 JWT 토큰 → 200")
		void validTokenAuthorized() throws Exception {
			String token = jwtProvider.createAccessToken("user1",
					List.of("ROLE_USER"));
			mvc.perform(get("/api/data")
							.header("Authorization", "Bearer " + token))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("잘못된 토큰 → 401")
		void invalidTokenUnauthorized() throws Exception {
			mvc.perform(get("/api/data")
							.header("Authorization", "Bearer invalid-token"))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("Bearer 접두사 없이 → 401")
		void noBearerPrefix() throws Exception {
			String token = jwtProvider.createAccessToken("user1", List.of());
			mvc.perform(get("/api/data")
							.header("Authorization", token))
					.andExpect(status().isUnauthorized());
		}
	}
}
