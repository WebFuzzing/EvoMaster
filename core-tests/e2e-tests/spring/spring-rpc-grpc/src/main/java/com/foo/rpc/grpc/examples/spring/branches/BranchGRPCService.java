package com.foo.rpc.grpc.examples.spring.branches;

import com.foo.rpc.grpc.examples.spring.branches.generated.BranchGRPCServiceGrpc;
import com.foo.rpc.grpc.examples.spring.branches.generated.BranchesPostDto;
import com.foo.rpc.grpc.examples.spring.branches.generated.BranchesResponseDto;
import com.foo.somedifferentpackage.examples.branches.BranchesImp;
import io.grpc.stub.StreamObserver;
import org.evomaster.client.java.instrumentation.example.branches.Branches;

public class BranchGRPCService extends BranchGRPCServiceGrpc.BranchGRPCServiceImplBase {

    @Override
    public void pos(BranchesPostDto request, StreamObserver<BranchesResponseDto> responseObserver) {
        Branches b = new BranchesImp();
        int value = b.pos(request.getX(), request.getY());
        BranchesResponseDto response = BranchesResponseDto.newBuilder().setValue(value).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void neg(BranchesPostDto request, StreamObserver<BranchesResponseDto> responseObserver) {
        Branches b = new BranchesImp();
        int value = b.neg(request.getX(), request.getY());
        BranchesResponseDto response = BranchesResponseDto.newBuilder().setValue(value).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void eq(BranchesPostDto request, StreamObserver<BranchesResponseDto> responseObserver) {
        Branches b = new BranchesImp();
        int value = b.eq(request.getX(), request.getY());
        BranchesResponseDto response = BranchesResponseDto.newBuilder().setValue(value).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
