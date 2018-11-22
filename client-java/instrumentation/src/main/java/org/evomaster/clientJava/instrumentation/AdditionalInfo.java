package org.evomaster.clientJava.instrumentation;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Besides code coverage, there can be additional info that we want
 * to collect at runtime when test cases are executed.
 */
public class AdditionalInfo implements Serializable {

    /**
     * In REST APIs, it can happen that some query parameters do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    private Set<String> queryParameters = new CopyOnWriteArraySet<>();


    /**
     * In REST APIs, it can happen that some HTTP headers do not
     * appear in the schema if they are indirectly accessed via
     * objects like WebRequest.
     * But we can track at runtime when such kind of objects are used
     * to access the query parameters
     */
    private Set<String> headers = new CopyOnWriteArraySet<>();


    public void addQueryParameter(String param){
        if(param != null && ! param.isEmpty()){
            queryParameters.add(param);
        }
    }

    public Set<String> getQueryParametersView(){
        return Collections.unmodifiableSet(queryParameters);
    }

    public void addHeader(String header){
        if(header != null && ! header.isEmpty()){
            headers.add(header);
        }
    }

    public Set<String> getHeadersView(){
        return Collections.unmodifiableSet(headers);
    }
}
