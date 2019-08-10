package org.evomaster.e2etests.graphql.examples;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.e2etests.utils.GraphqlTestBase;

public class GraphqlSpringTestBase extends GraphqlTestBase {

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        GraphqlTestBase.initClass(controller);
    }
}