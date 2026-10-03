package cn.edu.jdbcorm;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 实体主键使用字符串，由调用方赋值。 */
@Table("students")
public class Student {
    @Id @Column("id")
    private String id;
    @Column("name")
    private String name;
    @Column("major")
    private String major;
    @Column("age")
    private Integer age;
    @Column("enrollment_date")
    private LocalDate enrollmentDate;
    @Column("graduated")
    private Boolean graduated;
    @Column("tuition")
    private BigDecimal tuition;

    public Student() {}

    public Student(String id, String name, String major, Integer age, LocalDate enrollmentDate, Boolean graduated, BigDecimal tuition) {
        this.id = id;
        this.name = name;
        this.major = major;
        this.age = age;
        this.enrollmentDate = enrollmentDate;
        this.graduated = graduated;
        this.tuition = tuition;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public Boolean getGraduated() { return graduated; }
    public void setGraduated(Boolean graduated) { this.graduated = graduated; }

    public BigDecimal getTuition() { return tuition; }
    public void setTuition(BigDecimal tuition) { this.tuition = tuition; }

    @Override
    public String toString() {
        return "Student{" + "id=" + id + ", " + "name=" + name + ", " + "major=" + major + ", " + "age=" + age + ", " + "enrollmentDate=" + enrollmentDate + ", " + "graduated=" + graduated + ", " + "tuition=" + tuition + "}";
    }
}
