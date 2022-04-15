package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class CompositeTypeColumnDto {

    /**
     * The name of the composite type
     */
    public String name;

    /**
     * Returns if the column is a composite or a built-in type
     */
    public boolean isCompositeType;

    /**
     * The name of the composite type
     */
    public String type;

    public int size;

    public boolean nullable;

    public boolean isUnsigned = false;

    /**
     * precision of number
     * a negative number means that the precision is unspecified or inapplicable
     */
    public int precision = -1;

    /**
     * The number of dimensions for arrays, matrixs, etc.
     */
    public int numberOfDimensions = 0;

}
