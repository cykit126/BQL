package io.baxian.bql.sql;

import io.baxian.bql.BQLColumn;
import io.baxian.bql.BQLCompiler;
import io.baxian.bql.BQLException;
import io.baxian.bql.BQLMetadata;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MetadataTest {

    @Test
    public void delete() throws BQLException {
        String bql = "delete from logs";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        BQLMetadata metadata = compiler.getMetadata();
        assertEquals("logs", metadata.getTable());
    }

    @Test
    public void insert() throws BQLException {
        String bql = "insert into logs(`a`, `b`, `c`)values(1, 2, 3)";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        BQLMetadata metadata = compiler.getMetadata();

        assertEquals("logs", metadata.getTable());
    }

    @Test
    public void update() throws BQLException {
        String bql = "UPDATE logs SET a = 1, b = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        BQLMetadata metadata = compiler.getMetadata();

        assertEquals("logs", metadata.getTable());
    }

    @Test
    public void select() throws BQLException {
        String bql = "select wms.notify.no as id, notify.name, age from users";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        BQLMetadata metadata = compiler.getMetadata();
        assertEquals("users", metadata.getTable());

        List<BQLColumn> columns = metadata.getColumns();
        assertEquals(3, columns.size());

        BQLColumn idColumn = columns.get(0);
        assertEquals("notify", idColumn.getTable());
        assertEquals("no", idColumn.getName());
        assertEquals("id", idColumn.getAlias());

        BQLColumn nameColumn = columns.get(1);
        assertEquals("notify", nameColumn.getTable());
        assertEquals("name", nameColumn.getName());
        assertNull(nameColumn.getAlias());

        BQLColumn ageColumn = columns.get(2);
        assertEquals("age", ageColumn.getName());
        assertNull(ageColumn.getTable());
        assertNull(ageColumn.getAlias());
    }
}






















