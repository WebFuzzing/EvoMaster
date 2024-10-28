package com.foo.rest.emb.json.paypublicapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;

import static com.foo.rest.emb.json.paypublicapi.CreateCardPaymentRequest.METADATA;
import static com.foo.rest.emb.json.paypublicapi.RequestError.Code.CREATE_PAYMENT_VALIDATION_ERROR;
import static com.foo.rest.emb.json.paypublicapi.RequestError.aRequestError;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;


/**
 * This code is taken from pay-publicapi
 * G: https://github.com/alphagov/pay-publicapi
 * L: MIT
 * P: src/main/java/uk/gov/pay/api/json/RequestJsonParser.java
 */
public class RequestJsonParser {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static ExternalMetadata parsePaymentRequest(JsonNode paymentRequest) {
        if (paymentRequest.has(METADATA)) {
            return validateAndGetMetadata(paymentRequest);
        }
        return new ExternalMetadata(null);
    }


    private static ExternalMetadata validateAndGetMetadata(JsonNode paymentRequest) {
        Map<String, Object> metadataMap;
        try {
            metadataMap = objectMapper.convertValue(paymentRequest.get("metadata"), Map.class);
        } catch (IllegalArgumentException e) {
            RequestError requestError = aRequestError(METADATA, CREATE_PAYMENT_VALIDATION_ERROR,
                    "Must be an object of JSON key-value pairs");
            throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
        }

        if (metadataMap == null) {
            RequestError requestError = aRequestError(METADATA, CREATE_PAYMENT_VALIDATION_ERROR,
                    "Value must not be null");
            throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
        }

        ExternalMetadata metadata = new ExternalMetadata(metadataMap);
//        Set<ConstraintViolation<ExternalMetadata>> violations = validator.validate(metadata);
//        if (violations.size() > 0) {
//            String message = violations.stream()
//                    .map(ConstraintViolation::getMessage)
//                    .map(msg -> msg.replace("Field [metadata] ", ""))
//                    .map(StringUtils::capitalize)
//                    .collect(Collectors.joining(". "));
//            RequestError requestError = aRequestError(METADATA, CREATE_PAYMENT_VALIDATION_ERROR, message);
//            throw new WebApplicationException(Response.status(SC_UNPROCESSABLE_ENTITY).entity(requestError).build());
//        }
        return metadata;
    }
}
