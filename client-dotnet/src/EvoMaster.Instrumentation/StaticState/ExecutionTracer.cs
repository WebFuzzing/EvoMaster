using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using EvoMaster.Instrumentation.Heuristic;
using EvoMaster.Instrumentation_Shared;

namespace EvoMaster.Instrumentation.StaticState{
    public static class ExecutionTracer{
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


        /*
        A test case can be composed by 1 or more actions, eg HTTP calls.
        When we get the best distance for a testing target, we might
        also want to know which action in the test led to it.
        */
        private static int _actionIndex;

        /*
        A set of possible values used in the tests, needed for some kinds
        of taint analyses
        */
        private static ISet<string> _inputVariables = new HashSet<string>();

        /*
        Besides code coverage, there might be other events that we want to
        keep track during test execution.
        We keep track of it separately for each action
        */
        private static readonly IList<AdditionalInfo> AdditionalInfoList = new List<AdditionalInfo>();

        /*
        Keep track of expensive operations. Might want to skip doing them if too many.
        This should be re-set for each action
        */
        private static int _expensiveOperation;

        private static readonly object Lock = new object();


        /*
         One problem is that, once a test case is evaluated, some background tests might still be running.
         We want to kill them to avoid issue (eg, when evaluating new tests while previous threads are still running).
         */
        private static volatile bool _killSwitch;

        static ExecutionTracer(){
            Reset();
        }


        public static void Reset(){
            lock (Lock){
                ObjectiveCoverage.Clear();
                _actionIndex = 0;
                AdditionalInfoList.Clear();
                AdditionalInfoList.Add(new AdditionalInfo());
                _inputVariables = new HashSet<string>();
                _killSwitch = false;
                _expensiveOperation = 0;
            }
        }

        public static bool IsKillSwitch(){
            return _killSwitch;
        }

        public static void SetAction(Action action){
            lock (Lock){
                SetKillSwitch(false);
                _expensiveOperation = 0;
                if (action.GetIndex() != _actionIndex){
                    _actionIndex = action.GetIndex();
                    AdditionalInfoList.Add(new AdditionalInfo());
                }

                if (action.GetInputVariables() != null && action.GetInputVariables().Count != 0){
                    _inputVariables = action.GetInputVariables();
                }
            }
        }

        public static void SetKillSwitch(bool killSwitch){
            _killSwitch = killSwitch;
        }


        /// <summary>
        /// This could be based on static info of the input (eg, according to a precise name convention given
        /// by TaintInputName), or dynamic info given directly by the test itself (eg, the test at action can
        /// register a list of values to check for)
        /// </summary>
        /// <param name="input"></param>
        /// <returns>bool</returns>
        public static bool IsTaintInput(string input){
            return TaintInputName.IsTaintInput(input) || _inputVariables.Contains(input);
        }

        public static TaintType GetTaintType(string input){
            if (input == null){
                return TaintType.NONE;
            }

            if (IsTaintInput(input)){
                return TaintType.FULL_MATCH;
            }

            if (TaintInputName.IncludesTaintInput(input)
                || _inputVariables.ToList().Any(input.Contains)){
                return TaintType.PARTIAL_MATCH;
            }

            return TaintType.NONE;
        }

        public static IEnumerable<AdditionalInfo> ExposeAdditionalInfoList(){
            return AdditionalInfoList;
        }


        ///<summary>Report on the fact that a given line has been executed.</summary>
        // public static void ExecutedLine(string className, string methodName, string descriptor, int line)
        // {
        //     //This is done to prevent the SUT keep on executing code after a test case is evaluated
        //     if (IsKillSwitch())
        //     {
        //         //TODO
        //         // var initClass = Arrays.stream(Thread.CurrentThread..getStackTrace())
        //         //     .anyMatch(e -> e.getMethodName().equals("<clinit>"));
        //
        //         /*
        //             must NOT stop the initialization of a class, otherwise the SUT will be left in an
        //             inconsistent state in the following calls
        //          */
        //
        //         // if (!initClass)
        //         // {
        //         //     throw new KillSwitchException();
        //         // }
        //     }
        //
        //     //TODO
        //     //for targets to cover
        //     var lineId = ObjectiveNaming.LineObjectiveName(className, line);
        //     var classId = ObjectiveNaming.ClassObjectiveName(className);
        //     UpdateObjective(lineId, 1d);
        //     UpdateObjective(classId, 1d);
        //
        //     //to calculate last executed line
        //     var lastLine = className + "_" + line + "_" + methodName;
        //     var lastMethod = className + "_" + methodName + "_" + descriptor;
        //     MarkLastExecutedStatement(statementId);
        // }
        public static void MarkLastExecutedStatement(string statementId, string lastMethod){
            GetCurrentAdditionalInfo().PushLastExecutedStatement(statementId, lastMethod);
        }

        ///<returns>the number of objectives that have been encountered during the test execution</returns>
        public static int GetNumberOfObjectives() => ObjectiveCoverage.Count;

        public static int GetNumberOfObjectives(string prefix) =>
            ObjectiveCoverage.Count(e => prefix == null || e.Key.StartsWith(prefix));

        public static IDictionary<string, TargetInfo> GetInternalReferenceToObjectiveCoverage() => ObjectiveCoverage;

        public static void EnteringStatement(string className, string method, int lineNo, int index){
            var lineId = ObjectiveNaming.LineObjectiveName(className, lineNo);
            var classId = ObjectiveNaming.ClassObjectiveName(className);
            var statementId = ObjectiveNaming.StatementObjectiveName(className, lineNo, index);

            UpdateObjective(lineId, 1);
            UpdateObjective(classId, 1);
            UpdateObjective(statementId, 0.5);

            MarkLastExecutedStatement(statementId, method);
        }

        public static void CompletedStatement(string className, string method, int lineNo, int index){
            var statementId = ObjectiveNaming.StatementObjectiveName(className, lineNo, index);

            UpdateObjective(statementId, 1);

            MarkLastExecutedStatement(statementId, method);

            //HeuristicsForBooleans.clearLastEvaluation();
        }

        private static void UpdateObjective(string id, double value){
            if (value < 0d || value > 1d){
                throw new ArgumentException("Invalid value " + value + " out of range [0,1]");
            }

            //In the same execution, a target could be reached several times, so we should keep track of the best value found so far
            lock (Lock){
                if (ObjectiveCoverage.ContainsKey(id)){
                    var previous = ObjectiveCoverage[id].Value;
                    if (value > previous){
                        ObjectiveCoverage[id] = new TargetInfo(null, id, value, _actionIndex);
                    }
                } else{
                    ObjectiveCoverage.Add(id, new TargetInfo(null, id, value, _actionIndex));
                }
            }

            ObjectiveRecorder.Update(id, value);
        }

        /**
         * get fitness value of target with given id
         */
        public static double GetValue(string id){
            ObjectiveCoverage.TryGetValue(id, out var value);

            if (value is{Value:{ }}) return value.Value.Value;

            return 0;
        }

        // public static void ExecutedNumericComparison(string idTemplate, double lt, double eq, double gt) {
        //
        //     UpdateObjective(ObjectiveNaming.NumericComparisonObjectiveName(idTemplate, -1), lt);
        //     UpdateObjective(ObjectiveNaming.NumericComparisonObjectiveName(idTemplate, 0), eq);
        //     UpdateObjective(ObjectiveNaming.NumericComparisonObjectiveName(idTemplate, +1), gt);
        // }

        public static void UpdateBranchDistance(string className, int line, int branchId, string opCode, Truthness t){
            var forThen = ObjectiveNaming.BranchObjectiveName(className, line, branchId, opCode, true);
            var forElse = ObjectiveNaming.BranchObjectiveName(className, line, branchId, opCode, false);

            UpdateObjective(forThen, t.GetOfTrue());
            UpdateObjective(forElse, t.GetOfFalse());
        }

        /**
         * return number of objectives which are not covered but reached
         */
        public static int GetNumberOfNonCoveredObjectives(string prefix){
            return ObjectiveCoverage.Values.Count(x => x.DescriptiveId.StartsWith(prefix) && x.Value < 1);
        }

        /**
         * return a set of description of targets which are not covered
         */
        public static ISet<string> GetNonCoveredObjectives(string prefix){
            return ObjectiveCoverage
                .Where(e => prefix == null || e.Key.StartsWith(prefix))
                .Where(e => e.Value.Value < 1)
                .Select(e => e.Key).ToHashSet();
        }

        private static AdditionalInfo GetCurrentAdditionalInfo(){
            lock (Lock){
                return AdditionalInfoList[_actionIndex];
            }
        }
        

        public static void AddStringSpecialization(string taintInputName, StringSpecializationInfo info){
            GetCurrentAdditionalInfo().AddSpecialization(taintInputName, info);
        }

        public static void HandleTaintForStringEquals(string left, string right, StringComparison comparisonType){
            if (left == null || right == null){
                //nothing to do?
                return;
            }

            bool taintedLeft = IsTaintInput(left);
            bool taintedRight = IsTaintInput(right);

            var ignoreCase = comparisonType == StringComparison.OrdinalIgnoreCase 
                             || comparisonType == StringComparison.CurrentCultureIgnoreCase 
                             || comparisonType == StringComparison.InvariantCultureIgnoreCase;
            
            if (taintedLeft && taintedRight){
                // if (ignoreCase ? left.Equals(right, StringComparison.OrdinalIgnoreCase) : left.Equals(right)){
                //     //tainted, but compared to itself. so shouldn't matter
                //     return;
                // }
                if (left.Equals(right, comparisonType)){
                    return;
                }

                /*
                    We consider binding only for base versions of taint, ie we ignore
                    the special strings provided by the Core, as it would lead to nasty
                    side-effects
                 */
                if (!TaintInputName.IsTaintInput(left) || !TaintInputName.IsTaintInput(right)){
                    return;
                }

                //TODO could have EQUAL_IGNORE_CASE
                string id = left + "___" + right;
                AddStringSpecialization(left, new StringSpecializationInfo(StringSpecialization.EQUAL, id));
                AddStringSpecialization(right, new StringSpecializationInfo(StringSpecialization.EQUAL, id));
                return;
            }

            /*
              TODO need to consider string comparison
              https://docs.microsoft.com/en-us/dotnet/api/system.stringcomparison?view=netcore-3.1
             */
            StringSpecialization type = ignoreCase
                ? StringSpecialization.CONSTANT_IGNORE_CASE
                : StringSpecialization.CONSTANT;


            if (taintedLeft || taintedRight){
                if (taintedLeft){
                    AddStringSpecialization(left, new StringSpecializationInfo(type, right));
                } else{
                    AddStringSpecialization(right, new StringSpecializationInfo(type, left));
                }
            }
        }
        
        public static void ExecutedReplacedMethod(string idTemplate, ReplacementType type, Truthness t) {

            /*
                Considering the fact that the method has been executed, and so reached, cannot happen
                that any of the heuristic values is 0
             */
            Trace.Assert(t.GetOfTrue() != 0 && t.GetOfFalse() !=0);
            
            string idTrue = ObjectiveNaming.MethodReplacementObjectiveName(idTemplate, true, type);
            string idFalse = ObjectiveNaming.MethodReplacementObjectiveName(idTemplate, false, type);

            UpdateObjective(idTrue, t.GetOfTrue());
            UpdateObjective(idFalse, t.GetOfFalse());
        }
    }
}