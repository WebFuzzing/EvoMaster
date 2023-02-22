package com.foo.rest.examples.spring.auth0;

import com.auth0.client.auth.AuthAPI;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.AuthRequest;

public class Auth0Service {

    /*
    TODO
ManagementAPI mgmt = new ManagementAPI("{YOUR_DOMAIN}", accessToken);
     */

    public static String getToken(String domain, String clientId, String clientSecret) throws Exception{

        AuthAPI authAPI = new AuthAPI(domain, clientId, clientSecret);
        AuthRequest authRequest = authAPI.requestToken("http://evomaster.org/api/v2/");
        TokenHolder holder = authRequest.execute();
        String accessToken = holder.getAccessToken();

        return accessToken;
    }
}
