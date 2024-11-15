package org.evomaster.client.java.sql;

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Based on the database, there are internal schemas that we don't need to analyze
 */
public class SchemasToSkip {

    public static List<String> get(DatabaseType type){

        if(type == DatabaseType.H2){
            return Arrays.asList("INFORMATION_SCHEMA");
        }
        if(type == DatabaseType.MYSQL){
            return Arrays.asList("sys");
        }

        return Collections.emptyList();
    }
}
