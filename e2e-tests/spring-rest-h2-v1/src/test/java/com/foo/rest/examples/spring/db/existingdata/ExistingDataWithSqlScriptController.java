package com.foo.rest.examples.spring.db.existingdata;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import org.evomaster.client.java.sql.DbSpecification;

import java.util.Collections;
import java.util.List;

public class ExistingDataWithSqlScriptController extends SpringWithDbController {

    public ExistingDataWithSqlScriptController() {
        super(ExistingDataApp.class);
    }

    @Override
    public void resetStateOfSUT() {
        super.resetStateOfSUT();
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        List<DbSpecification> spec =  super.getDbSpecifications();
        if (spec != null && !spec.isEmpty()) {
            return Collections.singletonList(spec.get(0).withInitSqlOnResourcePath("/sql/existingdata.sql"));
        }
        return spec;
    }
}
