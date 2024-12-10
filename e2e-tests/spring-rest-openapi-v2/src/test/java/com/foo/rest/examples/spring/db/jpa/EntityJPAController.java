package com.foo.rest.examples.spring.db.jpa;

import com.foo.rest.examples.spring.db.SpringWithDbController;

import java.util.Arrays;
import java.util.List;

public class EntityJPAController extends SpringWithDbController {

    public EntityJPAController(){
        super(EntityJPAApplication.class);
    }

    @Override
    protected List<String> extraSettings(){
        return Arrays.asList(
                //https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.5-Release-Notes#SQL-Script-DataSource-Initialization
                "--spring.jpa.hibernate.ddl-auto=none",
                "--spring.sql.init.mode=always",
                "--spring.sql.init.schema-locations=classpath:sql/entityjpa.sql"
//                "--spring.jpa.properties.javax.persistence.schema-generation.create-source=metadata",
//                "--spring.jpa.properties.javax.persistence.schema-generation.scripts.action=create",
//                "--spring.jpa.properties.javax.persistence.schema-generation.scripts.create-target=sql/entity.sql"
        );
    }
}
