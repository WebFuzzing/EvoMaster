package com.foo.rest.examples.spring.db.javatypes;


import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Entity
public class JavaTypesEntity {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Primitive data types
     */
    private final boolean primitiveBoolean = true;
    private final char primitiveChar = Character.MAX_VALUE;
    private final short primitiveShort = Short.MAX_VALUE;
    private final byte primitiveByte = Byte.MAX_VALUE;
    private final int primitiveInt = Integer.MAX_VALUE;
    private final long primitiveLong = Long.MAX_VALUE;
    private final float primitiveFloat = Float.MAX_VALUE;
    private final double primitiveDouble = Double.MAX_VALUE;

    /**
     * Wrapped data types
     */
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

    /**
     * Date date type
     */
    @NotNull
    private final Date dateField;

    /**
     * By default Strings are stored as VARCHAR(255).
     * Any attempt to persist a string value beyond that length
     * leads to a "Value too long for column" SQL exception
     */
    @NotNull
    private final String shortString;

    private static final int H2_MAX_LENGTH_VARCHAR = 1000000000;

    @NotNull
    @Column(length = H2_MAX_LENGTH_VARCHAR)
    private final String longString;


    @NotNull
    private final BigDecimal bigDecimal = new BigDecimal(10);

    public JavaTypesEntity() {

        shortString = "Hello World";
        longString = new String(new char[300]).replace('\0', 'X');
        dateField = new Date();
        doubleField = Math.PI;
        floatField = (float) Math.E;
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

    public String getShortString() {
        return shortString;
    }

    public String getLongString() {
        return longString;
    }

}
