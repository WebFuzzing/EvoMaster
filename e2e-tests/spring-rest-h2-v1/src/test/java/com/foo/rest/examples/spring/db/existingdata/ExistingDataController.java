package com.foo.rest.examples.spring.db.existingdata;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import org.evomaster.client.java.sql.DbSpecification;

import java.util.Collections;
import java.util.List;

/**
 * Created by arcuri82 on 19-Jun-19.
 */
public class ExistingDataController extends SpringWithDbController {

    public ExistingDataController() {
        super(ExistingDataApp.class);
    }

    @Override
    public void resetStateOfSUT() {
        super.resetStateOfSUT();

//        ExistingDataEntityX x = new ExistingDataEntityX();
//        x.setId(42L);
//        x.setName("Foo");
//
//        ExistingDataRepositoryX rep = ctx.getBean(ExistingDataRepositoryX.class);
//        rep.save(x);
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        List<DbSpecification> spec =  super.getDbSpecifications();
        if (spec != null && !spec.isEmpty())
            return Collections.singletonList(spec.get(0)
                    .withInitSqlScript("INSERT INTO EXISTING_DATA_ENTITYX (ID, NAME) VALUES (42, 'Foo')"));
        return spec;
    }
}
