using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace EvoMaster.Instrumentation {
    [Serializable]
    public class Action {
        private readonly int _index;

        /**
         * A list (possibly empty) of String values used in the action.
         * This info can be used for different kinds of taint analysis, eg
         * to check how such values are used in the SUT
         */
        private ISet<string> _inputVariables;

        public Action(int index, IEnumerable<string> inputVariables) {
            _index = index;
            //JAVA version: this.inputVariables = Collections.unmodifiableSet(new HashSet<>(inputVariables));
            _inputVariables = ImmutableHashSet.CreateRange(inputVariables);
        }

        public int GetIndex() {
            return _index;
        }

        public ISet<string> GetInputVariables() {
            return _inputVariables;
        }
    }
}