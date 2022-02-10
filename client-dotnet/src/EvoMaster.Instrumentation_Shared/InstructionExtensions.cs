using System;
using System.Collections.Generic;
using System.Linq;
using EvoMaster.Instrumentation_Shared.Exceptions;
using Mono.Cecil;
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

        public static bool IsConditionalInstructionWithTwoArgs(this Instruction instruction) =>
            instruction.IsCompareWithTwoArgs() || instruction.IsBranchWithTwoArgs();

        public static bool IsCompareWithTwoArgs(this Instruction instruction) {
            var instructions = new List<OpCode> {
                OpCodes.Ceq,
                OpCodes.Clt,
                OpCodes.Clt_Un,
                OpCodes.Cgt,
                OpCodes.Cgt_Un
            };
            return instructions.Contains(instruction.OpCode);
        }

        public static bool IsBranchWithTwoArgs(this Instruction instruction) {
            var instructions = new List<OpCode> {
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

            return instructions.Contains(instruction.OpCode);
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

        public static bool IsLoadArgument(this Instruction instruction) {
            return instruction.OpCode.ToString().Contains("ldarg", StringComparison.OrdinalIgnoreCase);
        }

        public static int GetArgumentIndex(this Instruction ldArgInstruction,
            ICollection<ParameterDefinition> methodParams) {
            var opcode = ldArgInstruction.OpCode.ToString().ToLower();
            var arr = opcode.Split('_');
            if (arr.Length > 1) {
                return Convert.ToInt32(arr[1]);
            }

            if (methodParams.Count == 1) return 0;

            return methodParams.First(x =>
                x.Name.Equals(ldArgInstruction.Operand.ToString(), StringComparison.OrdinalIgnoreCase)).Index;
        }

        public static Type DetectType(this Instruction instruction, ICollection<ParameterDefinition> methodParams,
            IReadOnlyDictionary<string, string> localVarTypes) {
            var opCode = instruction.OpCode.ToString();

            if (instruction.Operand is FieldDefinition fieldDefinition) {
                return GetCSharpTypeByName(fieldDefinition.FieldType.Name);
            }

            if (instruction.Operand is VariableDefinition variableDefinition) {
                return GetCSharpTypeByName(variableDefinition.VariableType.Name);
            }

            if (instruction.Operand is MethodReference methodReference) {
                return GetCSharpTypeByName(methodReference.ReturnType.Name);
            }

            //get the type based on the associated stloc instruction
            if (instruction.IsLoadLocalVariable()) {
                var isTypeInferred = localVarTypes.TryGetValue(instruction.Operand.ToString(), out var typeName);

                if (isTypeInferred) {
                    return GetCSharpTypeByName(typeName);
                }
            }

            //checking methodParams.Count is for the situations which there exists a ldarg but in fact it is getting the "this"
            if (instruction.IsLoadArgument() && methodParams.Count > 0) {
                var argIndex = instruction.GetArgumentIndex(methodParams);

                var x = methodParams.ToArray()[argIndex].ParameterType;

                return GetCSharpTypeByName(x.Name);
            }

            if (opCode.Contains("i4", StringComparison.OrdinalIgnoreCase))
                return typeof(int);
            if (opCode.Contains("i8", StringComparison.OrdinalIgnoreCase))
                return typeof(long);
            if (opCode.Contains("r8", StringComparison.OrdinalIgnoreCase))
                return typeof(double);
            if (opCode.Contains("r4", StringComparison.OrdinalIgnoreCase))
                return typeof(float);

            if (instruction.OpCode == OpCodes.Ldlen)
                return typeof(int);

            throw new InstructionDataTypeNotInferredException(instruction);
        }

        private static Type GetCSharpTypeByName(string typeName) {
            switch (typeName) {
                case "Int16":
                    return typeof(short);
                case "Int32":
                case "Boolean":
                    return typeof(int);
                case "Int64":
                    return typeof(long);

                case "Single":
                    return typeof(float);
                case "Double":
                    return typeof(double);
            }

            throw new CSharpDataTypeNotInferredException(typeName);
        }

        public static bool IsStoreLocalVariable(this Instruction instruction) =>
            instruction.OpCode.ToString().StartsWith("stloc", StringComparison.OrdinalIgnoreCase);

        private static bool IsLoadLocalVariable(this Instruction instruction) =>
            instruction.OpCode.ToString().StartsWith("ldloc", StringComparison.OrdinalIgnoreCase) &&
            !instruction.OpCode.ToString().StartsWith("ldloca", StringComparison.OrdinalIgnoreCase);

        public static bool IsStringComparison(this Instruction instruction) {
            var operand = instruction.Operand.ToString();
            return operand.Contains("System.String::Equals") || operand.Contains("System.String::Contains") ||
                   operand.Contains("System.String::StartsWith") || operand.Contains("System.String::EndsWith");
        }

        public static bool IsObjectEqualsComparison(this Instruction instruction) {
            var operand = instruction.Operand.ToString();
            return operand.Contains("System.Object::Equals");
        }
    }
}