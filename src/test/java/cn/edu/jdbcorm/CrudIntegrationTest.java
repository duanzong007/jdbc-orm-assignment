package cn.edu.jdbcorm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 使用真实 PostgreSQL 验证，每个测试结束后回滚数据。 */
class CrudIntegrationTest {
    private Connection connection;

    @BeforeEach void beginTransaction() throws Exception {
        connection = Database.connect();
        connection.setAutoCommit(false);
    }
    @AfterEach void rollback() throws Exception {
        if (connection != null) {
            try { connection.rollback(); } finally { connection.close(); }
        }
    }

    private Student student() {
        return new Student("test-" + UUID.randomUUID(), "王同学", "计算机科学", 20,
                LocalDate.of(2024, 9, 1), false, new BigDecimal("6800.35"));
    }
    private College college() {
        return new College("test-" + UUID.randomUUID(), "信息学院", "T" + UUID.randomUUID().toString().substring(0, 12));
    }
    private Object storedValue(String table, String column, String id) throws Exception {
        try (var statement = connection.prepareStatement("SELECT " + column + " FROM " + table + " WHERE id = ?")) {
            statement.setString(1, id);
            try (var result = statement.executeQuery()) { return result.next() ? result.getObject(1) : null; }
        }
    }

    @Test void savesBothEntitiesAndTreatsQuotedTextAsData() throws Exception {
        Student student = student();
        student.setName("O'Reilly'); DROP TABLE students; --");
        student.setMajor(null);
        College college = college();
        assertEquals(1, JDBCTool.save(student, connection));
        assertEquals(1, JDBCTool.save(college, connection));
        assertEquals(student.getName(), storedValue("students", "name", student.getId()));
        assertNull(storedValue("students", "major", student.getId()));
        assertEquals(student.getTuition(), storedValue("students", "tuition", student.getId()));
        assertEquals(college.getCode(), storedValue("colleges", "code", college.getId()));
        assertFalse(connection.isClosed());
    }

    @Test void rejectsMissingPrimaryKeyBeforeExecutingSql() {
        Student student = student();
        student.setId(null);
        assertThrows(OrmException.class, () -> JDBCTool.save(student, connection));
    }
}
