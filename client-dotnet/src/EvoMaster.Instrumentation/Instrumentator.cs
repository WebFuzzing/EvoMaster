using System;
using System.Collections.Generic;
using System.IO;
using EvoMaster.Instrumentation_Shared;
using Mono.Cecil;
using Mono.Cecil.Cil;
using Mono.Cecil.Rocks;

namespace EvoMaster.Instrumentation {
    public class Instrumentator {
        private MethodReference _completedProbe;
        private MethodReference _enteringProbe;

        private readonly RegisteredTargets _registeredTargets = new RegisteredTargets();

        private List<CodeCoordinate> alreadyCompletedPoints = new List<CodeCoordinate>();

        /// <summary>
        /// This method instruments an assembly file and saves its instrumented version in the specified destination directory
        /// </summary>
        /// <param name="assembly">Name of the file to be instrumented</param>
        /// <param name="destination">Directory where the instrumented file should be copied in</param>
        /// <exception cref="ArgumentNullException"></exception>
        public void Instrument(string assembly, string destination) {
            if (string.IsNullOrEmpty(assembly)) throw new ArgumentNullException(assembly);
            if (string.IsNullOrEmpty(destination)) throw new ArgumentNullException(destination);

            //ReadSymbols is set true to enable getting line info
            var module =
                ModuleDefinition.ReadModule(assembly, new ReaderParameters {ReadSymbols = true});

            //TODO: check file extension

            _completedProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompletedStatement),
                        new[] {typeof(string), typeof(int), typeof(int)}));

            _enteringProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.EnteringStatement),
                        new[] {typeof(string), typeof(int), typeof(int)}));

            foreach (var type in module.Types) {
                if (type.Name == "<Module>") continue;

                alreadyCompletedPoints.Clear();

                foreach (var method in type.Methods) {
                    if (!method.HasBody) continue;

                    var ilProcessor = method.Body.GetILProcessor();

                    var mapping = method.DebugInformation.GetSequencePointMapping();

                    var lastEnteredLine = 0;
                    var lastEnteredColumn = 0;

                    method.Body.SimplifyMacros(); //This is to prevent overflow of short branch opcodes

                    for (var i = 0; i < int.MaxValue; i++) {
                        Instruction instruction;
                        try {
                            instruction = method.Body.Instructions[i];
                        }
                        catch (ArgumentOutOfRangeException) {
                            break;
                        }

                        mapping.TryGetValue(instruction, out var sequencePoint);

                        //skip return instructions which do not have any sequence point info
                        if ((sequencePoint == null || sequencePoint.IsHidden) && instruction.OpCode != OpCodes.Ret)
                            continue;

                        if (lastEnteredLine != 0 && lastEnteredColumn != 0) {
                            i = InsertCompletedStatementProbe(instruction.Previous, ilProcessor, i, type.Name,
                                lastEnteredLine, lastEnteredColumn);
                        }

                        if (sequencePoint == null) continue;

                        if (instruction.Previous != null && IsJumpOrExitInstruction(instruction.Previous) &&
                            instruction.Next != null) {
                            i = InsertEnteringStatementProbe(instruction.Next, ilProcessor, i, type.Name,
                                sequencePoint.StartLine,
                                sequencePoint.StartColumn);
                        }
                        else {
                            i = InsertEnteringStatementProbe(instruction, ilProcessor, i, type.Name,
                                sequencePoint.StartLine,
                                sequencePoint.StartColumn);
                        }

                        //To cover cases when ret has line number
                        if (instruction.OpCode == OpCodes.Ret) {
                            i = InsertCompletedStatementProbe(instruction, ilProcessor, i, type.Name,
                                sequencePoint.StartLine, sequencePoint.StartColumn);
                        }

                        lastEnteredColumn = sequencePoint.StartColumn;
                        lastEnteredLine = sequencePoint.StartLine;
                    }

                    method.Body.OptimizeMacros(); //Change back Br opcodes to Br.s if possible
                }
            }

            if (destination.Length > 1 && destination[^1] == '/') {
                destination = destination.Remove(destination.Length - 1, 1);
            }

            //saving unitsInfoDto in a json file
            var json = Newtonsoft.Json.JsonConvert.SerializeObject(_registeredTargets);
            File.WriteAllText("Targets.json", json);

            module.Write($"{destination}/{assembly}");
            Client.Util.SimpleLogger.Info($"Instrumented File Saved at \"{destination}\"");
        }

        private int InsertCompletedStatementProbe(Instruction instruction, ILProcessor ilProcessor,
            int byteCodeIndex, string className, int lineNo, int columnNo) {
            if (alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            if (instruction.Previous != null && IsJumpOrExitInstruction(instruction.Previous)) {
                return InsertCompletedStatementProbe(instruction.Previous, ilProcessor, byteCodeIndex, className,
                    lineNo, columnNo);
            }

            //TODO: check if we should register statements or not
            //register all targets(description of all targets, including units, lines and branches)
            _registeredTargets.Classes.Add(ObjectiveNaming.ClassObjectiveName(className));
            _registeredTargets.Lines.Add(ObjectiveNaming.LineObjectiveName(className, lineNo));

            alreadyCompletedPoints.Add(new CodeCoordinate(lineNo, columnNo));

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, instruction.ToString());
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var columnNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, columnNo);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _completedProbe);

            ilProcessor.InsertBefore(instruction, classNameInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, lineNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, columnNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, methodCallInstruction);
            byteCodeIndex++;

            return byteCodeIndex;
        }

        private int InsertEnteringStatementProbe(Instruction instruction, ILProcessor ilProcessor,
            int byteCodeIndex, string className, int lineNo, int columnNo) {
            //Do not add inserted probe if the statement is already covered by completed probe
            if (alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            if (instruction.Previous != null && IsJumpOrExitInstruction(instruction.Previous)) {
                return InsertEnteringStatementProbe(instruction.Previous, ilProcessor, byteCodeIndex, className,
                    lineNo, columnNo);
            }

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, className);
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var columnNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, columnNo);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _enteringProbe);

            ilProcessor.InsertBefore(instruction, classNameInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, lineNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, columnNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, methodCallInstruction);
            byteCodeIndex++;

            return byteCodeIndex;
        }

        private static bool IsJumpOrExitInstruction(Instruction instruction) =>
            IsJumpInstruction(instruction) || IsExitInstruction(instruction);

        //to detect instructions which jump to another instruction
        private static bool IsJumpInstruction(Instruction instruction) {
            return (instruction.OpCode.ToString().ToLower()[0].Equals('b') && instruction.OpCode != OpCodes.Break &&
                    instruction.OpCode != OpCodes.Box) || (instruction.OpCode == OpCodes.Leave) ||
                   (instruction.OpCode == OpCodes.Leave_S);
        }

        //to detect instruction which deviate the control flow but do not jump to a specific instruction
        private static bool IsExitInstruction(Instruction instruction) {
            return (instruction.OpCode == OpCodes.Throw) ||
                   (instruction.OpCode == OpCodes.Rethrow) || (instruction.OpCode == OpCodes.Endfinally);
        }
    }
}