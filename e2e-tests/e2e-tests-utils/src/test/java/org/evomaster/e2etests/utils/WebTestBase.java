package org.evomaster.e2etests.utils;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.core.EMConfig;
import org.evomaster.core.Main;
import org.evomaster.core.problem.webfrontend.WebIndividual;
import org.evomaster.core.search.Solution;

import java.util.List;

public abstract class WebTestBase extends EnterpriseTestBase{

    protected static void initClass(EmbeddedSutController controller) throws Exception {
        initClass(controller, new EMConfig());
    }

    protected Solution<WebIndividual> initAndRun(List<String> args){
        return (Solution<WebIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }
}
