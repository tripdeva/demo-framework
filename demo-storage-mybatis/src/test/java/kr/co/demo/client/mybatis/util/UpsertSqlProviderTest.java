package kr.co.demo.client.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import kr.co.demo.client.mybatis.config.DataSourceConfig;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class UpsertSqlProviderTest {

	@BeforeAll
	static void setUp() throws Exception {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:upserttest;DB_CLOSE_DELAY=-1");
		ds.setUsername("sa");

		Field field = DataSourceConfig.class.getDeclaredField("dataSource");
		field.setAccessible(true);
		field.set(null, ds);
	}

	@Test
	@DisplayName("H2 UPSERT SQL 생성")
	void buildH2Upsert() {
		Order order = new Order();
		order.setId(1L);
		order.setOrderNumber("ORD-001");
		order.setCustomerName("Test");
		order.setStatus(OrderStatus.PENDING);

		String sql = UpsertSqlProvider.build(order);

		assertThat(sql).contains("MERGE INTO orders");
		assertThat(sql).contains("KEY(id)");
		assertThat(sql).contains("VALUES");
	}

	@Test
	@DisplayName("UPSERT SQL에 ID 필드 포함")
	void upsertContainsId() {
		Order order = new Order();
		order.setId(99L);
		order.setOrderNumber("ORD-099");
		order.setCustomerName("User");
		order.setStatus(OrderStatus.CONFIRMED);

		String sql = UpsertSqlProvider.build(order);
		assertThat(sql).contains("id");
	}
}
