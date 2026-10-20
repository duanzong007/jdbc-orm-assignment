package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsRepositoryTest {
    @Test void includesEveryBookTypeEvenWhenItsLoanCountIsZero() throws Exception {
        var rows = new StatisticsRepository().bookTypes();
        assertEquals(9, rows.size());
        assertTrue(rows.stream().anyMatch(row -> row.name().equals("古典文学")));
        assertTrue(rows.stream().allMatch(row -> row.count() >= 0));
    }

    @Test void groupsLoanCountsByReaderMajor() throws Exception {
        var rows = new StatisticsRepository().majors();
        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().anyMatch(row -> row.name().equals("计算机科学与技术")));
    }
}
