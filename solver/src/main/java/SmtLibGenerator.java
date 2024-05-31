import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.dbconstraint.ConstraintDatabaseType;
import net.sf.jsqlparser.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

public class SmtLibGenerator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SmtLibGenerator.class.getName());

    private static final Map<String, String> TYPE_MAP = new HashMap<String, String>() {{
        put("BIGINT", "Int");
        put("INTEGER", "Int");
        put("FLOAT", "Real");
        put("DOUBLE", "Real");
        put("CHARACTER VARYING", "String");
        put("CHAR", "String");
    }};

    private final DbSchemaDto schema;
    private final ConstraintDatabaseType dbType;

    private final Integer numberOfRows = 2;

    public SmtLibGenerator(DbSchemaDto schemaDto) {
            this.schema = schemaDto;
            this.dbType = ConstraintDatabaseType.valueOf(schemaDto.databaseType.name());
    }

    public SMTLib generateSMT(Statement sqlQuery) {
        SMTLib smt = new SMTLib();

        appendTableDefinitions(smt);
        appendKeyConstraints(smt);
        appendQueryConstraints(smt, sqlQuery);
        appendGetValues(smt);

        return smt;
    }

    private void appendTableDefinitions(SMTLib smt) {
        // Define Datatype for each table as Row, for example, for Users table UsersRow
        for (TableDto table : schema.tables) {
            String tableName = table.name.substring(0, 1).toUpperCase() + table.name.substring(1).toLowerCase();
            String dataTypeName = tableName + "Row";
            smt.addNode(new SMTLib.DeclareDatatype(dataTypeName, getConstructors(table)));

            // Declare the variables for each table
            for (int i = 1; i <= numberOfRows; i++) {
                smt.addNode(new SMTLib.DeclareConst(table.name.toLowerCase() + i, dataTypeName));
            }
        }

        // TODO: Add Table Constraints
    }

    private void appendKeyConstraints(SMTLib smt) {
        // TODO
    }

    private void appendQueryConstraints(SMTLib smt, Statement sqlQuery) {
        // TODO
    }

    private void appendGetValues(SMTLib smt) {
        smt.addNode(new SMTLib.CheckSat());

        for (TableDto table : schema.tables) {
            String tableNameLower = table.name.toLowerCase();
            for (int i = 1; i <= numberOfRows; i++) {
                smt.addNode(new SMTLib.GetValue(tableNameLower + i));
            }


        }
    }

    private static List<SMTLib.DeclareConst> getConstructors(TableDto table) {
        return
                table.columns.stream()
                        .map(c -> {
                            String smtType = TYPE_MAP.get(c.type.toUpperCase());
                            return new SMTLib.DeclareConst(c.name, smtType);
                        })
                        .collect(Collectors.toList());
    }
}
