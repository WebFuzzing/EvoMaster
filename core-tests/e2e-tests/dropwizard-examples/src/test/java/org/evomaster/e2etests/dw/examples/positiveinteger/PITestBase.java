package org.evomaster.e2etests.dw.examples.positiveinteger;

import com.foo.rest.examples.dw.positiveinteger.PIController;
import org.evomaster.e2etests.utils.RestTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

@Disabled("DropWizard looks like it is keeping some static internal state, which screws the running of tests with different REST APIs")
public abstract class PITestBase extends RestTestBase{


    @BeforeAll
    public static void initClass() throws Exception {

        RestTestBase.initClass(new PIController());
    }

}
