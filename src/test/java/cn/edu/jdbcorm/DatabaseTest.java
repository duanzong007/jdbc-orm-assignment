package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

/** 验证本地数据库连接和表结构。 */
class DatabaseTest {
    @Test void connectsToBothAssignmentTables() throws Exception {
        try (Connection connection = Database.connect();
             var statement = connection.createStatement()) {
            assertTrue(connection.isValid(2));
            for (String table : new String[]{"students", "colleges"}) {
                try (var result = statement.executeQuery("SELECT * FROM " + table + " LIMIT 0")) {
                    assertTrue(result.getMetaData().getColumnCount() >= 3);
                }
            }
        }
    }
}
