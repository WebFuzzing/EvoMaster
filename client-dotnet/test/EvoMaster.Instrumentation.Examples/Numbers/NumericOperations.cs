using System;

namespace EvoMaster.Instrumentation.Examples.Numbers {
    public class NumericOperations {
        private int globalVar1 = 7;
        private int globalVar2 = 8;
        private static int staticVar = 7;
        public bool GreaterThan(double a, double b) {
            if (a > b)
                return true;
            else
                return false;
        }
        
        public bool GreaterThan(int a, int b) {
            if (a > b)
                return true;
            else
                return false;
        }

        public int CompareWithLocalVariable(int a) {
            int b = 7;
            if (a == b)
                return 0;
            else if (a > b)
                return 1;
            return -1;
        }
        public int CompareWithGlobalVariable(int a) {
            if (a == globalVar1)
                return 0;
            else if (a > globalVar1)
                return 1;
            return -1;
        }
        
        public int CompareTwoGlobalVariables() {
            if (globalVar2 == globalVar1)
                return 0;
            else if (globalVar2 > globalVar1)
                return 1;
            return -1;
        }
        
        public int CompareWithStaticVariable(int a) {
            if (a == staticVar)
                return 0;
            else if (a > staticVar)
                return 1;
            return -1;
        }
        
        public bool GreaterThan(float a, float b) {
            if (a > b)
                return true;
            else
                return false;
        }
    }
}