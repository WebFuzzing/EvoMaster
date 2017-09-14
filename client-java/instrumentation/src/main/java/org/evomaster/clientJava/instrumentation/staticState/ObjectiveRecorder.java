package org.evomaster.clientJava.instrumentation.staticState;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep track of all objective coverage so far.
 * This is different from ExecutionTrace that is reset after
 * each test execution.
 */
public class ObjectiveRecorder {


    /**
     * Key -> the unique id of the coverage objective
     * <br>
     * Value -> heuristic [0,1], where 1 means covered.
     *          Only the highest value found so far is kept.
     */
    private static final Map<Integer, Double> maxObjectiveCoverage =
            new ConcurrentHashMap<>(65536);

    /**
     * Key -> id of an objective/target
     * <br>
     * Value -> a mapped unique id in numeric format
     * <br>
     * Note: we need this mapping to reduce the id size,
     * as to reduce TCP bandwidth consumption when communicating
     * with the EvoMaster process
     */
    private static Map<String, Integer> idMapping =
            new ConcurrentHashMap<>(65536);

    private static Map<Integer, String> reversedIdMapping =
            new ConcurrentHashMap<>(65536);


    /**
     * Counter used to generate unique numeric ids for idMapping
     */
    private static final AtomicInteger idMappingCounter = new AtomicInteger(0);

    /**
     * Counter used to get unique ids, where the number ordering and continuity
     * is not important. In other words, if an entity gets "n", that does not
     * mean that its next call will get "n+1", just a value "k" with "k!=n"
     */
    private static final AtomicInteger counter = new AtomicInteger(0);

    /**
     * It will be the EvoMaster process that does ask the SUT controller
     * which objectives to report on.
     * This is needed to save bandwidth, as coverage of already covered objectives
     * would be redundant information (this is due to the use of archives).
     * However, EvoMaster process can only know of objectives that have been
     * reported so far. Therefore, we need a way to report every time a
     * new objective is found (not necessarily fully covered).
     * Here, we keep track of objective ids that have been encountered
     * for the first time and have not been reported yet to the EvoMaster
     * process.
     */
    private static final Queue<String> firstTimeEncountered = new ConcurrentLinkedQueue<>();


    /**
     * Reset all the static state in this class
     */
    public static void reset(){
        maxObjectiveCoverage.clear();
        idMapping.clear();
        reversedIdMapping.clear();
        idMappingCounter.set(0);
        firstTimeEncountered.clear();
        counter.set(0);
    }

    public static void printCoveragePerTarget(PrintWriter writer){
        for(Map.Entry<Integer, Double> entry : maxObjectiveCoverage.entrySet()){
            writer.println("" + entry.getKey() + " , " + entry.getValue());
        }
    }


    public static List<String> getTargetsSeenFirstTime(){

        return Collections.unmodifiableList(new ArrayList<>(firstTimeEncountered));
    }


    public static void clearFirstTimeEncountered(){
        firstTimeEncountered.clear();
    }


    public static int getAUniqueId(){
        return counter.getAndIncrement();
    }

    /**
     *
     * @param descriptiveId of the objective/target
     * @param value of the coverage heuristic, in [0,1]
     */
    public static void update(String descriptiveId, double value){

        Objects.requireNonNull(descriptiveId);
        if(value < 0d || value > 1){
            throw new IllegalArgumentException("Invalid value "+value +" out of range [0,1]");
        }

        int id = getMappedId(descriptiveId);

        if(! maxObjectiveCoverage.containsKey(id)){
            firstTimeEncountered.add(descriptiveId);
            maxObjectiveCoverage.put(id, value);

        } else {

            double old = maxObjectiveCoverage.get(id);
            if (value > old) {
                maxObjectiveCoverage.put(id, value);
            }
        }
    }

    public static int getMappedId(String descriptiveId){

        int id = idMapping.computeIfAbsent(descriptiveId, k -> idMappingCounter.getAndIncrement());
        reversedIdMapping.computeIfAbsent(id, k -> descriptiveId);

        return id;
    }


    public static Map<Integer, String> getDescriptiveIds(Collection<Integer> ids){

        Map<Integer, String> map = new HashMap<>(ids.size());
        for(Integer id: ids){
            map.put(id, getDescriptiveId(id));
        }

        return map;
    }

    public static String getDescriptiveId(int id){

        String descriptiveId = reversedIdMapping.get(id);
        if(descriptiveId == null){
            throw new IllegalArgumentException("Id '"+id+"' is not mapped");
        }

        return descriptiveId;
    }
}
