using System;
using System.Diagnostics;
using EvoMaster.Client.Util.Extensions;
using EvoMaster.Instrumentation.Heuristic;
using EvoMaster.Instrumentation.StaticState;
using EvoMaster.Instrumentation_Shared;

namespace EvoMaster.Instrumentation.Coverage.MethodReplacement{
    
    public class StringClassReplacement{
        public static bool EqualsObject(string caller, object anObject, string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);

            string left = caller;
            string right = anObject == null ? null : anObject.ToString();
            ExecutionTracer.HandleTaintForStringEquals(left, right, StringComparison.Ordinal);

            //not important if NPE
            bool result = caller.Equals(anObject);

            if (idTemplate == null){
                return result;
            }

            Truthness t;

            if (result){
                t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
            } else{
                if (!(anObject is String)){
                    t = new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1d);
                } else{
                    double baseS = DistanceHelper.H_NOT_NULL;
                    double distance = DistanceHelper.GetLeftAlignmentDistance(caller, anObject.ToString());
                    double h = DistanceHelper.HeuristicFromScaledDistanceWithBase(baseS, distance);
                    t = new Truthness(h, 1d);
                }
            }

            ExecutionTracer.ExecutedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

            return result;
        }

        public static bool Equals(string caller, string anotherString, string idTemplate){
            return Equals(caller, anotherString, StringComparison.Ordinal, idTemplate);
        }

        public static bool Equals(string caller, string anotherString, StringComparison comparisonType,
            string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);

            //TODO update ignore case (from java)
            //TODO handle various comparison type
            ExecutionTracer.HandleTaintForStringEquals(caller, anotherString, comparisonType);

            //not important if NPE
            bool result = caller.Equals(anotherString, comparisonType);


            if (idTemplate == null){
                return result;
            }

            if (anotherString == null){
                ExecutionTracer.ExecutedReplacedMethod(idTemplate, ReplacementType.BOOLEAN,
                    new Truthness(DistanceHelper.H_REACHED_BUT_NULL, 1));
                return false;
            }

            Truthness t;

            if (result){
                t = new Truthness(1d, DistanceHelper.H_NOT_NULL);
            } else{
                double baseS = DistanceHelper.H_NOT_NULL;
                double distance = DistanceHelper.GetLeftAlignmentDistance(
                    caller,
                    anotherString, comparisonType);
                double h = DistanceHelper.HeuristicFromScaledDistanceWithBase(baseS, distance);
                t = new Truthness(h, 1d);
            }

            ExecutionTracer.ExecutedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

            return result;
        }


        private static bool StartsOrEndWith(string caller, string prefix, StringComparison comparisonType, bool doStartWith ,int toffset,
            string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);
            
            bool result;
            if (doStartWith){
                result = caller.StartsWith(prefix, comparisonType);
            } else{
                result = caller.EndsWith(prefix, comparisonType);
            }
            

            if (idTemplate == null){
                return result;
            }

            int pl = prefix.Length;

            /*
                The penalty when there is a mismatch of lengths/offset
                should be at least pl, as should be always worse than
                when doing "equals" comparisons.
                Furthermore, need to add extra penalty in case string is
                shorter than prefix
             */
            int penalty = pl;
            if (caller.Length < pl){
                penalty += (pl - caller.Length);
            }

            Truthness t;

            if (toffset < 0){
                long dist = (-toffset + penalty) * Char.MaxValue;
                t = new Truthness(1d / (1d + dist), 1d);
            } else if (toffset > caller.Length - pl){
                long dist = (toffset + penalty) * Char.MaxValue;
                t = new Truthness(1d / (1d + dist), 1d);
            } else{
                int len = Math.Min(prefix.Length, caller.Length);
                String sub = caller.Substring(toffset, Math.Min(toffset + len, caller.Length)-toffset);
                return Equals(sub, prefix, comparisonType, idTemplate);
            }

            ExecutionTracer.ExecutedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
            return result;
        }

        public static bool StartsWith(string caller, string prefix, StringComparison comparisonType, string idTemplate){
            return StartsOrEndWith(caller, prefix, comparisonType, true, 0, idTemplate);
        }

        public static bool StartsWith(string caller, string prefix, string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);

            return StartsOrEndWith(caller, prefix, StringComparison.Ordinal, true ,0, idTemplate);
        }

        public static bool EndsWith(string caller, string suffix, string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);

            // handle null suffix here to make exception consistent 
            var offset = suffix == null ? -1 : caller.Length - suffix.Length;
            return StartsOrEndWith(caller, suffix, StringComparison.Ordinal, false, offset, idTemplate);
        }

        public static bool EndsWith(string caller, string suffix, StringComparison comparisonType, string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);
            
            // handle null suffix here to make exception consistent 
            var offset = suffix == null ? -1 : caller.Length - suffix.Length;
            return StartsOrEndWith(caller, suffix, comparisonType, false, offset, idTemplate);
        }


        // public static bool IsNullOrEmpty(string caller, string idTemplate) {
        //     ObjectExtensions.RequireNonNull<string>(caller);
        //
        //     if (idTemplate == null) {
        //         return caller.Is;
        //     }
        //
        //     int len = caller.Length;
        //     Truthness t = TruthnessUtils.GetTruthnessToEmpty(len);
        //
        //
        //     ExecutionTracer.ExecutedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        //     return caller.isEmpty();
        // }

        // public static bool contentEquals(String caller, CharSequence cs, String idTemplate) {
        //     if (cs == null) {
        //         return caller.contentEquals(cs);
        //     } else {
        //         return equals(caller, cs.toString(), idTemplate);
        //     }
        // }
        //
        //
        // public static bool contentEquals(string caller, StringBuffer sb, String idTemplate) {
        //     return equals(caller, sb.toString(), idTemplate);
        // }

        public static bool Contains(string caller, string s, string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);
            return Contains(caller, s, StringComparison.Ordinal, idTemplate);
        }

        public static bool Contains(string caller, string s, StringComparison comparisonType, string idTemplate){
            ObjectExtensions.RequireNonNull<string>(caller);

            bool result = caller.Contains(s, comparisonType);

            if (idTemplate == null){
                return result;
            }

            string k = s;
            if (caller.Length <= k.Length){
                return Equals(caller, k, comparisonType,idTemplate);
            }

            Truthness t;

            if (result){
                t = new Truthness(1, DistanceHelper.H_NOT_NULL);
            } else{
                Trace.Assert(caller.Length > k.Length);
                long best = long.MaxValue;

                for (int i = 0; i < (caller.Length - k.Length) + 1; i++){
                    // substring in dotnet is specified with startIndex and length
                    string sub = caller.Substring(i, k.Length);
                    // TODO comparisonType
                    long h = DistanceHelper.GetLeftAlignmentDistance(sub, k, comparisonType);
                    if (h < best){
                        best = h;
                    }
                }

                t = new Truthness(1d / (1d + best), 1);
            }

            ExecutionTracer.ExecutedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
            return result;
        }

        // public static bool matches(String caller, String regex, String idTemplate) {
        //     ObjectExtensions.RequireNonNull<string>(caller);
        //     
        //     if (regex == null) {
        //         // signals a NPE
        //         return caller.matches(regex);
        //     } else {
        //         return PatternMatchingHelper.matches(regex, caller, idTemplate);
        //     }
        // }
    }
}