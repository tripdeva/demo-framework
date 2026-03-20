package kr.co.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 테스트용 Spring Boot Application
 */
@SpringBootApplication
@MapperScan("kr.co.demo.domain.mapper")
public class TestApplication {
}
