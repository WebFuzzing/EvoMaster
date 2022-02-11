package org.evomaster.client.java.controller.internal.db.mysql;

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

public class DatabaseFakeMySQLSutController extends EmbeddedSutController {

    private final Connection sqlConnection;

    public DatabaseFakeMySQLSutController(Connection connection) {
        this.sqlConnection = connection;
    }

    @Override
    public DbSpecification getDbSpecification() {
        return new DbSpecification(){{
            dbType = DatabaseType.MYSQL;
            connections = Arrays.asList(sqlConnection);
            employSmartDbClean = true;
            schemaName = "test";
        }};
    }

    @Override
    public String getDatabaseDriverName() {
        return "com.mysql.cj.jdbc.Driver";
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
