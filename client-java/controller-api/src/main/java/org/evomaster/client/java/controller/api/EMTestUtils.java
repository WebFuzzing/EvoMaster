package org.evomaster.client.java.controller.api;

/*
    Note: this class is in this module, as to make sure that the exact same code
    is used in the EvoMaster Core (eg, when making HTTP calls) and
    as well in the generated tests

    WARNING: if you change any method name/signature, need to make sure that
    the code generation is updated as well

    Note: this code needs to be kept in sync among the different programming
    languages, eg, Java, JavaScript and C#.
 */

import javax.net.ssl.*;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

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
        String locationPath = locationURI.getPath();
        String[] locationTokens = locationPath.split("/");


        //the template is not a valid URL, due to {}
        String normalizedTemplate = expectedTemplate.replace("{","").replace("}","");
        URI templateURI = URI.create(normalizedTemplate);
        String templatePath = templateURI.getPath();
        String[] templateTokens = templatePath.split("/");


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
}
