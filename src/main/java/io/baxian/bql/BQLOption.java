package io.baxian.bql;

public class BQLOption {

    private String field;

    private Object value;

    public BQLOption(String field, Object value) {
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}
