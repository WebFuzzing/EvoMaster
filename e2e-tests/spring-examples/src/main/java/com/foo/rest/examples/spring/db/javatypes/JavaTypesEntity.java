package com.foo.rest.examples.spring.db.javatypes;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
public class JavaTypesEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private final Boolean booleanField;

    @NotNull
    private final Byte byteField;

    @NotNull
    private final Character characterField;

    @NotNull
    private final Short shortField;

    @NotNull
    private final Integer integerField;

    @NotNull
    private final Long longField;

    @NotNull
    private final Float floatField;

    @NotNull
    private final Double doubleField;

    @NotNull
    private final Date dateField;

    @NotNull
    private final String stringField;

    public JavaTypesEntity() {

        stringField = "Hello World";
        dateField = new Date();
        doubleField = Math.PI;
        floatField = (float)Math.E;
        longField = Long.MAX_VALUE;
        integerField = Integer.MAX_VALUE;
        shortField = Short.MAX_VALUE;
        characterField = 'X';
        byteField = Byte.MAX_VALUE;
        booleanField = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getBooleanField() {
        return booleanField;
    }

    public Byte getByteField() {
        return byteField;
    }

    public Character getCharacterField() {
        return characterField;
    }

    public Short getShortField() {
        return shortField;
    }

    public Integer getIntegerField() {
        return integerField;
    }

    public Long getLongField() {
        return longField;
    }

    public Float getFloatField() {
        return floatField;
    }

    public Double getDoubleField() {
        return doubleField;
    }

    public Date getDateField() {
        return dateField;
    }

    public String getStringField() {
        return stringField;
    }
}
