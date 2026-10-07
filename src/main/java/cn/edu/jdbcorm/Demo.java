package cn.edu.jdbcorm;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.UUID;

/** 演示两张表的增删改查，仅操作本次创建的记录。 */
public final class Demo {
    private Demo() {}

    public static void main(String[] args) throws Exception {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                System.out.println("============================================================");
                System.out.println("  JDBC 反射 ORM | 学生与学院增删改查演示");
                System.out.println("  数据库：" + connection.getCatalog() + " | " + connection.getMetaData().getDatabaseProductName());
                System.out.println("============================================================");
                String suffix = UUID.randomUUID().toString().substring(0, 8);
                demonstrateStudents(connection, suffix);
                demonstrateColleges(connection, suffix);
                connection.commit();
                System.out.println();
                System.out.println("全部验证通过，事务已提交；本次演示记录已清理。");
                System.out.println("============================================================");
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void demonstrateStudents(Connection connection, String suffix) {
        Student first = new Student("S1-" + suffix, "张同学", "计算机科学", 20,
                LocalDate.of(2024, 9, 1), false, new BigDecimal("6800.35"));
        Student second = new Student("S2-" + suffix, "李同学", "软件工程", 21,
                LocalDate.of(2023, 9, 1), false, new BigDecimal("6500.00"));
        System.out.println("\n[学生表 students]");
        require(JDBCTool.save(first, connection) == 1 && JDBCTool.save(second, connection) == 1, "学生新增");
        var list = JDBCTool.queryList("SELECT * FROM students WHERE id IN (?, ?) ORDER BY id",
                Student.class, connection, first.getId(), second.getId());
        require(list.size() == 2, "学生列表查询");
        System.out.println("新增：2 条 | 列表查询：" + list.size() + " 个对象");
        list.forEach(Demo::printStudent);
        first.setMajor("人工智能");
        first.setGraduated(true);
        first.setTuition(new BigDecimal("7200.19"));
        require(JDBCTool.update(first, connection) == 1, "学生更新");
        Student updated = JDBCTool.getOneById(first.getId(), Student.class, connection);
        require(updated != null && updated.getGraduated() && "人工智能".equals(updated.getMajor())
                && first.getTuition().equals(updated.getTuition()), "学生更新结果");
        System.out.println("更新：1 条 | 按 ID 查询更新结果：");
        printStudent(updated);
        require(JDBCTool.delete(first, connection) == 1, "学生删除");
        require(JDBCTool.getOneById(first.getId(), Student.class, connection) == null, "学生删除结果");
        require(JDBCTool.getOneById(second.getId(), Student.class, connection) != null, "其他学生记录");
        System.out.println("删除：1 条 | 再次查询：null | 另一条学生记录仍存在");
        require(JDBCTool.delete(second, connection) == 1, "学生演示数据清理");
    }

    private static void demonstrateColleges(Connection connection, String suffix) {
        College first = new College("C1-" + suffix, "信息工程学院", "CS-" + suffix);
        College second = new College("C2-" + suffix, "经济管理学院", "EC-" + suffix);
        System.out.println("\n[学院表 colleges]");
        require(JDBCTool.save(first, connection) == 1 && JDBCTool.save(second, connection) == 1, "学院新增");
        var list = JDBCTool.queryList("SELECT * FROM colleges WHERE id IN (?, ?) ORDER BY id",
                College.class, connection, first.getId(), second.getId());
        require(list.size() == 2, "学院列表查询");
        System.out.println("新增：2 条 | 列表查询：" + list.size() + " 个对象");
        list.forEach(Demo::printCollege);
        first.setName("计算机与人工智能学院");
        require(JDBCTool.update(first, connection) == 1, "学院更新");
        College updated = JDBCTool.getOneById(first.getId(), College.class, connection);
        require(updated != null && first.getName().equals(updated.getName()), "学院更新结果");
        System.out.println("更新：1 条 | 按 ID 查询更新结果：");
        printCollege(updated);
        require(JDBCTool.delete(first, connection) == 1, "学院删除");
        require(JDBCTool.getOneById(first.getId(), College.class, connection) == null, "学院删除结果");
        require(JDBCTool.getOneById(second.getId(), College.class, connection) != null, "其他学院记录");
        System.out.println("删除：1 条 | 再次查询：null | 另一条学院记录仍存在");
        require(JDBCTool.delete(second, connection) == 1, "学院演示数据清理");
    }

    private static void printStudent(Student student) {
        System.out.printf("  %s | %s | %s | %d 岁%n", student.getId(), student.getName(), student.getMajor(), student.getAge());
        System.out.printf("  入学：%s | 毕业：%s | 学费：%s 元%n", student.getEnrollmentDate(),
                student.getGraduated() ? "是" : "否", student.getTuition().toPlainString());
    }

    private static void printCollege(College college) {
        System.out.printf("  %s | %s | %s%n", college.getId(), college.getName(), college.getCode());
    }

    private static void require(boolean condition, String operation) {
        if (!condition) throw new IllegalStateException(operation + "验证失败");
    }
}
