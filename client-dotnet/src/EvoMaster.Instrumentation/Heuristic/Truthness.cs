using System;

namespace EvoMaster.Instrumentation.Heuristic{
    /**
     * 2 values: one for true, and one for false.
     * The values are in [0,1].
     * One of them is necessarily equal to 1 (which
     * represents the actual result of the expression),but not both, ie
     * an expression evaluates to either true or false.
     * The non-1 value represents how close the other option
     * would had been from being taken
     */
    public class Truthness{
        private readonly double _ofTrue;
        private readonly double _ofFalse;

        public Truthness(double ofTrue, double ofFalse){
            if (ofTrue < 0 || ofTrue > 1){
                throw new ArgumentException("Invalid value for ofTrue: " + ofTrue);
            }

            if (ofFalse < 0 || ofFalse > 1){
                throw new ArgumentException("Invalid value for ofFalse: " + ofFalse);
            }

            if (ofTrue != 1 && ofFalse != 1){
                throw new ArgumentException("At least one value should be equal to 1");
            }

            if (ofTrue == 1 && ofFalse == 1){
                throw new ArgumentException("Values cannot be both equal to 1");
            }

            _ofTrue = ofTrue;
            _ofFalse = ofFalse;
        }

        public Truthness Invert() => new Truthness(_ofFalse, _ofTrue);


        /**
        * @return a value in [0,1], where 1 means the expression evaluated to true
        */
        public double GetOfTrue() => _ofTrue;

        public bool IsTrue() => _ofTrue == 1d;

        /**
     * @return a value in [0,1], where 1 means the expression evaluated to false
     */
        public double GetOfFalse() => _ofFalse;

        public bool IsFalse() => _ofFalse == 1d;
    }
}