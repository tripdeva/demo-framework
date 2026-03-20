package kr.co.demo.client.mybatis.config;

import kr.co.demo.core.storage.enums.DialectType;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DataSourceConfig {

    private static DataSource dataSource;

    public DataSourceConfig(DataSource ds) {
        DataSourceConfig.dataSource = ds;
    }

    public static DataSource get() {
        return dataSource;
    }

    public static DialectType resolve(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData()
                    .getDatabaseProductName()
                    .toLowerCase();

            if (product.contains("mysql")) return DialectType.MYSQL;
            if (product.contains("postgres")) return DialectType.POSTGRES;
            if (product.contains("oracle")) return DialectType.ORACLE;
            if (product.contains("h2")) return DialectType.H2;

            throw new IllegalStateException("지원하지 않는 DB: " + product);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
