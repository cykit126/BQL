package io.baxian.bql;

public class InvalidOptionException extends BQLException {

    private String field;

    private Object value;

    public InvalidOptionException(String field, Object value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public String getMessage() {
        return String.format(
                "Unsupported option, field:%s, type:%s.",
                field,
                value != null ? value.getClass().getName() : "null");
    }

}
