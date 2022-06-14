package com.foo.rpc.examples.spring.db.directint;

import com.foo.rpc.examples.spring.db.SpringWithDbController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;

public class DbDirectIntController extends SpringWithDbController {

    private DbDirectIntService.Client client;

    public DbDirectIntController() {
        super(DbDirectIntApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(DbDirectIntService.Iface.class, client, RPCType.GENERAL);
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/dbdirectint";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new DbDirectIntService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }
}
