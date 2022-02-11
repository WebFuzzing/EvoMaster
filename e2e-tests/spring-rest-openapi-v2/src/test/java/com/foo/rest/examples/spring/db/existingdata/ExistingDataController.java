package com.foo.rest.examples.spring.db.existingdata;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import org.evomaster.client.java.controller.internal.db.DbSpecification;

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
    public DbSpecification setDbSpecification() {
        DbSpecification spec =  super.setDbSpecification();
        if (spec != null)
            spec.initSqlScript= "INSERT INTO EXISTING_DATA_ENTITYX (ID, NAME) VALUES (42, 'Foo')";
        return spec;
    }
}
