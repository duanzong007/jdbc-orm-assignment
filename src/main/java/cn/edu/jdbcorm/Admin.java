package cn.edu.jdbcorm;

import java.time.LocalDate;

@Table("user")
public class Admin extends User {
    @Column("permissionLevel") private Integer permissionLevel;
    @Column("entryTime") private LocalDate entryTime;

    public Integer getPermissionLevel() { return permissionLevel; }
    public void setPermissionLevel(Integer permissionLevel) { this.permissionLevel = permissionLevel; }
    public LocalDate getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDate entryTime) { this.entryTime = entryTime; }
    @Override public String[] hasPermission() { return new String[]{"users", "loans", "statistics", "books", "borrow", "return", "recommend-review"}; }
}
