package org.evomaster.client.java.controller.internal.db.h2;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.internal.db.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

public class DatabaseFakeH2SutController extends EmbeddedSutController {

    private final Connection sqlConnection;
    private final String initScript;

    public DatabaseFakeH2SutController(Connection connection) {
        this(connection, null);
    }

    public DatabaseFakeH2SutController(Connection connection, String initScript) {
        this.sqlConnection = connection;
        this.initScript = initScript;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return Arrays.asList(new DbSpecification(DatabaseType.H2, sqlConnection).withInitSqlScript(initScript));
    }

    @Override
    public String getDatabaseDriverName() {
        return "org.h2.Driver";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem("http://notused", null);
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
