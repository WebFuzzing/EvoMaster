package com.foo.rpc.examples.spring.authsetup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.rpc.examples.spring.SpringController;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.auth.LocalAuthenticationDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AuthLocalSetupController extends SpringController {

    private AuthSetupService.Client client;
    private ObjectMapper mapper = new ObjectMapper();

    public AuthLocalSetupController() {
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
    public boolean handleLocalAuthenticationSetup(String authenticationInfo) {
        try {
            LoginDto dto = mapper.readValue(authenticationInfo, LoginDto.class);
            client.login(dto);
            return true;
        } catch (JsonProcessingException | TException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(new AuthenticationDto() {{
            name = "foo";
            localAuthSetup = new LocalAuthenticationDto() {{
                authenticationInfo="{\n" +
                        "\"id\":\"foo\",\n" +
                        "\"passcode\":\"zXQV47zsrjfJRnTD\"\n" +
                        "}";
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