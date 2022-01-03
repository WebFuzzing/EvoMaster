package com.foo.micronaut.rest;

import io.micronaut.runtime.Micronaut;
import io.micronaut.context.ApplicationContext;

public class MicronautApplication {

    private final int port;
    private ApplicationContext context;

    public MicronautApplication(int port) {
        this.port = port;
    }

    public static void main(String[] args) {

        try {
            new MicronautApplication(9001).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            context = Micronaut.run(MicronautApplication.class, new String[]{
                    "-micronaut.server.port="+ port
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return this.port;
    }

    public boolean isRunning() {
        return context != null && context.isRunning();
    }

    public void stop() {
        context.stop();
    }
}
