package org.evomaster.clientJava.instrumentation;

import org.evomaster.clientJava.clientUtil.SimpleLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ClassScanner {

    public static void forceLoading(String packagePrefixes){
        forceLoading(findAllClassNames(packagePrefixes));
    }

    public static void forceLoading(Set<ClassName> names) {

        ClassLoader loader = ClassScanner.class.getClassLoader();

        for (ClassName name : names) {
            try {
                loader.loadClass(name.getFullNameWithDots());
            } catch (Exception e) {
                SimpleLogger.error("Failed to load " + name.getFullNameWithDots() + " : " + e.getMessage());
            }
        }
    }


    public static Set<ClassName> findAllClassNames(String packagePrefixes) {

        List<String> prefixes = Arrays.asList(packagePrefixes.split(","))
                .stream()
                .map(s -> s.trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Set<ClassName> names = new HashSet<>();

        searchInCurrentClassLoaderIfUrlOne(prefixes, names);

        /*
            TODO: this ll need to be updated when Java 9.
            And also we need special handling for Spring.
         */

        return names;
    }


    private static void searchInCurrentClassLoaderIfUrlOne(List<String> prefixes, Set<ClassName> names) {

        ClassLoader loader = ClassScanner.class.getClassLoader();

        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                URLClassLoader urlLoader = (URLClassLoader) loader;
                for (URL url : urlLoader.getURLs()) {
                    try {
                        URI uri = url.toURI();

                        File file = new File(uri);
                        String path = file.getAbsolutePath();

                        if(file.isDirectory()){
                            scanDirectory(prefixes, names, file, path);
                        } else if(path.endsWith(".jar")){
                            scanJar(prefixes, names, path);
                        }

                    } catch (Exception e) {
                        SimpleLogger.error("Error while parsing URL " + url);
                        continue;
                    }
                }
            }

            loader = loader.getParent();
        }
    }


    private static void scanDirectory(List<String> prefixes,
                               Set<ClassName> names,
                               File directory,
                               String classPathFolder) {

        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        if (!directory.canRead()) {
            SimpleLogger.warn("No permission to read: " + directory.getAbsolutePath());
            return;
        }

        for (File file : directory.listFiles()) {

            String relativeFilePath = file.getAbsolutePath()
                    .replace(classPathFolder + File.separator, "");

            if (file.isDirectory() && isAPotentialPrefix(relativeFilePath, prefixes)) {
                // recursion till we get to a file that is not a folder.
                scanDirectory(prefixes, names, file, classPathFolder);

            } else {
                if (!file.getName().endsWith(".class")) {
                    continue; // we are only interested in class files
                }

                ClassName className =  ClassName.get(relativeFilePath);

                if(isAMatch(className.getFullNameWithDots(), prefixes)){
                    names.add(className);
                }
            }
        }
    }

    private static boolean isAPotentialPrefix(String folder, List<String> prefixes){

        final String p = folder.replace(File.separatorChar, '.');

        return prefixes.stream()
                .anyMatch(s -> s.startsWith(p));
    }

    private static boolean isAMatch(String name, List<String> prefixes){

        return prefixes.stream()
                .anyMatch(s -> name.startsWith(s));
    }

    private static void scanJar(List<String> prefixes,
                                Set<ClassName> names,
                                String jarEntry) {

        try(JarFile zf = new JarFile(jarEntry)) {

            Enumeration<?> e = zf.entries();
            while (e.hasMoreElements()) {
                JarEntry ze = (JarEntry) e.nextElement();
                String entryName = ze.getName();

                if (!entryName.endsWith(".class")) {
                    continue;
                }

                ClassName className =  ClassName.get(entryName);

                if(isAMatch(className.getFullNameWithDots(), prefixes)){
                    names.add(className);
                }
            }
        } catch (IOException e) {
            SimpleLogger.error("Failed to open jar " + jarEntry + " : " + e.getMessage());
        }
    }
}
