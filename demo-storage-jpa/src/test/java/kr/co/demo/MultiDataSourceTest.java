package kr.co.demo;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.demo.client.jpa.config.DataSourceContextHolder;
import kr.co.demo.client.jpa.config.RoutingDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MultiDataSourceTest {

	@AfterEach
	void cleanup() {
		DataSourceContextHolder.clear();
	}

	@Nested
	@DisplayName("DataSourceContextHolder 테스트")
	class ContextHolderTests {

		@Test
		@DisplayName("기본값은 null (기본 DataSource)")
		void defaultIsNull() {
			assertThat(DataSourceContextHolder.get()).isNull();
		}

		@Test
		@DisplayName("set/get 동작")
		void setAndGet() {
			DataSourceContextHolder.set("secondary");
			assertThat(DataSourceContextHolder.get()).isEqualTo("secondary");
		}

		@Test
		@DisplayName("clear 후 null")
		void clearResets() {
			DataSourceContextHolder.set("secondary");
			DataSourceContextHolder.clear();
			assertThat(DataSourceContextHolder.get()).isNull();
		}

		@Test
		@DisplayName("여러 키 전환")
		void switchKeys() {
			DataSourceContextHolder.set("primary");
			assertThat(DataSourceContextHolder.get()).isEqualTo("primary");

			DataSourceContextHolder.set("secondary");
			assertThat(DataSourceContextHolder.get()).isEqualTo("secondary");

			DataSourceContextHolder.set("readonly");
			assertThat(DataSourceContextHolder.get()).isEqualTo("readonly");
		}
	}

	@Nested
	@DisplayName("RoutingDataSource 테스트")
	class RoutingTests {

		static class TestableRoutingDataSource extends RoutingDataSource {
			public Object publicDetermineKey() {
				return determineCurrentLookupKey();
			}
		}

		@Test
		@DisplayName("determineCurrentLookupKey가 ThreadLocal 값 반환")
		void determineLookupKey() {
			TestableRoutingDataSource routing = new TestableRoutingDataSource();

			DataSourceContextHolder.set("testKey");
			assertThat(routing.publicDetermineKey()).isEqualTo("testKey");

			DataSourceContextHolder.clear();
			assertThat(routing.publicDetermineKey()).isNull();
		}
	}
}
