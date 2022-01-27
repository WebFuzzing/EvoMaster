using System;
using System.Collections.Generic;
using System.Diagnostics;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation.Heuristic{
    public class HeuristicsForNonintegerComparisons{
        
        public static readonly ISet<Code> CODES = new HashSet<Code>(){
            //0xFE 0x01	ceq	Push 1 (of type int32) if value1 equals value2, else push 0.
            //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.ceq?view=net-6.0
            Code.Ceq,
            //0xFE 0x04	clt	Push 1 (of type int32) if value1 lower than value2, else push 0.
            //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.clt?view=net-6.0
            Code.Clt, 
            //0xFE 0x05	clt.un	Push 1 (of type int32) if value1 lower than value2, unsigned or unordered, else push 0.
            //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.clt_un?view=net-6.0
            Code.Clt_Un,
            //cgt	Push 1 (of type int32) if value1 greater that value2, else push 0.
            //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.cgt?view=net-6.0
            Code.Cgt,
            //cgt.un	Push 1 (of type int32) if value1 greater that value2, unsigned or unordered, else push 0.
            //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.cgt_un?view=net-6.0
            Code.Cgt_Un
        };

        public static readonly ISet<Code> UNSIGNED = new HashSet<Code>(){
            Code.Clt_Un, Code.Cgt_Un
        };

        public static Truthness GetForFloatAndDoubleComparison(double firstValue, double secondValue, Code opcode){
            
            switch(opcode){
                case Code.Ceq: 
                    return TruthnessUtils.GetEqualityTruthness(firstValue, secondValue);
                case Code.Clt:
                case Code.Clt_Un:
                    return TruthnessUtils.GetLessThanTruthness(firstValue, secondValue);
                case Code.Cgt:
                case Code.Cgt_Un:
                    return TruthnessUtils.GetLessThanTruthness(secondValue, firstValue);
                
                default:
                    throw new ArgumentException("Cannot handle opcode " + opcode+ " for double or float");
            }
        }
        
        public static Truthness GetForLongComparison(long firstValue, long secondValue, Code opcode){
            
            switch(opcode){
                case Code.Ceq: 
                    return TruthnessUtils.GetEqualityTruthness(firstValue, secondValue);
                case Code.Clt:
                case Code.Clt_Un:
                    return TruthnessUtils.GetLessThanTruthness(firstValue, secondValue);
                case Code.Cgt:
                case Code.Cgt_Un:
                    return TruthnessUtils.GetLessThanTruthness(secondValue, firstValue);
                
                default:
                    throw new ArgumentException("Cannot handle opcode " + opcode + " for long");
            }
        }
        
        public static Truthness GetForULongComparison(ulong firstValue, ulong secondValue, Code opcode){
            
            switch(opcode){
                case Code.Clt_Un:
                    return TruthnessUtils.GetLessThanTruthness(firstValue, secondValue);
                case Code.Cgt_Un:
                    return TruthnessUtils.GetLessThanTruthness(secondValue, firstValue);
                
                default:
                    throw new ArgumentException("Cannot handle opcode " + opcode + " for long");
            }
        }
    }
}