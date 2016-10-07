package io.baxian.bql.sql;

import io.baxian.bql.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UpdateTest {

    @Test
    public void simple() throws BQLException {
        String bql = "UPDATE logs SET a = 1, b = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("UPDATE `logs` SET `a` = 1, `b` = 2", sql);
    }

    @Test
    public void simpleCondition() throws BQLException {
        String bql = "UPDATE `logs` SET l = :a10 where a = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a10", 10);

        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("UPDATE `logs` SET `l` = ? WHERE `a` = ?", sql);

        List<BQLOption> optionValues = compiler.getOptions();
        assertEquals(2, optionValues.size());
        assertEquals(10, optionValues.get(0).getValue());
    }

    @Test
    public void withoutOptions() throws BQLException {
        String bql = "UPDATE `logs` SET a = :a, b = 2, c = 3 where d = :d";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("d", 10);

        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("UPDATE `logs` SET `b` = 2, `c` = 3 WHERE `d` = ?", sql);

        List<BQLOption> optionValues = compiler.getOptions();
        assertEquals(1, optionValues.size());
        assertEquals(10, optionValues.get(0).getValue());
    }

    @Test
    public void nullValue() throws BQLException {
        String bql = "UPDATE `logs` SET l = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a10", null);

        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("UPDATE `logs` SET `l` = NULL", sql);

        List<BQLOption> optionValues = compiler.getOptions();
        assertEquals(0, optionValues.size());
    }

    @Test
    public void expression1() throws BQLException {
        String bql = "UPDATE logs SET a = 1 + 2, b = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("UPDATE `logs` SET `a` = 1 + 2, `b` = 2", sql);
    }

    @Test
    public void expression2() throws BQLException {
        String bql = "UPDATE logs SET a = a + 2, b = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("UPDATE `logs` SET `a` = `a` + 2, `b` = 2", sql);
    }

    @Test
    public void expression3() throws BQLException {
        String bql = "UPDATE skus SET " +
                "inbound_quantity = inbound_quantity + :inbound_quantity," +
                "in_warehouse_quantity = in_warehouse_quantity + :inbound_quantity " +
                " WHERE sku = :sku";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("inbound_quantity", 10);
        options.put("sku", 2);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));
        String sql = compiler.output();

        assertEquals("UPDATE `skus` SET " +
                "`inbound_quantity` = `inbound_quantity` + ?," +
                " `in_warehouse_quantity` = `in_warehouse_quantity` + ?" +
                " WHERE `sku` = ?", sql);

        List<BQLOption> optimizedOptions = compiler.getOptions();
        assertEquals(3, optimizedOptions.size());
    }

    @Test(expected = MissingOptionException.class)
    public void missingOptions() throws BQLException {
        String bql = "UPDATE skus SET a = 1 WHERE sku = :sku";

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

















