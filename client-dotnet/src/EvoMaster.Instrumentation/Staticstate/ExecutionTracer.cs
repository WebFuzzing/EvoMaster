using System;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace EvoMaster.Instrumentation.Staticstate
{
    public static class ExecutionTracer
    {
        /*
         * Key -> the unique descriptive id of the coverage objective
         *
         * java version: private static final Map<String, TargetInfo> objectiveCoverage = new ConcurrentHashMap<>(65536);
         * Note that there is no constructor to only init capacity.
         * https://docs.microsoft.com/en-us/dotnet/api/system.collections.concurrent.concurrentdictionary-2?view=netcore-3.1
         * TODO need a further check whether the current concurrencylevel setting is property later.
         * */
        private static readonly IDictionary<string, TargetInfo> ObjectiveCoverage =
            new ConcurrentDictionary<string, TargetInfo>(Environment.ProcessorCount, 65536);


        /**
         *  A test case can be composed by 1 or more actions, eg HTTP calls.
         * When we get the best distance for a testing target, we might
         * also want to know which action in the test led to it.
         * */
        private static int _actionIndex = 0;

        /**
         * A set of possible values used in the tests, needed for some kinds
         * of taint analyses
         * */
        private static ISet<string> _inputVariables = new HashSet<string>();

        /**
         * Besides code coverage, there might be other events that we want to
         * keep track during test execution.
         * We keep track of it separately for each action
         */
        private static readonly IList<AdditionalInfo> AdditionalInfoList = new List<AdditionalInfo>();

        /**
         * Keep track of expensive operations. Might want to skip doing them if too many.
         * This should be re-set for each action
         */
        private static int _expensiveOperation = 0;

        /**
         * note that lock is keyword in C#
         */
        private static readonly object _lock = new object();


        /**
         * One problem is that, once a test case is evaluated, some background tests might still be running.
         * We want to kill them to avoid issue (eg, when evaluating new tests while previous threads
         * are still running).
         */
        private static volatile bool _killSwitch = false;

        static ExecutionTracer(){
            Reset();
        }


        public static void Reset()
        {
            lock (_lock) {
                ObjectiveCoverage.Clear();
                _actionIndex = 0;
                AdditionalInfoList.Clear();
                AdditionalInfoList.Add(new AdditionalInfo());
                _inputVariables = new HashSet<string>();
                _killSwitch = false;
                _expensiveOperation = 0;
            }
        }

        public static bool IsKillSwitch() {
            return _killSwitch;
        }

        public static void SetAction(Action action)
        {
            lock (_lock)
            {
                SetKillSwitch(false);
                _expensiveOperation = 0;
                if (action.GetIndex() != _actionIndex)
                {
                    _actionIndex = action.GetIndex();
                    AdditionalInfoList.Add(new AdditionalInfo());
                }

                if (action.GetInputVariables() != null && action.GetInputVariables().Count != 0)
                {
                    _inputVariables = action.GetInputVariables();
                }
            }
        }

        public static void SetKillSwitch(bool killSwitch)
        {
            ExecutionTracer._killSwitch = killSwitch;
        }
        
        public static IList<AdditionalInfo> ExposeAdditionalInfoList() {
            return AdditionalInfoList;
        }
        
    }
}