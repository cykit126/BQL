package io.baxian.bql;

import io.baxian.bql.framework.analysis.DepthFirstAdapter;
import io.baxian.bql.framework.node.*;

import java.util.List;

public class BQLMetadataCollector extends DepthFirstAdapter {

    private BQLMetadata metadata = new BQLMetadata();

    @Override
    public void caseASelectStatement(ASelectStatement statement) {
        collectTable(statement.getTable());
        collectColumns(statement.getColumns());
    }

    @Override
    public void caseADeleteStatement(ADeleteStatement statement) {
        collectTable(statement.getTable());
    }

    @Override
    public void caseAUpdateStatement(AUpdateStatement statement) {
        collectTable(statement.getTable());
    }

    @Override
    public void caseAInsertStatement(AInsertStatement statement) {
        collectTable(statement.getTable());
    }

    public BQLMetadata getMetadata() {
        return metadata;
    }

    private void collectTable(PTable table) {
        ATable aTable = (ATable)table;
        ATableComponents comps = (ATableComponents) aTable.getComponents();

        TIdentifier schema = comps.getSchema();
        if (schema != null) {
            metadata.setSchema(schema.getText());
        }

        metadata.setTable(comps.getTable().getText());

        TIdentifier alias = aTable.getAlias();
        if (alias != null) {
            metadata.setAlias(alias.getText());
        }
    }

    private void collectColumns(List<PColumn> columns) {
        for (int i = 0; i < columns.size(); ++i) {
            BQLColumn column = new BQLColumn();

            AColumn aColumn = (AColumn)columns.get(i);

            TIdentifier alias = aColumn.getAlias();

            TIdentifier function = aColumn.getFunction();
            if (function == null) {
                AColumnComponents comps = (AColumnComponents) aColumn.getComponents().get(0);

                TIdentifier table = comps.getTable();
                if (table != null) {
                    column.setTable(table.getText());
                }

                column.setName(comps.getColumn().getText());

                if (alias != null) {
                    column.setAlias(alias.getText());
                }
            } else {
                column.setName(alias.getText());
            }

            metadata.addColumn(column);
        }
    }
}

















