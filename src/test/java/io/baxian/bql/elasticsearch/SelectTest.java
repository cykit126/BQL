package io.baxian.bql.elasticsearch;

import io.baxian.bql.BQLCompiler;
import io.baxian.bql.BQLException;
import io.baxian.bql.ElasticSearchQueryGenerator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by luoweibin on 8/12/15.
 */
public class SelectTest {

    @Test
    public void simpleCondition() throws BQLException {
        String bql = "select id from `logs` as l where a = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a10", 10);

        compiler.optimize(options);

        ElasticSearchQueryGenerator generator = new ElasticSearchQueryGenerator(options);
        compiler.generate(generator);

        String sql = compiler.output();
        assertEquals("a:10", sql);
    }

    @Test
    public void simpleConditionRemoval() throws BQLException {
        String bql = "select id from `logs` as l where a = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());
        String sql = compiler.output();

        assertEquals("", sql);
    }

    @Test
    public void andOrCondition() throws BQLException {
        String bql = "select id from `logs` WHERE a = :p1 or b = :p2 and c = :p3";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p1", 1);
        options.put("p2", 20);
        options.put("p3", 10);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("a:1 OR b:20 AND c:10", sql);
    }

    @Test
    public void andOrConditionRemoveOr() throws BQLException {
        String bql = "select id from `logs` WHERE a = :p1 or b = :p2 and c = :p3";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p1", 20);
        options.put("p3", 10);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("a:20 OR c:10", sql);
    }

    @Test
    public void andOrConditionRemoveAnd() throws BQLException {
        String bql = "select id from `logs` WHERE a = :p1 or b = :p2 and c = :p3";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p1", 20);
        options.put("p2", 10);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("a:20 OR b:10", sql);
    }

    @Test
    public void conditionParens() throws BQLException {
        String bql = "select id from `logs` WHERE (a = :p1 or b = :p2) and c = :p3";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p1", 10);
        options.put("p2", 20);
        options.put("p3", 30);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("(a:10 OR b:20) AND c:30", sql);
    }

    @Test
    public void conditionParens3() throws BQLException {
        String bql = "select id from `logs` WHERE (a = :p1 AND b = :p2 or c = :p3) and d = :p4";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p1", 10);
        options.put("p2", 20);
        options.put("p3", 30);
        options.put("p4", 40);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("(a:10 AND b:20 OR c:30) AND d:40", sql);
    }

    @Test
    public void betweenCondition() throws BQLException {
        String bql = "select id from users where id between :left and :right";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("left", 10);
        options.put("right", 20);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("id:[10 TO 20]", sql);
    }

    @Test
    public void inCondition() throws BQLException {
        String bql = "select id from users where id in (1,2,:p2)";

        List<String> p2 = new ArrayList<String>();
        p2.add("b1");
        p2.add("b2");

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p2", p2);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("id:(1 OR 2 OR b1 OR b2)", sql);
    }

    @Test
    public void inConditionEmpty1() throws BQLException {
        String bql = "select id from users where id in (1,2,:p2)";

        Map<String, Object> options = new HashMap<String, Object>();

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("id:(1 OR 2)", sql);
    }

    @Test
    public void inConditionEmpty2() throws BQLException {
        String bql = "select id from users where id in (:p2)";

        Map<String, Object> options = new HashMap<String, Object>();

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("", sql);
    }

    @Test
    public void inConditionNull() throws BQLException {
        String bql = "select id from users where id in (:p2)";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p2", null);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("id:(NULL)", sql);
    }

    @Test
    public void notInCondition() throws BQLException {
        String bql = "select id from users where id not in (:p2, 1, 2)";

        List<String> p2 = new ArrayList<String>();
        p2.add("b1");
        p2.add("b2");

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p2", p2);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("NOT id:(b1 OR b2 OR 1 OR 2)", sql);
    }

    @Test
    public void notCondition() throws BQLException {
        String bql = "select id from users where not id = :p";

        Map<String,Object> options = new HashMap<String, Object>();
        options.put("p", 1);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("NOT id:1", sql);
    }

    @Test
    public void notConditionParens3() throws BQLException {
        String bql = "select id from `logs` WHERE NOT (a = :p1 AND b = :p2 or c = :p3) and d = :p4";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p1", 10);
        options.put("p2", 20);
        options.put("p3", 30);
        options.put("p4", 40);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("NOT (a:10 AND b:20 OR c:30) AND d:40", sql);
    }

    @Test
    public void notConditionRemoval() throws BQLException {
        String bql = "select id from users where not id = :p";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());

        String sql = compiler.output();
        assertEquals("", sql);
    }

    @Test
    public void notConditionParens() throws BQLException {
        String bql = "select id from users where not (id = :p1 or name = :p2)";

        Map<String,Object> options = new HashMap<String, Object>();
        options.put("p1", 1);
        options.put("p2", 2);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();
        assertEquals("NOT (id:1 OR name:2)", sql);
    }

    @Test
    public void intCondition() throws BQLException {
        String bql = "select wms.notify.id from users where id = 1 and age >= -100";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());

        String sql = compiler.output();
        assertEquals("id:1 AND age:>=-100", sql);
    }

    @Test
    public void stringLiteral() throws BQLException {
        String bql = "select id from users where id = '10000'";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());

        String sql = compiler.output();
        assertEquals("id:10000", sql);
    }

    @Test
    public void octetLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 0112";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());

        String sql = compiler.output();
        assertEquals("w.t.id:0112", sql);
    }

    @Test
    public void hexLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 0x112";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());

        String sql = compiler.output();
        assertEquals("w.t.id:0x112", sql);
    }

    @Test
    public void scientificLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 1.2e+10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());

        String sql = compiler.output();
        assertEquals("w.t.id:1.2e+10", sql);
    }

    @Test
    public void floatLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 1.276";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new ElasticSearchQueryGenerator());

        String sql = compiler.output();
        assertEquals("w.t.id:1.276", sql);
    }

    @Test
    public void zeroLiteralNotRecognized() throws BQLException {
        String bql = "SELECT lpn FROM stock_container_items WHERE LPN = :LPN AND quantity > 0";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("LPN", "1");

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new ElasticSearchQueryGenerator(options));

        String sql = compiler.output();

        assertEquals("LPN:1 AND quantity:>0", sql);
    }
}





























