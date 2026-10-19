package cn.edu.jdbcorm;

import java.time.LocalDateTime;

@Table("loan")
public class Loan extends Base {
    @Column("bookCopyId") private String bookCopyId;
    @Column("userId") private String userId;
    @Column("borrowDate") private LocalDateTime borrowDate;
    @Column("returnDate") private LocalDateTime returnDate;
    @Column("dueDate") private LocalDateTime dueDate;
    @Column("status") private String status;

    public String getBookCopyId() { return bookCopyId; }
    public void setBookCopyId(String bookCopyId) { this.bookCopyId = bookCopyId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
