package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryTest {
    @Test void registersMapsAndRejectsDuplicateStudentAccounts() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        Student student = new Student();
        student.setName("注册测试" + suffix);
        student.setPassword("test123");
        student.setPhone("197" + java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8));
        student.setRole("学生");
        student.setMajor("计算机科学与技术");
        student.setGrade("2026级");
        student.setCollege("测试学院");
        UserRepository repository = new UserRepository();
        repository.register(student);
        try {
            User actual = repository.login(student.getName(), student.getPassword());
            assertInstanceOf(Student.class, actual);
            assertEquals(student.getCollege(), ((Student) actual).getCollege());
            Student duplicateName = new Student();
            duplicateName.setName(student.getName());
            duplicateName.setPhone("196" + java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8));
            assertThrows(IllegalArgumentException.class, () -> repository.register(duplicateName));
            Student duplicatePhone = new Student();
            duplicatePhone.setName("另一个账号" + suffix);
            duplicatePhone.setPhone(student.getPhone());
            assertThrows(IllegalArgumentException.class, () -> repository.register(duplicatePhone));
        } finally {
            try (var connection = Database.connect(); var statement = connection.prepareStatement("DELETE FROM `user` WHERE id=?")) {
                statement.setString(1, student.getId());
                statement.executeUpdate();
            }
        }
    }
}
