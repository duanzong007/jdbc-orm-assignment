package cn.edu.jdbcorm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class StatisticsRepository {
    public List<CountRow> bookTypes() throws SQLException {
        String sql = "SELECT kinds.type,COUNT(l.id) AS total FROM (SELECT DISTINCT `type` FROM book WHERE `type` IS NOT NULL) kinds "
                + "LEFT JOIN book b ON b.`type`=kinds.`type` LEFT JOIN bookcopy c ON c.bookId=b.id "
                + "LEFT JOIN loan l ON l.bookCopyId=c.id GROUP BY kinds.`type` ORDER BY kinds.`type`";
        return query(sql);
    }

    public List<CountRow> majors() throws SQLException {
        String sql = "SELECT u.major,COUNT(l.id) AS total FROM `user` u LEFT JOIN loan l ON l.userId=u.id "
                + "WHERE u.major IS NOT NULL AND u.major<>'' GROUP BY u.major ORDER BY u.major";
        return query(sql);
    }

    private List<CountRow> query(String sql) throws SQLException {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<CountRow> rows = new ArrayList<>();
            while (result.next()) rows.add(new CountRow(result.getString(1), result.getLong(2)));
            return rows;
        }
    }

    public record CountRow(String name, long count) {}
}
