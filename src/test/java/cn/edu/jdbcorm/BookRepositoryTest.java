package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookRepositoryTest {
    @Test void createsOneCopyForEachUnitOfStock() throws Exception {
        Book book = new Book();
        book.setTitle("测试图书");
        book.setAuthor("测试作者");
        book.setType("计算机");
        book.setPublisher("测试出版社");
        book.setDays(14);
        book.setPrice(10.5);
        book.setStock(2);
        new BookRepository().add(book);
        try (var connection = Database.connect(); var statement = connection.prepareStatement("SELECT COUNT(*) FROM bookcopy WHERE bookId=?")) {
            statement.setString(1, book.getId());
            try (var result = statement.executeQuery()) {
                result.next();
                assertEquals(2, result.getInt(1));
            }
            try (var cleanup = connection.prepareStatement("DELETE FROM bookcopy WHERE bookId=?")) {
                cleanup.setString(1, book.getId());
                cleanup.executeUpdate();
            }
            try (var cleanup = connection.prepareStatement("DELETE FROM book WHERE id=?")) {
                cleanup.setString(1, book.getId());
                cleanup.executeUpdate();
            }
        }
    }
}
