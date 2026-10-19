package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationRepositoryTest {
    @Test void approvalAddsTheBookAndItsFirstCopy() throws Exception {
        Teacher user = new Teacher();
        user.setName("推荐测试" + java.util.UUID.randomUUID().toString().substring(0, 8));
        user.setPassword("test123");
        user.setPhone("198" + java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8));
        user.setRole("教师");
        user.setMajor("计算机科学");
        user.setCollege("测试学院");
        new UserRepository().register(user);
        RecommendationRepository repository = new RecommendationRepository();
        BookRecommendation item = new BookRecommendation();
        item.setUserId(user.getId());
        item.setBookName("推荐入库测试");
        item.setAuthor("测试作者");
        item.setPublisher("测试出版社");
        item.setPrice(20.0);
        repository.submit(item);
        repository.review(item.getId(), true, null);
        try (var connection = Database.connect()) {
            try (var statement = connection.prepareStatement("SELECT id FROM book WHERE title=? AND author=?")) {
                statement.setString(1, item.getBookName());
                statement.setString(2, item.getAuthor());
                try (var result = statement.executeQuery()) {
                    result.next();
                    String bookId = result.getString(1);
                    try (var copies = connection.prepareStatement("SELECT COUNT(*) FROM bookcopy WHERE bookId=?")) {
                        copies.setString(1, bookId);
                        try (var copyRows = copies.executeQuery()) { copyRows.next(); assertEquals(1, copyRows.getInt(1)); }
                    }
                    try (var cleanup = connection.prepareStatement("DELETE FROM bookcopy WHERE bookId=?")) {
                        cleanup.setString(1, bookId);
                        cleanup.executeUpdate();
                    }
                    try (var cleanup = connection.prepareStatement("DELETE FROM book WHERE id=?")) {
                        cleanup.setString(1, bookId);
                        cleanup.executeUpdate();
                    }
                }
            }
            try (var cleanup = connection.prepareStatement("DELETE FROM bookrecommend WHERE id=?")) {
                cleanup.setString(1, item.getId());
                cleanup.executeUpdate();
            }
            try (var cleanup = connection.prepareStatement("DELETE FROM `user` WHERE id=?")) {
                cleanup.setString(1, user.getId());
                cleanup.executeUpdate();
            }
        }
    }
}
