package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/** 验证多行结果、空值以及日期和金额的映射。 */
class ResultSetMappingTest {
    @Test void mapsMultipleRowsNullsDatesBooleansAndExactMoney() throws Exception {
        try (var connection = Database.connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                 SELECT * FROM (VALUES
                   ('S1', '张三', '计算机', 20, DATE '2024-09-01', false, 6800.35::numeric(12,2)),
                   ('S2', '李四', NULL, NULL, NULL, true, NULL)
                 ) AS students(id, name, major, age, enrollment_date, graduated, tuition)
                 """)) {
            var students = JDBCTool.resultSetToList(result, Student.class);
            assertEquals(2, students.size());
            assertEquals("张三", students.get(0).getName());
            assertEquals(LocalDate.of(2024, 9, 1), students.get(0).getEnrollmentDate());
            assertEquals(new BigDecimal("6800.35"), students.get(0).getTuition());
            assertEquals(false, students.get(0).getGraduated());
            assertNull(students.get(1).getAge());
            assertNull(students.get(1).getEnrollmentDate());
            assertNull(students.get(1).getTuition());
            assertEquals(true, students.get(1).getGraduated());
            assertFalse(result.isClosed());
            assertFalse(connection.isClosed());
        }
    }

    @Test void emptyQueryReturnsEmptyList() throws Exception {
        try (var connection = Database.connect(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT * FROM students WHERE false")) {
            assertTrue(JDBCTool.resultSetToList(result, Student.class).isEmpty());
        }
    }
}
