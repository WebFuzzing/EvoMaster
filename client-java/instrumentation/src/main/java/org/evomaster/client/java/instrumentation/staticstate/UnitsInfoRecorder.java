package org.evomaster.client.java.instrumentation.staticstate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep track of static info on the SUT related to its classes,
 * eg total number of lines
 *
 * WARNING: unless we force somehow to load all classes, this info
 * here will be partial
 *
 * TODO: we could force class loading at the end of the search, but
 * could be bit tricky when dealing with custom class-loaders like in Spring
 *
 * Created by arcuri82 on 27-Sep-19.
 */
public class UnitsInfoRecorder implements Serializable {

    private static UnitsInfoRecorder singleton = new UnitsInfoRecorder();

    //see entries in UnitsInfoDto

    private Set<String> unitNames;
    private AtomicInteger numberOfLines;
    private AtomicInteger numberOfBranches;
    private AtomicInteger numberOfReplacedMethodsInSut;
    private AtomicInteger numberOfReplacedMethodsInThirdParty;
    private AtomicInteger numberOfTrackedMethods;

    private UnitsInfoRecorder(){
        unitNames = new CopyOnWriteArraySet<>();
        numberOfLines = new AtomicInteger(0);
        numberOfBranches = new AtomicInteger(0);
        numberOfReplacedMethodsInSut = new AtomicInteger(0);
        numberOfReplacedMethodsInThirdParty = new AtomicInteger(0);
        numberOfTrackedMethods = new AtomicInteger(0);
    }

    /**
     * Only need for tests
     */
    public static void reset(){
        singleton = new UnitsInfoRecorder();
    }

    public static UnitsInfoRecorder getInstance(){
        return singleton;
    }

    public static void markNewUnit(String name){
        singleton.unitNames.add(name);
    }

    public static void markNewLine(){
        singleton.numberOfLines.incrementAndGet();
    }

    public static void markNewBranchPair(){
        singleton.numberOfBranches.addAndGet(2);
    }

    public static void markNewReplacedMethodInSut(){
        singleton.numberOfReplacedMethodsInSut.incrementAndGet();
    }

    public static void markNewReplacedMethodInThirdParty(){
        singleton.numberOfReplacedMethodsInThirdParty.incrementAndGet();
    }

    public static void markNewTrackedMethod(){
        singleton.numberOfTrackedMethods.incrementAndGet();
    }



    public  int getNumberOfUnits() {
        return unitNames.size();
    }

    public  Set<String> getUnitNames() {
        return Collections.unmodifiableSet(unitNames);
    }

    public  int getNumberOfLines() {
        return numberOfLines.get();
    }

    public  int getNumberOfBranches() {
        return numberOfBranches.get();
    }

    public  int getNumberOfReplacedMethodsInSut() {
        return numberOfReplacedMethodsInSut.get();
    }

    public  int getNumberOfReplacedMethodsInThirdParty() {
        return numberOfReplacedMethodsInThirdParty.get();
    }

    public  int getNumberOfTrackedMethods() {
        return numberOfTrackedMethods.get();
    }
}
