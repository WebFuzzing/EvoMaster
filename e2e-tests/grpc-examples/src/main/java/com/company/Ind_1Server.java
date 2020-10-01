package com.company;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Ind_1Server {
    private static final Logger logger = Logger.getLogger(Ind_1Server.class.getName());

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new Ind_1Impl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                Ind_1Server.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }
        ));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final Ind_1Server server = new Ind_1Server();
        server.start();
        server.blockUntilShutdown();
    }

    static class Ind_1Impl extends Ind_1Grpc.Ind_1ImplBase {

        @Override
        public void endpointA(B request,
                              StreamObserver<BResponse> responseObserver) {
            String name = request.getFieldA().getName();
            BResponse reply = BResponse.newBuilder().setFieldA(FieldNameA.newBuilder().setName(name).build()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


        @Override
        public void endpointB(F request,
                              StreamObserver<FResponse> responseObserver) {
            String name = request.getFieldA().getName();
            FResponse reply = FResponse.newBuilder().setFieldA(FieldNameA.newBuilder().setName(name).build()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


        public void endpointC(D request,
                              StreamObserver<DResponse> responseObserver) {
            String name = request.getFieldA().getName();
            DResponse reply = DResponse.newBuilder().setFieldA(FieldNameA.newBuilder().setName(name).build()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        /**
         *
         */
        public void endpointD(H request,
                              StreamObserver<HResponse> responseObserver) {
            String name = request.getFieldA().getName();
            HResponse reply = HResponse.newBuilder().setFieldA(FieldNameA.newBuilder().setName(name).build()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


    }

}