package com.foo.wiremock;

public class WiremockApplication {

    public static void main(String[] args) {
        WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(8089)); //No-args constructor will start on port 8080, no HTTPS
        wireMockServer.start();
    }
}
