using System;
using System.Diagnostics;
using EvoMaster.Client.Util;
using EvoMaster.Instrumentation.StaticState;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation.Heuristic{
    public class HeuristicsForBooleans{

        /**
         * compute distance for jump with one arguments
         */
        public static void ComputeDistanceForSingleJump(string className, int line, int branchId, long value, string codeString){
            Truthness t = null;
            var ok = Enum.TryParse(typeof(Code), codeString, true, out var opcode);

            if (!ok || !(opcode is Code code)){
                SimpleLogger.Warn("cannot parse "+codeString + "as Code");
                return;
            }

            if (HeuristicsForJumps.CODES.Contains(code)){
                t = HeuristicsForJumps.GetForSingleValueJump(value, code);
            }

            if (t != null){
                ExecutionTracer.UpdateBranchDistance(className, line, branchId, codeString, t);
            } else{
                SimpleLogger.Warn("Do not support to compute heuristics for types ("+value.GetType().FullName+")"+ " with opcode" + code);
            }
        }
        
        /**
         * compute distance for branch with two arguments
         */
        public static void ComputeDistanceForTwoArgs(string className, int line, int branchId, object firstValue, object secondValue, string codeString){
            
            Truthness t = null;
            var ok = Enum.TryParse(typeof(Code), codeString, true, out var opcode);

            if (!ok || !(opcode is Code code)){
                SimpleLogger.Warn("cannot parse "+codeString + "as Code");
                return;
            }
            
            if (HeuristicsForNonintegerComparisons.CODES.Contains(code)){
                if (firstValue is double dfvalue && secondValue is double dsvalue){
                    t = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(dfvalue,
                        dsvalue, code);
                } else if (firstValue is float ffvalue && secondValue is float fsvalue){
                    t = HeuristicsForNonintegerComparisons.GetForFloatAndDoubleComparison(ffvalue,
                        fsvalue, code);
                } else if (firstValue is long lfvalue && secondValue is long lsvalue){
                    t = HeuristicsForNonintegerComparisons.GetForLongComparison(lfvalue,
                        lsvalue, code);
                } else if (firstValue is int ifvalue && secondValue is int isvalue){
                    t = HeuristicsForNonintegerComparisons.GetForLongComparison(ifvalue,
                        isvalue, code);
                }
            }

            if (t != null){
                ExecutionTracer.UpdateBranchDistance(className, line, branchId, codeString, t);
            } else{
                SimpleLogger.Warn("Do not support to compute heuristics for types ("+firstValue.GetType().FullName+","+secondValue.GetType().FullName+")"+ " with opcode" + code);
            }
        }
        
    }
}