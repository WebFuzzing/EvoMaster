package org.evomaster.clientJava.instrumentation;

import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.evomaster.clientJava.clientUtil.SimpleLogger.*;


/**
 * This classloader will add probes to the loaded classes.
 * Although such probes might make the classes slower to execute,
 * these probes should not change the behavior of the instrumented
 * classes.
 * If they do, then it is either a problem related to timing, or
 * a bug in this library.
 * <br>
 * This is needed ONLY when the test cases are generated, and not
 * when they are run.
 * <br>
 * Note: this class loader can only be used during testing of EM.
 * For user controllers, we have to use a Java Agent that can intercept
 * ALL class loading.
 * The problem is that there are some packages we cannot instrument (eg javax.)
 * otherwise it becomes a nightmare to handle. But those packages could
 * use reflection to load classes that we do instrument.
 * In the moment we delegate to super classloader for those packages, we are
 * screwed, as some classes could be loaded twice.
 * An example is in javax.validation.Validation which can re-load hibernate
 * classes by searching for "default providers".
 */
public class InstrumentingClassLoader extends ClassLoader {

    private final Instrumentator instrumentator;
    private final ClassLoader classLoader;
    private final Map<String, Class<?>> classes;

    /**
     * Classloader needed to bootstrap the execution of your application.
     * This is needed because it will apply bytecode instrumentation to keep
     * track of which parts of the code is executed, and to inject heuristics
     * to help the generation of test cases that maximize coverage
     *
     * @param packagePrefixesToCover: a "," separated list of package prefixes or class names.
     *                              For example, "com.foo.,com.bar.Bar".
     *                              Note: be careful of using something as generate as "com."
     *                              or "org.", as most likely ALL your third-party libraries
     *                              would be instrumented as well, which could have a severe
     *                              impact on performance
     * @throws IllegalArgumentException if {@code packagePrefixesToCover} is invalid
     */
    public InstrumentingClassLoader(String packagePrefixesToCover)throws IllegalArgumentException {
        instrumentator = new Instrumentator(packagePrefixesToCover);
        classLoader = InstrumentingClassLoader.class.getClassLoader();
        classes = new LinkedHashMap<>();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        if (!ClassesToExclude.checkIfCanInstrument(ClassName.get(name))) {
            return loadNonInstrumented(name);
        }

        Class<?> result = classes.get(name);
        if (result != null) {
            return result;
        } else {
            ClassName className = new ClassName(name);
            Class<?> instrumentedClass = instrumentClass(className);

            if(instrumentedClass == null){
                return loadNonInstrumented(name);
            }

            return instrumentedClass;
        }
    }

    private Class<?> loadNonInstrumented(String name) throws ClassNotFoundException {
        Class<?> result = findLoadedClass(name);
        if (result != null) {
            return result;
        }
        result = classLoader.loadClass(name);
        return result;
    }

    private Class<?> instrumentClass(ClassName className) throws ClassNotFoundException {

        try (InputStream is = classLoader.getResourceAsStream(className.getAsResourcePath())) {

            if (is == null) {
                warn("Failed to find resource file for "+className.getAsResourcePath());
                return null;
            }

            byte[] byteBuffer = instrumentator.transformBytes(this, className, new ClassReader(is));
            createPackageDefinition(className.getFullNameWithDots());

            Class<?> result = defineClass(className.getFullNameWithDots(), byteBuffer, 0, byteBuffer.length);
            classes.put(className.getFullNameWithDots(), result);

            debug("Loaded class: " + className.getFullNameWithDots());
            return result;
        } catch (Throwable t) {
            error("Error while loading class " + className.getFullNameWithDots(), t);
            throw new ClassNotFoundException(t.getMessage(), t);
        }
    }

    /**
     * Before a new class is defined, we need to create a package definition for it
     *
     * @param className
     */
    private void createPackageDefinition(String className) {
        int i = className.lastIndexOf('.');
        if (i != -1) {
            String pkgname = className.substring(0, i);
            // Check if package already loaded.
            Package pkg = getPackage(pkgname);
            if (pkg == null) {
                definePackage(pkgname, null, null, null, null, null, null, null);
            }
        }
    }
}
