package com.foo.rpc.examples.spring.db.base;

import com.foo.rpc.examples.spring.db.SpringWithDbController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;

public class DbBaseController extends SpringWithDbController {

    private DbBaseService.Client client;

    public DbBaseController() {
        super(DbBaseApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(DbBaseService.Iface.class.getName(), client);
        }});
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/dbbase";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new DbBaseService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

    @Override
    public void resetStateOfSUT()  {
        try {
            // need a further check if we need per invocation
            client.getInputProtocol().getTransport().flush();
            client.getOutputProtocol().getTransport().flush();
        } catch (Exception e) {
            //e.printStackTrace();
        }

    }
}
