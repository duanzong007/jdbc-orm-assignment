package cn.edu.jdbcorm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BookRepository {
    public List<Book> search(String keyword) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM book WHERE (?='' OR title LIKE ? OR author LIKE ? OR type LIKE ? OR publisher LIKE ?) ORDER BY createTime DESC")) {
            String value = keyword == null ? "" : keyword.trim();
            statement.setString(1, value);
            for (int i = 2; i <= 5; i++) statement.setString(i, "%" + value + "%");
            try (ResultSet result = statement.executeQuery()) { return JDBCTool.resultSetToList(result, Book.class); }
        }
    }

    public void add(Book book) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                LocalDateTime now = LocalDateTime.now();
                book.setId(UUID.randomUUID().toString());
                book.setCreateTime(now);
                book.setUpdateTime(now);
                book.setStatus("正常");
                JDBCTool.save(book, connection);
                for (int i = 0; i < book.getStock(); i++) {
                    BookCopy copy = new BookCopy();
                    copy.setId(UUID.randomUUID().toString());
                    copy.setCreateTime(now);
                    copy.setUpdateTime(now);
                    copy.setBookId(book.getId());
                    copy.setBorrowed(false);
                    JDBCTool.save(copy, connection);
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof SQLException sqlException) throw sqlException;
                if (e instanceof RuntimeException runtimeException) throw runtimeException;
                throw new SQLException(e);
            }
        }
    }
}
