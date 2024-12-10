package com.foo.micronaut.latest;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.List;

public class MicronautTestController extends EmbeddedSutController {

    private MicronautApplication application;

    public MicronautTestController() { setControllerPort(0); }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public String startSut() {
        application = new MicronautApplication();
        try {
            application.run();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return "http://localhost:" + application.getPort();
    }

    protected int getSutPort() {
        return application.getPort();
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
        if (application != null) {
            try {
                application.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.micronaut.latest";
    }

    @Override
    public void resetStateOfSUT() {}

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }
//
//    @Override
//    public Connection getConnection() {
//        return null;
//    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:"+ application.getPort() +"/swagger/micronaut-latest.yml",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
