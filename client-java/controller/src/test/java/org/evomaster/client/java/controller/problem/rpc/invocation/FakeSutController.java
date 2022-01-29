package org.evomaster.client.java.controller.problem.rpc.invocation;

import com.thrift.example.artificial.RPCInterfaceExample;
import com.thrift.example.artificial.RPCInterfaceExampleImpl;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

/**
 * created by manzhang on 2021/11/27
 */
public class FakeSutController extends EmbeddedSutController {

    public boolean running;

    @Override
    public String startSut() {
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
        return null;
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        RPCInterfaceExampleImpl client = new RPCInterfaceExampleImpl();
        return new RPCProblem(new HashMap<String, Object>(){{
            put(RPCInterfaceExample.class.getName(), client);
        }});
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return null;
    }
}
