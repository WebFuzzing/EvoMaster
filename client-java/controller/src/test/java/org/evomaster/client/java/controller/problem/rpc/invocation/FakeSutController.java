package org.evomaster.client.java.controller.problem.rpc.invocation;

import com.thrift.example.artificial.RPCInterfaceExample;
import com.thrift.example.artificial.RPCInterfaceExampleImpl;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.LocalAuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.db.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * created by manzhang on 2021/11/27
 */
public class FakeSutController extends EmbeddedSutController {

    public boolean running;
    private RPCInterfaceExampleImpl sut = new RPCInterfaceExampleImpl();

    @Override
    public String startSut() {
        if (sut == null)
            sut = new RPCInterfaceExampleImpl();
        running = true;
        return null;
    }

    @Override
    public void stopSut() {
        running =false;
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
        return null;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                new AuthenticationDto(){{
                    name = "local";
                    localAuthSetup = new LocalAuthenticationDto(){{
                        authenticationInfo = "local_foo";
                    }};
                }}
        );
    }

    @Override
    public DbSpecification getDbSpecification() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>(){{
            put(RPCInterfaceExample.class.getName(), sut);
        }});
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return null;
    }

    @Override
    public boolean handleLocalAuthenticationSetup(String authenticationInfo) {
        boolean auth =  authenticationInfo.equals("local_foo");
        sut.setAuthorized(auth);
        return auth;
    }
}
