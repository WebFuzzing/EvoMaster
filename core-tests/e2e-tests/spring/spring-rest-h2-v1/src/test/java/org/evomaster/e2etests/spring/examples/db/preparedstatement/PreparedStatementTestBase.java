package org.evomaster.e2etests.spring.examples.db.preparedstatement;

import com.foo.rest.examples.spring.db.preparedstatement.PreparedStatementController;
import org.evomaster.e2etests.spring.examples.SpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public class PreparedStatementTestBase extends SpringTestBase {

    @BeforeAll
    public static void initClass() throws Exception {

        SpringTestBase.initClass(new PreparedStatementController());
    }
}
