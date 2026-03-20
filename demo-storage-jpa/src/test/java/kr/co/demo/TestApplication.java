package kr.co.demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 테스트용 Spring Boot Application
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "kr.co.demo.domain.repository")
public class TestApplication {
}
