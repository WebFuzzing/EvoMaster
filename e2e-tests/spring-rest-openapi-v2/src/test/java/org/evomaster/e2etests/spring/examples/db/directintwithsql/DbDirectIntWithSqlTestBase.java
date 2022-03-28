package org.evomaster.e2etests.spring.examples.db.directintwithsql;

import com.foo.rest.examples.spring.db.directintwithsql.DbDirectIntWithSqlController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class DbDirectIntWithSqlTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new DbDirectIntWithSqlController());
    }
}
