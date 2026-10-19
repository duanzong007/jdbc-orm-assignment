package cn.edu.jdbcorm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;

public final class UserRepository {
    private static final Map<String, Class<? extends User>> TYPES = Map.of(
            "管理员", Admin.class, "操作员", Operator.class, "学生", Student.class, "教师", Teacher.class);

    public void register(User user) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                if (exists(connection, user.getName(), user.getPhone())) {
                    throw new IllegalArgumentException("姓名或手机号已注册");
                }
                user.setId(java.util.UUID.randomUUID().toString());
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(user.getCreateTime());
                JDBCTool.save(user, connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof SQLException sqlException) throw sqlException;
                if (e instanceof RuntimeException runtimeException) throw runtimeException;
                throw new SQLException(e);
            }
        }
    }

    public User login(String name, String password) throws SQLException {
        try (Connection connection = Database.connect()) {
            String role;
            try (PreparedStatement statement = connection.prepareStatement("SELECT role FROM `user` WHERE name=?")) {
                statement.setString(1, name);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    role = result.getString(1);
                }
            }
            Class<? extends User> type = TYPES.get(role);
            if (type == null) return null;
            var rows = JDBCTool.queryList("SELECT * FROM `user` WHERE name=? AND password=?", type, connection, name, password);
            if (rows.isEmpty()) return null;
            User user = rows.get(0);
            try (PreparedStatement statement = connection.prepareStatement("UPDATE `user` SET loginTime=? WHERE id=?")) {
                statement.setObject(1, LocalDateTime.now());
                statement.setString(2, user.getId());
                statement.executeUpdate();
            }
            user.setLoginTime(LocalDateTime.now());
            return user;
        }
    }

    private boolean exists(Connection connection, String name, String phone) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM `user` WHERE name=? OR phone=? LIMIT 1")) {
            statement.setString(1, name);
            statement.setString(2, phone);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }
}
