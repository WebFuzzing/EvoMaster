using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using EvoMaster.Instrumentation_Shared;
using Mono.Cecil;
using Mono.Cecil.Cil;
using Mono.Cecil.Rocks;

namespace EvoMaster.Instrumentation {
    /// <summary>
    /// This class is responsible for instrumenting c# libraries.
    /// Instrumentation is done by the aid of Mono.Cecil library
    /// It generates a new dll file for the instrumented SUT
    /// </summary>
    public class Instrumentator {
        private MethodReference _completedProbe;
        private MethodReference _enteringProbe;
        private MethodReference _enteringBranchProbe;
        private readonly RegisteredTargets _registeredTargets = new RegisteredTargets();

        private readonly List<CodeCoordinate> _alreadyCompletedPoints = new List<CodeCoordinate>();

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

            _enteringBranchProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.EnteringBranch),
                        new[] {typeof(string), typeof(int), typeof(int)}));

            foreach (var type in module.Types.Where(type => type.Name != "<Module>")) {
                _alreadyCompletedPoints.Clear();

                foreach (var method in type.Methods) {
                    if (!method.HasBody) continue;

                    var ilProcessor = method.Body.GetILProcessor();

                    var mapping = method.DebugInformation.GetSequencePointMapping();

                    var lastEnteredLine = 0;
                    var lastEnteredColumn = 0;

                    method.Body.SimplifyMacros(); //This is to prevent overflow of short branch opcodes

                    var jumpsPerLineCounter = 0;

                    for (var i = 0; i < int.MaxValue; i++) {
                        Instruction instruction;
                        try {
                            instruction = method.Body.Instructions[i];
                        }
                        catch (ArgumentOutOfRangeException) {
                            break;
                        }

                        if (instruction.Next != null && instruction.Next.Next != null) {
                            if (instruction.Next.Next.IsConditionalJumpWithTwoArgs() &&
                                !instruction.IsConditionalJumpWithTwoArgs() &&
                                !instruction.Next.IsConditionalJumpWithTwoArgs()) {
                                mapping.TryGetValue(instruction, out var sp);

                                var l = lastEnteredLine;

                                if (sp != null) {
                                    l = sp.StartLine;
                                }

                                if (l != lastEnteredLine) jumpsPerLineCounter = 0;

                                i = InsertEnteringBranchProbe(instruction, method.Body.Instructions, ilProcessor, i,
                                    type.Name, l,
                                    jumpsPerLineCounter);

                                jumpsPerLineCounter++;
                            }
                        }

                        mapping.TryGetValue(instruction, out var sequencePoint);

                        // //skip return instructions which do not have any sequence point info
                        if ((sequencePoint == null || sequencePoint.IsHidden) && instruction.OpCode != OpCodes.Ret)
                            continue;

                        if (lastEnteredLine != 0 && lastEnteredColumn != 0) {
                            i = InsertCompletedStatementProbe(instruction.Previous,
                                method.Body.Instructions,
                                ilProcessor, i, type.Name,
                                lastEnteredLine, lastEnteredColumn);
                        }

                        if (sequencePoint == null) continue;

                        // if (instruction.Previous != null && instruction.Previous.IsJumpOrExitInstruction() &&
                        //     instruction.Next != null) {
                        //     i = InsertEnteringStatementProbe(instruction.Next, ilProcessor, i, type.Name,
                        //         sequencePoint.StartLine,
                        //         sequencePoint.StartColumn);
                        // }
                        // else {
                        i = InsertEnteringStatementProbe(instruction, method.Body.Instructions, ilProcessor,
                            i, type.Name,
                            sequencePoint.StartLine,
                            sequencePoint.StartColumn);
                        // }

                        // //To cover cases when ret has line number
                        // if (instruction.OpCode == OpCodes.Ret) {
                        //     Instruction probeFirstInstruction;
                        //     i = InsertCompletedStatementProbe(instruction, method.Body.Instructions, ilProcessor, i,
                        //         type.Name,
                        //         sequencePoint.StartLine, sequencePoint.StartColumn);
                        // }

                        lastEnteredColumn = sequencePoint.StartColumn;
                        lastEnteredLine = sequencePoint.StartLine;
                    }

                    method.Body.OptimizeMacros(); //Change back Br opcodes to Br.s if possible
                }
            }

            if (destination.Length > 1 && destination[^1] == '/') {
                destination = destination.Remove(destination.Length - 1, 1);
            }

            //saving unitsInfoDto in a json file as we need to retrieve them later during runtime of the instrumented SUT
            var json = Newtonsoft.Json.JsonConvert.SerializeObject(_registeredTargets);
            File.WriteAllText("Targets.json", json);

            module.Write($"{destination}/{assembly}");
            Client.Util.SimpleLogger.Info($"Instrumented File Saved at \"{destination}\"");
        }

        private int InsertCompletedStatementProbe(Instruction instruction,
            IEnumerable<Instruction> instructions,
            ILProcessor ilProcessor,
            int byteCodeIndex, string className, int lineNo, int columnNo) {
            if (_alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            //to prevent becoming the probe unreachable
            if (instruction.OpCode == OpCodes.Call &&
                instruction.Operand.ToString()!.Contains("EvoMaster.Instrumentation")) {
                return InsertCompletedStatementProbe(instruction.Previous.Previous.Previous, instructions, ilProcessor,
                    byteCodeIndex,
                    className,
                    lineNo, columnNo);
            }

            if (instruction.Previous != null && instruction.Previous.IsUnConditionalJumpOrExitInstruction()) {
                return InsertCompletedStatementProbe(instruction.Previous, instructions, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, columnNo);
            }

            //TODO: check if we should register statements or not
            //register all targets(description of all targets, including units, lines and branches)
            _registeredTargets.Classes.Add(ObjectiveNaming.ClassObjectiveName(className));
            _registeredTargets.Lines.Add(ObjectiveNaming.LineObjectiveName(className, lineNo));

            _alreadyCompletedPoints.Add(new CodeCoordinate(lineNo, columnNo));

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, instruction.ToString());//todo
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

            instruction.UpdateJumpsToTheCurrentInstruction(classNameInstruction, instructions);

            return byteCodeIndex;
        }

        private int InsertEnteringStatementProbe(Instruction instruction, IEnumerable<Instruction> instructions,
            ILProcessor ilProcessor,
            int byteCodeIndex, string className, int lineNo, int columnNo) {
            //Do not add inserted probe if the statement is already covered by completed probe
            if (_alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            //to prevent becoming the probe unreachable
            if (instruction.Previous != null && instruction.Previous.IsUnConditionalJumpOrExitInstruction()) {
                return InsertEnteringStatementProbe(instruction.Previous, instructions, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, columnNo);
            }

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, instruction.ToString());//todo
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

            instruction.UpdateJumpsToTheCurrentInstruction(classNameInstruction, instructions);

            return byteCodeIndex;
        }

        private int InsertEnteringBranchProbe(Instruction instruction, IEnumerable<Instruction> instructions,
            ILProcessor ilProcessor,
            int byteCodeIndex, string className, int lineNo, int branchId) {
            _registeredTargets.Branches.Add(ObjectiveNaming.BranchObjectiveName(className, lineNo, branchId, true));
            _registeredTargets.Branches.Add(ObjectiveNaming.BranchObjectiveName(className, lineNo, branchId, false));

            if (instruction.Previous != null && instruction.Previous.IsUnConditionalJumpOrExitInstruction()) {
                return InsertEnteringBranchProbe(instruction.Previous, instructions, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId);
            }

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, instruction.ToString());//todo
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var columnNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, branchId);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _enteringBranchProbe);

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
    }
}