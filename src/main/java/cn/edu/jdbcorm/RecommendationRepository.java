package cn.edu.jdbcorm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecommendationRepository {
    public List<Book> findBooks(String name, String author) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM book WHERE title LIKE ? OR author LIKE ? ORDER BY createTime DESC")) {
            statement.setString(1, "%" + name + "%");
            statement.setString(2, "%" + author + "%");
            try (ResultSet result = statement.executeQuery()) { return JDBCTool.resultSetToList(result, Book.class); }
        }
    }

    public void submit(BookRecommendation recommendation) throws SQLException {
        try (Connection connection = Database.connect()) {
            LocalDateTime now = LocalDateTime.now();
            recommendation.setId(UUID.randomUUID().toString());
            recommendation.setCreateTime(now);
            recommendation.setUpdateTime(now);
            recommendation.setStatus("待审");
            JDBCTool.save(recommendation, connection);
        }
    }

    public List<BookRecommendation> pending() throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM bookrecommend WHERE status='待审' ORDER BY createTime DESC");
             ResultSet result = statement.executeQuery()) {
            return JDBCTool.resultSetToList(result, BookRecommendation.class);
        }
    }

    public void review(String id, boolean approved, String reason) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                BookRecommendation recommendation;
                try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM bookrecommend WHERE id=? AND status='待审' FOR UPDATE")) {
                    statement.setString(1, id);
                    try (ResultSet result = statement.executeQuery()) {
                        var rows = JDBCTool.resultSetToList(result, BookRecommendation.class);
                        if (rows.isEmpty()) throw new IllegalArgumentException("推荐记录不存在或已经审核");
                        recommendation = rows.get(0);
                    }
                }
                LocalDateTime now = LocalDateTime.now();
                if (approved) {
                    Book book = new Book();
                    book.setId(UUID.randomUUID().toString());
                    book.setCreateTime(now);
                    book.setUpdateTime(now);
                    book.setTitle(recommendation.getBookName());
                    book.setAuthor(recommendation.getAuthor());
                    book.setType("其他");
                    book.setPublisher(recommendation.getPublisher());
                    book.setDays(30);
                    book.setPrice(recommendation.getPrice() == null ? 0.0 : recommendation.getPrice());
                    book.setStock(1);
                    book.setStatus("正常");
                    JDBCTool.save(book, connection);
                    BookCopy copy = new BookCopy();
                    copy.setId(UUID.randomUUID().toString());
                    copy.setCreateTime(now);
                    copy.setUpdateTime(now);
                    copy.setBookId(book.getId());
                    copy.setBorrowed(false);
                    JDBCTool.save(copy, connection);
                }
                try (PreparedStatement statement = connection.prepareStatement("UPDATE bookrecommend SET status=?,reason=?,updateTime=? WHERE id=?")) {
                    statement.setString(1, approved ? "已通过" : "已驳回");
                    statement.setString(2, approved ? null : reason);
                    statement.setObject(3, now);
                    statement.setString(4, id);
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

    public List<RecommendationRow> rows() throws SQLException {
        String sql = "SELECT r.id,r.bookName,r.author,r.publisher,r.price,r.status,r.reason,u.name FROM bookrecommend r JOIN `user` u ON u.id=r.userId ORDER BY r.createTime DESC";
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<RecommendationRow> rows = new ArrayList<>();
            while (result.next()) rows.add(new RecommendationRow(result.getString(1), result.getString(2), result.getString(3),
                    result.getString(4), result.getObject(5, Double.class), result.getString(6), result.getString(7), result.getString(8)));
            return rows;
        }
    }

    public record RecommendationRow(String id, String bookName, String author, String publisher,
                                    Double price, String status, String reason, String userName) {}
}
