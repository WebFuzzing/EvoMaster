package org.evomaster.client.java.controller.api.dto;

/**
 * a code to categorize how a request achieved
 */
public enum CustomizedCallResultCode {
    /**
     * successful scenario
     */
    SUCCESS,

    /**
     * service error
     */
    SERVICE_ERROR,

    /**
     * other error scenarios, eg user error
     */
    OTHER_ERROR
}
