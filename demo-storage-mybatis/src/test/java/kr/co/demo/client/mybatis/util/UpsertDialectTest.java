package kr.co.demo.client.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import kr.co.demo.client.mybatis.config.DataSourceConfig;
import kr.co.demo.domain.Order;
import kr.co.demo.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UpsertDialectTest {

	private Order testOrder() {
		Order o = new Order();
		o.setId(1L);
		o.setOrderNumber("ORD-001");
		o.setCustomerName("Test");
		o.setStatus(OrderStatus.PENDING);
		o.setTotalAmount(new BigDecimal("10000"));
		o.setOrderedAt(LocalDateTime.now());
		return o;
	}

	private void setDialect(String url) throws Exception {
		org.springframework.jdbc.datasource.DriverManagerDataSource ds =
				new org.springframework.jdbc.datasource.DriverManagerDataSource();
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl(url);
		ds.setUsername("sa");
		Field f = DataSourceConfig.class.getDeclaredField("dataSource");
		f.setAccessible(true);
		f.set(null, ds);
	}

	@Nested
	@DisplayName("H2 방언")
	class H2Dialect {
		@Test
		@DisplayName("MERGE INTO ... KEY(id) VALUES")
		void h2Upsert() throws Exception {
			setDialect("jdbc:h2:mem:h2test;DB_CLOSE_DELAY=-1");
			String sql = UpsertSqlProvider.build(testOrder());
			assertThat(sql).containsIgnoringCase("MERGE INTO");
			assertThat(sql).containsIgnoringCase("KEY(id)");
			assertThat(sql).containsIgnoringCase("VALUES");
		}

		@Test
		@DisplayName("ID 컬럼 포함")
		void containsId() throws Exception {
			setDialect("jdbc:h2:mem:h2test2;DB_CLOSE_DELAY=-1");
			String sql = UpsertSqlProvider.build(testOrder());
			assertThat(sql).contains("id");
		}

		@Test
		@DisplayName("@StorageColumn 있는 필드만 포함")
		void onlyAnnotatedFields() throws Exception {
			setDialect("jdbc:h2:mem:h2test3;DB_CLOSE_DELAY=-1");
			String sql = UpsertSqlProvider.build(testOrder());
			assertThat(sql).contains("order_no");
			assertThat(sql).contains("customer_name");
			assertThat(sql).contains("total_amount");
		}
	}

	@Nested
	@DisplayName("SQL 구조 검증")
	class SqlStructure {

		@Test
		@DisplayName("SQL에 테이블명 orders 포함")
		void containsTableName() throws Exception {
			setDialect("jdbc:h2:mem:sqltest;DB_CLOSE_DELAY=-1");
			String sql = UpsertSqlProvider.build(testOrder());
			assertThat(sql).contains("orders");
		}

		@Test
		@DisplayName("파라미터 바인딩 #{} 포함")
		void containsParameterBinding() throws Exception {
			setDialect("jdbc:h2:mem:sqltest2;DB_CLOSE_DELAY=-1");
			String sql = UpsertSqlProvider.build(testOrder());
			assertThat(sql).contains("#{");
		}

		@Test
		@DisplayName("NULL ID 주문의 upsert SQL도 생성 가능")
		void nullIdOrder() throws Exception {
			setDialect("jdbc:h2:mem:sqltest3;DB_CLOSE_DELAY=-1");
			Order o = testOrder();
			o.setId(null);
			String sql = UpsertSqlProvider.build(o);
			assertThat(sql).isNotBlank();
		}
	}
}
