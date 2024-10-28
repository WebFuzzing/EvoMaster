package com.foo.rest.emb.json.language;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This code is taken from LanguageTool
 * G: https://github.com/languagetool-org/languagetool
 * L: LGPL-2.1
 * P: src/main/java/org/languagetool/tools/Tools.java
 */
public class Tools {

    public static URL getUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
