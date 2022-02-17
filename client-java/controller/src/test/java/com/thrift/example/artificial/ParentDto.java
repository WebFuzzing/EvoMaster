package com.thrift.example.artificial;

public class ParentDto<T, U> extends ParentParentDto<T>{

    private U message;

    public U getMessage() {
        return message;
    }

    public void setMessage(U message) {
        this.message = message;
    }
}
