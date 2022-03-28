package org.evomaster.e2etests.spring.examples.db.directint;

import com.foo.rest.examples.spring.db.directint.DbDirectIntController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class DbDirectIntTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new DbDirectIntController());
    }

}
