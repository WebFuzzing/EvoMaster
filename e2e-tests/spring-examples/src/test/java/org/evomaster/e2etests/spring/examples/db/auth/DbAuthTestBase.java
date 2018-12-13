package org.evomaster.e2etests.spring.examples.db.auth;

import com.foo.rest.examples.spring.db.auth.DbAuthController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class DbAuthTestBase extends SpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new DbAuthController());
    }

}