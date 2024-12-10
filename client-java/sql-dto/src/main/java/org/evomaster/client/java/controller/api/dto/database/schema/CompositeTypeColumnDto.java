package org.evomaster.client.java.controller.api.dto.database.schema;

public class CompositeTypeColumnDto {

    /**
     * The name of the composite type
     */
    public String name;

    /**
     * Returns if the column is a composite or a built-in type.
     * Columns of composite types can be built-in (e.g. integer, varchar)
     * or other composite types. For example, given the following
     * composite types:
     *
     * CREATE TYPE Address (
     *   street: varchar,
     *   city: varchar
     * );
     * CREATE TYPE Person (
     *   name: varchar,
     *   addr: Address
     * );
     *
     * The column "name" of Person type has varchar type
     * (and columnTypeIsComposite==false).
     * In contrast, the colun "addr" of Person type has Address type
     * (and columnTypeIsComposite==true).
     */
    public boolean columnTypeIsComposite;

    /**
     * The name of the composite type
     */
    public String type;

    public int size;

    public boolean nullable;

    public boolean isUnsigned;

    /**
     * precision of number
     *
     * the scale is the number of digits to the right of the decimal point
     */
    public Integer scale;

    /**
     * The number of dimensions for arrays, matrixs, etc.
     */
    public int numberOfDimensions = 0;

}
