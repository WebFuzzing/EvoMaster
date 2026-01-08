using System;
using System.Collections.Generic;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation.Heuristic {
    
    /**
     * Class used to handle heuristics (eg kind of branch distance calculations)
     * for bytecode jumps. This will depend on the jump code and the values read
     * on the stack
     * */
    public class HeuristicsForJumps {
        
        public static readonly ISet<Code> CODES = new HashSet<Code>(){
            Code.Brfalse, Code.Brfalse_S, Code.Brtrue, Code.Brtrue_S,
            Code.Beq, Code.Beq_S, Code.Bne_Un, Code.Bne_Un_S, 
            Code.Blt, Code.Blt_S, Code.Blt_Un, Code.Blt_Un_S,
            Code.Ble, Code.Ble_S, Code.Ble_Un, Code.Ble_Un_S,
            Code.Bgt, Code.Bgt_S, Code.Bgt_Un, Code.Bgt_Un_S,
            Code.Bge, Code.Bge_S, Code.Bge_Un, Code.Bge_Un_S
        };

        //This are values compared against 0, like {@code value < 0}
        public static Truthness GetForSingleValueJump(long value, Code opcode) {
            switch (opcode) {
                
                //0x39	brfalse <int32 (target)>	Branch to target if value is zero (false).
                //0x39	brnull <int32 (target)>	Branch to target if value is null (alias for brfalse).
                //0x39	brzero <int32 (target)>	Branch to target if value is zero (alias for brfalse).
                //Transfers control to a target instruction if value is false, a null reference (Nothing in Visual Basic), or zero.
                //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.brfalse?view=net-6.0
                case Code.Brfalse:
                    return GetForValueComparison(value, 0, Code.Beq); 
                //0x2C	brfalse.s <int8 (target)>	Branch to target if value is zero (false), short form.
                case Code.Brfalse_S: 
                    return GetForValueComparison(value, 0, Code.Beq_S); 
                
                //0x3A	brtrue <int32 (target)>	Branch to target if value is non-zero (true).
                //0x3A	brinst <int32 (target)>	Branch to target if value is a non-null object reference (alias for brtrue).
                //Transfers control to a target instruction if value is true, not null, or non-zero.
                //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.brtrue?view=net-6.0
                case Code.Brtrue: 
                    return GetForSingleValueJump(value, Code.Brfalse).Invert(); 
                //0x2D	brtrue.s <int8 (target)>	Branch to target if value is non-zero (true), short form.
                //0x2D	brinst.s <int8 (target)>	Branch to target if value is a non-null object reference, short form (alias for brtrue.s).
                case Code.Brtrue_S: 
                    return GetForSingleValueJump(value, Code.Brfalse_S).Invert(); 
                
                // Man: DO NOT find following SingleValueJump for dotnet 
                // case OpCodes.IFLT: // ie, val < 0
                //     return GetForValueComparison(value, 0, Code.Blt); //JB: IF_ICMPLT
                //
                // case OpCodes.IFGE: // ie, val >= 0  -> ! (val < 0)
                //     return GetForSingleValueJump(value, OpCodes.IFLT).Invert();
                //
                // case OpCodes.IFLE: // ie, val <= 0  ->  0 >= val  -> ! (0 < val)
                //     return GetForValueComparison(0, value, Code.Blt).Invert(); //JB: IF_ICMPLT
                //
                // case OpCodes.IFGT: // ie, val > 0  ->  0 < val
                //     return GetForValueComparison(0, value, Code.Blt); //JB: IF_ICMPLT

                default:
                    throw new ArgumentException("Cannot handle opcode " + opcode);
            }
        }


        public static Truthness GetForValueComparison(long firstValue, long secondValue, Code opcode) {
            var a = firstValue;
            var b = secondValue;

            switch (opcode) {
                //0x3B	beq <int32 (target)>	Branch to target if equal.
                //0x2E	beq.s <int8 (target)>	Branch to target if equal, short form.
                //Transfers control to a target instruction if two values are equal.
                //https://docs.microsoft.com/en-us/dotnet/api/system.reflection.emit.opcodes.beq?view=net-6.0
                // a == b
                case Code.Beq: 
                case Code.Beq_S:
                    return TruthnessUtils.GetEqualityTruthness(a, b);

                //0x40	bne.un <int32 (target)>	Branch to target if unequal or unordered.
                //Transfers control to a target instruction when two unsigned integer values or unordered float values are not equal.
                //0x33	bne.un.s <int8 (target)>	Branch to target if unequal or unordered, short form.
                //Transfers control to a target instruction (short form) when two unsigned integer values or unordered float values are not equal.
                // a != b -> !(a==b)
                // Man: there do not exist 'Bne' and 'Bne_S'
                case Code.Bne_Un:  
                    return GetForValueComparison(firstValue, secondValue, Code.Beq).Invert();
                case Code.Bne_Un_S:
                    return GetForValueComparison(firstValue, secondValue, Code.Beq_S).Invert();

                //0x3E	ble <int32 (target)>	Branch to target if less than or equal to.
                //0x44	blt.un <int32 (target)>	Branch to target if less than (unsigned or unordered).
                //0x31	ble.s <int8 (target)>	Branch to target if less than or equal to, short form.
                //0x37	blt.un.s <int8 (target)>	Branch to target if less than (unsigned or unordered), short form.
                // a < b, (TODO: Man: might need to check if we distinguish branch distance for short, unsigned, or unordered)
                case Code.Blt:
                case Code.Blt_Un:
                case Code.Blt_S:
                case Code.Blt_Un_S:
                    return TruthnessUtils.GetLessThanTruthness(a, b);

                //0x3C	bge <int32 (target)>	Branch to target if greater than or equal to.
                //0x41	bge.un <int32 (target)>	Branch to target if greater than or equal to (unsigned or unordered).
                //0x2F	bge.s <int8 (target)>	Branch to target if greater than or equal to, short form.
                //0x34	bge.un.s <int8 (target)>	Branch to target if greater than or equal to (unsigned or unordered), short form.
                // a >= b  ->  ! (a < b)
                case Code.Bge: 
                    return GetForValueComparison(firstValue, secondValue, Code.Blt).Invert();
                case Code.Bge_Un:
                    return GetForValueComparison(firstValue, secondValue, Code.Blt_Un).Invert();
                case Code.Bge_S:
                    return GetForValueComparison(firstValue, secondValue, Code.Blt_Un_S).Invert();
                case Code.Bge_Un_S:
                    return GetForValueComparison(firstValue, secondValue, Code.Blt_S).Invert();

                //0x3E	ble <int32 (target)>	Branch to target if less than or equal to.
                //0x43	ble.un <int32 (target)>	Branch to target if less than or equal to (unsigned or unordered).
                //0x31	ble.s <int8 (target)>	Branch to target if less than or equal to, short form.
                //0x36	ble.un.s <int8 (target)>	Branch to target if less than or equal to (unsigned or unordered), short form.
                // ie, a <= b  ->  b >= a -> ! (b < a)
                case Code.Ble: 
                    return GetForValueComparison(secondValue, firstValue, Code.Blt).Invert();
                case Code.Ble_Un:
                    return GetForValueComparison(secondValue, firstValue, Code.Blt_Un).Invert();
                case Code.Ble_S: 
                    return GetForValueComparison(secondValue, firstValue, Code.Blt_S).Invert();
                case Code.Ble_Un_S:
                    return GetForValueComparison(secondValue, firstValue, Code.Blt_Un_S).Invert();
                
                //0x3D	bgt <int32 (target)>	Branch to target if greater than.
                //0x42	bgt.un <int32 (target)>	Branch to target if greater than (unsigned or unordered).
                //0x30	bgt.s <int8 (target)>	Branch to target if greater than, short form.
                //0x35	bgt.un.s <int8 (target)>	Branch to target if greater than (unsigned or unordered), short form.
                // a > b -> b < a
                case Code.Bgt: 
                    return GetForValueComparison(secondValue, firstValue, Code.Blt);
                case Code.Bgt_Un:
                    return GetForValueComparison(secondValue, firstValue, Code.Blt_Un);
                case Code.Bgt_S:
                    return GetForValueComparison(secondValue, firstValue, Code.Blt_S);
                case Code.Bgt_Un_S:
                    return GetForValueComparison(secondValue, firstValue, Code.Blt_Un_S);

                default:
                    throw new ArgumentException("Cannot handle opcode " + opcode);
            }
        }
        
        /*
            Man: there does not exist branch opcode for handling objects, as checked with the example as below.
            then to handle this, might need replacement method.
         
            var var1 = new { Name = "foo", Value = 42 };
            var var2 = new { Name = "bar", Value = 0 };
            var notNull = var1 != null;
            var equal = var1 == var2;
            
            corresponding bytecode for last two statements are
            
            // [17 13 - 17 40]
            IL_001a: ldloc.0      // var1
            IL_001b: ldnull
            IL_001c: cgt.un
            IL_001e: stloc.2      // notNull

            // [18 13 - 18 38]
            IL_001f: ldloc.0      // var1
            IL_0020: ldloc.1      // var2
            IL_0021: ceq
            IL_0023: stloc.3      // equal
            
         */
        // public static Truthness getForObjectComparison(Object first, Object second, int opcode) {
        //
        //     switch (opcode) {
        //         case Opcodes.IF_ACMPEQ: // ie, a == b
        //             return new Truthness(
        //                 first == second ? 1d : 0d,
        //                 first != second ? 1d : 0d
        //             );
        //
        //         case Opcodes.IF_ACMPNE: // ie, a != b
        //             return getForObjectComparison(first, second, Opcodes.IF_ACMPEQ).invert();
        //
        //         default:
        //             throw new IllegalArgumentException("Cannot handle opcode " + opcode);
        //     }
        // }
        //
        //
        // public static Truthness getForNullComparison(Object obj, int opcode) {
        //
        //     switch (opcode) {
        //
        //         case Opcodes.IFNULL: // ie, obj == null
        //             return getForObjectComparison(obj, null, Opcodes.IF_ACMPEQ);
        //
        //         case Opcodes.IFNONNULL: // ie, obj != null
        //             return getForObjectComparison(obj, null, Opcodes.IF_ACMPNE);
        //
        //         default:
        //             throw new IllegalArgumentException("Cannot handle opcode " + opcode);
        //     }
        // }
    }
}