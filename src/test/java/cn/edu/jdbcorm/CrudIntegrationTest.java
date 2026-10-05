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

    @Test void updatesOnlyTheRequestedStudentAndCollege() throws Exception {
        Student target = student();
        Student untouched = student();
        College college = college();
        College otherCollege = college();
        JDBCTool.save(target, connection);
        JDBCTool.save(untouched, connection);
        JDBCTool.save(college, connection);
        JDBCTool.save(otherCollege, connection);
        target.setName("更新后的姓名");
        target.setGraduated(true);
        target.setTuition(new BigDecimal("7200.19"));
        college.setName("计算机学院");
        assertEquals(1, JDBCTool.update(target, connection));
        assertEquals(1, JDBCTool.update(college, connection));
        assertEquals(target.getName(), storedValue("students", "name", target.getId()));
        assertEquals(true, storedValue("students", "graduated", target.getId()));
        assertEquals(target.getTuition(), storedValue("students", "tuition", target.getId()));
        assertEquals(untouched.getName(), storedValue("students", "name", untouched.getId()));
        assertEquals(college.getName(), storedValue("colleges", "name", college.getId()));
        assertEquals(otherCollege.getName(), storedValue("colleges", "name", otherCollege.getId()));
        assertEquals(0, JDBCTool.update(student(), connection));
    }
}
