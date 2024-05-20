package org.evomaster.client.java.sql.advanced.helpers.dump;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class PlainTextFileHelper {

    public static void append(String file, String text){
        try {
            if(!exists(file)){
                create(file);
            }
            Files.write(Paths.get(file), text.getBytes(), StandardOpenOption.APPEND);
        } catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    private static Boolean exists(String file){
        return Files.exists(Paths.get(file));
    }

    private static File create(String file){
        try {
            return Files.createFile(Paths.get(file)).toFile();
        } catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
