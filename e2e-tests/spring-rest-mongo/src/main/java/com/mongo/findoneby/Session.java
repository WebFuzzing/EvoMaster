package com.mongo.findoneby;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class Session {
    @Id
    private String id;
    @NotNull
    private String checksum;
    @NotNull
    private Object data;
    @NotNull
    @Size(min=3, message="source has a minimum length of 3")
    private String source;
    @NotNull
    private SessionType type;
}
