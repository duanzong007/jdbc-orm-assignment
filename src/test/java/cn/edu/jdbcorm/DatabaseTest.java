package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseTest {
    @Test void readsTheRestoredMysqlDatabase() throws Exception {
        try (var connection = Database.connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM book")) {
            result.next();
            assertEquals(54, result.getInt(1));
        }
    }
}
