package com.foo.rpc.examples.spring.db.existingdata;

import com.foo.rpc.examples.spring.db.SpringWithDbController;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.evomaster.client.java.controller.internal.db.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;

public class DbExistingDataController extends SpringWithDbController {

    private DbExistingDataService.Client client;

    public DbExistingDataController() {
        super(DbExistingDataApp.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(DbExistingDataService.Iface.class.getName(), client);
        }});
    }

    @Override
    public String startClient() {
        String url = "http://localhost:"+getSutPort()+"/dbexistingdata";
        try {
            // init client
            TTransport transport = new THttpClient(url);
            TProtocol protocol = new TBinaryProtocol(transport);
            client = new DbExistingDataService.Client(protocol);
        } catch (TTransportException e) {
            e.printStackTrace();
        }

        return url;
    }

    @Override
    public void resetStateOfSUT() {
        super.resetStateOfSUT();

//        ExistingDataEntityX x = new ExistingDataEntityX();
//        x.setId(42L);
//        x.setName("Foo");
//
//        ExistingDataRepositoryX rep = ctx.getBean(ExistingDataRepositoryX.class);
//        rep.save(x);
    }

    @Override
    public DbSpecification setDbSpecification() {
        DbSpecification spec = super.setDbSpecification();
        if (spec !=null)
            spec.initSqlScript = "INSERT INTO EXISTING_DATA_ENTITYX (ID, NAME) VALUES (42, 'Foo')";
        return spec;
    }
}
