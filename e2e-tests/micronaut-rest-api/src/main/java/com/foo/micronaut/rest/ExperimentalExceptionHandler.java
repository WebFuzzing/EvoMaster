package com.foo.micronaut.rest;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {ExperimentalException.class, ExceptionHandler.class})
public class ExperimentalExceptionHandler implements ExceptionHandler<ExperimentalException, HttpResponse<String>> {

    @Override
    public HttpResponse<String> handle(HttpRequest request, ExperimentalException exception) {
        return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"Crashed\"}");
    }

}