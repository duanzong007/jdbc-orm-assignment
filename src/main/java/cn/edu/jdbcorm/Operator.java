package cn.edu.jdbcorm;

@Table("user")
public class Operator extends User {
    @Column("area") private String area;
    @Column("duty") private String duty;

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getDuty() { return duty; }
    public void setDuty(String duty) { this.duty = duty; }
    @Override public String[] hasPermission() { return new String[]{"books", "book-edit", "book-copy", "borrow", "return", "my-loans"}; }
}
