using System.Collections.Generic;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation_Shared {
    public static class InstructionExtensions {
        public static void UpdateJumpsToTheCurrentInstruction(this Instruction oldTarget, Instruction newTarget,
            IEnumerable<Instruction> instructions) {
            foreach (var t in instructions) {
                if (t.Operand == oldTarget) {
                    t.Operand = newTarget;
                }
            }
        }

        public static bool IsConditionalJumpWithTwoArgs(this Instruction instruction) {
            var conditionalInstructions = new List<OpCode> {
                OpCodes.Ceq,
                OpCodes.Clt,
                OpCodes.Clt_Un,
                OpCodes.Cgt,
                OpCodes.Cgt_Un,
                OpCodes.Bgt,
                OpCodes.Bgt_S,
                OpCodes.Bgt_Un,
                OpCodes.Bgt_Un_S,
                OpCodes.Beq,
                OpCodes.Beq_S,
                OpCodes.Bge,
                OpCodes.Bge_S,
                OpCodes.Bge_Un,
                OpCodes.Bge_Un_S,
                OpCodes.Ble,
                OpCodes.Ble_S,
                OpCodes.Ble_Un,
                OpCodes.Ble_Un_S,
                OpCodes.Blt,
                OpCodes.Blt_S,
                OpCodes.Blt_Un,
                OpCodes.Blt_Un_S,
                OpCodes.Bne_Un,
                OpCodes.Bne_Un_S
            };

            return conditionalInstructions.Contains(instruction.OpCode);
        }

        public static bool IsConditionalJumpWithOneArg(this Instruction instruction) =>
            instruction.OpCode == OpCodes.Brfalse ||
            instruction.OpCode == OpCodes.Brfalse_S ||
            instruction.OpCode == OpCodes.Brtrue ||
            instruction.OpCode == OpCodes.Brtrue_S;

        public static bool IsJumpOrExitInstruction(this Instruction instruction) =>
            IsJumpInstruction(instruction) || IsExitInstruction(instruction);

        //to detect instructions which jump to another instruction
        private static bool IsJumpInstruction(this Instruction instruction) {
            return (instruction.OpCode.ToString().ToLower()[0].Equals('b') && instruction.OpCode != OpCodes.Break &&
                    instruction.OpCode != OpCodes.Box) ||
                   (instruction.OpCode == OpCodes.Ceq) ||
                   (instruction.OpCode == OpCodes.Cgt) ||
                   (instruction.OpCode == OpCodes.Cgt_Un) ||
                   (instruction.OpCode == OpCodes.Clt) ||
                   (instruction.OpCode == OpCodes.Clt_Un);
        }

        private static bool IsExitInstruction(this Instruction instruction) {
            return (instruction.OpCode == OpCodes.Throw) ||
                   (instruction.OpCode == OpCodes.Rethrow) || (instruction.OpCode == OpCodes.Endfinally) ||
                   (instruction.OpCode == OpCodes.Leave) ||
                   (instruction.OpCode == OpCodes.Leave_S);
        }
    }
}