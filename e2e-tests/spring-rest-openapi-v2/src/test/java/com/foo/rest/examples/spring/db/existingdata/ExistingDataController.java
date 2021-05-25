package com.foo.rest.examples.spring.db.existingdata;

import com.foo.rest.examples.spring.db.SpringWithDbController;

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

        ExistingDataEntityX x = new ExistingDataEntityX();
        x.setId(42L);
        x.setName("Foo");

        ExistingDataRepositoryX rep = ctx.getBean(ExistingDataRepositoryX.class);
        rep.save(x);
    }
}
