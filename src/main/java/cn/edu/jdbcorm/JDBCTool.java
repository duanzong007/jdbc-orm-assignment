package cn.edu.jdbcorm;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 通用 JDBC 工具；连接和传入的结果集由调用方关闭。 */
public final class JDBCTool {
    private JDBCTool() {}

    /** 将剩余查询记录转换成对象，列名或别名需要与 @Column 对应。 */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) {
        Objects.requireNonNull(rs, "rs");
        EntityMetadata mapping = EntityMetadata.of(clazz);
        Map<String, EntityMetadata.Binding> byName = new HashMap<>();
        mapping.columns.forEach(column -> byName.put(column.name(), column));
        try {
            var metadata = rs.getMetaData();
            List<EntityMetadata.Binding> selected = new ArrayList<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                String label = metadata.getColumnLabel(i);
                var column = byName.get(label);
                if (column == null) throw new OrmException("查询列未配置映射：" + label);
                if (selected.contains(column)) throw new OrmException("查询列重复：" + label);
                selected.add(column);
            }
            List<T> result = new ArrayList<>();
            while (rs.next()) {
                T entity = clazz.cast(mapping.newInstance());
                for (int i = 0; i < selected.size(); i++) {
                    var binding = selected.get(i);
                    binding.write(entity, convert(rs.getObject(i + 1), binding.field().getType()));
                }
                result.add(entity);
            }
            return result;
        } catch (SQLException e) {
            throw new OrmException("查询结果映射失败：" + clazz.getSimpleName(), e);
        }
    }

    /** 保存全部映射字段，主键由调用方提供。 */
    public static <T> int save(T obj, Connection connection) {
        Objects.requireNonNull(obj, "obj");
        EntityMetadata mapping = EntityMetadata.of(obj.getClass());
        mapping.requiredId(obj);
        String names = String.join(", ", mapping.columns.stream().map(EntityMetadata.Binding::sqlName).toList());
        String placeholders = String.join(", ", java.util.Collections.nCopies(mapping.columns.size(), "?"));
        String sql = "INSERT INTO " + mapping.table + " (" + names + ") VALUES (" + placeholders + ")";
        return execute(sql, mapping.columns.stream().map(column -> column.read(obj)).toList(), connection);
    }

    /** 根据主键更新其他字段，不修改主键本身。 */
    public static <T> int update(T obj, Connection connection) {
        Objects.requireNonNull(obj, "obj");
        EntityMetadata mapping = EntityMetadata.of(obj.getClass());
        Object id = mapping.requiredId(obj);
        var columns = mapping.nonIdColumns();
        if (columns.isEmpty()) throw new OrmException("实体没有可更新的字段");
        String assignments = String.join(", ", columns.stream().map(column -> column.sqlName() + " = ?").toList());
        List<Object> values = new ArrayList<>(columns.stream().map(column -> column.read(obj)).toList());
        values.add(id);
        return execute("UPDATE " + mapping.table + " SET " + assignments + " WHERE " + mapping.id.sqlName() + " = ?",
                values, connection);
    }

    /** 根据主键删除一条记录，返回实际删除的行数。 */
    public static <T> int delete(T obj, Connection connection) {
        Objects.requireNonNull(obj, "obj");
        EntityMetadata mapping = EntityMetadata.of(obj.getClass());
        Object id = mapping.requiredId(obj);
        return execute("DELETE FROM " + mapping.table + " WHERE " + mapping.id.sqlName() + " = ?",
                List.of(id), connection);
    }

    /** 按主键查询，记录不存在时返回 null。 */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) {
        if (id == null || id.isBlank()) throw new OrmException("查询主键不能为空");
        EntityMetadata mapping = EntityMetadata.of(clazz);
        List<T> result = queryList("SELECT * FROM " + mapping.table + " WHERE " + mapping.id.sqlName() + " = ?",
                clazz, connection, id);
        return result.isEmpty() ? null : result.get(0);
    }

    /** 统一执行带参数的查询，并自动关闭内部创建的语句和结果集。 */
    public static <T> List<T> queryList(String sql, Class<T> clazz, Connection connection, Object... parameters) {
        Objects.requireNonNull(connection, "connection");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
            try (ResultSet result = statement.executeQuery()) {
                return resultSetToList(result, clazz);
            }
        } catch (SQLException e) {
            throw new OrmException("数据库查询失败：" + sql, e);
        }
    }

    /** 值通过占位符绑定，不直接拼接到 SQL 中。 */
    private static int execute(String sql, List<Object> values, Connection connection) {
        Objects.requireNonNull(connection, "connection");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) statement.setObject(i + 1, values.get(i));
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new OrmException("数据库操作失败：" + sql, e);
        }
    }

    /** 将 JDBC 返回的日期、数值等类型转换成实体属性类型。 */
    private static Object convert(Object value, Class<?> type) {
        if (value == null) {
            if (type.isPrimitive()) throw new OrmException("SQL 空值不能赋给基本类型属性");
            return null;
        }
        if (type.isInstance(value)) return value;
        if (type == LocalDate.class && value instanceof java.sql.Date date) return date.toLocalDate();
        if ((type == Integer.class || type == int.class) && value instanceof Number number) {
            return new BigDecimal(number.toString()).intValueExact();
        }
        if ((type == Boolean.class || type == boolean.class) && value instanceof Boolean) return value;
        throw new OrmException("不支持的类型转换：" + value.getClass().getName() + " -> " + type.getName());
    }
}
