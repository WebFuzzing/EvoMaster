# Re-used Code

In some cases, _EvoMaster_ imported (and then modified) code from other open-source projects.
When this happened, it is explicitly stated in the files themselves. 
Still, to give a general overview (and comply to the different licence requirements), they
are listed here:

* _ComputeClassWriter.java_: from [ASM](https://asm.ow2.io/) library. Released under custom INRIA license.

* _RegexEcma262.g4_: from [Antlr examples](https://github.com/antlr/grammars-v4/blob/master/ecmascript/ECMAScript.g4).
  Released under MIT license.
    
* _RegexDistanceUtils.java_: from [EvoSuite](http://www.evosuite.org) unit test generator. 
  Released under GNU Lesser General Public
  
* _RegexDistanceUtilsTest.java_: from [EvoSuite](http://www.evosuite.org) unit test generator. 
  Released under GNU Lesser General Public
  
* The following files in the graphs module were adapted from [EvoSuite](http://www.evosuite.org) 
  unit test generator. Released under GNU Lesser General Public License:
  
  * _AnnotatedLabel.java_: Label wrapper with bytecode instruction metadata.
  * _AnnotatedMethodNode.java_: MethodNode wrapper with additional analysis data.
  * _CFGClassVisitor.java_: ASM class visitor for CFG construction.
  * _CFGMethodVisitor.java_: ASM method visitor for CFG construction.
  * _EvoMasterGraph.java_: Base graph class extending JGraphT
  * _GraphPool.java_: Pool for storing and retrieving CFG/CDG instances.
  * _ActualControlFlowGraph.java_: Reduced CFG containing only branch nodes.
  * _BasicBlock.java_: Represents a basic block in the CFG.
  * _BytecodeInstruction.java_: Represents a single bytecode instruction.
  * _BytecodeInstructionPool.java_: Pool for bytecode instruction instances.
  * _CFGGenerator.java_: Generates CFGs from bytecode.
  * _ControlDependency.java_: Represents a control dependency relationship.
  * _ControlFlowEdge.java_: Edge in the control flow graph.
  * _ControlFlowGraph.java_: Abstract base class for control flow graphs.
  * _EntryBlock.java_: Represents the entry point of a CFG.
  * _ExitBlock.java_: Represents exit points of a CFG.
  * _RawControlFlowGraph.java_: Full CFG with all bytecode instructions.
  * _Branch.java_: Represents a branch in the CFG.
  * _BranchPool.java_: Pool for branch instances.
  * _ControlDependenceGraph.java_: Computes CDG from CFG using dominating frontiers.
  * _DominatorNode.java_: Auxiliary data structure for dominator computation.
  * _DominatorTree.java_: Computes immediate dominators using Lengauer-Tarjan algorithm.