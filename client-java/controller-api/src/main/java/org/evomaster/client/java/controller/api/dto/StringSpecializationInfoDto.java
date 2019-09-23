package org.evomaster.client.java.controller.api.dto;

public class StringSpecializationInfoDto {

    public String stringSpecialization;

    public String value;

    public String type;

    public StringSpecializationInfoDto(){}

    public StringSpecializationInfoDto(String stringSpecialization, String value, String type) {
        this.stringSpecialization = stringSpecialization;
        this.value = value;
        this.type = type;
    }
}
