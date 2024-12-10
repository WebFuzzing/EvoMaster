package org.evomaster.e2etests.spring.examples.db.base;

import com.foo.rest.examples.spring.db.base.DbBaseController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class DbBaseTestBase extends SpringTestBase{


    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new DbBaseController());
    }

}
