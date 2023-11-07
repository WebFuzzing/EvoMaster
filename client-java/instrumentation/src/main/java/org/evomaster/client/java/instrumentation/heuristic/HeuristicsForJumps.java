package org.evomaster.client.java.instrumentation.heuristic;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.objectweb.asm.Opcodes;

/**
 * Class used to handle heuristics (eg kind of branch distance calculations)
 * for bytecode jumps. This will depend on the jump code and the values read
 * on the stack
 */
public class HeuristicsForJumps {

    /**
     * This are values compared against 0, like {@code value < 0}
     *
     * @return
     */
    public static Truthness getForSingleValueJump(int value, int opcode) {

        switch (opcode) {
            case Opcodes.IFEQ: // ie, val == 0
                return getForValueComparison(value, 0, Opcodes.IF_ICMPEQ);

            case Opcodes.IFNE: // ie val != 0  ->  ! (val == 0)
                return getForSingleValueJump(value, Opcodes.IFEQ).invert();

            case Opcodes.IFLT: // ie, val < 0
                return getForValueComparison(value, 0, Opcodes.IF_ICMPLT);

            case Opcodes.IFGE: // ie, val >= 0  -> ! (val < 0)
                return getForSingleValueJump(value, Opcodes.IFLT).invert();

            case Opcodes.IFLE: // ie, val <= 0  ->  0 >= val  -> ! (0 < val)
                return getForValueComparison(0, value, Opcodes.IF_ICMPLT).invert();

            case Opcodes.IFGT: // ie, val > 0  ->  0 < val
                return getForValueComparison(0, value, Opcodes.IF_ICMPLT);

            default:
                throw new IllegalArgumentException("Cannot handle opcode " + opcode);
        }
    }


    public static Truthness getForValueComparison(int firstValue, int secondValue, int opcode) {

        int a = firstValue;
        int b = secondValue;

        switch (opcode) {
            case Opcodes.IF_ICMPEQ: // ie, a == b
            {
                return TruthnessUtils.getEqualityTruthness(a,b);
            }
            case Opcodes.IF_ICMPNE: // ie, a != b
                return getForValueComparison(firstValue, secondValue, Opcodes.IF_ICMPEQ).invert();

            case Opcodes.IF_ICMPLT: // ie, a < b
                return TruthnessUtils.getLessThanTruthness(a, b);

            case Opcodes.IF_ICMPGE: // ie, a >= b  ->  ! (a < b)
                return getForValueComparison(firstValue, secondValue, Opcodes.IF_ICMPLT).invert();

            case Opcodes.IF_ICMPLE: // ie, a <= b  ->  b >= a -> ! (b < a)
                return getForValueComparison(secondValue, firstValue, Opcodes.IF_ICMPLT).invert();

            case Opcodes.IF_ICMPGT: // ie, a > b  ->  b < a
                return getForValueComparison(secondValue, firstValue, Opcodes.IF_ICMPLT);

            default:
                throw new IllegalArgumentException("Cannot handle opcode " + opcode);
        }
    }




    public static Truthness getForObjectComparison(Object first, Object second, int opcode) {

        switch (opcode) {
            case Opcodes.IF_ACMPEQ: // ie, a == b
                return new Truthness(
                        first == second ? 1d : 0d,
                        first != second ? 1d : 0d
                );

            case Opcodes.IF_ACMPNE: // ie, a != b
                return getForObjectComparison(first, second, Opcodes.IF_ACMPEQ).invert();

            default:
                throw new IllegalArgumentException("Cannot handle opcode " + opcode);
        }
    }


    public static Truthness getForNullComparison(Object obj, int opcode) {

        switch (opcode) {

            case Opcodes.IFNULL: // ie, obj == null
                return getForObjectComparison(obj, null, Opcodes.IF_ACMPEQ);

            case Opcodes.IFNONNULL: // ie, obj != null
                return getForObjectComparison(obj, null, Opcodes.IF_ACMPNE);

            default:
                throw new IllegalArgumentException("Cannot handle opcode " + opcode);
        }
    }

}
