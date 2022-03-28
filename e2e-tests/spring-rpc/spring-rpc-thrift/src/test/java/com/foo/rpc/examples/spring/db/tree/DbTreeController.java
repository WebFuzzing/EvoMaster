package com.foo.rpc.examples.spring.db.tree;

import com.foo.rpc.examples.spring.db.SpringWithDbController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;

public class DbTreeController extends SpringWithDbController {

    private DbTreeService.Client client;

    public DbTreeController() {
        super(DbTreeApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(DbTreeService.Iface.class.getName(), client);
        }});
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/dbtree";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new DbTreeService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }
}

