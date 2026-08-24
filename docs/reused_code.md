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
* _asyncapi/sut/bookworm-rating.yaml_: AsyncAPI description from
  [BookWorm](https://github.com/foxminchan/BookWorm), used unmodified as a test resource of the
  `asyncapi-parser` module. Released under MIT license.

* _asyncapi/sut/everest.yaml_: AsyncAPI description from
  [EVerest](https://github.com/EVerest/EVerest), used unmodified as a test resource of the
  `asyncapi-parser` module. Released under Apache-2.0 license.

* _asyncapi/sut/microcks.yaml_: AsyncAPI description from
  [Microcks](https://github.com/microcks/microcks), used unmodified as a test resource of the
  `asyncapi-parser` module. Released under Apache-2.0 license.

* _asyncapi/sut/openagents-cache.yaml_: AsyncAPI description of the OpenAgents shared-cache API,
  used unmodified as a test resource of the `asyncapi-parser` module. Released under MIT license.
