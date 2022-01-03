package com.foo.micronaut.rest;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.sql.Connection;
import java.util.List;

public class MicronautTestController extends EmbeddedSutController {

    private MicronautApplication application;

    public MicronautTestController() {}

    @Override
    public String startSut() {
        application = new MicronautApplication(9000);
        try {
            application.run();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return "http://localhost:" + application.getPort();
    }

    @Override
    public boolean isSutRunning() {
        if (application == null) {
            return false;
        }
        return application.isRunning();
    }

    @Override
    public void stopSut() {
        if (application == null) {
            try {
                application.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.micronaut.rest";
    }

    @Override
    public void resetStateOfSUT() {
        //nothing to do
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:8080"+"/api/swagger.json",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
