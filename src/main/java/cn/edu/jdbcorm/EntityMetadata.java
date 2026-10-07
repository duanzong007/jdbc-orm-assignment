package cn.edu.jdbcorm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** 检查并缓存实体映射，增删改查共用这份信息。 */
final class EntityMetadata {
    private static final ClassValue<EntityMetadata> CACHE = new ClassValue<>() {
        @Override protected EntityMetadata computeValue(Class<?> type) {
            return new EntityMetadata(type);
        }
    };

    final String table;
    final List<Binding> columns;
    final Binding id;
    private final Constructor<?> constructor;

    static EntityMetadata of(Class<?> type) { return CACHE.get(type); }

    private EntityMetadata(Class<?> type) {
        Table annotation = type.getAnnotation(Table.class);
        if (annotation == null) throw new OrmException("实体缺少 @Table 注解：" + type.getName());
        table = quote(annotation.value());
        try {
            constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new OrmException("实体需要无参构造方法：" + type.getName(), e);
        }
        var bindings = new ArrayList<Binding>();
        var names = new HashSet<String>();
        Binding primaryKey = null;
        for (Field field : type.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column == null) continue;
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                throw new OrmException("映射属性必须是可修改的实例字段：" + field.getName());
            }
            quote(column.value());
            if (!names.add(column.value())) throw new OrmException("列名重复：" + column.value());
            field.setAccessible(true);
            Binding binding = new Binding(field, column.value());
            bindings.add(binding);
            if (field.isAnnotationPresent(Id.class)) {
                if (primaryKey != null) throw new OrmException("仅支持一个主键");
                primaryKey = binding;
            }
        }
        if (primaryKey == null) throw new OrmException("实体需要一个带列映射的主键");
        if (primaryKey.field().getType() != String.class) {
            throw new OrmException("主键类型需要是 String");
        }
        columns = List.copyOf(bindings);
        id = primaryKey;
    }

    Object newInstance() {
        try { return constructor.newInstance(); }
        catch (ReflectiveOperationException e) { throw new OrmException("无法创建实体对象", e); }
    }

    List<Binding> nonIdColumns() { return columns.stream().filter(c -> c != id).toList(); }

    Object requiredId(Object entity) {
        Object value = id.read(entity);
        if (value == null || value.toString().isBlank()) throw new OrmException("主键不能为空");
        return value;
    }

    static String quote(String identifier) {
        if (!identifier.matches("[a-z_][a-z0-9_]*")) throw new OrmException("不合法的表名或列名：" + identifier);
        return "\"" + identifier + "\"";
    }

    record Binding(Field field, String name) {
        String sqlName() { return quote(name); }
        Object read(Object entity) {
            try { return field.get(entity); }
            catch (IllegalAccessException e) { throw new OrmException("无法读取属性：" + name, e); }
        }
        void write(Object entity, Object value) {
            try { field.set(entity, value); }
            catch (IllegalAccessException | IllegalArgumentException e) {
                throw new OrmException("无法赋值列：" + name + " to " + field.getType().getName(), e);
            }
        }
    }
}
