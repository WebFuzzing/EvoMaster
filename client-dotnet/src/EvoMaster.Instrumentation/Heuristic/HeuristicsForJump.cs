using System;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation.Heuristic {
    public class HeuristicsForJumps {
        //This are values compared against 0, like {@code value < 0}

        public static Truthness GetForSingleValueJump(int value, Code opcode) {
            switch (opcode) {
                case Code.Brfalse: // ie, val == 0; JB: IFEQ 
                    return GetForValueComparison(value, 0, Code.Beq); //JB: IF_ICMPEQ

                case Code.Brtrue: // ie val != 0  ->  ! (val == 0); JB: IFNE
                    return GetForSingleValueJump(value, Code.Brfalse).Invert(); //JB: IFEQ

                //TODO: uncomment and fix these lines
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


        public static Truthness GetForValueComparison(int firstValue, int secondValue, Code opcode) {
            var a = firstValue;
            var b = secondValue;

            switch (opcode) {
                case Code.Beq: // ie, a == b; JB: IF_ICMPEQ
                    return TruthnessUtils.GetEqualityTruthness(a, b);

                case Code.Bne_Un: // ie, a != b; JB: IF_ICMPNE
                    return GetForValueComparison(firstValue, secondValue, Code.Beq).Invert();

                case Code.Blt: // ie, a < b; JB: IF_ICMPLT
                    return TruthnessUtils.GetLessThanTruthness(a, b);

                case Code.Bge: // ie, a >= b  ->  ! (a < b); JB: IF_ICMPGE
                    return GetForValueComparison(firstValue, secondValue, Code.Blt).Invert();

                case Code.Ble: // ie, a <= b  ->  b >= a -> ! (b < a); JB: IF_ICMPLE
                    return GetForValueComparison(secondValue, firstValue, Code.Blt).Invert();

                case Code.Bgt: // ie, a > b  ->  b < a; JB: IF_ICMPGT
                    return GetForValueComparison(secondValue, firstValue, Code.Blt);

                default:
                    throw new ArgumentException("Cannot handle opcode " + opcode);
            }
        }

        //TODO: uncomment and complete this method
        // public static Truthness GetForObjectComparison(Object first, Object second, int opcode)
        // {
        //     switch (opcode)
        //     {
        //         case OpCodes.IF_ACMPEQ: // ie, a == b
        //             return new Truthness(
        //                 first == second ? 1d : 0d,
        //                 first != second ? 1d : 0d
        //             );
        //
        //         case OpCodes.IF_ACMPNE: // ie, a != b
        //             return GetForObjectComparison(first, second, OpCodes.IF_ACMPEQ).Invert();
        //
        //         default:
        //             throw new ArgumentException("Cannot handle opcode " + opcode);
        //     }
        // }

        //TODO: uncomment and complete this method
        // public static Truthness GetForNullComparison(Object obj, Code opcode)
        // {
        //     switch (opcode)
        //     {
        //         case OpCodes.IFNULL: // ie, obj == null
        //             return GetForObjectComparison(obj, null, OpCodes.IF_ACMPEQ);
        //
        //         case OpCodes.IFNONNULL: // ie, obj != null
        //             return GetForObjectComparison(obj, null, OpCodes.IF_ACMPNE);
        //
        //         default:
        //             throw new ArgumentException("Cannot handle opcode " + opcode);
        //     }
        // }
    }
}