package org.evomaster.client.java.instrumentation.staticstate;

import org.evomaster.client.java.instrumentation.ClassAnalyzer;
import org.evomaster.client.java.instrumentation.JpaConstraint;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private AtomicInteger numberOfInstrumentedNumberComparisons;


    /*
        Key -> DTO full name
        Value -> OpenAPI object schema
        TODO should consider if also adding info on type, eg JSON vs XML
     */
    private Map<String,String> parsedDtos;

    private List<JpaConstraint> jpaConstraints;

    private volatile boolean analyzedClasses;

    /*
        Key -> DTO full name
        Value -> OpenAPI object schema

        User might need to get schema of specific jvm dto classes
        and such jvm classes might not be read with jackson or gson
        this field is to collect a map of such specified jvm dto classes to their schema
     */
    private Map<String, String> extractedSpecifiedDtos;

    /**
     * Keeping track, for each instrumented class, of which classloader was used.
     * Note: a class can loaded several times from different classloaders.
     *
     * Key -> full class name, with dots
     * Value -> non-empty list of classloaders
     */
    private transient Map<String, List<ClassLoader>> classLoaders;

    private UnitsInfoRecorder(){
        unitNames = new CopyOnWriteArraySet<>();
        numberOfLines = new AtomicInteger(0);
        numberOfBranches = new AtomicInteger(0);
        numberOfReplacedMethodsInSut = new AtomicInteger(0);
        numberOfReplacedMethodsInThirdParty = new AtomicInteger(0);
        numberOfTrackedMethods = new AtomicInteger(0);
        numberOfInstrumentedNumberComparisons = new AtomicInteger(0);
        parsedDtos = new ConcurrentHashMap<>();
        jpaConstraints = new CopyOnWriteArrayList<>();
        analyzedClasses = false;
        extractedSpecifiedDtos = new ConcurrentHashMap<>();
        classLoaders = new ConcurrentHashMap<>();
    }

    /**
     * Only need for tests. DO NOT CALL DIRECTLY IN THE SEARCH!!!
     * otherwise, it would make impossible to run experiments twice on same running SUT,
     * as classes are loaded only once
     */
    public static void reset(){
        UnitsInfoRecorder copy = new UnitsInfoRecorder();
        /*
            A class can be loaded only once for same classloader.
            Cleaning this transient data structure would imply that the same info cannot be inferred again,
            which would make not possible to use the same SUT in 2 different E2E
         */
        copy.classLoaders.putAll(singleton.classLoaders);
        singleton = copy;
    }

    public static UnitsInfoRecorder getInstance(){
        return singleton;
    }

    public static void forceLoadingLazyDataStructures(){
        singleton.getJpaConstraints();
    }

    public static void registerClassLoader(String className, ClassLoader classLoader){
        singleton.classLoaders.putIfAbsent(className, new CopyOnWriteArrayList<>());
        singleton.classLoaders.get(className).add(classLoader);
    }

    public static void markNewUnit(String name){
        synchronized (singleton) {
            singleton.unitNames.add(name);
            singleton.analyzedClasses = false;
            singleton.jpaConstraints.clear();
        }
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

    public static void markNewInstrumentedNumberComparison(){
        singleton.numberOfInstrumentedNumberComparisons.incrementAndGet();
    }

    public static void registerNewParsedDto(String name, String schema){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("Empty dto name");
        }
        if(schema == null || schema.isEmpty()){
            throw new IllegalArgumentException("Empty schema");
        }

        if(! singleton.parsedDtos.containsKey(name)){
            singleton.parsedDtos.put(name, schema);
        }
    }

    public static Boolean isDtoSchemaRegister(String name){
        return singleton.parsedDtos.containsKey(name);
    }

    public static void registerSpecifiedDtoSchema(Map<String, String> schemaMap){
        for (String name: schemaMap.keySet()){
            if(name == null || name.isEmpty()){
                throw new IllegalArgumentException("registerSpecifiedDtoSchema: empty dto name");
            }

            String schema = schemaMap.get(name);

            if(schema == null || schema.isEmpty()){
                throw new IllegalArgumentException("registerSpecifiedDtoSchema: empty schema");
            }

            if(! singleton.extractedSpecifiedDtos.containsKey(name)){
                singleton.extractedSpecifiedDtos.put(name, schema);
            }
        }

    }

    public static void registerNewJpaConstraint(JpaConstraint constraint){
        singleton.jpaConstraints.add(constraint);
    }

    public ClassLoader getSutClassLoader(){
        if(unitNames.isEmpty()){
            return null;
        }
        return classLoaders.get(unitNames.stream().findFirst().get()).get(0);
    }

    public List<JpaConstraint> getJpaConstraints(){

        /*
            Tricky, could not find a good way to intercept where classes are loaded...
            when using transformation in Agent, we can intercept _before_ loading, but not _after_.
            So, here we do it lazily, by forcing loading on get()
         */
        synchronized (singleton) {
            if (!analyzedClasses) {
                ClassAnalyzer.doAnalyze(unitNames);
                analyzedClasses = true;
            }

            return Collections.unmodifiableList(jpaConstraints);
        }
    }

    public boolean areClassesAnalyzed(){
        return analyzedClasses;
    }

    public  int getNumberOfUnits() {
        return unitNames.size();
    }

    public  Set<String> getUnitNames() {
        return Collections.unmodifiableSet(unitNames);
    }

    public Map<String,String> getParsedDtos(){
        return Collections.unmodifiableMap(parsedDtos);
    }

    public Map<String,String> getExtractedSpecifiedDtos(){
        return Collections.unmodifiableMap(extractedSpecifiedDtos);
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

    public  int getNumberOfInstrumentedNumberComparisons(){
        return numberOfInstrumentedNumberComparisons.get();
    }

    public List<ClassLoader> getClassLoaders(String className){
        return classLoaders.get(className);
    }

    public ClassLoader getFirstClassLoader(String className){
        List<ClassLoader> loaders = getClassLoaders(className);
        if(loaders == null || loaders.isEmpty()){
            return null;
        }
        return loaders.get(0);
    }
}
