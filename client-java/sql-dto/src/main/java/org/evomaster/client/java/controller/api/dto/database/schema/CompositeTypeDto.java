package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class CompositeTypeDto {

    /**
     * The name of the composite type
     */
    public String name;

    /**
     * A list of descriptions for each column in the type.
     * Composite types can be nested.
     */
    public List<CompositeTypeColumnDto> columns = new ArrayList<>();


}
