package io.baxian.bql;

public class MissingOptionException extends BQLException {

    private String option;

    public MissingOptionException(String option) {
        this.option = option;
    }

    @Override
    public String getMessage() {
        return String.format("缺少参数 %s 。", option);
    }

}

























