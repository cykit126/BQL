package io.baxian.bql;

import java.util.Map;

/**
 * Created by luoweibin on 8/12/15.
 */
public class BQLException extends Exception {

    private String sql;

    private String bql;

    private Map<String, Object> options;

    public BQLException() {}

    public BQLException(Throwable cause) {
        super(cause);
    }

    public BQLException(String bql, String sql, Map<String, Object> options, Throwable cause) {
        super(cause);

        this.sql = sql;
        this.bql = bql;
        this.options = options;
    }

    public String getSql() {
        return sql;
    }

    public String getBql() {
        return bql;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    @Override
    public String getMessage() {
        return String.format("bql:%s, sql:%s, options:%s, error:%s",
                bql != null ? bql : "null",
                sql != null ? sql : "null",
                options != null ? options.toString() : "null",
                super.getMessage());
    }
}


















