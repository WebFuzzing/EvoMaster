package com.example.demo.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResult implements Serializable {

    protected String code;

    protected String msg;
}
