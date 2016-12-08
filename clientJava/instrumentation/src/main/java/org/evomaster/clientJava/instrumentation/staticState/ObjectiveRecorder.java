package org.evomaster.clientJava.instrumentation.staticState;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
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
    private static final Map<String, Double> maxObjectiveCoverage =
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
        idMappingCounter.set(0);
        firstTimeEncountered.clear();
        counter.set(0);
    }

    public static int getAUniqueId(){
        return counter.getAndIncrement();
    }

    /**
     *
     * @param id of the objective/target
     * @param value of the coverage heuristic, in [0,1]
     */
    public static void update(String id, double value){
        Objects.requireNonNull(id);
        if(value < 0d || value > 1){
            throw new IllegalArgumentException("Invalid value "+value +" out of range [0,1]");
        }

        idMapping.computeIfAbsent(id, k -> idMappingCounter.getAndIncrement());

        if(! maxObjectiveCoverage.containsKey(id)){
            firstTimeEncountered.add(id);
            maxObjectiveCoverage.put(id, value);

        } else {

            double old = maxObjectiveCoverage.get(id);
            if (value > old) {
                maxObjectiveCoverage.put(id, value);
            }
        }
    }
}
