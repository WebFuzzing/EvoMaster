package com.thrift.example.artificial;

public class PrivateFieldInResponseDto {

    public int pubField;


    public PrivateFieldInRequestDto getPriRequest() {
        return priRequest;
    }

    public void setPriRequest(PrivateFieldInRequestDto priRequest) {
        this.priRequest = priRequest;
    }

    private PrivateFieldInRequestDto priRequest;

}
