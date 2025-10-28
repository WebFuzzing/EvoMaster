
namespace EvoMaster.Instrumentation.Examples.Strings{
    public class SwitchString{

        public int switchString(string value){
            var res = 0;
            
            switch (value){
                case "one": 
                    res= 1; 
                    break;
                case "two": 
                    res = 2; 
                    break;
                case "three":
                    res = 3;
                    break;
            }

            return res;
        }

    }
}