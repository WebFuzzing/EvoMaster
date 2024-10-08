package com.foo.rpc.grpc.examples.spring.triangle;

import com.foo.rpc.grpc.examples.spring.branches.generated.triangle.CheckTriangleGRPCServiceGrpc;
import com.foo.rpc.grpc.examples.spring.branches.generated.triangle.DtoResponse;
import com.foo.rpc.grpc.examples.spring.branches.generated.triangle.TriangleRequest;

import io.grpc.stub.StreamObserver;

public class CheckTriangleGRPCService extends CheckTriangleGRPCServiceGrpc.CheckTriangleGRPCServiceImplBase {

    @Override
    public void checkTriangle(TriangleRequest request, StreamObserver<DtoResponse> responseObserver) {
        int result = classify(request.getA(), request.getB(), request.getC());
        DtoResponse response = DtoResponse.newBuilder().setResultAsInt(result).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private int classify(int a, int b, int c) {

        if (a <= 0 || b <= 0 || c <= 0) {
            return 0;
        }

        if (a == b && b == c) {
            return 3;
        }

        int max = Math.max(a, Math.max(b, c));

        if ((max == a && max - b - c >= 0) ||
                (max == b && max - a - c >= 0) ||
                (max == c && max - a - b >= 0)) {
            return 0;
        }

        if (a == b || b == c || a == c) {
            return 2;
        } else {
            return 1;
        }
    }
}
