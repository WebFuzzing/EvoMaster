package com.thrift.example.artificial;

import java.util.Arrays;
import java.util.List;

public enum EnumWithConstructor {

    FIRST(1, "first", Arrays.asList(EnumKind.ONE)),
    SECOND(2, "second", Arrays.asList(EnumKind.ONE, EnumKind.TWO)),
    THIRD(3, "third", Arrays.asList(EnumKind.ONE, EnumKind.TWO, EnumKind.THREE));

    private int code;

    private String desc;

    private List<EnumKind> kindList;


    EnumWithConstructor(int code, String desc, List<EnumKind> kindList) {
        this.code = code;
        this.desc = desc;
        this.kindList = kindList;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public List<EnumKind> getKindList() {
        return kindList;
    }

    @Override
    public String toString() {
        return "EnumWithConstructor{" +
                "code=" + code +
                ", desc='" + desc + '\'' +
                ", kindList=" + kindList +
                '}';
    }
}
