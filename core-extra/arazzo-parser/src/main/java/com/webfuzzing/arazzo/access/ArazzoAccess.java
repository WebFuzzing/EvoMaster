package com.webfuzzing.arazzo.access;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Read Arrazzo documents
 */
public class ArazzoAccess {
    public static String readFromDisk(String arazzoLocation) throws Exception {
        String fileScheme = "file:";

        Path path;
        try {
            if (arazzoLocation.toLowerCase().startsWith(fileScheme)) {
                path = Paths.get(URI.create(arazzoLocation));
            } else {
                path = Paths.get(arazzoLocation);
            }
        } catch (Exception e) {
            throw new Exception("The file path provided for the Arazzo Schema " + arazzoLocation + " ended up with the following error: " + e.getMessage());
        }

        if ((!Files.exists(path))) {
            throw new Exception("The provided Arazzo file does not exist: " + arazzoLocation);
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new Exception("Error reading the Arazzo file: " + e.getMessage());
        }

    }
}
