package io.baxian.bql;

import io.baxian.bql.framework.analysis.DepthFirstAdapter;

public abstract class BQLGenerator extends DepthFirstAdapter {

    public abstract String output();

}
