package cn.edu.jdbcorm;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** 创建数据库连接，连接关闭和事务提交由调用方负责。 */
public final class Database {
    private Database() {}

    public static Connection connect() throws SQLException {
        Properties config = new Properties();
        try (InputStream input = Database.class.getResourceAsStream("/database.properties")) {
            if (input == null) throw new IOException("database.properties not found");
            config.load(input);
        } catch (IOException e) {
            throw new SQLException("Cannot load database configuration", e);
        }
        String url = setting("DB_URL", config.getProperty("db.url"));
        String user = setting("DB_USER", config.getProperty("db.user"));
        if (user == null || user.isBlank()) user = System.getProperty("user.name");
        String password = setting("DB_PASSWORD", config.getProperty("db.password", ""));
        return DriverManager.getConnection(url, user, password);
    }

    private static String setting(String environment, String fallback) {
        String value = System.getenv(environment);
        return value == null ? fallback : value;
    }
}
