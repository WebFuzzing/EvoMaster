package org.evomaster.clientJava.instrumentation;

import org.objectweb.asm.ClassReader;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.evomaster.clientJava.clientUtil.SimpleLogger.debug;
import static org.evomaster.clientJava.clientUtil.SimpleLogger.error;


/**
 * This classloader will add probes to the loaded classes.
 * Although such probes might make the classes slower to execute,
 * these probes should not change the behavior of the instrumented
 * classes.
 * If they do, then it is either a problem related to timing, or
 * a bug in this library.
 * <br>
 * This is needed ONLY when the test cases are generated, and not
 * when they are run
 */
public class InstrumentingClassLoader extends ClassLoader {

    private final Instrumentator instrumentator;
    private final ClassLoader classLoader;
    private final Map<String, Class<?>> classes;

    public InstrumentingClassLoader() {
        instrumentator = new Instrumentator();
        classLoader = InstrumentingClassLoader.class.getClassLoader();
        classes = new LinkedHashMap<>();
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        if (!ClassesToExclude.checkIfCanInstrument(name)) {
            Class<?> result = findLoadedClass(name);
            if (result != null) {
                return result;
            }
            result = classLoader.loadClass(name);
            return result;
        }

        Class<?> result = classes.get(name);
        if (result != null) {
            return result;
        } else {
            Class<?> instrumentedClass = instrumentClass(name);
            return instrumentedClass;
        }
    }

    private Class<?> instrumentClass(String fullyQualifiedTargetClass) throws ClassNotFoundException {
        String className = fullyQualifiedTargetClass.replace('.', '/');

        try (InputStream is = classLoader.getResourceAsStream(className + ".class")) {

            if (is == null) {
                throw new ClassNotFoundException("Failed to find resource file for "+fullyQualifiedTargetClass);
            }

            byte[] byteBuffer = instrumentator.transformBytes(this, className, new ClassReader(is));
            createPackageDefinition(fullyQualifiedTargetClass);

            Class<?> result = defineClass(fullyQualifiedTargetClass, byteBuffer, 0, byteBuffer.length);
            classes.put(fullyQualifiedTargetClass, result);

            debug("Loaded class: " + fullyQualifiedTargetClass);
            return result;
        } catch (Throwable t) {
            error("Error while loading class " + fullyQualifiedTargetClass, t);
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
