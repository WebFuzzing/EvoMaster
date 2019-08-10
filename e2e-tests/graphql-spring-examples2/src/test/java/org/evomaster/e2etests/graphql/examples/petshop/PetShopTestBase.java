package org.evomaster.e2etests.graphql.examples.petshop;

import com.foo.graphql.examples.spring.petshop.GraphQLSpringBootTutorialApplicationController;
import org.evomaster.e2etests.graphql.examples.GraphqlSpringTestBase;
import org.junit.jupiter.api.BeforeAll;

public abstract class PetShopTestBase extends GraphqlSpringTestBase {


    @BeforeAll
    public static void initClass() throws Exception {

        GraphQLSpringBootTutorialApplicationController controller = new GraphQLSpringBootTutorialApplicationController();
        GraphqlSpringTestBase.initClass(controller);
    }
}
