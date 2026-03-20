package kr.co.demo.client.mybatis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kr.co.demo.core.storage.enums.DialectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class DataSourceConfigTest {

	@Nested
	@DisplayName("dialect 감지 테스트")
	class DialectDetection {

		@Test
		@DisplayName("H2 DataSource → DialectType.H2")
		void h2Dialect() {
			DriverManagerDataSource ds = new DriverManagerDataSource();
			ds.setDriverClassName("org.h2.Driver");
			ds.setUrl("jdbc:h2:mem:dialecttest;DB_CLOSE_DELAY=-1");
			ds.setUsername("sa");

			DialectType dialect = DataSourceConfig.resolve(ds);
			assertThat(dialect).isEqualTo(DialectType.H2);
		}
	}

	@Nested
	@DisplayName("DataSource 홀더 테스트")
	class HolderTests {

		@Test
		@DisplayName("set 후 get")
		void setAndGet() {
			DriverManagerDataSource ds = new DriverManagerDataSource();
			ds.setDriverClassName("org.h2.Driver");
			ds.setUrl("jdbc:h2:mem:holdertest;DB_CLOSE_DELAY=-1");
			ds.setUsername("sa");

			// DataSourceConfig는 @Component이므로 Spring이 주입하지만
			// 단위테스트에서는 리플렉션으로 설정
			try {
				java.lang.reflect.Field f = DataSourceConfig.class
						.getDeclaredField("dataSource");
				f.setAccessible(true);
				f.set(null, ds);

				assertThat(DataSourceConfig.get()).isEqualTo(ds);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
