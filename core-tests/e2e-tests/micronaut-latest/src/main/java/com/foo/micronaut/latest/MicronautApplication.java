package com.foo.micronaut.latest;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "micronaut",
                version = "latest",
                description = "Micronaut Latest E2E Test API"))
public class MicronautApplication {

    private ApplicationContext context;

    public MicronautApplication() {}


    public static void main(String[] args) {
        try {
            new MicronautApplication().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            context = Micronaut.run(MicronautApplication.class,
                    "-micronaut.server.port=${random.port}",
                    "-micronaut.router.static-resources.swagger.paths=classpath:META-INF/swagger",
                    "-micronaut.router.static-resources.swagger.mapping=/swagger/**");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return (context.getEnvironment().getProperty("micronaut.server.port", Integer.class).isPresent()) ? context.getEnvironment().getProperty("micronaut.server.port", Integer.class).get() : 0;
    }

    public boolean isRunning() {
        return context != null && context.isRunning();
    }

    public void stop() {
        context.stop();
    }
}
