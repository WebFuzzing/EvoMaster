package org.evomaster.clientJava.controller.db;

import org.evomaster.clientJava.controller.EmbeddedSutController;
import org.evomaster.clientJava.controller.problem.ProblemInfo;
import org.evomaster.clientJava.controller.problem.RestProblem;
import org.evomaster.clientJava.controllerApi.dto.AuthenticationDto;
import org.evomaster.clientJava.controllerApi.dto.SutInfoDto;

import java.sql.Connection;
import java.util.List;

public class DatabaseFakeSutController extends EmbeddedSutController{

    private final Connection connection;

    public DatabaseFakeSutController(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public String getDatabaseDriverName() {
        return "org.h2.Driver";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(null, null);
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
