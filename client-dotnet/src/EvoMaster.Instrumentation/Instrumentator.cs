using System;
using System.Collections.Generic;
using System.IO;
using EvoMaster.Instrumentation_Shared;
using EvoMaster.Instrumentation.StaticState;
using Mono.Cecil;
using Mono.Cecil.Cil;
using Mono.Cecil.Rocks;

namespace EvoMaster.Instrumentation {
    public class Instrumentator {
        private MethodReference _completedProbe;
        private MethodReference _enteringProbe;

        private readonly RegisteredTargets _registeredTargets = new RegisteredTargets();

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
                    typeof(Instrumentator).GetMethod(name: "CompletedStatement",
                        types: new[] {typeof(string), typeof(int), typeof(int)}));

            _enteringProbe =
                module.ImportReference(
                    typeof(Instrumentator).GetMethod(name: "EnteringStatement",
                        types: new[] {typeof(string), typeof(int), typeof(int)}));

            foreach (var type in module.Types) {
                if (type.Name == "<Module>") continue;

                foreach (var method in type.Methods) {
                    if (!method.HasBody) continue;

                    var ilProcessor = method.Body.GetILProcessor();

                    var mapping = method.DebugInformation.GetSequencePointMapping();

                    var lastCompletedLine = 0;
                    var lastCompletedColumn = 0;
                    var alreadyCompletedLines = new List<int>();
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

                        if ((sequencePoint == null || sequencePoint.IsHidden) && instruction.OpCode != OpCodes.Ret)
                            continue;

                        if (sequencePoint != null) {
                            i = InsertEnteringStatementProbe(instruction, ilProcessor, i, type.Name,
                                sequencePoint.StartLine,
                                sequencePoint.StartColumn);
                        }

                        if (lastCompletedLine != 0 && lastCompletedColumn != 0) {
                            //This is to prevent insertion of completed probe after branch opcode
                            //Checking alreadyCompletedLines is in order to control calling Completed probe in loops two times...
                            //However I'm not sure this will work in all cases, if it didn't work, we can try branchInstruction.Operand.Next
                            if (IsBranchInstruction(instruction.Previous) &&
                                !alreadyCompletedLines.Contains(lastCompletedLine)) {
                                i = InsertCompletedStatementProbe(instruction.Previous, ilProcessor, i, type.Name,
                                    lastCompletedLine, lastCompletedColumn);
                            }
                            else {
                                i = InsertCompletedStatementProbe(instruction, ilProcessor, i, type.Name,
                                    lastCompletedLine, lastCompletedColumn);

                                //To cover cases when ret has line number
                                if (instruction.OpCode == OpCodes.Ret) {
                                    if (sequencePoint != null) {
                                        i = InsertCompletedStatementProbe(instruction, ilProcessor, i, type.Name,
                                            sequencePoint.StartLine, sequencePoint.StartColumn);
                                    }
                                }
                            }

                            alreadyCompletedLines.Add(lastCompletedLine);
                        }

                        if (sequencePoint == null || sequencePoint.IsHidden) continue;

                        lastCompletedColumn = sequencePoint.StartColumn;
                        lastCompletedLine = sequencePoint.StartLine;
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
            //TODO: check if we should register statements or not
            //register all targets(description of all targets, including units, lines and branches)
            _registeredTargets.Classes.Add(ObjectiveNaming.ClassObjectiveName(className));
            _registeredTargets.Lines.Add(ObjectiveNaming.LineObjectiveName(className, lineNo));

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, className);
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

        private static bool IsBranchInstruction(Instruction instruction) =>
            instruction.OpCode.ToString().ToLower()[0].Equals('b') && instruction.OpCode != OpCodes.Break &&
            instruction.OpCode != OpCodes.Box;

        //This method is called by the probe inserted after each covered statement in the instrumented SUT
        public static void CompletedStatement(string className, int lineNo, int columnNo) {
            ExecutionTracer.CompletedStatement(className, lineNo, columnNo);
        }

        public static void EnteringStatement(string className, int lineNo, int columnNo) {
            ExecutionTracer.EnteringStatement(className, lineNo, columnNo);
        }
    }
}