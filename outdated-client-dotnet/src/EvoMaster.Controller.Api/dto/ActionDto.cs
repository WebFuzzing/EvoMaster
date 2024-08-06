using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    public class ActionDto {
        /**
         * The index of this action in the test.
         * Eg, in a test with 10 indices, the index would be
         * between 0 and 9
         *
         * NEED to check with Amid about 'int?' for Index
         */
        public int Index { get; set; }

        /**
         * A list (possibly empty) of String values used in the action.
         * This info can be used for different kinds of taint analysis, eg
         * to check how such values are used in the SUT
         */
        public IList<string> InputVariables { get; set; } = new List<string>();
    }
}