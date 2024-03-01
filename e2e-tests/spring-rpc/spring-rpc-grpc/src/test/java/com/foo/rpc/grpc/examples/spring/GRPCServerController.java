package com.foo.rpc.grpc.examples.spring;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class GRPCServerController extends EmbeddedSutController {

    protected ManagedChannel channel;
    private Server server;

    private final BindableService registeredService;

    public GRPCServerController(BindableService service){
        registeredService = service;
        super.setControllerPort(0);
    }

    abstract public String startClient();

    @Override
    public String startSut() {

        try {
            server = ServerBuilder.forPort(0).addService(registeredService).build();
            server.start();

            startClient();
            return "http://localhost:"+server.getPort();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    protected int getSutPort() {
        return server.getPort();
    }

    @Override
    public boolean isSutRunning() {
        return (server != null && !server.isShutdown() && !server.isTerminated());
    }

    @Override
    public void stopSut() {

        try {
            if (channel != null)
                channel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
            if (server != null)
                server.shutdown().awaitTermination(2, TimeUnit.SECONDS);

            server = null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }



    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.";
    }

    @Override
    public void resetStateOfSUT() {
        //nothing to do
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }


    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
