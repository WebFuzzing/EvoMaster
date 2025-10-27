using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using EvoMaster.Client.Util;
using EvoMaster.Client.Util.Extensions;
using EvoMaster.Instrumentation_Shared;
using EvoMaster.Instrumentation_Shared.Exceptions;
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
        private MethodReference _completedStatementProbe;
        private MethodReference _enteringStatementProbe;

        private MethodReference _compareAndComputeDistanceProbeForInt;
        private MethodReference _compareAndComputeDistanceProbeForDouble;
        private MethodReference _compareAndComputeDistanceProbeForFloat;
        private MethodReference _compareAndComputeDistanceProbeForLong;
        private MethodReference _compareAndComputeDistanceProbeForShort;

        private MethodReference _computeDistanceForOneArgJumpsProbeForInt;
        private MethodReference _computeDistanceForOneArgJumpsProbeForDouble;
        private MethodReference _computeDistanceForOneArgJumpsProbeForFloat;
        private MethodReference _computeDistanceForOneArgJumpsProbeForLong;
        private MethodReference _computeDistanceForOneArgJumpsProbeForShort;

        private MethodReference _stringEquality;
        private MethodReference _stringCompareWithStringComparison;
        private MethodReference _stringCompare;
        private MethodReference _objectEquals;

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

            _completedStatementProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompletedStatement),
                        new[] {typeof(string), typeof(string), typeof(int), typeof(int)}));

            _enteringStatementProbe =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.EnteringStatement),
                        new[] {typeof(string), typeof(string), typeof(int), typeof(int)}));

            _compareAndComputeDistanceProbeForInt =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(int), typeof(int), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForDouble =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(double), typeof(double), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForFloat =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(float), typeof(float), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForLong =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(long), typeof(long), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));
            _compareAndComputeDistanceProbeForShort =
                module.ImportReference(
                    typeof(Probes).GetMethod(nameof(Probes.CompareAndComputeDistance),
                        new[] {
                            typeof(short), typeof(short), typeof(string), typeof(string), typeof(int),
                            typeof(int)
                        }));

            _computeDistanceForOneArgJumpsProbeForInt = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(int), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForDouble = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(double), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForFloat = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(float), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForLong = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(long), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _computeDistanceForOneArgJumpsProbeForShort = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ComputeDistanceForOneArgJumps),
                    new[] {typeof(short), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _stringEquality = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.StringEquality),
                    new[] {typeof(string), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _stringCompareWithStringComparison = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.StringCompareWithComparison),
                    new[] {
                        typeof(string), typeof(string), typeof(int), typeof(string), typeof(string), typeof(int),
                        typeof(int)
                    }));

            _stringCompare = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.StringCompare),
                    new[] {typeof(string), typeof(string), typeof(string), typeof(string), typeof(int), typeof(int)}));

            _objectEquals = module.ImportReference(
                typeof(Probes).GetMethod(nameof(Probes.ObjectEquality),
                    new[] {typeof(object), typeof(object), typeof(string), typeof(int), typeof(int)}));

            foreach (var type in module.Types.Where(type => type.Name != "<Module>")) {
                _alreadyCompletedPoints.Clear();

                foreach (var method in type.Methods) {
                    if (!method.HasBody) continue;

                    var ilProcessor = method.Body.GetILProcessor();

                    var mapping = method.DebugInformation.GetSequencePointMapping();

                    var lastEnteredLine = 0;
                    var lastEnteredColumn = 0;
                    var lastEnteredLineForBranches = 0;

                    method.Body.SimplifyMacros(); //This is to prevent overflow of short branch opcodes

                    var branchPerLineCounter = 0;

                    var localVariableTypes = new Dictionary<string, string>();

                    for (var i = 0; i < int.MaxValue; i++) {
                        Instruction instruction;
                        try {
                            instruction = method.Body.Instructions[i];
                        }
                        catch (ArgumentOutOfRangeException) {
                            break;
                        }

                        if (instruction.OpCode.Equals(OpCodes.Call) &&
                            instruction.Operand.ToString().Contains("String::op_Equality")) {
                            mapping.TryGetValue(instruction, out var sp);

                            var l = lastEnteredLine;

                            if (sp != null) {
                                l = sp.StartLine;
                            }

                            //if (l != lastEnteredLine) branchPerLineCounter = 0;

                            i = ReplaceStringEquality(instruction, ilProcessor, i, type.Name, l, branchPerLineCounter);
                            branchPerLineCounter++;
                        }

                        if ((instruction.OpCode.Equals(OpCodes.Callvirt) ||
                             instruction.OpCode.Equals(OpCodes.Call)) &&
                            instruction.IsStringComparison()) {
                            mapping.TryGetValue(instruction, out var sp);

                            var l = lastEnteredLine;

                            if (sp != null) {
                                l = sp.StartLine;
                            }

                            var checksComparison = instruction.Operand.ToString().Contains("StringComparison");

                            i = ReplaceStringComparisons(instruction, ilProcessor, i, type.Name, l,
                                branchPerLineCounter,
                                checksComparison);
                            branchPerLineCounter++;
                        }

                        if (instruction.OpCode.Equals(OpCodes.Callvirt) &&
                            instruction.IsObjectEqualsComparison()) {
                            mapping.TryGetValue(instruction, out var sp);

                            var l = lastEnteredLine;

                            if (sp != null) {
                                l = sp.StartLine;
                            }

                            i = ReplaceObjectComparisons(instruction, ilProcessor, i, type.Name, l,
                                branchPerLineCounter);
                            branchPerLineCounter++;
                        }

                        if (instruction.IsStoreLocalVariable()) {
                            switch (instruction.Operand) {
                                case VariableDefinition definition:
                                    localVariableTypes.TryAddOrUpdate(definition.ToString(),
                                        definition.VariableType.Name);
                                    break;
                                case FieldDefinition fieldDefinition:
                                    localVariableTypes.TryAddOrUpdate(fieldDefinition.ToString(),
                                        fieldDefinition.FieldType.Name);
                                    break;
                            }
                        }

                        if (instruction.Next != null && instruction.Next.Next != null) {
                            if (instruction.Next.Next.IsConditionalInstructionWithTwoArgs()) {
                                mapping.TryGetValue(instruction, out var sp);

                                var l = lastEnteredLine;

                                if (sp != null) {
                                    l = sp.StartLine;
                                }

                                i = EnteringBranch(instruction.Next.Next, method, localVariableTypes, i,
                                    type.Name, l,
                                    branchPerLineCounter, true);
                                lastEnteredLineForBranches = l;
                                branchPerLineCounter++;
                            }
                        }

                        //check if it is brtrue or brfalse, provided that it is not added by instruction replacement
                        if (instruction.IsConditionalJumpWithOneArg() &&
                            !(instruction.Previous.OpCode == OpCodes.Call &&
                              instruction.Previous.ToString().Contains(nameof(Probes.CompareAndComputeDistance)))) {
                            mapping.TryGetValue(instruction, out var sp);

                            var l = lastEnteredLine;

                            if (sp != null) {
                                l = sp.StartLine;
                            }

                            i = EnteringBranch(instruction, method, localVariableTypes,
                                i,
                                type.Name, l, branchPerLineCounter, false);
                            lastEnteredLineForBranches = l;
                            branchPerLineCounter++;
                        }

                        mapping.TryGetValue(instruction, out var sequencePoint);

                        // skip non-return instructions which do not have any sequence point info
                        if ((sequencePoint == null || sequencePoint.IsHidden) && instruction.OpCode != OpCodes.Ret) {
                            continue;
                        }

                        if (lastEnteredLine != 0 && lastEnteredColumn != 0) {
                            i = InsertCompletedStatementProbe(instruction.Previous, ilProcessor, i, type.Name,
                                method.Name,
                                lastEnteredLine,
                                lastEnteredColumn);
                        }

                        if (sequencePoint != null) {
                            i = InsertEnteringStatementProbe(instruction, method.Body, ilProcessor,
                                i, type.Name, method.Name, sequencePoint.StartLine, sequencePoint.StartColumn);

                            if (lastEnteredLineForBranches != sequencePoint.StartLine)
                                branchPerLineCounter = 0;

                            lastEnteredColumn = sequencePoint.StartColumn;
                            lastEnteredLine = sequencePoint.StartLine;

                            if (method.Body.Instructions.Last().Equals(instruction) &&
                                instruction.OpCode == OpCodes.Ret) {
                                i = InsertCompletedStatementProbe(instruction, ilProcessor, i, type.Name, method.Name,
                                    lastEnteredLine, lastEnteredColumn);
                            }
                        }
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
            SimpleLogger.Info($"Instrumented File Saved at \"{destination}\"");
        }

        private int InsertCompletedStatementProbe(Instruction instruction,
            ILProcessor ilProcessor, int byteCodeIndex, string className, string methodName, int lineNo, int columnNo) {
            if (_alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            if (instruction.IsUnConditionalJumpOrExitInstruction()) {
                return InsertCompletedStatementProbe(instruction.Previous, ilProcessor, byteCodeIndex,
                    className, methodName, lineNo, columnNo);
            }

            if (instruction.OpCode == OpCodes.Call &&
                instruction.Operand.ToString()!.Contains("EvoMaster.Instrumentation.Probes::ComputingBranchDistance")) {
                return InsertCompletedStatementProbe(instruction.Previous.Previous.Previous.Previous, ilProcessor,
                    byteCodeIndex,
                    className, methodName, lineNo, columnNo);
            }

            //TODO: check if we should register statements or not
            //register all targets(description of all targets, including units, lines and branches)
            _registeredTargets.Classes.Add(ObjectiveNaming.ClassObjectiveName(className));
            _registeredTargets.Lines.Add(ObjectiveNaming.LineObjectiveName(className, lineNo));

            _alreadyCompletedPoints.Add(new CodeCoordinate(lineNo, columnNo));

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, className);
            var methodNameInstruction = ilProcessor.Create(OpCodes.Ldstr, methodName);
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var columnNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, columnNo);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _completedStatementProbe);

            ilProcessor.InsertAfter(instruction, methodCallInstruction);
            byteCodeIndex++;
            ilProcessor.InsertAfter(instruction, columnNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertAfter(instruction, lineNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertAfter(instruction, methodNameInstruction);
            byteCodeIndex++;
            ilProcessor.InsertAfter(instruction, classNameInstruction);
            byteCodeIndex++;

            return byteCodeIndex;
        }

        private int InsertEnteringStatementProbe(Instruction instruction, MethodBody methodBody,
            ILProcessor ilProcessor, int byteCodeIndex, string className, string methodName, int lineNo, int columnNo) {
            //Do not add inserted probe if the statement is already covered by completed probe
            if (_alreadyCompletedPoints.Contains(new CodeCoordinate(lineNo, columnNo))) return byteCodeIndex;

            if (instruction.Previous != null && instruction.OpCode == instruction.Previous.OpCode &&
                instruction.OpCode == OpCodes.Leave) {
                return byteCodeIndex; //todo: shouldn't be skipped
            }

            //Check if the current instruction is the first instruction after a finally block
            var updateHandlerEnd = false;
            ExceptionHandler exceptionHandler = null;
            if (instruction.Previous != null && instruction.Previous.OpCode == OpCodes.Endfinally) {
                exceptionHandler = methodBody.ExceptionHandlers.FirstOrDefault(x => x.HandlerEnd == instruction);

                if (exceptionHandler != null) updateHandlerEnd = true;
            }

            if (!updateHandlerEnd && instruction.Previous != null &&
                instruction.Previous.OpCode == OpCodes.Leave) {
                //skip putting any entering probe right after a leave instruction. This happens at the end of try and also catch blocks.
                //ideally we should put the probe outside the block which ends by leave instruction, but haven't found any solution for this
                //for now, just skip to prevent breaking the code TODO
                return byteCodeIndex;

                // exceptionHandler = methodBody.ExceptionHandlers.FirstOrDefault(x => x.TryEnd == instruction);
                // if (exceptionHandler != null)
                //     return byteCodeIndex; //todo: shouldn't be skipped
            }

            //prevents the probe becoming unreachable at the end of a try block
            if (instruction.Previous != null && instruction.Previous.OpCode == OpCodes.Leave &&
                instruction.Next.OpCode == OpCodes.Leave) {
                return InsertEnteringStatementProbe(instruction.Next, methodBody, ilProcessor, byteCodeIndex, className,
                    methodName,
                    lineNo, columnNo);
            }

            var classNameInstruction = ilProcessor.Create(OpCodes.Ldstr, className);
            var methodNameInstruction = ilProcessor.Create(OpCodes.Ldstr, methodName);
            var lineNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, lineNo);
            var columnNumberInstruction = ilProcessor.Create(OpCodes.Ldc_I4, columnNo);
            var methodCallInstruction = ilProcessor.Create(OpCodes.Call, _enteringStatementProbe);

            ilProcessor.InsertBefore(instruction, classNameInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, methodNameInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, lineNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, columnNumberInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, methodCallInstruction);
            byteCodeIndex++;

            //update the end of finally block to prevent insertion of the probe at the end of finally block which makes it unreachable
            if (exceptionHandler != null)
                exceptionHandler.HandlerEnd = classNameInstruction;

            instruction.UpdateJumpsToTheCurrentInstruction(classNameInstruction, methodBody.Instructions);

            return byteCodeIndex;
        }

        private int EnteringBranch(Instruction branchInstruction, MethodDefinition methodDefinition,
            IReadOnlyDictionary<string, string> localVarTypes, int byteCodeIndex, string className, int lineNo,
            int branchId, bool isBranchInstructionWithTwoArgs) {
            RegisterBranchTarget(branchInstruction.OpCode, className, lineNo, branchId);

            if (isBranchInstructionWithTwoArgs)
                byteCodeIndex =
                    InsertCompareAndComputeDistanceProbe(branchInstruction, methodDefinition, localVarTypes,
                        byteCodeIndex,
                        className,
                        lineNo,
                        branchId);
            else
                byteCodeIndex = InsertComputeDistanceForOneArgJumpsProbe(branchInstruction, methodDefinition,
                    localVarTypes, byteCodeIndex, className, lineNo,
                    branchId); //TODO: do further check for branchId

            return byteCodeIndex;
        }

        private void RegisterBranchTarget(OpCode branchOpCode, string className, int lineNo, int branchId) {
            _registeredTargets.Branches.Add(
                ObjectiveNaming.BranchObjectiveName(className, lineNo, branchId, branchOpCode.ToString(), true));
            _registeredTargets.Branches.Add(
                ObjectiveNaming.BranchObjectiveName(className, lineNo, branchId, branchOpCode.ToString(), false));
        }

        private void RegisterReplacementTarget(string className, int lineNo, int branchId) {
            var idTemplate = ObjectiveNaming.MethodReplacementObjectiveNameTemplate(className, lineNo, branchId);
            _registeredTargets.Branches.Add(
                ObjectiveNaming.MethodReplacementObjectiveNameForBoolean(idTemplate, true));
            _registeredTargets.Branches.Add(
                ObjectiveNaming.MethodReplacementObjectiveNameForBoolean(idTemplate, false));
        }

        //There are comparison instructions such as ceq and branch instructions such as bgt which pop two values from the evaluation stack
        //To calculate branch distance, we have to duplicate those two values and give them to a method to do that
        //Since we couldn't find a way to duplicate two top values on the stack, we added this method which modifies the bytecode to imitate
        //the behaviour of the branch in addition to calculation of the branch distance
        private int InsertCompareAndComputeDistanceProbe(Instruction branchInstruction,
            MethodDefinition methodDefinition, IReadOnlyDictionary<string, string> localVarTypes,
            int byteCodeIndex, string className, int lineNo, int branchId) {
            MethodReference probe = null;

            if (branchInstruction.IsConditionalInstructionWithTwoArgs()) {
                try {
                    var type = branchInstruction.Previous.DetectType(methodDefinition.Parameters, localVarTypes) ??
                               branchInstruction.Previous.Previous.DetectType(methodDefinition.Parameters,
                                   localVarTypes);

                    if (type == typeof(int))
                        probe = _compareAndComputeDistanceProbeForInt;
                    else if (type == typeof(double))
                        probe = _compareAndComputeDistanceProbeForDouble;
                    else if (type == typeof(float))
                        probe = _compareAndComputeDistanceProbeForFloat;
                    else if (type == typeof(long))
                        probe = _compareAndComputeDistanceProbeForLong;
                    else if (type == typeof(short))
                        probe = _compareAndComputeDistanceProbeForShort;
                    //TODO: other types
                }
                catch (InstrumentationException e) {
                    //TODO
                    //skipping undetected data types
                    SimpleLogger.Warn(e.ToString());

                    return byteCodeIndex;
                }
            }

            var ilProcessor = methodDefinition.Body.GetILProcessor();

            //if the instruction is of type comparison (such as ceq and cgt), we imitate it by calling a probe which does exactly what is expected from the instruction
            if (branchInstruction.IsCompareWithTwoArgs()) {
                var res = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex, className,
                    lineNo,
                    branchId,
                    branchInstruction.OpCode);
                byteCodeIndex = res.byteCodeIndex;

                branchInstruction.UpdateJumpsToTheCurrentInstruction(res.firstInstruction,
                    methodDefinition.Body.Instructions);
                ilProcessor.Replace(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
            }
            //the rest are the jump instructions which we should replace them with a compare and jump
            //bgt equals to cgt + brtrue
            else if (branchInstruction.OpCode == OpCodes.Bgt || branchInstruction.OpCode == OpCodes.Bgt_Un) {
                var res = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Cgt);
                byteCodeIndex = res.byteCodeIndex;

                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                branchInstruction.UpdateJumpsToTheCurrentInstruction(res.firstInstruction,
                    methodDefinition.Body.Instructions);

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brtrue, (Instruction) target));
            }
            //beq equals to ceq + brtrue
            else if (branchInstruction.OpCode == OpCodes.Beq) {
                var res = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Ceq);
                byteCodeIndex = res.byteCodeIndex;

                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                branchInstruction.UpdateJumpsToTheCurrentInstruction(res.firstInstruction,
                    methodDefinition.Body.Instructions);

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brtrue, (Instruction) target));
            }
            //bge equals to clt + brfalse
            else if (branchInstruction.OpCode == OpCodes.Bge || branchInstruction.OpCode == OpCodes.Bge_Un) {
                var res = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Clt);
                byteCodeIndex = res.byteCodeIndex;

                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                branchInstruction.UpdateJumpsToTheCurrentInstruction(res.firstInstruction,
                    methodDefinition.Body.Instructions);

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brfalse, (Instruction) target));
            }
            //ble equals to cgt + brfalse
            else if (branchInstruction.OpCode == OpCodes.Ble || branchInstruction.OpCode == OpCodes.Ble_Un) {
                var res = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Cgt);
                byteCodeIndex = res.byteCodeIndex;

                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                branchInstruction.UpdateJumpsToTheCurrentInstruction(res.firstInstruction,
                    methodDefinition.Body.Instructions);

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brfalse, (Instruction) target));
            }
            //blt equals to clt + brtrue
            else if (branchInstruction.OpCode == OpCodes.Blt || branchInstruction.OpCode == OpCodes.Blt_Un) {
                var res = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Clt);
                byteCodeIndex = res.byteCodeIndex;

                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                branchInstruction.UpdateJumpsToTheCurrentInstruction(res.firstInstruction,
                    methodDefinition.Body.Instructions);

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brtrue, (Instruction) target));
            }
            //bne equals to ceq + brfalse
            else if (branchInstruction.OpCode == OpCodes.Bne_Un) {
                var res = InsertValuesBeforeBranchInstruction(branchInstruction, ilProcessor, byteCodeIndex,
                    className,
                    lineNo, branchId,
                    branchInstruction.OpCode, OpCodes.Ceq);
                byteCodeIndex = res.byteCodeIndex;

                ilProcessor.InsertBefore(branchInstruction,
                    ilProcessor.Create(OpCodes.Call, probe));
                byteCodeIndex++;

                branchInstruction.UpdateJumpsToTheCurrentInstruction(res.firstInstruction,
                    methodDefinition.Body.Instructions);

                var target = branchInstruction.Operand;
                ilProcessor.Replace(branchInstruction, ilProcessor.Create(OpCodes.Brfalse, (Instruction) target));
            }

            //instruction.UpdateJumpsToTheCurrentInstruction(classNameInstruction, methodBody.Instructions);

            return byteCodeIndex;
        }

        private (int byteCodeIndex, Instruction firstInstruction) InsertValuesBeforeBranchInstruction(
            Instruction instruction, ILProcessor ilProcessor,
            int byteCodeIndex, string className, int lineNo, int branchId, OpCode originalOpCode,
            OpCode? newOpcode = null) {
            newOpcode ??= originalOpCode;

            var pushOpCodeStringInstruction = ilProcessor.Create(OpCodes.Ldstr, newOpcode.Value.ToString());
            ilProcessor.InsertBefore(instruction, pushOpCodeStringInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, className));
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, lineNo));
            byteCodeIndex++;
            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, branchId));
            byteCodeIndex++;

            return (byteCodeIndex, pushOpCodeStringInstruction);
        }

        private int InsertComputeDistanceForOneArgJumpsProbe(Instruction branchInstruction,
            MethodDefinition methodDefinition, IReadOnlyDictionary<string, string> localVarTypes,
            int byteCodeIndex, string className, int lineNo, int branchId) {
            MethodReference probe = null;
            Type type;

            try {
                type = branchInstruction.Previous.DetectType(methodDefinition.Parameters, localVarTypes);
            }
            catch (InstrumentationException e) {
                //TODO
                //skipping the undetected data types for now
                SimpleLogger.Warn(e.ToString());
                return byteCodeIndex;
            }

            if (type == typeof(int))
                probe = _computeDistanceForOneArgJumpsProbeForInt;
            else if (type == typeof(double))
                probe = _computeDistanceForOneArgJumpsProbeForDouble;
            else if (type == typeof(float))
                probe = _computeDistanceForOneArgJumpsProbeForFloat;
            else if (type == typeof(long))
                probe = _computeDistanceForOneArgJumpsProbeForLong;
            else if (type == typeof(short))
                probe = _computeDistanceForOneArgJumpsProbeForShort;
            // else probe = _computeDistanceForOneArgJumpsProbeForInt;
            //TODO: other types

            var ilProcessor = methodDefinition.Body.GetILProcessor();

            var dupInstruction = ilProcessor.Create(OpCodes.Dup);
            ilProcessor.InsertBefore(branchInstruction, dupInstruction);
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction,
                ilProcessor.Create(OpCodes.Ldstr, branchInstruction.OpCode.ToString()));
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction, ilProcessor.Create(OpCodes.Ldstr, className));
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction, ilProcessor.Create(OpCodes.Ldc_I4, lineNo));
            byteCodeIndex++;
            ilProcessor.InsertBefore(branchInstruction, ilProcessor.Create(OpCodes.Ldc_I4, branchId));
            byteCodeIndex++;

            ilProcessor.InsertBefore(branchInstruction,
                ilProcessor.Create(OpCodes.Call, probe));
            byteCodeIndex++;

            branchInstruction.UpdateJumpsToTheCurrentInstruction(dupInstruction, methodDefinition.Body.Instructions);
            return byteCodeIndex;
        }

        private int ReplaceStringEquality(Instruction instruction, ILProcessor ilProcessor, int byteCodeIndex,
            string className, int lineNo, int branchId) {
            RegisterReplacementTarget(className, lineNo, branchId);

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, className));
            byteCodeIndex++;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, lineNo));
            byteCodeIndex++;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, branchId));
            byteCodeIndex++;

            ilProcessor.Replace(instruction, ilProcessor.Create(OpCodes.Call, _stringEquality));

            return byteCodeIndex;
        }

        private int ReplaceStringComparisons(Instruction instruction, ILProcessor ilProcessor, int byteCodeIndex,
            string className, int lineNo, int branchId, bool checkStringComparison = false) {
            RegisterReplacementTarget(className, lineNo, branchId);

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, instruction.Operand.ToString()));
            byteCodeIndex++;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, className));
            byteCodeIndex++;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, lineNo));
            byteCodeIndex++;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, branchId));
            byteCodeIndex++;

            ilProcessor.Replace(instruction,
                checkStringComparison
                    ? ilProcessor.Create(OpCodes.Call, _stringCompareWithStringComparison)
                    : ilProcessor.Create(OpCodes.Call, _stringCompare));

            return byteCodeIndex;
        }

        private int ReplaceObjectComparisons(Instruction instruction, ILProcessor ilProcessor, int byteCodeIndex,
            string className, int lineNo, int branchId) {
            RegisterReplacementTarget(className, lineNo, branchId);

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldstr, className));
            byteCodeIndex++;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, lineNo));
            byteCodeIndex++;

            ilProcessor.InsertBefore(instruction, ilProcessor.Create(OpCodes.Ldc_I4, branchId));
            byteCodeIndex++;

            ilProcessor.Replace(instruction, ilProcessor.Create(OpCodes.Call, _objectEquals));

            return byteCodeIndex;
        }
    }
}