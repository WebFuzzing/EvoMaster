package org.evomaster.clientJava.instrumentation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassesToExclude {

    private static final List<String> excludedClasses;

    static  {

        List<String> list = new ArrayList<>();

        InputStream excludedClassesStream =
                ClassesToExclude.class.getClassLoader()
                        .getResourceAsStream("skipInstrumentationList.txt");

        try(BufferedReader br = new BufferedReader(new InputStreamReader(excludedClassesStream))){

            String line;
            while ((line = br.readLine()) != null) {
                String element = line.trim();
                if(! element.startsWith("//") && !element.isEmpty()) {
                    list.add(element);
                }
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        excludedClasses = Collections.unmodifiableList(list);
    }

    public static List<String> getPackagePrefixesShouldNotBeInstrumented() {
        return excludedClasses;
    }

    public static boolean checkIfCanInstrument(String className) {
        for (String s : getPackagePrefixesShouldNotBeInstrumented()) {
            if (className.startsWith(s)) {
                return false;
            }
        }

        return true;
    }
}
