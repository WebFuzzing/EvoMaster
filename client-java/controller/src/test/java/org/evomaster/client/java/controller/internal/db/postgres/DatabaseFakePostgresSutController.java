package org.evomaster.client.java.controller.internal.db.postgres;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.internal.db.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DatabaseFakePostgresSutController extends EmbeddedSutController {

    private final Connection sqlConnection;
    private final String initScript;

    private static final String PUBLIC_SCHEMA = "public";

    public DatabaseFakePostgresSutController(Connection connection) {
        this(connection, null);
    }

    public DatabaseFakePostgresSutController(Connection connection, String initScript) {
        this.sqlConnection = connection;
        this.initScript = initScript;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        if(initScript != null)
            return Collections.singletonList(new DbSpecification(DatabaseType.POSTGRES, sqlConnection).withInitSqlScript(initScript).withSchemas(PUBLIC_SCHEMA));
        else
            return Collections.singletonList(new DbSpecification(DatabaseType.POSTGRES, sqlConnection).withSchemas(PUBLIC_SCHEMA));
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
        return "foo";
    }

    @Override
    public void stopSut() {
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public boolean isSutRunning() {
        return false;
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
