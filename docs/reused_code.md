# Re-used Code

In some cases, _EvoMaster_ imported (and then modified) code from other open-source projects.
When this happened, it is explicitly stated in the files themselves. 
Still, to give a general overview (and comply to the different licence requirements), they
are listed here:

* _ComputeClassWriter.java_: from [ASM](https://asm.ow2.io/) library. Released under custom INRIA license.

* _RegexEcma262.g4_: from [Antlr examples](https://github.com/antlr/grammars-v4/blob/master/ecmascript/ECMAScript.g4).
  Released under MIT license.
  
* _CustomSummaryGeneratingListener.java_ and _CustomMutableTestExecutionSummary.java_:
  from JUnit 5.
  Released under Eclipse Public License.
  These classes will be removed when this [PR](https://github.com/junit-team/junit5/issues/1947)
  will be part of the next JUnit release.  
  
  