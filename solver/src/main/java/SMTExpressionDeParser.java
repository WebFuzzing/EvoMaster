import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

public class SMTExpressionDeParser extends ExpressionDeParser {

    private StringBuilder conditionBuilder;
    private String rowVariable;

    public SMTExpressionDeParser(StringBuilder conditionBuilder, String rowVariable) {
        this.conditionBuilder = conditionBuilder;
        this.rowVariable = rowVariable;
    }

    @Override
    public void visit(AndExpression andExpression) {
        conditionBuilder.append("(and ");
        andExpression.getLeftExpression().accept(this);
        conditionBuilder.append(" ");
        andExpression.getRightExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(OrExpression orExpression) {
        conditionBuilder.append("(or ");
        orExpression.getLeftExpression().accept(this);
        conditionBuilder.append(" ");
        orExpression.getRightExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(NotExpression notExpression) {
        conditionBuilder.append("(not ");
        notExpression.getExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(EqualsTo equalsTo) {
        conditionBuilder.append("(= ");
        equalsTo.getLeftExpression().accept(this);
        conditionBuilder.append(" ");
        equalsTo.getRightExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(GreaterThan greaterThan) {
        conditionBuilder.append("(> ");
        greaterThan.getLeftExpression().accept(this);
        conditionBuilder.append(" ");
        greaterThan.getRightExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(GreaterThanEquals greaterThanEquals) {
        conditionBuilder.append("(>= ");
        greaterThanEquals.getLeftExpression().accept(this);
        conditionBuilder.append(" ");
        greaterThanEquals.getRightExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(MinorThan lessThan) {
        conditionBuilder.append("(< ");
        lessThan.getLeftExpression().accept(this);
        conditionBuilder.append(" ");
        lessThan.getRightExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(MinorThanEquals lessThanEquals) {
        conditionBuilder.append("(<= ");
        lessThanEquals.getLeftExpression().accept(this);
        conditionBuilder.append(" ");
        lessThanEquals.getRightExpression().accept(this);
        conditionBuilder.append(")");
    }

    @Override
    public void visit(Column column) {
        conditionBuilder.append("(").append(column.getColumnName().toUpperCase()).append(" ").append(rowVariable).append(")");
    }

    @Override
    public void visit(StringValue stringValue) {
        conditionBuilder.append("\"").append(stringValue.getValue()).append("\"");
    }

    @Override
    public void visit(LongValue longValue) {
        conditionBuilder.append(longValue.getValue());
    }

    @Override
    public void visit(DoubleValue doubleValue) {
        conditionBuilder.append(doubleValue.getValue());
    }

    // Add other visit methods as needed for different types of expressions
}
