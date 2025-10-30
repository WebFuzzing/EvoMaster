package com.example.demo.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idCardNo;

    private String name;

    private String phone;
}