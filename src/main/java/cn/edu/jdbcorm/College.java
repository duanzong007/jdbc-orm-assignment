package cn.edu.jdbcorm;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 实体主键使用字符串，由调用方赋值。 */
@Table("colleges")
public class College {
    @Id @Column("id")
    private String id;
    @Column("name")
    private String name;
    @Column("code")
    private String code;

    public College() {}

    public College(String id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @Override
    public String toString() {
        return "College{" + "id=" + id + ", " + "name=" + name + ", " + "code=" + code + "}";
    }
}
