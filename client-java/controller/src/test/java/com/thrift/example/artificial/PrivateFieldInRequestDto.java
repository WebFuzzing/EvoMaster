package com.thrift.example.artificial;

import java.util.List;

public class PrivateFieldInRequestDto {

    public String pubField;

    public String getPriField() {
        return priField;
    }

    public void setPriField(String priField) {
        this.priField = priField;
    }

    private String priField;

    private List<String> stringList;

    public String getPubField() {
        return pubField;
    }

    public void setPubField(String pubField) {
        this.pubField = pubField;
    }

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }
}
