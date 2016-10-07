package io.baxian.bql;

import io.baxian.bql.framework.node.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by luoweibin on 8/17/15.
 */
public class ElasticSearchQueryGenerator extends BQLGenerator {

    private Map<String, Object> options;

    private StringBuilder query;

    private Map<String, Integer> opPriorities = new HashMap<String, Integer>();

    public ElasticSearchQueryGenerator() {
        this(new HashMap<String, Object>());
    }

    public ElasticSearchQueryGenerator(Map<String, Object> options) {
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
        query = new StringBuilder();
    }

    @Override
    public void caseASelectStatement(ASelectStatement statement) {

        PExpr condition = statement.getWhereCondition();
        if (condition != null) {
            buildWhereClause(query, statement.getWhereCondition());
        }

    }

    public String output() {
        return query.toString();
    }

    private void buildWhereClause(StringBuilder sql, PExpr root) {
        String condition = visitExpr(root);
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
        result.append(toOpString(expr.getOp()));
        result.append(toValueString(right));

        return result.toString();
    }

    private String buildBetweenExpr(ABetweenExpr expr) {
        AColumnComponents components = (AColumnComponents) expr.getColumnComponents();

        StringBuilder result = new StringBuilder();
        result.append(toColumnString(components));
        result.append("[");
        result.append(toValueString(expr.getLeft()));
        result.append(" TO ");
        result.append(toValueString(expr.getRight()));
        result.append("]");

        return result.toString();
    }

    private String buildInExpr(AInExpr expr) {
        StringBuilder result = new StringBuilder();

        AColumnComponents comps = (AColumnComponents)expr.getColumnComponents();
        result.append(toColumnString(comps));
        result.append("(");

        {
            int i = 0;
            for (PValue value : expr.getValue()) {
                if (i > 0) {
                    result.append(" OR ");
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

        result.append("NOT ");
        AColumnComponents comps = (AColumnComponents)expr.getColumnComponents();
        result.append(toColumnString(comps));
        result.append("(");

        {
            int i = 0;
            for (PValue value : expr.getValue()) {
                if (i > 0) {
                    result.append(" OR ");
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
            return "";
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

    private String toColumnString(AColumnComponents columnComponents) {
        StringBuilder result = new StringBuilder();
        TIdentifier schema = columnComponents.getSchema();
        if (schema != null) {
            result.append(schema.getText());
            result.append(".");
        }

        TIdentifier table = columnComponents.getTable();
        if (table != null) {
            result.append(table.getText());
            result.append(".");
        }

        TIdentifier column = columnComponents.getColumn();
        result.append(column.getText());
        result.append(":");

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
        } else if (value instanceof AStringValue) {
            String v = value.toString().trim();
            if (!v.isEmpty()) {
                v = v.substring(1, v.length() - 1);
            }
            return v;
        } else if ((value instanceof AIntValue)
                || (value instanceof AFloatValue)
                || (value instanceof AScientificValue)
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
                    result.append(" OR ");
                }
                if (list.get(i) != null) {
                    result.append(list.get(i).toString());
                } else {
                    result.append("NULL");
                }
            }
            return result.toString();
        } else {
            if (optionValue != null) {
                return optionValue.toString();
            } else {
                return "NULL";
            }
        }
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
}





















