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
    public List<BookCopy> copies(String bookId) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM bookcopy WHERE bookId=? ORDER BY createTime DESC")) {
            statement.setString(1, bookId);
            try (ResultSet result = statement.executeQuery()) { return JDBCTool.resultSetToList(result, BookCopy.class); }
        }
    }

    public void addCopy(String bookId) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement("SELECT status FROM book WHERE id=? FOR UPDATE")) {
                    statement.setString(1, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next() || !"正常".equals(result.getString(1))) throw new IllegalArgumentException("图书不存在或已废弃");
                    }
                }
                LocalDateTime now = LocalDateTime.now();
                BookCopy copy = new BookCopy();
                copy.setId(UUID.randomUUID().toString());
                copy.setCreateTime(now);
                copy.setUpdateTime(now);
                copy.setBookId(bookId);
                copy.setBorrowed(false);
                JDBCTool.save(copy, connection);
                try (PreparedStatement statement = connection.prepareStatement("UPDATE book SET stock=stock+1,updateTime=? WHERE id=?")) {
                    statement.setObject(1, now);
                    statement.setString(2, bookId);
                    statement.executeUpdate();
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

    public void deleteCopy(String copyId) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                String bookId;
                try (PreparedStatement statement = connection.prepareStatement("SELECT bookId,isBorrowed FROM bookcopy WHERE id=? FOR UPDATE")) {
                    statement.setString(1, copyId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("副本不存在");
                        bookId = result.getString(1);
                        if (result.getBoolean(2)) throw new IllegalArgumentException("外借中的副本不能删除");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM loan WHERE bookCopyId=?")) {
                    statement.setString(1, copyId);
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        if (result.getInt(1) > 0) throw new IllegalArgumentException("有历史借阅记录的副本不能删除");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM bookcopy WHERE id=?")) {
                    statement.setString(1, copyId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("UPDATE book SET stock=stock-1,updateTime=? WHERE id=? AND stock>0")) {
                    statement.setObject(1, LocalDateTime.now());
                    statement.setString(2, bookId);
                    if (statement.executeUpdate() != 1) throw new SQLException("图书库存不一致");
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

    public void updateInfo(Book book) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE book SET title=?,author=?,`type`=?,publisher=?,days=?,price=?,updateTime=? WHERE id=?")) {
            statement.setString(1, book.getTitle());
            statement.setString(2, book.getAuthor());
            statement.setString(3, book.getType());
            statement.setString(4, book.getPublisher());
            statement.setInt(5, book.getDays());
            statement.setDouble(6, book.getPrice());
            statement.setObject(7, LocalDateTime.now());
            statement.setString(8, book.getId());
            if (statement.executeUpdate() != 1) throw new IllegalArgumentException("图书不存在");
        }
    }

    public void discard(String id) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                int stock;
                try (PreparedStatement statement = connection.prepareStatement("SELECT stock FROM book WHERE id=? FOR UPDATE")) {
                    statement.setString(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("图书不存在");
                        stock = result.getInt(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COUNT(*),SUM(CASE WHEN isBorrowed=b'1' THEN 1 ELSE 0 END) FROM bookcopy WHERE bookId=?")) {
                    statement.setString(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        if (result.getInt(1) != stock || result.getInt(2) != 0) {
                            throw new IllegalArgumentException("仍有外借副本或库存数量不一致，不能废弃");
                        }
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("UPDATE book SET status='已废弃',updateTime=? WHERE id=?")) {
                    statement.setObject(1, LocalDateTime.now());
                    statement.setString(2, id);
                    statement.executeUpdate();
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

    public Book find(String id) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM book WHERE id=?")) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                var rows = JDBCTool.resultSetToList(result, Book.class);
                return rows.isEmpty() ? null : rows.get(0);
            }
        }
    }

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
