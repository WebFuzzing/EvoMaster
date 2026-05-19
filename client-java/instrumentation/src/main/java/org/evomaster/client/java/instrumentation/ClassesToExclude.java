package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ClassesToExclude {

    private static final Set<String> excludedClasses;
    private static final Set<String> includedClasses;

    private static final String EM_REPRESENTATIVE_CLASS = "org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer";
    private static final String EM_CLASS_LOAD_ERROR_MSG = "Fail to load "+ EM_REPRESENTATIVE_CLASS;
    //Adding a couple of known class loaders that are not able to load EM classes to avoid the above warning on every e2e test
    private static final String PLATFORM_CLASS_LOADER = "jdk.internal.loader.ClassLoaders$PlatformClassLoader";
    private static final String EXT_CLASS_LOADER = "sun.misc.Launcher$ExtClassLoader";

    static  {

        InputStream excludedClassesStream =
                ClassesToExclude.class.getClassLoader().getResourceAsStream("skipInstrumentationList.txt");

        Set<String> toSkip = new HashSet<>(getNotCommentedLines(excludedClassesStream));

        String custom = System.getProperty(Constants.PROP_SKIP_CLASSES);
        if(custom != null && !custom.isEmpty()){
            toSkip.addAll(Arrays.asList(custom.split(",")));
        }

        excludedClasses = Collections.unmodifiableSet(toSkip);

        InputStream includedClassesStream =
                ClassesToExclude.class.getClassLoader().getResourceAsStream("keepInstrumentationList.txt");

        includedClasses = Collections.unmodifiableSet(new HashSet<>(getNotCommentedLines(includedClassesStream)));
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

    public static Set<String> getPackagePrefixesShouldNotBeInstrumented() {
        return excludedClasses;
    }

    /**
     *
     * @param loader class loader to check
     * @return if the loader could load EM class
     */
    private static boolean canLoadEMClass(ClassLoader loader){
        try {
            loader.loadClass(EM_REPRESENTATIVE_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            //New logic to avoid logging an unnecessary warning on every test
            String loaderName = loader.getClass().getName();
            if (PLATFORM_CLASS_LOADER.equals(loaderName) || EXT_CLASS_LOADER.equals(loaderName)) {
                return false;
            }
            // reduce string concatenation, then only print the msg as EM_CLASS_LOAD_ERROR_MSG
            SimpleLogger.uniqueWarn(EM_CLASS_LOAD_ERROR_MSG);
            return false;
        }
    }

    public static boolean checkIfCanInstrument(ClassLoader loader, ClassName cn) {

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

        /*
            if the loader cannot load EM classes, we skip instrumentation for the class, i.e., cn
         */
        return loader == null || canLoadEMClass(loader);
    }
}
