using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using EvoMaster.Client.Util;
using EvoMaster.Client.Util.Extensions;

namespace EvoMaster.Instrumentation_Shared {
    public static class ObjectiveNaming {
        /**
     * Prefix identifier for class coverage objectives.
     * A class is "covered" if at least one of its lines is executed.
     */
        private const string Class = "Class";

        /**
     * Prefix identifier for line coverage objectives
     */
        private const string Line = "Line";

        /**
     * Prefix identifier for statement coverage objectives
     */
        private const string Statement = "Statement";

        /**
     * Prefix identifier for branch coverage objectives
     */
        public const string Branch = "Branch";

        /**
     * Tag used in a branch id to specify it is for the "true"/then branch
     */
        public const string TrueBranch = "_trueBranch";

        /**
     * Tag used in a branch id to specify it is for the "false"/else branch
     */
        public const string FalseBranch = "_falseBranch";

        /**
     * Prefix identifier for MethodReplacement objectives, where we want
     * to cover both possible outcomes, eg true and false
     */
        private const string MethodReplacement = "MethodReplacement";


        /**
     * Prefix identifier for objectives related to calling methods without exceptions
     */
        private const string SuccessCall = "Success_Call";

        /**
     * Numeric comparison for non-ints, ie long, double and float
     */
        private const string NumericComparison = "NumericComparison";

        /*
            WARNING: originally where interning all strings, to save memory.
            but that looks like it was having quite a performance hit on LanguageTool.
    
            For the most used methods, added some memoization, as those methods look like
            among the most expensive/used during performance profiling.
    
            One problem though is due to multi-params for indexing... we could use unique global
            ids already at instrumentation time (to force a single lookup, instead of a chain
            of maps), but that would require a major refactoring.
         */

        //TODO: capacity 10_000
        private static readonly IDictionary<string, string> CacheClass = new ConcurrentDictionary<string, string>();

        public static string ClassObjectiveName(string className) {
            return CacheClass.ComputeIfAbsent(className, c => $"{Class}_{ClassName.Get(c).GetFullNameWithDots()}");
            //string name = CLASS + "_" + ClassName.get(className).getFullNameWithDots();
            //return name;//.intern();
        }

        public static string NumericComparisonObjectiveName(string id, int res) {
            var name = $"{NumericComparison}_{id}_{(res == 0 ? "EQ" : (res < 0 ? "LT" : "GT"))}";
            return name; //.intern();
        }

        //TODO: capacity 10_000
        private static readonly IDictionary<string, IDictionary<int, string>> LineCache =
            new ConcurrentDictionary<string, IDictionary<int, string>>();

        public static string LineObjectiveName(string className, int line) {
            var map =
                LineCache.ComputeIfAbsent(className,
                    c => new ConcurrentDictionary<int, string>()); //TODO: capacity 1000
            return map.ComputeIfAbsent(line,
                l => $"{Line}_at_{ClassName.Get(className).GetFullNameWithDots()}_{PadNumber(line)}");
        }

        public static string StatementObjectiveName(string className, int line, int index) =>
            $"{Statement}_{className}_{PadNumber(line)}_{index}";

        //TODO: capacity 10_000
        private static readonly IDictionary<string, IDictionary<int, IDictionary<int, string>>> CacheSuccessCall =
            new ConcurrentDictionary<string, IDictionary<int, IDictionary<int, string>>>();

        public static string SuccessCallObjectiveName(string className, int line, int index) {
            var m0 =
                CacheSuccessCall.ComputeIfAbsent(className,
                    c => new ConcurrentDictionary<int, IDictionary<int, string>>()); //TODO: capacity 10_000
            var
                m1 = m0.ComputeIfAbsent(line, l => new ConcurrentDictionary<int, string>()); //TODO: capacity 10
            return m1.ComputeIfAbsent(index, i =>
                $"{SuccessCall}_at_{ClassName.Get(className).GetFullNameWithDots()}_{PadNumber(line)}_{index}");
        }

        public static string MethodReplacementObjectiveNameTemplate(string className, int line, int index) {
            var name =
                $"{MethodReplacement}_at_{ClassName.Get(className).GetFullNameWithDots()}_{PadNumber(line)}_{index}";
            return name; //.intern();
        }

        public static string MethodReplacementObjectiveNameForBoolean(string template, bool result){
            return MethodReplacementObjectiveName(template, result, ReplacementType.BOOLEAN);
        }
        
        public static string MethodReplacementObjectiveName(string template, bool result, ReplacementType type) {
            if (template == null || !template.StartsWith(MethodReplacement)) {
                throw new ArgumentException($"Invalid template for bool method replacement: {template}");
            }

            var name = $"{template}_{type}_{result}";
            return name; //.intern();
        }


        private static readonly
            ConcurrentDictionary<string,
                IDictionary<int, IDictionary<int, IDictionary<string, IDictionary<bool, string>>>>>
            BranchCache = new
                ConcurrentDictionary<string,
                    IDictionary<int, IDictionary<int, IDictionary<string, IDictionary<bool, string>>>>>(); //TODO: capacity 10_000

        private static readonly ConcurrentHashSet<string> BranchCacheSet = new ConcurrentHashSet<string>();

        /**
         * in .net, a position at a line might result in more than one branch related opCodes,
         * eg, > could have cgt and brfalse, then in order to describe branch targets, we include opCode to
         * define its description.
         * note that opCode is a postfix used in branch description
         */
        public static string BranchObjectiveName(string className, int line, int branchId, string opCode,
            bool thenBranch) {
            // return thenBranch
            //     ? $"{Branch}_at_{ClassName.Get(className).GetFullNameWithDots()}_at_line_{PadNumber(line)}_position_{branchId}_opcode_{opCode}_TrueBranch"
            //     : $"{Branch}_at_{ClassName.Get(className).GetFullNameWithDots()}_at_line_{PadNumber(line)}_position_{branchId}_opcode_{opCode}_FalseBranch";

            var m0 =
                BranchCache.ComputeIfAbsent(className,
                    k => new ConcurrentDictionary<int,
                        IDictionary<int, IDictionary<string, IDictionary<bool, string>>>>()); //TODO: capacity 10_000
            var m1 = m0.ComputeIfAbsent(line,
                k => new ConcurrentDictionary<int, IDictionary<string, IDictionary<bool, string>>>()); //TODO: capacity 10
            var
                m2 = m1.ComputeIfAbsent(branchId, k => new ConcurrentDictionary<string, IDictionary<bool, string>>()); //TODO: capacity 2

            var m3 = m2.ComputeIfAbsent(opCode, k => new ConcurrentDictionary<bool, string>());
            
            return m3.ComputeIfAbsent(thenBranch, k => {
                var name =
                    $"{Branch}_at_{ClassName.Get(className).GetFullNameWithDots()}_at_line_{PadNumber(line)}_position_{branchId}_opcode_{opCode}";
                if (thenBranch) {
                    name += TrueBranch;
                }
                else {
                    name += FalseBranch;
                }

                return name;
            });
        }

        private static string PadNumber(int val) {
            if (val < 0) {
                throw new ArgumentException("Negative number to pad");
            }

            if (val < 10) {
                return $"0000{val}";
            }

            if (val < 100) {
                return $"000{val}";
            }

            if (val < 1_000) {
                return $"00{val}";
            }

            if (val < 10_000) {
                return $"0{val}";
            }
            else {
                return $"{val}";
            }
        }
    }
}