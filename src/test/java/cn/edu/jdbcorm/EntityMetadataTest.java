package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 验证不合法的映射在执行 SQL 前被拒绝。 */
class EntityMetadataTest {
    @Table("colleges; DROP TABLE colleges") static class InvalidTable {
        @Id @Column("id") String id;
    }
    @Table("bad") static class MissingId { @Column("name") String name; }
    @Table("bad") static class DuplicateColumn {
        @Id @Column("id") String id;
        @Column("id") String duplicate;
    }

    @Test void rejectsUnsafeOrAmbiguousMappings() {
        assertThrows(OrmException.class, () -> EntityMetadata.of(InvalidTable.class));
        assertThrows(OrmException.class, () -> EntityMetadata.of(MissingId.class));
        assertThrows(OrmException.class, () -> EntityMetadata.of(DuplicateColumn.class));
    }
}
