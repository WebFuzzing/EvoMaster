package com.foo.rpc.examples.spring.authsetup;

import com.foo.rpc.examples.spring.SpringController;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.auth.JsonAuthRPCEndpointDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AuthSetupController extends SpringController {

    private AuthSetupService.Client client;

    public AuthSetupController() {
        super(AuthSetupApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(AuthSetupService.Iface.class.getName(), client);
        }}, new HashMap<String, List<String>>(){{
            put(AuthSetupService.Iface.class.getName(), Arrays.asList("login", "logout"));
        }}, null, null, null, RPCType.GENERAL);
    }

    @Override
    public String startClient() {
        String url = "http://localhost:" + getSutPort() + "/auth";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new AuthSetupService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(new AuthenticationDto() {{
            name = "foo";
            jsonAuthEndpoint = new JsonAuthRPCEndpointDto() {{
                endpointName = "login";
                interfaceName = AuthSetupService.Iface.class.getName();
                jsonPayloads = Arrays.asList(
                        "{\n" +
                                "\"id\":\"foo\",\n" +
                                "\"passcode\":\"zXQV47zsrjfJRnTD\"\n" +
                                "}"
                );
                classNames = Arrays.asList(
                        "com.foo.rpc.examples.spring.authsetup.LoginDto"
                );
            }};
        }},
                new AuthenticationDto() {{
                    name = "bar";
                    jsonAuthEndpoint = new JsonAuthRPCEndpointDto() {{
                        endpointName = "login";
                        interfaceName = AuthSetupService.Iface.class.getName();
                        jsonPayloads = Arrays.asList(
                                "{\n" +
                                        "\"id\":\"bar\",\n" +
                                        "\"passcode\":\"5jbNvXvaejDG5MhS\"\n" +
                                        "}"
                        );
                        classNames = Arrays.asList(
                                "com.foo.rpc.examples.spring.authsetup.LoginDto"
                        );
                    }};
                }}

        );
    }

    @Override
    public void resetStateOfSUT() {
        if (client != null) {
            try {
                client.logout();
            } catch (TException e) {
                e.printStackTrace();
            }
        }
    }
}