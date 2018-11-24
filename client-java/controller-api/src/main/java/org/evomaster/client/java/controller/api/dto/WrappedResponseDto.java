package org.evomaster.client.java.controller.api.dto;

import java.util.Objects;

/**
 * In REST, when we have an error, at most we would see a HTTP
 * status code.
 * But it can be very useful to get an actual description of the error.
 * So, it is a common practice to have "Wrapped Responses", which can
 * contain the error message (if any)
 *
 * Created by arcuri82 on 05-Nov-18.
 */
public class WrappedResponseDto<T> {

    /**
     * The actual payload we are sending and are "wrapping" here
     */
    public T data;

    /**
     * A message describing the error, if any.
     * If this is not null, then "data" must be null.
     */
    public String error;

    public static <K> WrappedResponseDto<K> withData(K data){
        WrappedResponseDto<K> dto = new WrappedResponseDto<>();
        dto.data = data;
        return dto;
    }

    public static WrappedResponseDto<?> withNoData(){
        return new WrappedResponseDto<>();
    }

    public static WrappedResponseDto<?> withError(String error){
        Objects.requireNonNull(error);
        if(error.isEmpty()){
            throw new IllegalArgumentException("Empty error message");
        }

        WrappedResponseDto<?> dto = new WrappedResponseDto<>();
        dto.error = error;
        return dto;
    }
}
