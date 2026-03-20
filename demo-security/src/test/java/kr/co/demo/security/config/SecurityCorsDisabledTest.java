package kr.co.demo.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = SecurityCorsDisabledTest.TestApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
		"security.enabled=false",
		"security.cors.enabled=false"
})
class SecurityCorsDisabledTest {

	@SpringBootApplication
	@RestController
	static class TestApp {
		@GetMapping("/test")
		String test() { return "ok"; }
	}

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("CORS disabled → Access-Control-Allow-Origin 헤더 없음")
	void noCorsHeaders() throws Exception {
		mvc.perform(options("/test")
						.header("Origin", "http://evil.com")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
