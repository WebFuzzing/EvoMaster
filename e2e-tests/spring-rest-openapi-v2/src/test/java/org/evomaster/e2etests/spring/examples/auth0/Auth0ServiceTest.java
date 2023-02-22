package org.evomaster.e2etests.spring.examples.auth0;

import com.foo.rest.examples.spring.auth0.Auth0Service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Auth0ServiceTest {


    @Test
    public void testGetToken() throws Exception{

        String domain = "localhost:8080";
        String clientId = "bar";
        String clientSecret = "123";

        String token = Auth0Service.getToken(domain, clientId, clientSecret);
        assertNotNull(token);

        //TODO more checks
    }
}
