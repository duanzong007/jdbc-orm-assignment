package cn.edu.jdbcorm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LoanRepository {
    public void returnBook(String userId, String copyId, boolean staff) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                String sql = "SELECT id FROM loan WHERE bookCopyId=? AND status IN ('未还','超期')" + (staff ? "" : " AND userId=?") + " FOR UPDATE";
                String loanId;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, copyId);
                    if (!staff) statement.setString(2, userId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("没有找到可归还的借阅记录");
                        loanId = result.getString(1);
                    }
                }
                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement statement = connection.prepareStatement("UPDATE loan SET returnDate=?,status='已还',updateTime=? WHERE id=?")) {
                    statement.setObject(1, now);
                    statement.setObject(2, now);
                    statement.setString(3, loanId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("UPDATE bookcopy SET isBorrowed=b'0',updateTime=? WHERE id=?")) {
                    statement.setObject(1, now);
                    statement.setString(2, copyId);
                    if (statement.executeUpdate() != 1) throw new SQLException("图书副本不存在");
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

    public void borrow(String userId, String bookId) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                int days;
                try (PreparedStatement statement = connection.prepareStatement("SELECT days FROM book WHERE id=? AND status='正常' FOR UPDATE")) {
                    statement.setString(1, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("图书不存在或已废弃");
                        days = result.getInt(1);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM loan WHERE userId=? AND status='未还'")) {
                    statement.setString(1, userId);
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        if (result.getInt(1) >= 5) throw new IllegalArgumentException("每位读者最多同时借阅 5 本图书");
                    }
                }
                String copyId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM bookcopy WHERE bookId=? AND isBorrowed=b'0' ORDER BY createTime LIMIT 1 FOR UPDATE")) {
                    statement.setString(1, bookId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("当前没有可借副本");
                        copyId = result.getString(1);
                    }
                }
                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement statement = connection.prepareStatement("UPDATE bookcopy SET isBorrowed=b'1',updateTime=? WHERE id=? AND isBorrowed=b'0'")) {
                    statement.setObject(1, now);
                    statement.setString(2, copyId);
                    if (statement.executeUpdate() != 1) throw new SQLException("副本状态已变化，请重试");
                }
                Loan loan = new Loan();
                loan.setId(UUID.randomUUID().toString());
                loan.setCreateTime(now);
                loan.setUpdateTime(now);
                loan.setBookCopyId(copyId);
                loan.setUserId(userId);
                loan.setBorrowDate(now);
                loan.setDueDate(now.plusDays(days));
                loan.setStatus("未还");
                JDBCTool.save(loan, connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof SQLException sqlException) throw sqlException;
                if (e instanceof RuntimeException runtimeException) throw runtimeException;
                throw new SQLException(e);
            }
        }
    }

    public List<LoanRecord> mine(String userId, boolean activeOnly) throws SQLException {
        String sql = "SELECT l.id,l.bookCopyId,b.title,u.name,l.borrowDate,l.dueDate,l.returnDate,l.status "
                + "FROM loan l JOIN bookcopy c ON c.id=l.bookCopyId JOIN book b ON b.id=c.bookId JOIN `user` u ON u.id=l.userId "
                + "WHERE l.userId=?" + (activeOnly ? " AND l.status='未还'" : "") + " ORDER BY l.borrowDate DESC";
        return query(sql, userId);
    }

    public List<Book> availableBooks() throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT b.* FROM book b WHERE b.status='正常' AND EXISTS (SELECT 1 FROM bookcopy c WHERE c.bookId=b.id AND c.isBorrowed=b'0') ORDER BY b.title")) {
            try (ResultSet result = statement.executeQuery()) { return JDBCTool.resultSetToList(result, Book.class); }
        }
    }

    public List<LoanRecord> active() throws SQLException {
        String sql = "SELECT l.id,l.bookCopyId,b.title,u.name,l.borrowDate,l.dueDate,l.returnDate,l.status "
                + "FROM loan l JOIN bookcopy c ON c.id=l.bookCopyId JOIN book b ON b.id=c.bookId JOIN `user` u ON u.id=l.userId "
                + "WHERE l.status IN ('未还','超期') ORDER BY l.borrowDate DESC";
        return query(sql);
    }

    private List<LoanRecord> query(String sql, String value) throws SQLException {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                List<LoanRecord> rows = new ArrayList<>();
                while (result.next()) rows.add(new LoanRecord(result.getString(1), result.getString(2), result.getString(3),
                        result.getString(4), time(result, 5), time(result, 6), time(result, 7), result.getString(8)));
                return rows;
            }
        }
    }

    private List<LoanRecord> query(String sql) throws SQLException {
        try (Connection connection = Database.connect(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<LoanRecord> rows = new ArrayList<>();
            while (result.next()) rows.add(new LoanRecord(result.getString(1), result.getString(2), result.getString(3),
                    result.getString(4), time(result, 5), time(result, 6), time(result, 7), result.getString(8)));
            return rows;
        }
    }

    private LocalDateTime time(ResultSet result, int index) throws SQLException {
        var value = result.getTimestamp(index);
        return value == null ? null : value.toLocalDateTime();
    }
}
