package org.evomaster.e2etests.utils;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rpc.RPCCallResult;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.search.Solution;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RPCTestBase extends WsTestBase{

    protected Solution<RPCIndividual> initAndRun(List<String> args){
        return (Solution<RPCIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    public static void assertResponseContainCustomizedException(Solution<RPCIndividual> solution, String exceptionName, String content){
        boolean ok = solution.getIndividuals().stream().anyMatch(s->
                s.evaluatedActions().stream().anyMatch(e-> {
                    String body = ((RPCCallResult)e.getResult()).getCustomizedExceptionBody();
                    return body != null && body.contains(exceptionName) && body.contains(content);
                }));
        assertTrue(ok, "do not find any exception matched with "+exceptionName+ " "+ content);
    }

    public static void assertResponseContainException(Solution<RPCIndividual> solution, String exceptionName){
        boolean ok = solution.getIndividuals().stream().anyMatch(s->
                s.evaluatedActions().stream().anyMatch(e-> {
                    String code = ((RPCCallResult)e.getResult()).getExceptionCode();
                    return code != null && code.equals(exceptionName);
                }));
        assertTrue(ok);
    }
}
