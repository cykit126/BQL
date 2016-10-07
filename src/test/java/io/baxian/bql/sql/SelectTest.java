package io.baxian.bql.sql;

import io.baxian.bql.BQLCompiler;
import io.baxian.bql.BQLException;
import io.baxian.bql.BQLOption;
import io.baxian.bql.SQLGenerator;
import org.junit.Test;

import java.sql.SQLException;
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
    public void simple() throws BQLException {
        String bql = "select wms.notify.id, notify.`title` from logs";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(null);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("SELECT `wms`.`notify`.`id`, `notify`.`title` FROM `logs`", sql);
    }

    @Test
    public void distinct() throws BQLException {
        String bql = "select distinct id from logs";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(null);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("SELECT DISTINCT `id` FROM `logs`", sql);
    }

    @Test
    public void partitions() throws BQLException {
        String bql = "select `id`, `title` from `logs` PARTITION (p0, p1, p2)";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(null);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("SELECT `id`, `title` FROM `logs` PARTITION (p0, p1, p2)", sql);
    }

    @Test
    public void columnAlias() throws BQLException {
        String bql = "select id as i, `title` as t from `logs`";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(null);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("SELECT `id` AS `i`, `title` AS `t` FROM `logs`", sql);
    }

    @Test
    public void  aggregateAndAlias()throws BQLException{
        String bql = "select count(id) as total from `logs`";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(null);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("SELECT count(`id`) AS `total` FROM `logs`", sql);
    }

    @Test
    public void  aggregate()throws BQLException{
        String bql = "select count(id) as count from `logs`";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(null);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("SELECT count(`id`) AS `count` FROM `logs`", sql);
    }

    @Test
    public void tableAlias() throws BQLException {
        String bql = "select id from `logs` as l";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(null);
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals("SELECT `id` FROM `logs` AS `l`", sql);
    }

    @Test
    public void simpleCondition() throws BQLException {
        String bql = "select id from `logs` as l where a = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);;

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("a10", 10);

        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` AS `l` WHERE `a` = ?", sql);

        List<BQLOption> optionValues = compiler.getOptions();
        assertEquals(1, optionValues.size());

        BQLOption option = optionValues.get(0);
        assertEquals("a10", option.getField());
        assertEquals(10, option.getValue());
    }

    @Test
    public void simpleConditionRemoval() throws BQLException {
        String bql = "select id from `logs` as l where a = :a10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` AS `l`", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(0, values.size());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` WHERE `a` = ? OR `b` = ? AND `c` = ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(3, values.size());
        assertEquals(1, values.get(0).getValue());
        assertEquals(20, values.get(1).getValue());
        assertEquals(10, values.get(2).getValue());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` WHERE `a` = ? OR `c` = ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(2, values.size());
        assertEquals(20, values.get(0).getValue());
        assertEquals(10, values.get(1).getValue());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` WHERE `a` = ? OR `b` = ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(2, values.size());
        assertEquals(20, values.get(0).getValue());
        assertEquals(10, values.get(1).getValue());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` WHERE (`a` = ? OR `b` = ?) AND `c` = ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(3, values.size());
        assertEquals(10, values.get(0).getValue());
        assertEquals(20, values.get(1).getValue());
        assertEquals(30, values.get(2).getValue());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` WHERE (`a` = ? AND `b` = ? OR `c` = ?) AND `d` = ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(4, values.size());
        assertEquals(10, values.get(0).getValue());
        assertEquals(20, values.get(1).getValue());
        assertEquals(30, values.get(2).getValue());
        assertEquals(40, values.get(3).getValue());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE `id` BETWEEN ? AND ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(2, values.size());
        assertEquals(10, values.get(0).getValue());
        assertEquals(20, values.get(1).getValue());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE `id` IN (1, 2, ?, ?)", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(2, values.size());
        assertEquals("b1", values.get(0).getValue());
        assertEquals("b2", values.get(1).getValue());
    }

    @Test
    public void inConditionEmpty1() throws BQLException {
        String bql = "select id from users where id in (1,2,:p2)";

        Map<String, Object> options = new HashMap<String, Object>();

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE `id` IN (1, 2)", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(0, values.size());
    }

    @Test
    public void inConditionEmpty2() throws BQLException {
        String bql = "select id from users where id in (:p2)";

        Map<String, Object> options = new HashMap<String, Object>();

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users`", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(0, values.size());
    }

    @Test
    public void inConditionNull() throws BQLException {
        String bql = "select id from users where id in (:p2)";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p2", null);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE `id` IN (NULL)", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(0, values.size());
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE `id` NOT IN (?, ?, 1, 2)", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(2, values.size());
        assertEquals("b1", values.get(0).getValue());
        assertEquals("b2", values.get(1).getValue());
    }

    @Test
    public void notCondition() throws BQLException {
        String bql = "select id from users where not id = :p";

        Map<String,Object> options = new HashMap<String, Object>();
        options.put("p", 1);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE NOT `id` = ?", sql);
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `logs` WHERE NOT (`a` = ? AND `b` = ? OR `c` = ?) AND `d` = ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(4, values.size());
        assertEquals(10, values.get(0).getValue());
        assertEquals(20, values.get(1).getValue());
        assertEquals(30, values.get(2).getValue());
        assertEquals(40, values.get(3).getValue());
    }

    @Test
    public void notConditionRemoval() throws BQLException {
        String bql = "select id from users where not id = :p";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users`", sql);
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
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE NOT (`id` = ? OR `name` = ?)", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(2, values.size());
        assertEquals(1, values.get(0).getValue());
        assertEquals(2, values.get(1).getValue());
    }

    @Test
    public void isCondition() throws BQLException {
        String bql = "select id from users where id is :p";

        Map<String,Object> options = new HashMap<String, Object>();
        options.put("p", 1);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE `id` IS ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(1, values.size());
        assertEquals(1, values.get(0).getValue());
    }

    @Test
    public void isConditionRemoval() throws BQLException {
        String bql = "select id from users where id is :p";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users`", sql);
    }

    @Test
    public void isNotCondition() throws BQLException {
        String bql = "select id from users where id is not null";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` WHERE `id` IS NOT NULL", sql);
    }

    @Test
    public void isNotConditionRemoval() throws BQLException {
        String bql = "select id from users where id is not :p";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users`", sql);
    }

    @Test
    public void groupBy() throws BQLException {
        String bql = "select id from users group by `id`";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` GROUP BY `id`", sql);
    }

    @Test
    public void groupByAndAggregate()throws BQLException{

        String bql = "select min(age) as max_age from users group by `gender`";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT min(`age`) AS `max_age` FROM `users` GROUP BY `gender`", sql);
    }

    @Test
    public void orderBy() throws BQLException {
        String bql = "select id from users order by `id` desc, `name` asc";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` ORDER BY `id` DESC, `name` ASC", sql);
    }

    @Test
    public void limitCount() throws BQLException {
        String bql = "select id from users limit :p";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p", 10);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` LIMIT ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(1, values.size());
        assertEquals(10, values.get(0).getValue());
    }

    @Test
    public void limitOffsetAndCount() throws BQLException {
        String bql = "select id from users limit :p1,:p2";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("p1", 10);
        options.put("p2", 20);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));

        String sql = compiler.output();
        assertEquals("SELECT `id` FROM `users` LIMIT ?, ?", sql);

        List<BQLOption> values = compiler.getOptions();
        assertEquals(2, values.size());
        assertEquals(10, values.get(0).getValue());
        assertEquals(20, values.get(1).getValue());
    }

    @Test
    public void columnWithSchema() throws BQLException {
        String bql = "select wms.notify.id from users";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users`", sql);
    }

    @Test
    public void columnWithTable() throws BQLException {
        String bql = "select notify.id from users";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `notify`.`id` FROM `users`", sql);
    }

    @Test
    public void intCondition() throws BQLException {
        String bql = "select wms.notify.id from users where id = 1 and age >= -100";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `id` = 1 AND `age` >= -100", sql);
    }

    @Test
    public void conditionWithTable() throws BQLException {
        String bql = "select wms.notify.id from users where t.id = 1";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `t`.`id` = 1", sql);
    }

    @Test
    public void conditionWithSchema() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 1";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `w`.`t`.`id` = 1", sql);
    }

    @Test
    public void stringLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = '10000'";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `w`.`t`.`id` = '10000'", sql);
    }

    @Test
    public void octetLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 0112";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `w`.`t`.`id` = 0112", sql);
    }

    @Test
    public void hexLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 0x112";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `w`.`t`.`id` = 0x112", sql);
    }

    @Test
    public void scientificLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 1.2e+10";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `w`.`t`.`id` = 1.2e+10", sql);
    }

    @Test
    public void floatLiteral() throws BQLException {
        String bql = "select wms.notify.id from users where w.t.id = 1.276";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `w`.`t`.`id` = 1.276", sql);
    }

    @Test
    public void operator() throws BQLException {
        String bql = "select wms.notify.id from users where id / 10 = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `id` / 10 = 2", sql);
    }

    @Test
    public void valueParens() throws BQLException {
        String bql = "select wms.notify.id from users where (id + 4) / 10 = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE (`id` + 4) / 10 = 2", sql);
    }

    @Test
    public void operatorPriority() throws BQLException {
        String bql = "select wms.notify.id from users where id + 4 / 10 = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE `id` + 4 / 10 = 2", sql);
    }

    @Test
    public void valueOpearatorAndCondtionOperator() throws BQLException {
        String bql = "select wms.notify.id from users where (id + 4) / 10 = 2 AND c = 2";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();
        assertEquals("SELECT `wms`.`notify`.`id` FROM `users` WHERE (`id` + 4) / 10 = 2 AND `c` = 2", sql);
    }

    @Test
    public void full() throws SQLException, BQLException {
        String bql = "select id as k, users.name as n, wms.users.age "
                + "from wms.topic as t PARTITION (p1, p2, p3) "
                + "where t.id = 1 or a between 2 and 3 AND id in (4,5,6) "
                + "group by `groupId` "
                + "order by orderId desc "
                + "limit 1,20";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());

        String sql = compiler.output();

        String expect = "SELECT `id` AS `k`, `users`.`name` AS `n`, `wms`.`users`.`age` "
                + "FROM `wms`.`topic` AS `t` PARTITION (p1, p2, p3) "
                + "WHERE `t`.`id` = 1 OR `a` BETWEEN 2 AND 3 AND `id` IN (4, 5, 6) "
                + "GROUP BY `groupId` "
                + "ORDER BY `orderId` DESC "
                + "LIMIT 1, 20";
        assertEquals(expect, sql);
    }

    @Test
    public void zeroLiteralNotRecognized() throws BQLException {
        String bql = "SELECT lpn FROM stock_container_items WHERE LPN = :LPN AND quantity > 0";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("LPN", "1");

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));
        String sql = compiler.output();

        assertEquals("SELECT `lpn` FROM `stock_container_items` WHERE `LPN` = ? AND `quantity` > 0", sql);
    }

    @Test
    public void bit1() throws BQLException {
        String bql = "SELECT id FROM users WHERE (flag & :flag > 0 OR flag = :flag)";

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("flag", 1);

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize(options);
        compiler.generate(new SQLGenerator(options));
        String sql = compiler.output();

        assertEquals(sql, "SELECT `id` FROM `users` WHERE `flag` & ? > 0 OR `flag` = ?");
    }

    @Test
    public void bit2() throws BQLException {
        String bql = "SELECT id FROM users WHERE (flag & :flag > 0 OR flag = :flag)";

        BQLCompiler compiler = new BQLCompiler();
        compiler.compile(bql);
        compiler.optimize();
        compiler.generate(new SQLGenerator());
        String sql = compiler.output();

        assertEquals(sql, "SELECT `id` FROM `users`");
    }
}





























