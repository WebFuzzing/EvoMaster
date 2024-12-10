package org.evomaster.e2etests.utils;

import org.evomaster.core.Main;
import org.evomaster.core.problem.rpc.RPCCallResult;
import org.evomaster.core.problem.rpc.RPCIndividual;
import org.evomaster.core.problem.util.ParamUtil;
import org.evomaster.core.search.Solution;
import org.evomaster.core.search.gene.collection.ArrayGene;
import org.evomaster.core.search.gene.collection.MapGene;
import org.evomaster.core.search.gene.interfaces.CollectionGene;
import org.evomaster.core.search.gene.Gene;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RPCTestBase extends EnterpriseTestBase {

    protected Solution<RPCIndividual> initAndRun(List<String> args){
        return (Solution<RPCIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    public void assertResponseContainCustomizedException(Solution<RPCIndividual> solution, String exceptionName, String content){
        boolean ok = solution.getIndividuals().stream().anyMatch(s->
                s.evaluatedMainActions().stream().anyMatch(e-> {
                    String body = ((RPCCallResult)e.getResult()).getCustomizedExceptionBody();
                    return body != null && body.contains(exceptionName) && body.contains(content);
                }));
        assertTrue(ok, seedMsg()+" do not find any exception matched with "+exceptionName+ " "+ content);
    }

    public static void assertResponseContainException(Solution<RPCIndividual> solution, String exceptionName){
        boolean ok = solution.getIndividuals().stream().anyMatch(s->
                s.evaluatedMainActions().stream().anyMatch(e-> {
                    String code = ((RPCCallResult)e.getResult()).getExceptionCode();
                    return code != null && code.equals(exceptionName);
                }));
        assertTrue(ok);
    }


    public void assertContentInResponseForEndpoint(Solution<RPCIndividual> solution, String methodName, String content){
        List<String> comparedHistory = new ArrayList<>();
        boolean ok = containsContent(solution, methodName, content, comparedHistory);
        String errorMsg = seedMsg()+" cannot find the content " +content+ " from responses" +System.lineSeparator() + String.join(System.lineSeparator(), comparedHistory);
        assertTrue(ok, errorMsg);

    }


    public String seedMsg(){
        return "With Seed:"+defaultSeed;
    }

    public void assertAnyContentInResponseForEndpoint(Solution<RPCIndividual> solution, String methodName, List<String> contents){
        List<String> comparedHistory = new ArrayList<>();
        boolean ok = contents.stream().anyMatch(content->containsContent(solution, methodName, content, comparedHistory));
        String errorMsg = seedMsg()+" cannot find any of " +String.join(",", contents)+ " from responses" +System.lineSeparator() + String.join(System.lineSeparator(), comparedHistory);

        assertTrue(ok, errorMsg);
    }

    public void assertAllContentInResponseForEndpoint(Solution<RPCIndividual> solution, String methodName, List<String> contents){
        List<String> comparedHistory = new ArrayList<>();
        boolean ok = contents.stream().allMatch(content->containsContent(solution, methodName, content, comparedHistory));
        String errorMsg = seedMsg()+" cannot find all " +String.join(",", contents)+ " from responses" +System.lineSeparator() + String.join(System.lineSeparator(), comparedHistory);
        assertTrue(ok, errorMsg);
    }

    public void assertNoneContentInResponseForEndpoint(Solution<RPCIndividual> solution, String methodName, List<String> contents){
        List<String> comparedHistory = new ArrayList<>();
        boolean ok = contents.stream().noneMatch(content->containsContent(solution, methodName, content, comparedHistory));
        String errorMsg = seedMsg()+" none should be found, but find any " +String.join(",", contents)+ " from responses" +System.lineSeparator() + String.join(System.lineSeparator(), comparedHistory);
        assertTrue(ok, errorMsg);
    }

    public boolean containsContent(Solution<RPCIndividual> solution, String methodName, String content, List<String> comparedHistory){
        return solution.getIndividuals().stream().anyMatch(s->
                s.getIndividual().seeMainExecutableActions().stream().anyMatch(a-> {
                    if (a.getName().equals(methodName)){
                        Gene gene = null;
                        if (a.getResponse() != null)
                            gene = a.getResponse().getGene();
                        return containContent(gene, content, comparedHistory);
                    }else return false;
                }));
    }

    public void assertRPCEndpointResult(Solution<RPCIndividual> solution, String methodName, String result){
        boolean ok = solution.getIndividuals().stream().anyMatch(s->
                s.evaluatedMainActions().stream().anyMatch(e->{
                    String code = ((RPCCallResult)e.getResult()).getInvocationCode();
                    return e.getAction().getName().equals(methodName) && code!=null && code.equals(result);
                }));

        assertTrue(ok);
    }

    public void assertSizeInResponseForEndpoint(Solution<RPCIndividual> solution, String methodName, Integer min, Integer max){
        boolean ok = solution.getIndividuals().stream().anyMatch(s->
                s.getIndividual().seeMainExecutableActions().stream().anyMatch(a-> {
                    int size = -1;
                    if (a.getResponse()!=null){
                        size = getCollectionSize( a.getResponse().getGene());
                    }
                    return a.getName().equals(methodName) && (min == null || size >= min) && (max == null ||  size <= max);
                    }));

        assertTrue(ok);
    }

    private boolean containContent(Gene gene, String content, List<String> comparedHistory){
        if (content == null) return true;
        if (gene == null) return false;

        Gene valueGene = ParamUtil.Companion.getValueGene(gene);
        if (valueGene.isPrintable()){
            comparedHistory.add(valueGene.getValueAsRawString());
            return valueGene.getValueAsRawString().contains(content);
        }
        //TODO fix other types
        return false;
    }

    private int getCollectionSize(Gene gene){
        if (gene == null) return -1;
        Gene valueGene = ParamUtil.Companion.getValueGene(gene);
        if (!(valueGene instanceof CollectionGene)){
           return -1;
        }

        if (valueGene instanceof ArrayGene)
            return ((ArrayGene)valueGene).getViewOfElements().size();
        if (valueGene instanceof MapGene)
            return ((MapGene) valueGene).getAllElements().size();
        return -1;
    }
}
