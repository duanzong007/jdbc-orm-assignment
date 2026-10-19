package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoanRepositoryTest {
    @Test void borrowsAnAvailableCopyAndSetsTheDueDate() throws Exception {
        Student user = new Student();
        user.setName("借阅测试" + java.util.UUID.randomUUID().toString().substring(0, 8));
        user.setPassword("test123");
        user.setPhone("199" + java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8));
        user.setRole("学生");
        user.setMajor("计算机科学与技术");
        user.setCollege("测试学院");
        new UserRepository().register(user);
        LoanRepository repository = new LoanRepository();
        Book book = repository.availableBooks().get(0);
        repository.borrow(user.getId(), book.getId());
        LoanRecord loan = repository.mine(user.getId(), true).get(0);
        assertEquals(book.getTitle(), loan.bookTitle());
        assertTrue(loan.dueDate().isAfter(loan.borrowDate()));
        assertThrows(IllegalArgumentException.class, () -> repository.returnBook("other-user", loan.bookCopyId(), false));
        repository.returnBook(user.getId(), loan.bookCopyId(), false);
        assertTrue(repository.mine(user.getId(), true).isEmpty());
        assertEquals("已还", repository.mine(user.getId(), false).get(0).status());
        try (var connection = Database.connect()) {
            try (var statement = connection.prepareStatement("UPDATE bookcopy SET isBorrowed=b'0' WHERE id=?")) {
                statement.setString(1, loan.bookCopyId());
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("DELETE FROM loan WHERE id=?")) {
                statement.setString(1, loan.id());
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("DELETE FROM `user` WHERE id=?")) {
                statement.setString(1, user.getId());
                statement.executeUpdate();
            }
        }
    }
}
