package org.evomaster.test.utils;

/*
    Note: this class is in this module, as to make sure that the exact same code
    is used in the EvoMaster Core (eg, when making HTTP calls) and
    as well in the generated tests

    WARNING: if you change any method name/signature, need to make sure that
    the code generation is updated as well

    Note: this code needs to be kept in sync among the different programming
    languages, eg, Java, JavaScript and C#.
 */

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class containing utility functions that can be used in the
 * automatically generated tests
 */
public class EMTestUtils {

    /**
     *
     * @param locationHeader a URI-reference, coming from a "location" header. See RFC 7231.
     *                       Note: it can be a relative reference
     * @param expectedTemplate a full URI of the target resource, but with some path elements
     *                         that might (or might not) be unresolved. If {@code locationHeader} is not
     *                         empty, it will replace the beginning of this template.
     * @return a fully resolved URI for the target resource. If there are problems, just
     *          return the input locationHeader. If this latter is empty/null, then return the template
     */
    public static String resolveLocation(String locationHeader, String expectedTemplate){

        if(locationHeader==null || locationHeader.isEmpty()){
            return expectedTemplate;
        }

        URI locationURI;
        try{
            locationURI = URI.create(locationHeader);
        } catch (Exception e){
            return locationHeader;
        }

        /*
            Default behavior of split() is "peculiar", to say the least...
            /a
            would be "/" split into ["",a]
            the same as
            /a/
            !!!!!!!!
            to get trailing "", need to put a negative limit...
         */
        int wtfJava = -1;

        String locationPath = locationURI.getPath();
        String[] locationTokens = locationPath.split("/",wtfJava);


        //the template is not a valid URL, due to {}
        String normalizedTemplate = expectedTemplate.replace("{","").replace("}","");
        URI templateURI = URI.create(normalizedTemplate);
        String templatePath = templateURI.getPath();
        String[] templateTokens = templatePath.split("/",wtfJava);


        String targetPath = locationPath;

        if(templateTokens.length > locationTokens.length){
            /*
                This is to handle cases like:

                POST /elements
                PUT  /elements/{id}/x

                where the location header of POST does point to

                /elements/{id}

                and not directly to "x"
             */

            for(int i=locationTokens.length; i < templateTokens.length; i++){
                targetPath += "/" + templateTokens[i];
            }
        }


        URI targetURI;

        try {
            if (locationURI.isAbsolute() || locationURI.getHost() != null) {

                targetURI = new URI(
                        locationURI.getScheme(),
                        locationURI.getUserInfo(),
                        locationURI.getHost(),
                        locationURI.getPort(),
                        targetPath,
                        locationURI.getQuery(),
                        locationURI.getFragment());

            } else {
                targetURI = new URI(
                        templateURI.getScheme(),
                        templateURI.getUserInfo(),
                        templateURI.getHost(),
                        templateURI.getPort(),
                        targetPath,
                        templateURI.getQuery(),
                        templateURI.getFragment());
            }
        }catch (Exception e){
            //shouldn't really happen
            throw new RuntimeException(e);
        }

        return targetURI.toString();
    }


    /**
     * @param uri a string representing a URI
     * @return whether the given input string is either empty or a valid URI
     */
    public static boolean isValidURIorEmpty(String uri){

        if(uri == null || uri.trim().isEmpty()){
            return true;
        }

        try{
            URI.create(uri);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    /**
     * Resolves the absolute path to the Java executable using the given
     * JDK environment variable name.
     *
     * <p>This method expects the environment variable (e.g. {@code JAVA_HOME})
     * to point to a JDK installation directory. It appends {@code "bin"} and
     * {@code "java"} to construct the full path to the Java executable.</p>
     *
     *
     * @param jdkEnvVarName the name of the JDK environment variable
     *                      (e.g. {@code "JAVA_HOME"})
     * @return the absolute path to the Java executable as a String
     * @throws RuntimeException if the environment variable is not defined or empty
     */
    public static String extractJDKPathWithEnvVarName(String jdkEnvVarName){
        return extractPathWithEnvVar(jdkEnvVarName, "bin", "java").toString();
    }

    /**
     * Resolves the absolute path to a System-Under-Test (SUT) JAR file
     * using environment variables.
     *
     *
     * @param sutDistEnvVarName the environment variable that contains the base
     *                          directory of the SUT distribution
     * @param sutJarEnvVarName the name of the JAR file (or relative path inside the distribution)
     * @return the absolute path to the SUT JAR file as a String
     * @throws RuntimeException if the distribution environment variable is not defined or empty
     */
    public static String extractSutJarNameWithEnvVarName(String sutDistEnvVarName, String sutJarEnvVarName){
        return extractPathWithEnvVar(sutDistEnvVarName, sutJarEnvVarName).toString();
    }

    /**
     * Resolves an absolute {@link Path} using the value of a given environment variable
     * as the base directory and appending additional path segments.
     *
     * <p>For example, if {@code envVarName} is {@code "JAVA_HOME"} and
     * {@code others} contains {@code "bin", "java"}, this method will return:</p>
     *
     * <pre>
     * $JAVA_HOME/bin/java   (Linux/macOS)
     * %JAVA_HOME%\bin\java  (Windows)
     * </pre>
     *
     * <p>The resulting path is converted to an absolute path.</p>
     *
     * @param envVarName the name of the environment variable (e.g. {@code "JAVA_HOME"})
     * @param others additional path segments to append to the environment variable path
     * @return the resolved absolute {@link Path}
     * @throws RuntimeException if the environment variable is not defined or empty
     */
    private static Path extractPathWithEnvVar(String envVarName, String... others){
        String javaHome = System.getenv(envVarName);

        if (javaHome == null || javaHome.isEmpty()) {
            throw new IllegalArgumentException("Environment variable does not seem to be defined: " + envVarName);
        }

        Path javaExecutable = Paths.get(javaHome, others);
        return javaExecutable.toAbsolutePath();
    }
}
