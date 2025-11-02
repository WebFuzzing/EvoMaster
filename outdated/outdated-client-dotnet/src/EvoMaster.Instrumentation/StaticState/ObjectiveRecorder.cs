using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using EvoMaster.Client.Util;
using EvoMaster.Client.Util.Extensions;

namespace EvoMaster.Instrumentation.StaticState {
    /**
 * Keep track of all objective coverage so far.
 * This is different from ExecutionTrace that is reset after
 * each test execution.
 */
    public class ObjectiveRecorder {
        /*
        * Key -> the unique id of the coverage objective
        * <br>
        * Value -> heuristic [0,1], where 1 means covered.
        * Only the highest value found so far is kept.
        */
        private static readonly IDictionary<int, double> MaxObjectiveCoverage =
            new ConcurrentDictionary<int, double>();


        /*
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
        public static readonly ConcurrentHashSet<string> AllTargets = new ConcurrentHashSet<string>();

        /*
        * Key -> id of an objective/target
        * <br>
        * Value -> a mapped unique id in numeric format
        * <br>
        * Note: we need this mapping to reduce the id size,
        * as to reduce TCP bandwidth consumption when communicating
        * with the EvoMaster process
        */
        private static IDictionary<string, int> _idMapping = new ConcurrentDictionary<string, int>();

        private static IDictionary<int, string> _reversedIdMapping = new ConcurrentDictionary<int, string>();

        /*
        Counter used to generate unique numeric ids for idMapping
        */
        //TODO: should be atomic, final
        private static int IdMappingCounter;

        /*
        Counter used to get unique ids, where the number ordering and continuity
        is not important. In other words, if an entity gets "n", that does not
        mean that its next call will get "n+1", just a value "k" with "k!=n"
        */
        //TODO: should be atomic, final
        private static int Counter;

        /*
        It will be the EvoMaster process that does ask the SUT controller
        which objectives to report on.
        This is needed to save bandwidth, as coverage of already covered objectives
        would be redundant information (this is due to the use of archives).
        However, EvoMaster process can only know of objectives that have been
        reported so far. Therefore, we need a way to report every time a
        new objective is found (not necessarily fully covered).
        Here, we keep track of objective ids that have been encountered
        for the first time and have not been reported yet to the EvoMaster
        process
        */
        private static readonly ConcurrentQueue<string> FirstTimeEncountered = new ConcurrentQueue<string>();


        /**
     * Reset all the static state in this class
     */
        public static void Reset(bool alsoAtLoadTime) {
            MaxObjectiveCoverage.Clear();
            _idMapping.Clear();
            _reversedIdMapping.Clear();
            //TODO: check
            IdMappingCounter = 0;
            FirstTimeEncountered.Clear();
            //TODO: check
            Counter = 0;

            if (alsoAtLoadTime) {
                /*
                Shouldn't always reset it, because
                it is only computed at SUT classloading time
             */
                AllTargets.Clear();
            }
        }

        ///<summary>
        /// Mark the existence of a testing target.
        /// This is important to do when SUT classes are loaded and instrumented.
        /// This cannot be done with the added probes in the instrumentation, as what executed in the SUT depends on test data.
        /// </summary>
        /// <param name="target">A descriptive string representing the id of the target</param>
        public static void RegisterTarget(string target) {
            if (target == null || string.IsNullOrEmpty(target)) {
                throw new ArgumentException("Empty target name");
            }

            AllTargets.Add(target);
        }

        /// <returns>A coverage value in [0,1]</returns>
        public static double ComputeCoverage(string prefix) {
            var n = 0;
            var covered = 0;

            foreach (var id in AllTargets) {
                if (!id.StartsWith(prefix)) {
                    continue;
                }

                n++;
                if (_idMapping.ContainsKey(id)) {
                    var numericId = _idMapping[id];
                    var h = MaxObjectiveCoverage[numericId];
                    if (h == 1d) {
                        covered++;
                    }
                }
            }

            if (n == 0) {
                return 1d;
            }

            return (double) covered / (double) n;
        }

        // public static void PrintCoveragePerTarget(PrintWriter writer)
        // {
        //     AllTargets.stream()
        //         .sorted()
        //         .forEachOrdered(id-> {
        //         double? h = 0;
        //         if (_idMapping.containsKey(id))
        //         {
        //             int numericId = _idMapping.get(id);
        //             h = MaxObjectiveCoverage.get(numericId);
        //         }
        //
        //         writer.println(id + " , " + h);
        //     });
        // }


        public static IList<string> GetTargetsSeenFirstTime() {
            return new List<string>(FirstTimeEncountered).AsReadOnly();
        }


        public static void ClearFirstTimeEncountered() {
            FirstTimeEncountered.Clear();
        }


        ///<param name="descriptiveId">Descriptive Id of the objective/target</param>
        ///<param name="value">value of the coverage heuristic, in [0,1]</param>
        public static void Update(string descriptiveId, double value) {
            if (descriptiveId == null) throw new ArgumentNullException();
            if (value < 0d || value > 1) {
                throw new ArgumentException("Invalid value " + value + " out of range [0,1]");
            }

            var id = GetMappedId(descriptiveId);

            if (!MaxObjectiveCoverage.ContainsKey(id)) {
                FirstTimeEncountered.Enqueue(descriptiveId);
                MaxObjectiveCoverage.Add(id, value);
            }
            else {
                var old = MaxObjectiveCoverage[id];
                if (value > old) {
                    MaxObjectiveCoverage[id] = value;
                }
            }
        }

        public static int GetMappedId(string descriptiveId) {
            var id = _idMapping.ComputeIfAbsent(descriptiveId, k => {
                //int x = IdMappingCounter.getAndIncrement();
                var x = IdMappingCounter;
                Interlocked.Increment(ref IdMappingCounter);

                _reversedIdMapping.ComputeIfAbsent(x, t => descriptiveId);
                return x;
            });
            //reversedIdMapping.computeIfAbsent(id, k -> descriptiveId);
            return id;
        }


        public static Dictionary<int, string> GetDescriptiveIds(ICollection<int> ids) {
            return ids.ToDictionary(id => id, GetDescriptiveId);
        }

        public static string GetDescriptiveId(int id) {
            var descriptiveId = _reversedIdMapping[id];

            if (descriptiveId == null) {
                throw new ArgumentException("Id '" + id + "' is not mapped");
            }

            return descriptiveId;
        }
    }
}