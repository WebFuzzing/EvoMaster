import ObjectiveNaming from "../shared/ObjectiveNaming";
import UnitsInfoRecorder from "./UnitsInfoRecorder";

export default class ObjectiveRecorder {


    /**
     * Key -> the unique numeric id of the coverage objective
     * <br>
     * Value -> heuristic [0,1], where 1 means covered.
     * Only the highest value found so far is kept.
     */
    private static readonly maxObjectiveCoverage = new Map<number, number>();


    /**
     * Keep track of all target ids.
     * In contrast to the other data-structures in this class,
     * this info is when the SUT classes are loaded.
     * However, it is also important to notice that which classes
     * are loaded depends on what is executed.
     * We can force the loading of all classes, but usually that
     * is not a good idea, as static initializers might have
     * side effects.
     * However, we can do that at the end of the search once we are
     * done.
     * This can be useful to calculate how many targets we have missed.
     */
    private static readonly allTargets = new Set<string>();


    /**
     * Key -> id of an objective/target
     * <br>
     * Value -> a mapped unique id in numeric format
     * <br>
     * Note: we need this mapping to reduce the id size,
     * as to reduce TCP bandwidth consumption when communicating
     * with the EvoMaster process
     */
    private static idMapping = new Map<string, number>();

    private static reversedIdMapping = new Map<number, string>();


    /**
     * Counter used to generate unique numeric ids for idMapping
     */
    private static idMappingCounter: number = 0;

    /**
     * Counter used to get unique ids, where the number ordering and continuity
     * is not important. In other words, if an entity gets "n", that does not
     * mean that its next call will get "n+1", just a value "k" with "k!=n"
     */
    private static counter: number = 0;

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
    private static readonly firstTimeEncountered: Array<string> = new Array<string>();


    /**
     * Reset all the static state in this class
     */
    public static reset(alsoAtLoadTime: boolean) {
        ObjectiveRecorder.maxObjectiveCoverage.clear();
        ObjectiveRecorder.idMapping.clear();
        ObjectiveRecorder.reversedIdMapping.clear();
        ObjectiveRecorder.idMappingCounter = 0;
        ObjectiveRecorder.firstTimeEncountered.length = 0;
        ObjectiveRecorder.counter = 0;

        if (alsoAtLoadTime) {
            /*
                Shouldn't always reset it, because
                it is only computed at SUT classloading time

                TODO does this still apply for JavaScript?
             */
            ObjectiveRecorder.allTargets.clear();
        }
    }

    public static getNumberOfTargets() : number{
        return ObjectiveRecorder.allTargets.size;
    }


    /**
     * Mark the existence of a testing target.
     * This is important to do when SUT classes are loaded
     * and instrumented.
     * This cannot be done with the added probes in the
     * instrumentation, as what executed in the SUT depends
     * on test data.
     *
     * @param target a descriptive string representing the id of the target
     */
    public static registerTarget(target: string) {
        if (!target) {
            throw new Error("Empty target name");
        }
        ObjectiveRecorder.allTargets.add(target);

        if(target.startsWith(ObjectiveNaming.FILE)){
            UnitsInfoRecorder.markNewUnit(ObjectiveNaming.getFileIdFromFileObjectiveName(target));
        } else if(target.startsWith(ObjectiveNaming.LINE)){
            UnitsInfoRecorder.markNewLine();
        } else if(target.startsWith(ObjectiveNaming.BRANCH)){
            UnitsInfoRecorder.markNewBranch();
        }
    }

    /**
     * @return a coverage value in [0,1]
     */
    public static computeCoverage(prefix: string): number {

        let n = 0;
        let covered = 0;

        for (let id of ObjectiveRecorder.allTargets) {
            if (!id.startsWith(prefix)) {
                continue;
            }

            n++;
            if (ObjectiveRecorder.idMapping.has(id)) {
                const numericID = ObjectiveRecorder.idMapping.get(id);
                const h = ObjectiveRecorder.maxObjectiveCoverage.get(numericID);
                if (h === 1) {
                    covered++;
                }
            }
        }

        if (n == 0) {
            return 1;
        }

        return covered / n;
    }

// public static void printCoveragePerTarget(PrintWriter writer) {
//
//     allTargets.stream()
//         .sorted()
//         .forEachOrdered(id -> {
//             double h = 0;
//             if (idMapping.containsKey(id)) {
//                 int numericID = idMapping.get(id);
//                 h = maxObjectiveCoverage.get(numericID);
//             }
//             writer.println(id + " , " + h);
//         });
// }


    public static getTargetsSeenFirstTime(): Array<string> {
        return [...ObjectiveRecorder.firstTimeEncountered];
    }


    public static clearFirstTimeEncountered() {
        ObjectiveRecorder.firstTimeEncountered.length = 0;
    }


    /**
     * @param descriptiveId of the objective/target
     * @param value         of the coverage heuristic, in [0,1]
     */
    public static update(descriptiveId: string, value: number) {


        if (!descriptiveId) {
            throw new Error("Invalid empty id");
        }

        if (value < 0 || value > 1) {
            throw new Error("Invalid value " + value + " out of range [0,1]");
        }

        const id = ObjectiveRecorder.getMappedId(descriptiveId);

        if (!ObjectiveRecorder.maxObjectiveCoverage.has(id)) {
            ObjectiveRecorder.firstTimeEncountered.push(descriptiveId);
            ObjectiveRecorder.maxObjectiveCoverage.set(id, value);

        } else {

            const old = ObjectiveRecorder.maxObjectiveCoverage.get(id);
            if (value > old) {
                ObjectiveRecorder.maxObjectiveCoverage.set(id, value);
            }
        }
    }

    public static getMappedId(descriptiveId: string): number {

        if(! ObjectiveRecorder.idMapping.has(descriptiveId)){
            ObjectiveRecorder.idMapping.set(descriptiveId, ObjectiveRecorder.idMappingCounter++);
        }

        const id = ObjectiveRecorder.idMapping.get(descriptiveId);

        if(! ObjectiveRecorder.reversedIdMapping.has(id)){
            ObjectiveRecorder.reversedIdMapping.set(id, descriptiveId);
        }

        return id;
    }


    public static getDescriptiveIds(ids: Set<number>): Map<number, string> {

        const map = new Map<number, string>();
        for (let id of ids) {
            map.set(id, ObjectiveRecorder.getDescriptiveId(id));
        }

        return map;
    }

    public static getDescriptiveId(id: number): string {

        const descriptiveId = ObjectiveRecorder.reversedIdMapping.get(id);
        if (!descriptiveId) {
            throw new Error("Id '" + id + "' is not mapped");
        }

        return descriptiveId;
    }
}
