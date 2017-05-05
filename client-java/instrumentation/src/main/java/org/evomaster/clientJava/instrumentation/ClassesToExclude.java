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
    private static final List<String> includedClasses;


    static  {

        InputStream excludedClassesStream =
                ClassesToExclude.class.getClassLoader()
                        .getResourceAsStream("skipInstrumentationList.txt");

        excludedClasses = getNotCommentedLines(excludedClassesStream);

        InputStream includedClassesStream =
                ClassesToExclude.class.getClassLoader()
                        .getResourceAsStream("keepInstrumentationList.txt");

        includedClasses = getNotCommentedLines(includedClassesStream);
    }

    private static List<String> getNotCommentedLines(InputStream excludedClassesStream) {
        List<String> list = new ArrayList<>();

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

        return Collections.unmodifiableList(list);
    }

    public static List<String> getPackagePrefixesShouldNotBeInstrumented() {
        return excludedClasses;
    }

    public static boolean checkIfCanInstrument(ClassName cn) {

        String className = cn.getFullNameWithDots();

        outer: for (String s : excludedClasses) {
            if (className.startsWith(s)) {

                for(String k : includedClasses){
                    if(className.startsWith(k)){
                        continue outer;
                    }
                }

                return false;
            }
        }

        return true;
    }
}
