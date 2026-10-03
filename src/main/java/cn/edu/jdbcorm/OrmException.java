package cn.edu.jdbcorm;

/** 封装数据库和反射异常，保留原始原因。 */
public class OrmException extends RuntimeException {
    public OrmException(String message) { super(message); }
    public OrmException(String message, Throwable cause) { super(message, cause); }
}
