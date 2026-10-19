package cn.edu.jdbcorm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityMetadataTest {
    @Test void readsMappedFieldsFromTheWholeClassHierarchy() {
        EntityMetadata student = EntityMetadata.of(Student.class);
        assertEquals("`user`", student.table);
        assertTrue(student.columns.stream().anyMatch(column -> column.name().equals("id")));
        assertTrue(student.columns.stream().anyMatch(column -> column.name().equals("createTime")));
        assertTrue(student.columns.stream().anyMatch(column -> column.name().equals("name")));
        assertTrue(student.columns.stream().anyMatch(column -> column.name().equals("grade")));
    }

    @Test void ignoresObjectAssociationsThatAreNotDatabaseColumns() {
        EntityMetadata book = EntityMetadata.of(Book.class);
        assertFalse(book.columns.stream().anyMatch(column -> column.field().getName().equals("bookCopies")));
        assertEquals("`book`", book.table);
    }
}
