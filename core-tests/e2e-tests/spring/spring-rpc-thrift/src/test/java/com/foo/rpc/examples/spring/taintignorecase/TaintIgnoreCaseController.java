package com.foo.rpc.examples.spring.taintignorecase;

import com.foo.rpc.examples.spring.SpringController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;

public class TaintIgnoreCaseController extends SpringController {
    private TTransport transport;
    private TProtocol protocol;
    private TaintIgnoreCaseService.Client client;

    public TaintIgnoreCaseController(){
        super(TaintIgnoreCaseApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(TaintIgnoreCaseService.Iface.class.getName(), client);
        }});
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/taintignorecase";
        try {
            // init client
            transport = new THttpClient(url);
            protocol = new TBinaryProtocol(transport);
            client = new TaintIgnoreCaseService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

//    @Override
//    public void resetStateOfSUT()  {
//        try {
//            // need a further check if we need per invocation
//            client.getInputProtocol().getTransport().flush();
//            client.getOutputProtocol().getTransport().flush();
//        } catch (TTransportException e) {
//            e.printStackTrace();
//        }
//
//    }
}
