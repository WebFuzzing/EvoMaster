package org.evomaster.client.java.instrumentation.tracker;

import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

/**
 * Tracking methods injected into the bytecode
 */
@Deprecated
public class Tracker {

    public static final String TRACK_QUERY_PARAMETER_METHOD_NAME = "trackQueryParameter";
    public static final String TRACK_QUERY_PARAMETER_DESCRIPTOR = "(Ljava/lang/String;)Ljava/lang/String;";

    public static String trackQueryParameter(String param){

        ExecutionTracer.addQueryParameter(param);

        return param;
    }


    public static final String TRACK_HEADER_METHOD_NAME = "trackHeader";
    public static final String TRACK_HEADER_DESCRIPTOR = "(Ljava/lang/String;)Ljava/lang/String;";

    public static String trackHeader(String header){

        ExecutionTracer.addHeader(header);

        return header;
    }


    public static final String TRACK_INPUT_STREAM_METHOD_NAME = "trackInputStream";
    public static final String TRACK_INPUT_STREAM_DESCRIPTOR = "()V";

    public static void trackInputStream(){

        //TODO
    }
}
