package com.foo.micronaut.patio;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Micronaut Patio API",
                version = "latest",
                description = "My API"
        )
)
public class MicronautPatioApplication {

    private ApplicationContext context;

    public MicronautPatioApplication() {}

    public static void main(String[] args) {
        try {
            new MicronautPatioApplication().run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            context = Micronaut.run(MicronautPatioApplication.class,
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
