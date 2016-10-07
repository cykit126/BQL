package io.baxian.bql;

public class SyntaxException extends BQLException {

    private String bql;

    private String error;

    public SyntaxException(String bql, String error) {
        this.bql = bql;
        this.error = error;
    }

    @Override
    public String getMessage() {
        return String.format("BQL语法错误：%s。输入：%s", error, bql);
    }
}
