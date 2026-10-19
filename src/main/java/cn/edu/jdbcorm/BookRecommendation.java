package cn.edu.jdbcorm;

@Table("bookrecommend")
public class BookRecommendation extends Base {
    @Column("userId") private String userId;
    @Column("bookName") private String bookName;
    @Column("author") private String author;
    @Column("publisher") private String publisher;
    @Column("price") private Double price;
    @Column("status") private String status;
    @Column("reason") private String reason;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
