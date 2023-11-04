package org.evomaster.client.java.controller.api.dto.database.schema;

import java.util.ArrayList;
import java.util.List;

public class EnumeratedTypeDto {
    /**
     * The name of the enumerated type
     */
    public String name;

    /**
     * A list of the values for the enumerated type
     */
    public List<String> values = new ArrayList<>();


}
