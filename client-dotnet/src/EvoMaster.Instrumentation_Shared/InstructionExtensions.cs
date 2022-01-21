using System.Collections.Generic;
using Mono.Cecil.Cil;

namespace EvoMaster.Instrumentation_Shared {
    public static class InstructionExtensions {
        /// <summary>
        /// Find any jump to the old instruction and make them refer to the new instruction.
        /// This will prevent the newly inserted instruction become unreachable
        /// An Example of the issue: jump oldIns; ... ; oldIns => jump oldIns; ... ; newIns; oldIns; (newIns is unreachable)
        /// How it will look like after this method: jump newIns; ... ; newIns; oldIns (newIns isn't unreachable anymore)
        /// </summary>
        /// <param name="oldTarget">The old instruction which shouldn't be jumped to anymore</param>
        /// <param name="newTarget">The new instruction to jump to</param>
        /// <param name="instructions">Instructions of the method's body</param>
        public static void UpdateJumpsToTheCurrentInstruction(this Instruction oldTarget, Instruction newTarget,
            IEnumerable<Instruction> instructions) {
            foreach (var t in instructions) {
                if (t.Operand == oldTarget) {
                    t.Operand = newTarget;
                }
            }
        }

        public static bool IsConditionalInstructionWithTwoArgs(this Instruction instruction) {
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

            return conditionalInstructions.Contains(instruction.OpCode) ||
                   (instruction.OpCode==OpCodes.Call && instruction.Operand.ToString().Contains(nameof(BranchInstructionReplacement)));
}

        public static bool IsConditionalJumpWithOneArg(this Instruction instruction) =>
            instruction.OpCode == OpCodes.Brfalse ||
            instruction.OpCode == OpCodes.Brfalse_S ||
            instruction.OpCode == OpCodes.Brtrue ||
            instruction.OpCode == OpCodes.Brtrue_S;

        public static bool IsUnConditionalJumpOrExitInstruction(this Instruction instruction) =>
            IsUnConditionalJump(instruction) || IsExitInstruction(instruction);

        private static bool IsUnConditionalJump(this Instruction instruction) =>
            instruction.OpCode == OpCodes.Br || instruction.OpCode == OpCodes.Br_S;

        private static bool IsExitInstruction(this Instruction instruction) {
            return (instruction.OpCode == OpCodes.Throw) || (instruction.OpCode == OpCodes.Rethrow) ||
                   (instruction.OpCode == OpCodes.Endfinally) ||
                   (instruction.OpCode == OpCodes.Leave) || (instruction.OpCode == OpCodes.Leave_S) ||
                   (instruction.OpCode == OpCodes.Ret);
        }
    }
}