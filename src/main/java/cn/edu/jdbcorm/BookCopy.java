package cn.edu.jdbcorm;

@Table("bookcopy")
public class BookCopy extends Base {
    @Column("bookId") private String bookId;
    @Column("isBorrowed") private Boolean borrowed;

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public Boolean getBorrowed() { return borrowed; }
    public void setBorrowed(Boolean borrowed) { this.borrowed = borrowed; }
}
