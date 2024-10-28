package com.foo.rest.emb.json.language;

import java.net.MalformedURLException;
import java.net.URL;

public class Tools {

    public static URL getUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
