package org.evomaster.client.java.sql.advanced.select_query;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

import java.util.*;

public class FunctionFinder {

    public final static Set<String> AGGREGATION_FUNCTION_NAMES = new HashSet<>(
        Arrays.asList("COUNT", "SUM", "MIN", "MAX", "AVG"));

    private Select select;

    public FunctionFinder(Select select) {
        this.select = select;
    }

    public List<Function> getFunctions() {
        List<Function> functions = new LinkedList<>();
        ExpressionDeParser expressionDeParser = new ExpressionDeParser() {
            @Override
            public void visit(Function function) {
                if(!AGGREGATION_FUNCTION_NAMES.contains(function.getName().toUpperCase())) {
                    functions.add(function);
                }
            }
            @Override
            public void visit(Select select) {
            }
        };
        SelectDeParser selectDeParser = new SelectDeParser(expressionDeParser, expressionDeParser.getBuffer());
        select.accept(selectDeParser);
        return functions;
    }
}