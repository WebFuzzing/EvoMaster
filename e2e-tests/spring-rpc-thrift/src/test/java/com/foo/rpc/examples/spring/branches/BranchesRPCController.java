package com.foo.rpc.examples.spring.branches;

import com.foo.rpc.examples.spring.SpringController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;

public class BranchesRPCController extends SpringController {

    private BranchesService.Client client;

    public BranchesRPCController() {
        super(BranchesApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put("com.foo.rpc.examples.spring.branches.BranchesService$Iface", client);
        }});
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/branches";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new BranchesService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }
}
