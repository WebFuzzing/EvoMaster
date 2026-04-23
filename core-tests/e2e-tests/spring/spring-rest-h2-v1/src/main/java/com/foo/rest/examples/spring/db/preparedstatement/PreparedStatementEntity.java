package com.foo.rest.examples.spring.db.preparedstatement;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "Foo")
public class PreparedStatementEntity {


    @Id
    @Column(name="identifier")
    @NotNull
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Integer identifier;


    @Column(name="integervalue")
    // integer value
    @NotNull
    private Integer integerValue;

    // string value
    @Column(name="stringvalue")
    @NotNull
    private String stringValue;

    // boolean value
   @Column(name="booleanvalue")
   @NotNull
   private boolean booleanValue;

    // empty constructor
    public PreparedStatementEntity() { }


    // get method for the integerValue
    public Integer getIntegerValue() {
        return integerValue;
    }

    // set method for the integerValue
    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    // get method for the stringValue
    public String getStringValue() {
        return stringValue;
    }

    // set method for the stringValue
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    // get method for the booleanValue
    public boolean isBooleanValue() {
        return booleanValue;
    }

    // set method for the booleanValue
    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }
}
