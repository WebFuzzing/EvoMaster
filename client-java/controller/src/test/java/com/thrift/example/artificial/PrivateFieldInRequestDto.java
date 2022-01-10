package com.thrift.example.artificial;

public class PrivateFieldInRequestDto {

    public String pubField;

    public String getPriField() {
        return priField;
    }

    public void setPriField(String priField) {
        this.priField = priField;
    }

    private String priField;


}
