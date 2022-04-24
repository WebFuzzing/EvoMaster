package com.thrift.example.artificial;

import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Null;

public class ImmutableObj {

    @Null
    private Long nullLong;

    @AssertFalse
    private boolean pbool;

    @AssertTrue
    private Boolean wbool;

    public Long getNullLong() {
        return nullLong;
    }

    public void setNullLong(Long nullLong) {
        this.nullLong = nullLong;
    }

    public boolean isPbool() {
        return pbool;
    }

    public void setPbool(boolean pbool) {
        this.pbool = pbool;
    }

    public Boolean getWbool() {
        return wbool;
    }

    public void setWbool(Boolean wbool) {
        this.wbool = wbool;
    }

    @Override
    public String toString() {
        return "ImmutableObj{" +
                "nullLong=" + nullLong +
                ", pbool=" + pbool +
                ", wbool=" + wbool +
                '}';
    }
}
