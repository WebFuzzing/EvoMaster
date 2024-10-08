package com.foo.rpc.grpc.examples.spring.triangle;

import com.foo.rpc.grpc.examples.spring.GRPCServerController;
import com.foo.rpc.grpc.examples.spring.branches.generated.triangle.CheckTriangleGRPCServiceGrpc;

import java.util.HashMap;

import io.grpc.ManagedChannelBuilder;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

public class CheckTriangleGRPCServiceController extends GRPCServerController {

    private CheckTriangleGRPCServiceGrpc.CheckTriangleGRPCServiceBlockingStub stub;

    public CheckTriangleGRPCServiceController() {
        super(new CheckTriangleGRPCService());
    }

    @Override
    public String startClient() {
        channel = ManagedChannelBuilder.forAddress("localhost", getSutPort()).usePlaintext().build();
        stub = CheckTriangleGRPCServiceGrpc.newBlockingStub(channel);


        return "started:"+!(channel.isShutdown() || channel.isTerminated());
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(CheckTriangleGRPCServiceGrpc.CheckTriangleGRPCServiceBlockingStub.class.getName(), stub);
        }});
    }
}
