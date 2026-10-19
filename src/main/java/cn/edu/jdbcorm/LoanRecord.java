package cn.edu.jdbcorm;

import java.time.LocalDateTime;

public record LoanRecord(String id, String bookCopyId, String bookTitle, String userName,
                         LocalDateTime borrowDate, LocalDateTime dueDate,
                         LocalDateTime returnDate, String status) {}
