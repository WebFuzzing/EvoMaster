package com.thrift.example.artificial;

import javax.validation.constraints.NotNull;

public class AuthLoginDto {

    @NotNull
    public String id;

    @NotNull
    public String passcode;
}
