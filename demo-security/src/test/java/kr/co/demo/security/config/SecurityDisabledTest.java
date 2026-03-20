package kr.co.demo.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest(classes = SecurityDisabledTest.TestApp.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "security.enabled=false")
class SecurityDisabledTest {

	@SpringBootApplication
	@RestController
	static class TestApp {
		@GetMapping("/test")
		String test() { return "ok"; }

		@GetMapping("/admin/secret")
		String secret() { return "secret"; }
	}

	@Autowired
	private MockMvc mvc;

	@Test
	@DisplayName("security.enabled=false → 인증 없이 모든 요청 200")
	void allRequestsPermitted() throws Exception {
		mvc.perform(get("/test")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("security.enabled=false → /admin 경로도 인증 불필요")
	void adminAlsoPermitted() throws Exception {
		mvc.perform(get("/admin/secret")).andExpect(status().isOk());
	}
}
