package com.foo.rpc.grpc.examples.spring.branches;

import com.foo.rpc.grpc.examples.spring.GRPCServerController;
import com.foo.rpc.grpc.examples.spring.branches.generated.BranchGRPCServiceGrpc;
import io.grpc.ManagedChannelBuilder;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RPCProblem;

import java.util.HashMap;

public class BranchGRPCServiceController extends GRPCServerController {

    private BranchGRPCServiceGrpc.BranchGRPCServiceBlockingStub stub;

    public BranchGRPCServiceController() {
        super(new BranchGRPCService());
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RPCProblem(new HashMap<String, Object>() {{
            put(BranchGRPCServiceGrpc.BranchGRPCServiceBlockingStub.class.getName(), stub);
        }});
    }

    @Override
    public String startClient() {
        channel = ManagedChannelBuilder.forAddress("localhost", getSutPort()).usePlaintext().build();
        stub = BranchGRPCServiceGrpc.newBlockingStub(channel);


        return "started:"+!(channel.isShutdown() || channel.isTerminated());
    }
}
