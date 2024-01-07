package org.evomaster.client.java.controller.api.dto.auth;

import java.util.List;

/**
 * process auth setting with endpoints in interfaces
 */
public class JsonAuthRPCEndpointDto {

    /**
     * The id representing this user that is going to login
     */
    public String userId;

    /**
     * The interface which contains the endpoint
     */
    public String interfaceName;

    /**
     * The endpoint where to process the auth setting
     */
    public String endpointName;

    /**
     * a list of the payload to send, as stringified JSON object
     */
    public List<String> jsonPayloads;

    /**
     * a list of the class name of the json object
     *
     * [jsonPayloads] are used to specify values of params for RPC endpoints
     * here are needed to explicitly specify what param type are with its full class name if they are not java.lang.String
     */
    public List<String> classNames;

    /**
     * specify the auth if it is only applicable to specified endpoints with corresponding annotation
     * Note that it is nullable indicating that the auth could be applied for all endpoints (eg, global auth)
     */
    public String annotationOnEndpoint;

}
