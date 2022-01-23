using System.Diagnostics;
using EvoMaster.Client.Util;
using EvoMaster.Instrumentation.StaticState;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation.Heuristic{
    public class HeuristicsForBooleans{

        /**
         * compute heuristics for single value  
         */
        public static void CompareSingleValueJump(string className, int line, int branchId, int value, Code code){
            if (HeuristicsForJumps.CODES.Contains(code)){
                var t = HeuristicsForJumps.GetForSingleValueJump(value, code);
                ExecutionTracer.UpdateBranchDistance(className, line, branchId, t);
            }else{
                SimpleLogger.Warn("Do not support to compute single value jump with opcode" + code);
            }
        }

        /**
         * compute heuristics for comparison method with opcode
         */
        public static void Compare(string className, int line, int branchId, object firstValue, object secondValue, Code code){

            Truthness t = null;
            
            //TODO need to check with Amid about brfalse/brtrue and `null` case
            
            if (HeuristicsForJumps.CODES.Contains(code)){
                Trace.Assert(firstValue is int);
                Trace.Assert(secondValue is int);
                t = HeuristicsForJumps.GetForValueComparison((int)firstValue, (int)secondValue, code);
            } else if (HeuristicsForNonintegerComparisons.CODES.Contains(code)){
                if (firstValue is double dfvalue && secondValue is double dsvalue){
                    t = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(dfvalue,
                        dsvalue, code);
                } else if (firstValue is float ffvalue && secondValue is float fsvalue){
                    t = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(ffvalue,
                        fsvalue, code);
                } else if (firstValue is long lfvalue && secondValue is long lsvalue){
                    t = HeuristicsForNonintegerComparisons.GetForLongComparison(lfvalue,
                        lsvalue, code);
                } 
            }

            if (t != null){
                ExecutionTracer.UpdateBranchDistance(className, line, branchId, t);
            } else{
                SimpleLogger.Warn("Do not support to compute heuristics for types ("+firstValue.GetType().FullName+","+secondValue.GetType().FullName+")"+ " with opcode" + code);
            }
        }

    }
}