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

    @Test void protectsStockDuringEditAndRequiresAllCopiesReturnedBeforeDiscard() throws Exception {
        Book book = new Book();
        book.setTitle("资料更新测试");
        book.setAuthor("测试作者");
        book.setType("计算机");
        book.setPublisher("测试出版社");
        book.setDays(14);
        book.setPrice(10.5);
        book.setStock(1);
        BookRepository repository = new BookRepository();
        repository.add(book);
        try (var connection = Database.connect()) {
            book.setTitle("更新后的书名");
            book.setStock(0);
            repository.updateInfo(book);
            try (var statement = connection.prepareStatement("SELECT stock FROM book WHERE id=?")) {
                statement.setString(1, book.getId());
                try (var result = statement.executeQuery()) { result.next(); assertEquals(1, result.getInt(1)); }
            }
            try (var statement = connection.prepareStatement("UPDATE bookcopy SET isBorrowed=b'1' WHERE bookId=?")) {
                statement.setString(1, book.getId());
                statement.executeUpdate();
            }
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> repository.discard(book.getId()));
            try (var statement = connection.prepareStatement("UPDATE bookcopy SET isBorrowed=b'0' WHERE bookId=?")) {
                statement.setString(1, book.getId());
                statement.executeUpdate();
            }
            repository.discard(book.getId());
            try (var statement = connection.prepareStatement("SELECT status FROM book WHERE id=?")) {
                statement.setString(1, book.getId());
                try (var result = statement.executeQuery()) { result.next(); assertEquals("已废弃", result.getString(1)); }
            }
            try (var statement = connection.prepareStatement("DELETE FROM bookcopy WHERE bookId=?")) {
                statement.setString(1, book.getId());
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("DELETE FROM book WHERE id=?")) {
                statement.setString(1, book.getId());
                statement.executeUpdate();
            }
        }
    }
}
