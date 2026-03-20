package kr.co.demo.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = SecurityStatefulTest.TestApp.class)
@TestPropertySource(properties = {
		"security.session-policy=stateful",
		"security.public-urls=/public/**"
})
class SecurityStatefulTest {

	@SpringBootApplication
	@RestController
	static class TestApp {}

	@Autowired
	private ApplicationContext context;

	@Test
	@DisplayName("session-policy=stateful → JWT 필터 빈 미등록")
	void noJwtFilter() {
		assertThat(context.getBeanNamesForType(
				kr.co.demo.security.filter.JwtAuthenticationFilter.class))
				.isEmpty();
	}

	@Test
	@DisplayName("session-policy=stateful → JwtTokenProvider 빈 미등록")
	void noJwtProvider() {
		assertThat(context.getBeanNamesForType(
				kr.co.demo.security.jwt.JwtTokenProvider.class))
				.isEmpty();
	}

	@Test
	@DisplayName("PasswordEncoder는 등록됨")
	void passwordEncoderExists() {
		assertThat(context.getBeanNamesForType(
				org.springframework.security.crypto.password.PasswordEncoder.class))
				.isNotEmpty();
	}

	@Test
	@DisplayName("SecurityFilterChain 존재")
	void filterChainExists() {
		assertThat(context.getBeanNamesForType(
				org.springframework.security.web.SecurityFilterChain.class))
				.isNotEmpty();
	}
}
