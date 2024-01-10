package org.evomaster.client.java.controller.internal.db.sql.mysql;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static org.evomaster.client.java.controller.internal.db.sql.mysql.DatabaseMySQLTestInit.DB_NAME;

public class DatabaseFakeMySQLSutController extends EmbeddedSutController {

    private final Connection sqlConnection;
    private final String initScript;

    public boolean running;

    public DatabaseFakeMySQLSutController(Connection connection) {
        this(connection, null);
    }

    public DatabaseFakeMySQLSutController(Connection connection, String initScript) {
        this.sqlConnection = connection;
        this.initScript = initScript;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        if(initScript != null)
            return Arrays.asList(new DbSpecification(DatabaseType.MYSQL, sqlConnection).withInitSqlScript(initScript).withSchemas(DB_NAME));
        else
            return Arrays.asList(new DbSpecification(DatabaseType.MYSQL, sqlConnection).withSchemas("test"));
    }



    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem("null", null);
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public String startSut() {
        running = true;

        DbCleaner.clearDatabase(sqlConnection, DB_NAME,null, DatabaseType.MYSQL);
        return "foo";
    }

    @Override
    public void stopSut() {
        running = false;
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public boolean isSutRunning() {
        return running;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "none";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }


}
