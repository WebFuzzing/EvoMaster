import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.dbconstraint.ConstraintDatabaseType;
import net.sf.jsqlparser.statement.Statement;

public class SmtLibGenerator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SmtLibGenerator.class.getName());

    private final DbSchemaDto schema;
    private final ConstraintDatabaseType dbType;

    public SmtLibGenerator(DbSchemaDto schemaDto) {
            this.schema = schemaDto;
            this.dbType = ConstraintDatabaseType.valueOf(schemaDto.databaseType.name());
    }

    public SMTLib generateSMT(Statement sqlQuery) {
        return new SMTLib();
    }
}
