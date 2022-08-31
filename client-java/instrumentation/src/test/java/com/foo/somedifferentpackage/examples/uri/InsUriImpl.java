package com.foo.somedifferentpackage.examples.uri;

import org.evomaster.client.java.instrumentation.example.uri.InsUri;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class InsUriImpl implements InsUri {

    @Override
    public String getProtocol(String s){

        try {
            return (new URL(s)).getProtocol();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }


    @Override
    public String getPath(String s){

        try {
            return (new URL( (new URI(new String(s))).toString())).getPath();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }



    @Override
    public String url0(String s) {

        try {
            new URL(s);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }

        return "OK";
    }

    @Override
    public String url1(String s) {
        try {
            new URL(null,s);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }

        return "OK";
    }

    @Override
    public String url2(String s) {
        try {
            new URL((URL)null,s,null);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }

        return "OK";
    }

    @Override
    public String uri0(String s) {
        try {
            new URI(s);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return "OK";
    }

    @Override
    public String uri1(String s) {
        URI.create(s);
        return "OK";
    }

    @Override
    public String uri2(String s) {
        URI uri = URI.create("https://foo.com");
        uri.resolve(s);
        return "OK";
    }
}
