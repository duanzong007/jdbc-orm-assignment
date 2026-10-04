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
                if (column == null) throw new OrmException("Unmapped result column: " + label);
                if (selected.contains(column)) throw new OrmException("Duplicate result column: " + label);
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
            throw new OrmException("Cannot map query result to " + clazz.getSimpleName(), e);
        }
    }

    private static Object convert(Object value, Class<?> type) {
        if (value == null) {
            if (type.isPrimitive()) throw new OrmException("SQL NULL cannot be assigned to a primitive field");
            return null;
        }
        if (type.isInstance(value)) return value;
        if (type == LocalDate.class && value instanceof java.sql.Date date) return date.toLocalDate();
        if ((type == Integer.class || type == int.class) && value instanceof Number number) {
            return new BigDecimal(number.toString()).intValueExact();
        }
        if ((type == Boolean.class || type == boolean.class) && value instanceof Boolean) return value;
        throw new OrmException("Unsupported conversion: " + value.getClass().getName() + " -> " + type.getName());
    }
}
