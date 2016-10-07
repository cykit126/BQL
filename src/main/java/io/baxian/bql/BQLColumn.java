package io.baxian.bql;

public class BQLColumn {

    private String table;

    private String name;

    private String alias;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getNameForResultSet() {
        return alias != null ? alias : name;
    }
}
