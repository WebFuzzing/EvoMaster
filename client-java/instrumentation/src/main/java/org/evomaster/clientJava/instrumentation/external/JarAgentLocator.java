package org.evomaster.clientJava.instrumentation.external;

import org.evomaster.clientJava.clientUtil.SimpleLogger;
import org.evomaster.clientJava.instrumentation.InstrumentingAgent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarAgentLocator {

    public static String getAgentJarPath(){

        String jarFilePath = getFromProperty();

        if(jarFilePath == null) {
            String classPath = System.getProperty("java.class.path");
            jarFilePath = searchInAClassPath(classPath);
        }

        if(jarFilePath==null){
            jarFilePath = searchInCurrentClassLoaderIfUrlOne();
        }

        if(jarFilePath==null){
            jarFilePath = searchInCurrentClassLoaderIfItProvidesClasspathAPI();
        }

        if(jarFilePath==null){
			/*
			 * this could happen in Eclipse or during test execution in Maven, and so search in compilation 'target' folder
			 */
            jarFilePath = searchInFolder("target");
        }

        if(jarFilePath==null){
            jarFilePath = searchInFolder("lib");
        }

        if(jarFilePath==null){
            //TODO could check in ~/.m2, but issue in finding right version
        }

        return jarFilePath;
    }

    //----------------------------------------------------------------------------------

    private static boolean isAgentJar(String path) throws IllegalArgumentException{

        if(path.endsWith("classes")){
			/*
				we need to treat this specially:
				eg, Jenkins/Maven on Linux on a module with only tests ended up
				with not creating "target/classes" (it does on Mac though) but still putting
				it on the classpath
			 */
            return false;
        }

        File file = new File(path);
        if(!file.exists()){
            throw new IllegalArgumentException("Non-existing file "+path);
        }

        String name = file.getName();

        if(name.toLowerCase().contains("evomaster") &&
                name.endsWith(".jar")){

            try (JarFile jar = new JarFile(file)){
                Manifest manifest = jar.getManifest();
                if(manifest == null){
                    return false;
                }

                Attributes attributes = manifest.getMainAttributes();

                String premain = attributes.getValue("Premain-Class");
                if(premain == null || premain.isEmpty()){
                    return false;
                }

                String agentClass = attributes.getValue("Agent-Class");
                String agent = InstrumentingAgent.class.getName(); // this is hardcoded in the pom.xml file
                if(agentClass != null && agentClass.trim().equalsIgnoreCase(agent)){
                    return true;
                }
            } catch (IOException e) {
                return false;
            }
        }

        return false;
    }

    private static String getFromProperty(){

        String path = System.getProperty("evomaster.instrumentation.jar.path");
        if(path == null){
            return null;
        }
        //if user specify a JAR path, but then it is invalid, then need to throw warning
        if(! isAgentJar(path)){
            throw new IllegalStateException("Specified instrumenting jar file is invalid");
        }

        return path;
    }

    private static String searchInAClassPath(String classPath){
        String[] tokens = classPath.split(File.pathSeparator);

        for(String entry : tokens){
            if(entry==null || entry.isEmpty()){
                continue;
            }
            if(isAgentJar(entry)){
                return entry;
            }
        }
        return null;
    }

    private static String searchInCurrentClassLoaderIfItProvidesClasspathAPI(){

        /*
            this could happen for AntClassLoader.
            Note: we cannot use instanceof here, as we do not want to add further third-party dependencies
         */

        ClassLoader loader = JarAgentLocator.class.getClassLoader();
        while(loader != null){

            try {
                Method m = loader.getClass().getMethod("getClasspath");
                String classPath = (String) m.invoke(loader);
                String jar = searchInAClassPath(classPath);
                if(jar != null){
                    return jar;
                }
            } catch (Exception e) {
                //OK, this can happen, not really an error
            }

            loader = loader.getParent();
        }

        return null;
    }

    private static String searchInCurrentClassLoaderIfUrlOne() {

        Set<URI> uris = new HashSet<>();

        ClassLoader loader = JarAgentLocator.class.getClassLoader();
        while(loader != null){
            if(loader instanceof URLClassLoader){
                URLClassLoader urlLoader = (URLClassLoader) loader;
                for(URL url : urlLoader.getURLs()){
                    try {
                        URI uri = url.toURI();
                        uris.add(uri);

                        File file = new File(uri);
                        String path = file.getAbsolutePath();
                        if(isAgentJar(path)){
                            return path;
                        }
                    } catch (Exception e) {
                        SimpleLogger.error("Error while parsing URL "+url);
                        continue;
                    }
                }
            }

            loader = loader.getParent();
        }

        String msg = "Failed to find Agent jar in current classloader. URLs of classloader:";
        for(URI uri : uris){
            msg += "\n"+uri.toString();
        }
        SimpleLogger.warn(msg);

        return null;
    }

    private static String searchInFolder(String folder) {

        File target = new File(folder);
        if(!target.exists()){
            SimpleLogger.debug("No target folder "+target.getAbsolutePath());
            return null;
        }

        if(!target.isDirectory()){
            SimpleLogger.debug("'target' exists, but it is not a folder");
            return null;
        }

        for(File file : target.listFiles()){
            String path = file.getAbsolutePath();
            if(isAgentJar(path)){
                return path;
            }
        }

        return null;
    }
}
