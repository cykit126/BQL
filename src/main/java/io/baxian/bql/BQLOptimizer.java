package io.baxian.bql;

import io.baxian.bql.framework.analysis.DepthFirstAdapter;
import io.baxian.bql.framework.node.*;

import java.util.*;

/**
 * Created by luoweibin on 8/17/15.
 */
public class BQLOptimizer extends DepthFirstAdapter {

    private Map<String, Object> options = new HashMap<String, Object>();

    private List<BQLOption> optimizedOptions = new ArrayList<BQLOption>();

    private BQLException error;

    public BQLOptimizer(Map<String, Object> options) {
        if (options != null) {
            this.options = options;
        }
    }

    public List<BQLOption> getOptimizedOptions() {
        return optimizedOptions;
    }

    @Override
    public void caseASelectStatement(ASelectStatement statement) {
        List<PValue> partitions = statement.getPartitions();
        optimizePartitions(partitions, optimizedOptions);

        PExpr current = statement.getWhereCondition();
        try {
            current = visitExpr(current, optimizedOptions, false);
        } catch (MissingOptionException e) {
            error = e;
            return;
        }

        statement.setWhereCondition(current);

        PLimit limit = statement.getLimit();
        statement.setLimit(optimizeLimit(limit, optimizedOptions));
    }

    @Override
    public void caseADeleteStatement(ADeleteStatement statement) {
        PExpr current = statement.getWhereCondition();
        try {
            current = visitExpr(current, optimizedOptions, true);
        } catch (MissingOptionException e) {
            error = e;
            return;
        }

        statement.setWhereCondition(current);

        PLimit limit = statement.getLimit();
        statement.setLimit(optimizeLimit(limit, optimizedOptions));
    }

    @Override
    public void caseAUpdateStatement(AUpdateStatement statement) {
        List<PUpdateColumn> columns = statement.getColumns();
        optimizeUpdateColumns(columns, optimizedOptions);

        PExpr current = statement.getWhereCondition();
        try {
            current = visitExpr(current, optimizedOptions, true);
        } catch (MissingOptionException e) {
            error = e;
            return;
        }
        statement.setWhereCondition(current);

        PLimit limit = statement.getLimit();
        statement.setLimit(optimizeLimit(limit, optimizedOptions));
    }

    @Override
    public void caseAInsertStatement(AInsertStatement statement) {
        try {
            optimizeInsertColumns(statement.getUpdateCloumns());
            optimizeInsertValues(statement.getRows());
        } catch (BQLException e) {
            error = e;
        }
    }

    private void optimizeInsertValues(List<PRow> rows) throws MissingOptionException {
        for (PRow row : rows) {
            ARow aRow = (ARow) row;

            List<PValue> values = aRow.getValues();
            for (int i = 0; i < values.size(); ++i) {
                PValue value = values.get(i);
                PValue newValue = optimizeValue(value, optimizedOptions, false);
                if (newValue != null) {
                    values.set(i, newValue);
                } else {
                    values.set(i, new ANullValue());
                }
            }
        }
    }

    private void optimizeInsertColumns(List<PUpdateColumn> columns) throws MissingOptionException {
        List<PUpdateColumn> optimizedColumns = new ArrayList<PUpdateColumn>();

        for (PUpdateColumn c : columns) {
            AUpdateColumn column = (AUpdateColumn)c;

            PValue oldValue = column.getValue();
            PValue newValue = optimizeValue(oldValue, optimizedOptions, false);
            column.setValue(newValue);
            if (newValue != null) {
                optimizedColumns.add(c);
            }
        }

        columns.clear();
        columns.addAll(optimizedColumns);
    }

    private void optimizeUpdateColumns(List<PUpdateColumn> columns, List<BQLOption> values) {
        List<PUpdateColumn> optimizedColumns = new ArrayList<PUpdateColumn>();

        for (PUpdateColumn c : columns) {
            AUpdateColumn column = (AUpdateColumn)c;

            PValue oldValue = column.getValue();
            PValue newValue = null;
            try {
                newValue = optimizeValue(oldValue, values, false);
            } catch (MissingOptionException e) {
                error = e;
                return;
            }
            column.setValue(newValue);

            if (newValue != null) {
                optimizedColumns.add(c);
            }
        }

        columns.clear();
        columns.addAll(optimizedColumns);
    }

    private void optimizePartitions(List<PValue> partitions, List<BQLOption> values) {
        List<PValue> optimizedPartitions = new ArrayList<PValue>();

        for (PValue partition : partitions) {

            if (partition instanceof AOptionValue) {
                String option = ((AOptionValue)partition).getIdentifier().getText();

                if (options.containsKey(option)) {

                    Object value = options.get(option);
                    if (value != null) {
                        optimizedPartitions.add(partition);

                        if (value instanceof List) {
                            for (Object v : (List)value) {
                                if (v != null) {
                                    values.add(new BQLOption(option, v));
                                }
                            }
                        } else {
                            values.add(new BQLOption(option, value));
                        }
                    }
                }
            } else {
                optimizedPartitions.add(partition);
            }
        }

        partitions.clear();
        partitions.addAll(optimizedPartitions);
    }

    private PLimit optimizeLimit(PLimit limit, List<BQLOption> values) {
        ALimit aLimit = (ALimit)limit;

        PValue offset = aLimit.getOffset();

        if (offset != null && offset instanceof AOptionValue) {
            String option = ((AOptionValue)offset).getIdentifier().getText();
            Object value = options.get(option);
            if (value == null) {
                return null;
            }
            values.add(new BQLOption(option, value));
        }

        PValue count = aLimit.getCount();
        if (count != null && count instanceof AOptionValue) {
            String option = ((AOptionValue)count).getIdentifier().getText();
            Object value = options.get(option);
            if (value == null) {
                return null;
            }
            values.add(new BQLOption(option, value));
        }

        return limit;
    }

    private PExpr optimizeExpr(PExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        if (expr instanceof ABinaryExpr) {
            ABinaryExpr binExpr = (ABinaryExpr)expr;
            return optimizeBinaryExpr(binExpr, values, optionRequired);
        } else if (expr instanceof ABetweenExpr) {
            ABetweenExpr betweenExpr = (ABetweenExpr)expr;
            return optimizeBetweenExpr(betweenExpr, values, optionRequired);
        } else if (expr instanceof AInExpr) {
            AInExpr aInExpr = (AInExpr)expr;
            return optimizeInExpr(aInExpr, values, optionRequired);
        } else if (expr instanceof ANotInExpr) {
            ANotInExpr aNotInExpr = (ANotInExpr) expr;
            return optimizeNotInExpr(aNotInExpr, values, optionRequired);
        } else if (expr instanceof AIsExpr) {
            AIsExpr aIsExpr = (AIsExpr) expr;
            return optimizeIsExpr(aIsExpr, values, optionRequired);
        } else if (expr instanceof AIsNotExpr) {
            AIsNotExpr aIsNotExpr = (AIsNotExpr)expr;
            return optimizeIsNotExpr(aIsNotExpr, values, optionRequired);
        } else {
            return expr;
        }
    }

    private PValue optimizeAddValue(AAddValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeSubstractValueString(ASubstractValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeMultiplyValueString(AMultiplyValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeDivideValueString(ADivideValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeModuloValueString(AModuloValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeBitOrValueString(ABitOrValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeBitAndValueString(ABitAndValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeBitXorValueString(ABitXorValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeLeftShiftValueString(ALeftShiftValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeRightShiftValueString(ARightShiftValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(value.getLeft(), values, optionRequired);
        PValue right = optimizeValue(value.getRight(), values, optionRequired);

        if (left != null && right != null) {
            return value;
        } else {
            return null;
        }
    }

    private PValue optimizeOptionValue(PValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        String option = ((AOptionValue) value).getIdentifier().getText();
        if (options.containsKey(option)) {
            Object optionValue = options.get(option);
            if (optionValue == null) {
                return new ANullValue();
            } else if (optionValue instanceof Collection) {
                Collection collection = (Collection)optionValue;
                if (!collection.isEmpty()) {
                    for (Object v : collection) {
                        values.add(new BQLOption(option, v));
                    }
                } else {
                    return null;
                }
            } else {
                values.add(new BQLOption(option, optionValue));
            }
            return value;
        } else {
            if (optionRequired) {
                throw new MissingOptionException(option);
            }
            return null;
        }
    }

    private PValue optimizeValue(PValue value, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        if (value instanceof AOptionValue) {
            return optimizeOptionValue(value, values, optionRequired);
        } else if (value instanceof AAddValue) {
            return optimizeAddValue((AAddValue) value, values, optionRequired);
        } else if (value instanceof ASubstractValue) {
            return optimizeSubstractValueString((ASubstractValue) value, values, optionRequired);
        } else if (value instanceof AMultiplyValue) {
            return optimizeMultiplyValueString((AMultiplyValue) value, values, optionRequired);
        } else if (value instanceof ADivideValue) {
            return optimizeDivideValueString((ADivideValue) value, values, optionRequired);
        } else if (value instanceof ABitOrValue) {
            return optimizeBitOrValueString((ABitOrValue) value, values, optionRequired);
        } else if (value instanceof ABitAndValue) {
            return optimizeBitAndValueString((ABitAndValue) value, values, optionRequired);
        } else if (value instanceof ABitXorValue) {
            return optimizeBitXorValueString((ABitXorValue) value, values, optionRequired);
        } else if (value instanceof AModuloValue) {
            return optimizeModuloValueString((AModuloValue) value, values, optionRequired);
        } else if (value instanceof ALeftShiftValue) {
            return optimizeLeftShiftValueString((ALeftShiftValue) value, values, optionRequired);
        } else if (value instanceof ARightShiftValue) {
            return optimizeRightShiftValueString((ARightShiftValue) value, values, optionRequired);
        } else if ((value instanceof AIntValue)
                || (value instanceof AFloatValue)
                || (value instanceof AScientificValue)
                || (value instanceof AStringValue)
                || (value instanceof AHexValue)
                || (value instanceof AOctetValue)
                || (value instanceof ANullValue)
                || (value instanceof AIdentifierValue)
                || (value instanceof AColumnValue)) {
            return value;
        } else {
            // TODO 需要做一些警告
            return null;
        }
    }

    private PExpr optimizeIsExpr(AIsExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue value = expr.getValue();
        if (value instanceof AOptionValue) {
            String option = ((AOptionValue)value).getIdentifier().getText();
            if (options.containsKey(option)) {
                Object optionValue = options.get(option);
                values.add(new BQLOption(option, optionValue));
            } else {
                if (optionRequired) {
                    throw new MissingOptionException(option);
                }
                return null;
            }
        }

        return expr;
    }

    private PExpr optimizeIsNotExpr(AIsNotExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue value = expr.getValue();
        if (value instanceof AOptionValue) {
            String option = ((AOptionValue)value).getIdentifier().getText();
            if (options.containsKey(option)) {
                Object optionValue = options.get(option);
                values.add(new BQLOption(option, optionValue));
            } else {
                if (optionRequired) {
                    throw new MissingOptionException(option);
                }
                return null;
            }
        }

        return expr;
    }

    private PExpr optimizeBinaryExpr(ABinaryExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(expr.getLeft(), values, optionRequired);
        expr.setLeft(left);

        PValue right = optimizeValue(expr.getRight(), values, optionRequired);
        expr.setRight(right);

        if (left != null && right != null) {
            return expr;
        } else {
            return null;
        }
    }

    private PExpr optimizeBetweenExpr(ABetweenExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        PValue left = optimizeValue(expr.getLeft(), values, optionRequired);
        expr.setLeft(left);

        PValue right = optimizeValue(expr.getRight(), values, optionRequired);
        expr.setRight(right);

        if (left != null && right != null) {
            return expr;
        } else {
            return null;
        }
    }

    private PExpr optimizeInExpr(AInExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        List<PValue> newValues = new ArrayList<PValue>();

        List<PValue> inValues = expr.getValue();
        for (PValue value : inValues) {
            PValue newValue = optimizeValue(value, values, optionRequired);
            if (newValue != null) {
                newValues.add(newValue);
            }
        }

        if (newValues.isEmpty()) {
            return null;
        }

        inValues.clear();
        inValues.addAll(newValues);

        return expr;
    }

    private PExpr optimizeNotInExpr(ANotInExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        List<PValue> newValues = new ArrayList<PValue>();

        List<PValue> inValues = expr.getValue();
        for (PValue value : inValues) {

            if (value instanceof AOptionValue) {

                String option = ((AOptionValue)value).getIdentifier().getText();
                if (!options.containsKey(option)) {
                    if (optionRequired) {
                        throw new MissingOptionException(option);
                    } else {
                        if (optionRequired) {
                            throw new MissingOptionException(option);
                        }
                        continue;
                    }
                }

                Object optionValue = options.get(option);
                if (optionValue instanceof List) {

                    List list = (List)optionValue;
                    if (list.isEmpty()) {
                        continue;
                    }

                    for (Object v : (List)optionValue) {
                        values.add(new BQLOption(option, v));
                    }
                } else {
                    values.add(new BQLOption(option, optionValue));
                }

                newValues.add(value);
            } else {
                newValues.add(value);
            }
        }

        inValues.clear();
        inValues.addAll(newValues);

        return newValues.isEmpty() ? null : expr;
    }

    private PExpr visitExpr(PExpr expr, List<BQLOption> values, boolean optionRequired) throws MissingOptionException {
        if (expr instanceof AConditionExpr) {
            AConditionExpr condition = (AConditionExpr) expr;
            PExpr left = condition.getLeft();
            if (left != null) {
                left = visitExpr(left, values, optionRequired);
                condition.setLeft(left);
            }

            PExpr right = condition.getRight();
            if (right != null) {
                right = visitExpr(right, values, optionRequired);
                condition.setRight(right);
            }

            if (left == null && right == null) {
                return null;
            } else if (left == null) {
                return right;
            } else if (right == null) {
                return left;
            } else {
                return condition;
            }
        } else if (expr instanceof ANotExpr) {
            ANotExpr aNotExpr = (ANotExpr)expr;

            PExpr childExpr = aNotExpr.getExpr();
            childExpr = visitExpr(childExpr, values, optionRequired);
            aNotExpr.setExpr(childExpr);

            if (childExpr != null) {
                return expr;
            } else {
                return null;
            }
        } else {
            return optimizeExpr(expr, values, optionRequired);
        }
    }

    public BQLException getError() {
        return error;
    }
}


























