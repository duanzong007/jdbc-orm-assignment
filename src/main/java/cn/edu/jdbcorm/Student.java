package cn.edu.jdbcorm;

@Table("user")
public class Student extends User {
    @Column("grade") private String grade;
    @Column("college") private String college;

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    @Override public String[] hasPermission() { return new String[]{"borrow", "return", "my-loans"}; }
}
