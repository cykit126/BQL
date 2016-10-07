package io.baxian.bql;

import io.baxian.bql.framework.lexer.Lexer;
import io.baxian.bql.framework.node.Start;
import io.baxian.bql.framework.parser.Parser;

import java.io.BufferedReader;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * Created by luoweibin on 8/11/15.
 */
public class BQLCompiler {

    private String message;

    private String output;

    private List<BQLOption> optimizedOptions;

    private BQLMetadata metadata;

    private Start ast;

    private Start optimzedAst;

    public BQLCompiler() {
    }

    public void compile(String bql) throws BQLException {
        message = null;
        PushbackReader reader = new PushbackReader(new BufferedReader(new StringReader(bql)));
        Lexer lexer = new Lexer(reader);
        Parser parser = new Parser(lexer);
        try {
            ast = parser.parse();

            // 收集metadata
            BQLMetadataCollector metadataCollector = new BQLMetadataCollector();
            ast.apply(metadataCollector);
            metadata = metadataCollector.getMetadata();
        } catch (Exception e) {
            throw new SyntaxException(bql, e.getMessage());
        }
    }

    public void optimize() throws BQLException {
        optimize(null);
    }

    public void optimize(Map<String, Object> options) throws BQLException {
        optimzedAst = (Start)ast.clone();

        BQLOptimizer optimizer = new BQLOptimizer(options);
        optimzedAst.apply(optimizer);
        BQLException error = optimizer.getError();
        if (error != null) {
            throw error;
        }

        optimizedOptions = optimizer.getOptimizedOptions();
    }

    public void generate(BQLGenerator generator) throws BQLException {
        optimzedAst.apply(generator);
        output = generator.output();
    }

    public String output() {
        return output;
    }

    public List<BQLOption> getOptions() {
        return optimizedOptions;
    }

    public String getMessage() {
        return message;
    }

    public BQLMetadata getMetadata() {
        return metadata;
    }

}
















