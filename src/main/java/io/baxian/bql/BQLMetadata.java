package io.baxian.bql;

import java.util.ArrayList;
import java.util.List;

public class BQLMetadata {

    private String schema;

    private String table;

    private String alias;

    private List<BQLColumn> columns = new ArrayList<BQLColumn>();

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public List<BQLColumn> getColumns() {
        return columns;
    }

    public void addColumn(BQLColumn column) {
        if (column != null) {
            columns.add(column);
        }
    }

    public String getSchema() {
        return schema;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
