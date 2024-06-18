import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.controller.api.dto.database.schema.*;
import org.evomaster.dbconstraint.ConstraintDatabaseType;
import org.evomaster.dbconstraint.ast.SqlCondition;
import org.evomaster.dbconstraint.parser.SqlConditionParserException;
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser;

import java.util.*;
import java.util.stream.Collectors;

public class SMTGenerator {

    private final DbSchemaDto schema;
    private static final Map<String, String> TYPE_MAP = new HashMap<String, String>() {{
        put("BIGINT", "Int");
        put("INTEGER", "Int");
        put("FLOAT", "Real");
        put("DOUBLE", "Real");
        put("CHARACTER VARYING", "String");
        put("CHAR", "String");
    }};
    private final JSqlConditionParser parser;
    private final ConstraintDatabaseType dbType;
    private final Integer numberOfRows = 2;

    public SMTGenerator(DbSchemaDto schemaDto) {
        this.schema = schemaDto;
        this.parser = new JSqlConditionParser();
        this.dbType = ConstraintDatabaseType.valueOf(schemaDto.databaseType.name());
    }

    public String generateSMT(String sqlQuery) {
        StringBuilder smt = new StringBuilder();

        appendTableDefinitions(smt);
        appendKeyConstraints(smt);
        appendQueryConstraints(smt, sqlQuery);
        appendGetValues(smt);

        return smt.toString();
    }

    private void appendQueryConstraints(StringBuilder smt, String sqlQuery) {
        List<String> tableNames;
        Expression condition;
        List<Expression> joinConditions = new ArrayList<>();
        try {
            tableNames = extractTableNamesAndJoinConditions(sqlQuery, joinConditions);
            condition = extractCondition(sqlQuery);
        } catch (JSQLParserException e) {
            throw new RuntimeException("Error when parsing table and condition from sqlQuery: " + sqlQuery, e);
        }

        List<TableDto> tablesInQuery = new ArrayList<>();
        for (String tableName : tableNames) {
            TableDto table = schema.tables.stream()
                    .filter(t -> t.name.equalsIgnoreCase(tableName))
                    .findFirst()
                    .orElse(null);

            if (table == null) {
                throw new RuntimeException("Table not found in schema: " + tableName);
            }
            tablesInQuery.add(table);
        }

        // Add join constraints
        addJoinConstraints(joinConditions, smt);

        // Add where constraints
        addQueryConstraints(tablesInQuery, condition, smt);
    }

    private void appendTableDefinitions(StringBuilder smt) {
        for (TableDto table : schema.tables) {
            generateTableDefinitions(table, smt);
        }
    }

    private List<String> extractTableNamesAndJoinConditions(String sqlQuery, List<Expression> joinConditions) throws JSQLParserException {
        Statement selectStatement = CCJSqlParserUtil.parse(sqlQuery);
        Select select = (Select) selectStatement;
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        List<String> tableNames = new ArrayList<>();

        FromItem fromItem = plainSelect.getFromItem();
        tableNames.add(fromItem.toString());

        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                FromItem joinItem = join.getRightItem();
                tableNames.add(joinItem.toString());
                joinConditions.addAll(join.getOnExpressions());
            }
        }
        return tableNames;
    }

    private Expression extractCondition(String sqlQuery) throws JSQLParserException {
        Statement selectStatement = CCJSqlParserUtil.parse(sqlQuery);
        Select select = (Select) selectStatement;
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        return plainSelect.getWhere();
    }

    public void generateTableDefinitions(TableDto table, StringBuilder smt) {
        String tableName = table.name.substring(0, 1).toUpperCase() + table.name.substring(1).toLowerCase();
        String columnsConcat = table.columns.stream()
                .map(c -> c.name.toLowerCase())
                .reduce((a, b) -> a + "-" + b)
                .orElse("");

        smt.append("(declare-datatypes () ((")
                .append(tableName).append("Row (")
                .append(columnsConcat).append(" ");

        for (ColumnDto column : table.columns) {
            String smtType = TYPE_MAP.get(column.type.toUpperCase());
            if (smtType == null) {
                throw new RuntimeException("Unsupported column type: " + column.type);
            }
            smt.append("(").append(column.name.toUpperCase()).append(" ").append(smtType).append(") ");
        }
        smt.append("))))\n");

        for (int i = 1; i <= numberOfRows; i++) {
            smt.append("(declare-const ").append(table.name.toLowerCase()).append(i)
                    .append(" ").append(tableName).append("Row)\n");
        }

        for (TableCheckExpressionDto check : table.tableCheckExpressions) {
            try {
                SqlCondition condition = parser.parse(check.sqlCheckExpression, this.dbType);
                for (int i = 1; i <= numberOfRows; i++) {
                    String constraint = parseCheckExpression(table, condition, i);
                    smt.append("(assert ").append(constraint).append(")\n");
                }
            } catch (SqlConditionParserException e) {
                throw new RuntimeException("Error parsing check expression: " + check.sqlCheckExpression, e);
            }
        }
    }

    private String parseCheckExpression(TableDto table, SqlCondition condition, int index) {
        StringBuilder smt = new StringBuilder();

        SMTConditionVisitor visitor = new SMTConditionVisitor(smt, table.name.toLowerCase());
        condition.accept(visitor, index);

        return smt.toString();
    }

    private void addJoinConstraints(List<Expression> joinConditions, StringBuilder smt) {
        if (!joinConditions.isEmpty()) {
            for (int i = 1; i <= numberOfRows; i++) {
                for (Expression joinCondition : joinConditions) {
                    String tableName = joinCondition.toString().split("\\.")[0];
                    String rowVariable = tableName.toLowerCase() + i;
                    StringBuilder joinConditionBuilder = new StringBuilder();
                    SMTExpressionDeParser expressionDeParser = new SMTExpressionDeParser(joinConditionBuilder, rowVariable);
                    joinCondition.accept(expressionDeParser);
                    smt.append("(assert ").append(joinConditionBuilder).append(")\n");
                }
            }
        }
    }

    private void addQueryConstraints(List<TableDto> tables, Expression where, StringBuilder smt) {
        if (where != null) {
            for (TableDto table : tables) {
                for (int i = 1; i <= numberOfRows; i++) {
                    String rowVariable = table.name.toLowerCase() + i;
                    StringBuilder conditionBuilder = new StringBuilder();
                    SMTExpressionDeParser expressionDeParser = new SMTExpressionDeParser(conditionBuilder, rowVariable);
                    where.accept(expressionDeParser);

                    smt.append("(assert ").append(conditionBuilder).append(")\n");
                }
            }
        }
    }

    private void appendGetValues(StringBuilder smt) {
        smt.append("(check-sat)\n");
        for (TableDto table : schema.tables) {
            String tableNameLower = table.name.toLowerCase();
            for (int i = 1; i <= numberOfRows; i++) {
                smt.append("(get-value (").append(tableNameLower).append(i).append("))\n");
            }
        }
    }

    private void appendKeyConstraints(StringBuilder smt) {
        for (TableDto table : schema.tables) {
            // Add primary key constraints
            List<ColumnDto> primaryKeys = table.columns.stream()
                    .filter(c -> c.primaryKey)
                    .collect(Collectors.toList());

            for (int i = 1; i <= numberOfRows; i++) {
                for (int j = i + 1; j <= numberOfRows; j++) {
                    for (ColumnDto primaryKey : primaryKeys) {
                        smt.append("(assert (distinct (")
                                .append(primaryKey.name.toUpperCase()).append(" ")
                                .append(table.name.toLowerCase()).append(i).append(") (")
                                .append(primaryKey.name.toUpperCase()).append(" ")
                                .append(table.name.toLowerCase()).append(j).append(")))\n");
                    }
                }
            }

            // Add foreign key constraints
            for (ForeignKeyDto foreignKey : table.foreignKeys) {
                String referencedTable = foreignKey.targetTable.toLowerCase();
                TableDto referencedTableDto = schema.tables.stream()
                        .filter(t -> t.name.equalsIgnoreCase(foreignKey.targetTable))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Referenced table not found: " + foreignKey.targetTable));

                List<ColumnDto> referencedPrimaryKeys = referencedTableDto.columns.stream()
                        .filter(c -> c.primaryKey)
                        .collect(Collectors.toList());

                if (referencedPrimaryKeys.isEmpty()) {
                    throw new RuntimeException("Referenced table has no primary key: " + foreignKey.targetTable);
                }

                ColumnDto referencedPrimaryKey = referencedPrimaryKeys.get(0); // Assuming single-column primary keys

                for (String sourceColumn : foreignKey.sourceColumns) {
                    String sourceColumnUpper = sourceColumn.toUpperCase();
                    String referencedColumnUpper = referencedPrimaryKey.name.toUpperCase();

                    for (int i = 1; i <= numberOfRows; i++) {
                        smt.append("(assert (or ");
                        for (int j = 1; j <= numberOfRows; j++) {
                            smt.append("(= (")
                                    .append(sourceColumnUpper).append(" ")
                                    .append(table.name.toLowerCase()).append(i).append(") (")
                                    .append(referencedColumnUpper).append(" ")
                                    .append(referencedTable).append(j).append(")) ");
                        }
                        smt.append("))\n");
                    }
                }
            }
        }
    }
}
