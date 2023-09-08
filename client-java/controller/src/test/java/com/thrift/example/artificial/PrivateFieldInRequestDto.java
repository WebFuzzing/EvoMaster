package com.thrift.example.artificial;

import java.util.List;
import java.util.Map;

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

    private EnumKind priEnum;

    public void setPriEnum(EnumKind priEnum) {
        this.priEnum = priEnum;
    }

    public EnumKind getPriEnum() {
        return priEnum;
    }

    private Boolean priBoolean;

    public void setPriBoolean(Boolean priBoolean) {
        this.priBoolean = priBoolean;
    }

    public Boolean getPriBoolean() {
        return priBoolean;
    }


    private boolean pribool;

    public boolean isPribool() {
        return pribool;
    }

    public void setPribool(boolean pribool) {
        this.pribool = pribool;
    }

    public Byte getPriBByte() {
        return priBByte;
    }

    public void setPriBByte(Byte priBByte) {
        this.priBByte = priBByte;
    }

    private Byte priBByte;


    private byte pribyte;

    public byte getPribyte() {
        return pribyte;
    }

    public void setPribyte(byte pribyte) {
        this.pribyte = pribyte;
    }

    private Character priCharacter;

    private char priChar;

    public Character getPriCharacter() {
        return priCharacter;
    }

    public void setPriCharacter(Character priCharacter) {
        this.priCharacter = priCharacter;
    }

    public char getPriChar() {
        return priChar;
    }

    public void setPriChar(char priChar) {
        this.priChar = priChar;
    }

    private short priShort;

    public Short getPriShort() {
        return priShort;
    }

    public void setPriShort(short priShort) {
        this.priShort = priShort;
    }

    private Short priSShort;

    public Short getPriSShort() {
        return priSShort;
    }

    public void setPriSShort(Short priSShort) {
        this.priSShort = priSShort;
    }


    private Map<String, String> priMap;

    public Map<String, String> getPriMap() {
        return priMap;
    }

    public void setPriMap(Map<String, String> priMap) {
        this.priMap = priMap;
    }
}
