package org.evomaster.e2etests.utils;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;

import java.util.List;

public class RPCTestBase extends WsTestBase{

    protected Solution<RPCIndividual> initAndRun(List<String> args){
        return (Solution<RPCIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }
}
