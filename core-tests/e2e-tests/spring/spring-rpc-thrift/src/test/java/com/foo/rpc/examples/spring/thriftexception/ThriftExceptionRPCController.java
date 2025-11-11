package com.foo.rpc.examples.spring.thriftexception;

import com.foo.rpc.examples.spring.SpringController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;
import java.util.Map;

public class ThriftExceptionRPCController extends SpringController {

    private ThriftExceptionService.Client client;

    public ThriftExceptionRPCController(){
        super(ThriftExceptionApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(ThriftExceptionService.Iface.class, client, RPCType.GENERAL);
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/thriftexception";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new ThriftExceptionService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

    @Override
    public Map<Class, Integer> getExceptionImportanceLevels() {
        return new HashMap<Class, Integer>(){{
            put(BadResponse.class, 1);
            put(ErrorResponse.class, 0);
        }};
    }
}
