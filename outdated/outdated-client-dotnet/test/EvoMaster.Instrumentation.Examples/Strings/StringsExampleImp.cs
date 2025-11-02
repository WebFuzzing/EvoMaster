namespace EvoMaster.Instrumentation.Examples.Strings{
    public class StringsExampleImp : IStringsExample{
        
        public bool isFooWithDirectReturn(string value) {
            return "foo".Equals(value);
        }

        // public bool isFooWithDirectReturnUsingReplacement(string value){
        //     return StringClassReplacement.Equals("foo", value, METHOD_REPLACEMENT);
        // }

        public bool isFooWithBooleanCheck(string value) {
            return "foo".Equals(value) == true;
        }

        public bool isFooWithNegatedBooleanCheck(string value) {
            return "foo".Equals(value) != false;
        }

        public bool isFooWithIf(string value) {
            if("foo".Equals(value)){
                return true;
            } else {
                return false;
            }
        }

        public bool isFooWithLocalVariable(string value) {

            bool local = "foo".Equals(value);

            return local;
        }

        public bool isFooWithLocalVariableInIf(string value) {

            bool local;
            if("foo".Equals(value)){
                local = true;
            } else {
                local = false;
            }

            return local;
        }

        public bool isNotFooWithLocalVariable(string value) {

            bool local = ! "foo".Equals(value);

            return local;
        }

        public bool isBarWithPositiveX(string value, int x) {

            bool local = value.Equals("bar") && x > 0;

            return local;
        }
    }
}