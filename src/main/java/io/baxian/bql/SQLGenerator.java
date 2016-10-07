package io.baxian.bql;

import io.baxian.bql.framework.node.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luoweibin on 8/17/15.
 */
public class SQLGenerator extends BQLGenerator {

    private Map<String, Object> options;

    private StringBuilder sql;

    private Map<String, Integer> opPriorities = new HashMap<String, Integer>();

    public SQLGenerator() {
        this(new HashMap<String, Object>());
    }

    public SQLGenerator(Map<String, Object> options) {
        if (options != null) {
            this.options = options;
        }

        opPriorities.put(ABitInvertValue.class.getName(), 1);

        opPriorities.put(AMultiplyValue.class.getName(), 2);
        opPriorities.put(AModuloValue.class.getName(), 2);
        opPriorities.put(ADivideValue.class.getName(), 2);

        opPriorities.put(AAddValue.class.getName(), 3);
        opPriorities.put(ASubstractValue.class.getName(), 3);

        opPriorities.put(ABitAndValue.class.getName(), 4);

        opPriorities.put(ABitXorValue.class.getName(), 5);

        opPriorities.put(ABitOrValue.class.getName(), 6);

        opPriorities.put(ALeftShiftValue.class.getName(), 7);
        opPriorities.put(ARightShiftValue.class.getName(), 7);
    }

    @Override
    public void inStart(Start node) {
        sql = new StringBuilder();
    }

    @Override
    public void caseASelectStatement(ASelectStatement statement) {
        sql.append("SELECT ");

        buildSelectColumns(sql, statement.getColumns());

        sql.append(" FROM ");

        buildTable(sql, statement.getTable());

        buildPartitions(sql, statement.getPartitions());

        PExpr condition = statement.getWhereCondition();
        if (condition != null) {
            buildWhereClause(sql, statement.getWhereCondition());
        }

        List<PColumnComponents> groupByColumns = statement.getGroupByColumns();
        if (!groupByColumns.isEmpty()) {
            buildGroupByColumns(sql, groupByColumns);
        }

        List<POrderByItem> orderByItems = statement.getOrderByItems();
        if (!orderByItems.isEmpty()) {
            buildOrderByItems(sql, orderByItems);
        }

        PLimit limit = statement.getLimit();
        if (limit != null) {
            buildLimit(sql, limit);
        }
    }

    @Override
    public void caseADeleteStatement(ADeleteStatement statement) {
        sql.append("DELETE FROM ");

        buildTable(sql, statement.getTable());

        PExpr condition = statement.getWhereCondition();
        if (condition != null) {
            buildWhereClause(sql, statement.getWhereCondition());
        }

        PLimit limit = statement.getLimit();
        if (limit != null) {
            buildLimit(sql, limit);
        }
    }

    @Override
    public void caseAUpdateStatement(AUpdateStatement statement) {
        sql.append("UPDATE ");

        buildTable(sql, statement.getTable());

        sql.append(" SET ");

        buildUpdateColumns(sql, statement.getColumns());

        PExpr condition = statement.getWhereCondition();
        if (condition != null) {
            buildWhereClause(sql, statement.getWhereCondition());
        }

        PLimit limit = statement.getLimit();
        if (limit != null) {
            buildLimit(sql, limit);
        }
    }

    @Override
    public void caseAInsertStatement(AInsertStatement statement) {
        sql.append("INSERT INTO ");

        buildTable(sql, statement.getTable());

        List<TIdentifier> columns = statement.getColumns();
        buildInsertColumns(sql, columns);

        sql.append(" VALUES ");

        List<PRow> rows = statement.getRows();
        buildInsertValues(sql, rows);

        buildInsertUpdateClause(sql, statement.getUpdateCloumns());
    }

    private void buildInsertUpdateClause(StringBuilder sql, List<PUpdateColumn> columns) {
        if (!columns.isEmpty()) {
            sql.append(" ON DUPLICATE KEY UPDATE ");

            buildInsertUpdateColumns(sql, columns);
        }
    }

    private void buildInsertUpdateColumns(StringBuilder sql, List<PUpdateColumn> columns) {
        boolean hasComma = false;

        for (PUpdateColumn c : columns) {
            if (hasComma) {
                sql.append(", ");
            }

            AUpdateColumn aColumn = (AUpdateColumn)c;

            PValue value = aColumn.getValue();
            if (value instanceof AOptionValue) {
                AColumnComponents comps = (AColumnComponents) aColumn.getComponents();
                sql.append(toColumnString(comps));
                sql.append(" = ");
                sql.append(toOptionValueString(value));
                hasComma = true;
            } else {
                AColumnComponents comps = (AColumnComponents) aColumn.getComponents();
                sql.append(toColumnString(comps));
                sql.append(" = ");
                sql.append(toValueString(value));
                hasComma = true;
            }
        }
    }

    private void buildInsertValues(StringBuilder sql, List<PRow> rows) {
        for (int i = 0; i < rows.size(); ++i) {
            if (i > 0) {
                sql.append(", ");
            }

            sql.append("(");

            ARow aRow = (ARow) rows.get(i);
            List<PValue> valueList = aRow.getValues();
            for (int j = 0; j < valueList.size(); ++j) {
                if (j > 0) {
                    sql.append(", ");
                }

                PValue value = valueList.get(j);
                if (value instanceof AOptionValue) {
                    sql.append(toValueString(value));
                } else {
                    sql.append(toValueString(value));
                }
            }

            sql.append(")");
        }
    }

    private void buildInsertColumns(StringBuilder sql, List<TIdentifier> columns) {
        sql.append(" (");

        for (int i = 0; i < columns.size(); ++i) {
            if (i > 0) {
                sql.append(", ");
            }

            TIdentifier column = columns.get(i);
            sql.append("`");
            sql.append(column.getText());
            sql.append("`");
        }

        sql.append(")");
    }

    private void buildUpdateColumns(StringBuilder sql, List<PUpdateColumn> columns) {
        boolean hasComma = false;

        for (PUpdateColumn c : columns) {
            if (hasComma) {
                sql.append(", ");
            }

            AUpdateColumn aColumn = (AUpdateColumn)c;

            AColumnComponents comps = (AColumnComponents) aColumn.getComponents();
            sql.append(toColumnString(comps));
            sql.append(" = ");
            sql.append(toValueString(aColumn.getValue()));

            hasComma = true;
        }
    }

    private void buildSelectColumns(StringBuilder sql, List<PColumn> columns) {
        for (int i = 0; i < columns.size(); ++i) {
            if (i > 0) {
                sql.append(", ");
            }

            AColumn aColumn = (AColumn)columns.get(i);
            TIdentifier function = aColumn.getFunction();
            if (function == null) {
                if (aColumn.getDistinct() != null) {
                    sql.append("DISTINCT ");
                }

                AColumnComponents comps = (AColumnComponents) aColumn.getComponents().get(0);
                sql.append(toColumnString(comps));
            } else {
                sql.append(function.getText());
                sql.append("(");

                boolean hasComma = false;
                List<PColumnComponents> operands = aColumn.getComponents();
                for (PColumnComponents comp : operands) {
                    if (hasComma) {
                        sql.append(", ");
                    }
                    AColumnComponents aComp = (AColumnComponents)comp;
                    sql.append(toColumnString(aComp));

                    hasComma = true;
                }

                sql.append(")");
            }

            TIdentifier alias = aColumn.getAlias();
            if (alias != null) {
                sql.append(" AS `");
                sql.append(alias.getText());
                sql.append("`");
            }
        }
    }

    private void buildTable(StringBuilder sql, PTable t) {
        ATable aTable = (ATable)t;
        ATableComponents comps = (ATableComponents) aTable.getComponents();

        TIdentifier schema = comps.getSchema();
        if (schema != null) {
            sql.append("`");
            sql.append(schema.getText());
            sql.append("`.");
        }

        TIdentifier table = comps.getTable();
        sql.append("`");
        sql.append(table.getText());
        sql.append("`");

        TIdentifier alias = aTable.getAlias();
        if (alias != null) {
            sql.append(" AS `");
            sql.append(alias.getText());
            sql.append("`");
        }
    }

    private void buildPartitions(StringBuilder sql, List<PValue> partitions) {
        if (!partitions.isEmpty()) {
            sql.append(" PARTITION (");

            int i = 0;
            for (PValue partition : partitions) {
                if (i > 0) {
                    sql.append(", ");
                }

                sql.append(toValueString(partition, false));

                ++i;
            }

            sql.append(")");
        }
    }

    private void buildWhereClause(StringBuilder sql, PExpr root) {
        String condition = visitExpr(root);

        sql.append(" WHERE ");
        sql.append(condition);
    }

    private String visitExpr(PExpr expr) {
        if (expr instanceof AConditionExpr) {
            AConditionExpr condition = (AConditionExpr) expr;

            StringBuilder result = new StringBuilder();

            PExpr left = condition.getLeft();

            boolean leftParenNeeded = conditionParenNeeded(condition, left);
            if (leftParenNeeded) {
                result.append("(");
            }
            result.append(visitExpr(left));
            if (leftParenNeeded) {
                result.append(")");
            }

            result.append(" ");
            result.append(toOpString(condition.getOp()));
            result.append(" ");

            PExpr right = condition.getRight();

            boolean rightParenNeed = conditionParenNeeded(condition, right);
            if (rightParenNeed) {
                result.append("(");
            }
            result.append(visitExpr(right));
            if (rightParenNeed) {
                result.append(")");
            }

            return result.toString();
        } else if (expr instanceof ANotExpr) {
            ANotExpr aNotExpr = (ANotExpr)expr;

            boolean parenNeeded = notParenNeeded(aNotExpr.getExpr());

            StringBuilder result = new StringBuilder();
            result.append("NOT ");

            if (parenNeeded) {
                result.append("(");
            }

            result.append(visitExpr(aNotExpr.getExpr()));

            if (parenNeeded) {
                result.append(")");
            }

            return result.toString();
        } else {
            return buildExprSQL(expr);
        }
    }

    private String buildExprSQL(PExpr expr) {
        if (expr instanceof ABinaryExpr) {
            return buildBinaryExpr((ABinaryExpr)expr);
        } else if (expr instanceof ABetweenExpr) {
            return buildBetweenExpr((ABetweenExpr) expr);
        } else if (expr instanceof AInExpr) {
            return buildInExpr((AInExpr) expr);
        } else if (expr instanceof ANotInExpr) {
            return buildNotInExpr((ANotInExpr) expr);
        } else if (expr instanceof ANotExpr) {
            return buildNotExpr((ANotExpr) expr);
        } else if (expr instanceof AIsExpr) {
            return buildIsExpr((AIsExpr) expr);
        } else if (expr instanceof AIsNotExpr) {
            return buildIsNotExpr((AIsNotExpr)expr);
        } else {
            return String.format("__UNSUPPORTED_EXPRESSION__(%s)", expr.toString());
        }
    }

    private String buildIsNotExpr(AIsNotExpr expr) {
        StringBuilder result = new StringBuilder();

        result.append(toColumnString((AColumnComponents)expr.getColumnComponents()));
        result.append(" IS NOT ");
        result.append(toValueString(expr.getValue()));

        return result.toString();
    }

    private String buildIsExpr(AIsExpr expr) {
        StringBuilder result = new StringBuilder();

        result.append(toColumnString((AColumnComponents)expr.getColumnComponents()));
        result.append(" IS ");
        result.append(toValueString(expr.getValue()));

        return result.toString();
    }

    private String buildNotExpr(ANotExpr expr) {
        return "NOT " + buildExprSQL(expr.getExpr());
    }

    private String buildBinaryExpr(ABinaryExpr expr) {
        PValue left = expr.getLeft();
        PValue right = expr.getRight();

        StringBuilder result = new StringBuilder();
        result.append(toValueString(left));

        result.append(" ");
        result.append(toOpString(expr.getOp()));
        result.append(" ");

        result.append(toValueString(right));

        return result.toString();
    }

    private String buildBetweenExpr(ABetweenExpr expr) {
        AColumnComponents components = (AColumnComponents) expr.getColumnComponents();

        StringBuilder result = new StringBuilder();
        result.append(toColumnString(components));
        result.append(" BETWEEN ");
        result.append(toValueString(expr.getLeft()));
        result.append(" AND ");
        result.append(toValueString(expr.getRight()));

        return result.toString();
    }

    private String buildInExpr(AInExpr expr) {
        StringBuilder result = new StringBuilder();

        AColumnComponents comps = (AColumnComponents)expr.getColumnComponents();
        result.append(toColumnString(comps));
        result.append(" IN (");

        {
            int i = 0;
            for (PValue value : expr.getValue()) {
                if (i > 0) {
                    result.append(", ");
                }

                result.append(toValueString(value));

                ++i;
            }
        }

        result.append(")");

        return result.toString();
    }

    private String buildNotInExpr(ANotInExpr expr) {
        StringBuilder result = new StringBuilder();

        AColumnComponents comps = (AColumnComponents)expr.getColumnComponents();
        result.append(toColumnString(comps));
        result.append(" NOT IN (");

        {
            int i = 0;
            for (PValue value : expr.getValue()) {
                if (i > 0) {
                    result.append(", ");
                }

                result.append(toValueString(value));

                ++i;
            }
        }

        result.append(")");

        return result.toString();
    }

    private String toOpString(POp op) {
        if (op instanceof ALtOp) {
            return "<";
        } else if (op instanceof AGtOp) {
            return ">";
        } else if (op instanceof ALteqOp) {
            return "<=";
        } else if (op instanceof AGteqOp) {
            return ">=";
        } else if (op instanceof AEqOp) {
            return "=";
        } else if (op instanceof ANotEqOp) {
            return "!=";
        } else if (op instanceof AOrOp) {
            return "OR";
        } else if (op instanceof AAndOp) {
            return "AND";
        } else {
            return String.format("__UNSUPPORTED_OPERATOR__(%s)", op.getClass().getName());
        }
    }

    private boolean conditionParenNeeded(AConditionExpr parent, PExpr child) {
        POp op = parent.getOp();
        if (!(op instanceof AAndOp)) {
            return false;
        }

        if (!(child instanceof AConditionExpr)) {
            return false;
        }

        AConditionExpr condition = (AConditionExpr)child;
        return condition.getOp() instanceof AOrOp;
    }

    private boolean notParenNeeded(PExpr child) {
        if (!(child instanceof AConditionExpr)) {
            return false;
        }

        AConditionExpr condition = (AConditionExpr)child;
        return condition.getOp() instanceof AOrOp;
    }

    private void buildGroupByColumns(StringBuilder sql, List<PColumnComponents> columns) {
        sql.append(" GROUP BY ");

        int i = 0;
        for (PColumnComponents c : columns) {
            if (i > 0) {
                sql.append(",");
            }

            AColumnComponents comps = (AColumnComponents)c;
            sql.append(toColumnString(comps));

            ++i;
        }
    }

    private void buildOrderByItems(StringBuilder sql, List<POrderByItem> items) {
        sql.append(" ORDER BY ");

        int i = 0;
        for (POrderByItem item : items) {
            if (i > 0) {
                sql.append(", ");
            }

            AOrderByItem orderByItem = (AOrderByItem)item;
            sql.append(toColumnString((AColumnComponents)orderByItem.getColumn()));
            sql.append(" ");

            POrder order = orderByItem.getOrder();
            if (order instanceof AAscOrder) {
                sql.append("ASC");
            } else if (order instanceof ADescOrder) {
                sql.append("DESC");
            }

            ++i;
        }
    }

    private void buildLimit(StringBuilder sql, PLimit limit) {
        ALimit aLimit = (ALimit)limit;

        PValue offset = aLimit.getOffset();
        PValue count = aLimit.getCount();

        if (offset != null && count != null) {
            sql.append(" LIMIT ");
            sql.append(toValueString(offset));
            sql.append(", ");
            sql.append(toValueString(count));
        } else if (count != null) {
            sql.append(" LIMIT ");
            sql.append(toValueString(count));
        }
    }

    private String toColumnString(AColumnComponents columnComponents) {
        StringBuilder result = new StringBuilder();
        TIdentifier schema = columnComponents.getSchema();
        if (schema != null) {
            result.append("`");
            result.append(schema.getText());
            result.append("`");
            result.append(".");
        }

        TIdentifier table = columnComponents.getTable();
        if (table != null) {
            result.append("`");
            result.append(table.getText());
            result.append("`");
            result.append(".");
        }

        TIdentifier column = columnComponents.getColumn();
        result.append("`");
        result.append(column.getText());
        result.append("`");

        return result.toString();
    }

    private String toValueStringWithParen(PValue parent, PValue value) {
        StringBuilder result = new StringBuilder();

        int parenNeeded = compareOperatorPriority(parent, value);
        if (parenNeeded < 0) {
            result.append("(");
        }

        result.append(toValueString(value));

        if (parenNeeded < 0) {
            result.append(")");
        }

        return result.toString();
    }

    private String toValueString(PValue value) {
        return toValueString(value, true);
    }

    private String toValueString(PValue value, boolean quoteIdentifier) {
        if (value instanceof AOptionValue) {
            return toOptionValueString(value);
        } else if (value instanceof AColumnValue) {
            AColumnValue column = (AColumnValue) value;
            return toColumnString((AColumnComponents) column.getColumnComponents());
        } else if (value instanceof AAddValue) {
            return toAddValueString((AAddValue) value);
        } else if (value instanceof ASubstractValue) {
            return toSubstractValueString((ASubstractValue) value);
        } else if (value instanceof AMultiplyValue) {
            return toMultiplyValueString((AMultiplyValue) value);
        } else if (value instanceof ADivideValue) {
            return toDivideValueString((ADivideValue) value);
        } else if (value instanceof ABitOrValue) {
            return toBitOrValueString((ABitOrValue) value);
        } else if (value instanceof ABitAndValue) {
            return toBitAndValueString((ABitAndValue) value);
        } else if (value instanceof ABitXorValue) {
            return toBitXorValueString((ABitXorValue) value);
        } else if (value instanceof AModuloValue) {
            return toModuloValueString((AModuloValue) value);
        } else if (value instanceof ALeftShiftValue) {
            return toLeftShiftValueString((ALeftShiftValue) value);
        } else if (value instanceof ARightShiftValue) {
            return toRightShiftValueString((ARightShiftValue) value);
        } else if (value instanceof AIdentifierValue) {
            return toIdentifierValueString(value, quoteIdentifier);
        } else if ((value instanceof AIntValue)
                || (value instanceof AFloatValue)
                || (value instanceof AScientificValue)
                || (value instanceof AStringValue)
                || (value instanceof AHexValue)
                || (value instanceof AOctetValue)) {
            return value.toString().trim();
        } else if (value instanceof ANullValue) {
            return "NULL";
        } else {
            return String.format("__UNSUPPORTED_VALUE__(%s)", value.toString());
        }
    }

    private String toAddValueString(AAddValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" + ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toSubstractValueString(ASubstractValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" - ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toMultiplyValueString(AMultiplyValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" * ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toDivideValueString(ADivideValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" / ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toModuloValueString(AModuloValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueString(value.getLeft()));
        result.append(" % ");
        result.append(toValueString(value.getRight()));
        return result.toString();
    }

    private String toBitOrValueString(ABitOrValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" | ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toBitAndValueString(ABitAndValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" & ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toBitXorValueString(ABitXorValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" ^ ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toLeftShiftValueString(ALeftShiftValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" << ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toRightShiftValueString(ARightShiftValue value) {
        StringBuilder result = new StringBuilder();
        result.append(toValueStringWithParen(value, value.getLeft()));
        result.append(" >> ");
        result.append(toValueStringWithParen(value, value.getRight()));
        return result.toString();
    }

    private String toOptionValueString(PValue value) {
        String option = ((AOptionValue) value).getIdentifier().getText();
        Object optionValue = options.get(option);
        if (optionValue instanceof List) {
            StringBuilder result = new StringBuilder();
            List list = (List)optionValue;
            for (int i = 0; i < list.size(); ++i) {
                if (i > 0) {
                    result.append(", ");
                }
                if (list.get(i) != null) {
                    result.append("?");
                } else {
                    result.append("NULL");
                }
            }
            return result.toString();
        } else {
            if (optionValue != null) {
                return "?";
            } else {
                return "NULL";
            }
        }
    }

    private String getOptionName(AOptionValue value) {
        return value.getIdentifier().getText();
    }

    private String toIdentifierValueString(PValue value, boolean quoteIdentifier) {
        StringBuilder result = new StringBuilder();

        boolean isIdentity = (value instanceof AIdentifierValue);
        if (isIdentity && quoteIdentifier) {
            result.append("`");
        }

        result.append(value.toString().trim());

        if (isIdentity && quoteIdentifier) {
            result.append("`");
        }

        return result.toString();
    }

    private int compareOperatorPriority(PValue parent, PValue child) {
        if (parent == null) {
            return 0;
        }

        if (child == null) {
            return 0;
        }

        String parentClass = parent.getClass().getName();
        String childClass = child.getClass().getName();

        int parentPriority = opPriorities.getOrDefault(parentClass, 0);
        int childPriority = opPriorities.getOrDefault(childClass, 0);

        return parentPriority - childPriority;
    }

    @Override
    public String output() {
        return sql.toString();
    }
}





















