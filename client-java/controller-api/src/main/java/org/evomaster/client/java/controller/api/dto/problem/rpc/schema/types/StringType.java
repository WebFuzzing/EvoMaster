package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types;

/**
 * created by manzhang on 2021/11/15
 */
public class StringType extends TypeSchema {
    public final static String STRING_TYPE_NAME = String.class.getSimpleName();
    public final static String FULL_STRING_TYPE_NAME = String.class.getName();


    public StringType() {
        super(STRING_TYPE_NAME, FULL_STRING_TYPE_NAME);
    }
}
