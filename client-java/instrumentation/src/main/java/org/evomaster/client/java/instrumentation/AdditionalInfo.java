package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.instrumentation.shared.TaintInputName;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

    /**
     * Map from taint input name to string specializations for it
     */
    private Map<String, Set<StringSpecializationInfo>> stringSpecializations = new ConcurrentHashMap<>();


    public void addSpecialization(String taintInputName, StringSpecializationInfo info){
        if(!TaintInputName.isTaintInput(taintInputName)){
            throw new IllegalArgumentException("No valid input name: " + taintInputName);
        }
        Objects.requireNonNull(info);

        Set<StringSpecializationInfo> set = stringSpecializations.putIfAbsent(taintInputName, new CopyOnWriteArraySet<>());
        set.add(info);
    }

    public Map<String, Set<StringSpecializationInfo>> getStringSpecializationsView(){
        //note: this does not prevent modifying the sets inside it
        return Collections.unmodifiableMap(stringSpecializations);
    }

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
