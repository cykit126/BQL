package io.baxian.bql.sql;

import io.baxian.bql.BQLCompiler;
import io.baxian.bql.BQLException;
import io.baxian.bql.BQLOption;
import io.baxian.bql.SQLGenerator;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InsertTest {

    @Test
    public void simple() throws BQLException {
        String bql = "insert into logs(`a`, `b`, `c`)values(1, 2, 3)";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("INSERT INTO `logs` (`a`, `b`, `c`) VALUES (1, 2, 3)", sql);
    }

    @Test
    public void multipleValues() throws BQLException {
        String bql = "insert into logs(a, b, c)values(1, 2, 3),(2, 3, 4)";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("INSERT INTO `logs` (`a`, `b`, `c`) VALUES (1, 2, 3), (2, 3, 4)", sql);
    }

    @Test
    public void withOptions() throws BQLException {
        String bql = "insert into logs(a, b, c)values(:a, :b, 1)";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("INSERT INTO `logs` (`a`, `b`, `c`) VALUES (NULL, NULL, 1)", sql);
    }

    @Test
    public void insertOrUpdate() throws BQLException {
        String bql = "insert into logs(a, b, c)values(:a, :b, 1) ON DUPLICATE KEY UPDATE a=:a, b=:b";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a", 10);
        options.put("b", 20);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));
        String sql = compiler.output();

        assertEquals("INSERT INTO `logs` (`a`, `b`, `c`) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE `a` = ?, `b` = ?", sql);

        List<BQLOption> optionValues = compiler.getOptions();
        assertEquals(4, optionValues.size());
        assertEquals(10, optionValues.get(0).getValue());
        assertEquals(20, optionValues.get(1).getValue());
        assertEquals(10, optionValues.get(2).getValue());
        assertEquals(20, optionValues.get(3).getValue());
    }

    @Test
    public void nullTest() throws BQLException {
        String bql = "insert into logs(a)values(:a)";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a", null);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("INSERT INTO `logs` (`a`) VALUES (NULL)", sql);
    }

    @Test
    public void expressionTest1() throws BQLException {
        String bql = "insert into logs(`a`, `b`, `c`)values(1 + 2, 2, 3)";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("INSERT INTO `logs` (`a`, `b`, `c`) VALUES (1 + 2, 2, 3)", sql);
    }

    @Test
    public void expressionTest2() throws BQLException {
        String bql = "insert into logs(`a`, `b`, `c`)values(:a + 2, 2, 3)";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a", 10);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));
        String sql = compiler.output();

        assertEquals("INSERT INTO `logs` (`a`, `b`, `c`) VALUES (? + 2, 2, 3)", sql);

        List<BQLOption> optimizedOptions = compiler.getOptions();
        assertEquals(1, optimizedOptions.size());
    }

    @Test
    public void keyword() {
        String bql = "INSERT INTO `op_logs`" +
                "(`type`,`key`,`action`,`time`,`who_id`,`who_name`,`data`)" +
                "VALUES(:type, :key, :action, :time, :who_id, :who_name, :data)";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a", 10);

        BQLCompiler compiler = new BQLCompiler();
        try {
            compiler.compile(bql);
            assertTrue(false);
        } catch (BQLException e) {
            assertTrue(true);
        }
    }
}
























