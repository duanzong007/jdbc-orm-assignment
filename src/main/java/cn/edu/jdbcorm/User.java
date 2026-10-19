package cn.edu.jdbcorm;

import java.time.LocalDateTime;

public abstract class User extends Base {
    @Column("name") private String name;
    @Column("password") private String password;
    @Column("phone") private String phone;
    @Column("role") private String role;
    @Column("major") private String major;
    @Column("loginTime") private LocalDateTime loginTime;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public abstract String[] hasPermission();
}
