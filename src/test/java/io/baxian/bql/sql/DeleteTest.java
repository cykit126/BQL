package io.baxian.bql.sql;

import io.baxian.bql.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeleteTest {

    @Test
    public void simple() throws BQLException {
        String bql = "delete from logs";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("DELETE FROM `logs`", sql);
    }

    @Test
    public void simpleCondition() throws BQLException {
        String bql = "delete from `logs` as l where a = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a10", 10);

        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("DELETE FROM `logs` AS `l` WHERE `a` = ?", sql);

        List<BQLOption> optionValues = compiler.getOptions();
        assertEquals(1, optionValues.size());
        assertEquals(10, optionValues.get(0).getValue());
    }

    @Test
    public void nullTest() throws BQLException {
        String bql = "delete from `logs` as l where a = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a10", null);

        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("DELETE FROM `logs` AS `l` WHERE `a` = NULL", sql);

        List<BQLOption> optionValues = compiler.getOptions();
        assertEquals(0, optionValues.size());
    }

    @Test(expected = MissingOptionException.class)
    public void missingOptions() throws BQLException {
        String bql = "DELETE FROM skus WHERE sku = :sku";

        BQLCompiler compiler = new BQLCompiler();
        try {
            compiler.compile(bql);
        } catch (BQLException e) {
            assertTrue(false);
        }

        compiler.optimize();
        compiler.generate(new SQLGenerator());
    }
}


























