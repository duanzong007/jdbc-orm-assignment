package cn.edu.jdbcorm;

import java.util.List;

@Table("book")
public class Book extends Base {
    @Column("title") private String title;
    @Column("author") private String author;
    @Column("type") private String type;
    @Column("publisher") private String publisher;
    @Column("days") private Integer days;
    @Column("price") private Double price;
    @Column("stock") private Integer stock;
    @Column("status") private String status;
    @Column("introduction") private String introduction;
    private List<BookCopy> bookCopies;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public List<BookCopy> getBookCopies() { return bookCopies; }
    public void setBookCopies(List<BookCopy> bookCopies) { this.bookCopies = bookCopies; }
}
