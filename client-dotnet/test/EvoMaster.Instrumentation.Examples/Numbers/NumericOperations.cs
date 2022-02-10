using System;

namespace EvoMaster.Instrumentation.Examples.Numbers {
    public class NumericOperations {
        private int globalVar1 = 7;
        private int globalVar2 = 8;
        private static int staticVar = 7;
        
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

        public bool GreaterThan(long a, long b) {
            if (a > b)
                return true;
            else
                return false;
        }

        public bool GreaterThan(short a, short b) {
            if (a > b)
                return true;
            else
                return false;
        }

        public bool CompareWithInfinite(int a, int b) {
            if (a / 0 > b)
                return true;
            else
                return false;
        }

        public int Divide(int a, int b) {
            int result;
            try {
                result = a / b;
            }
            catch (DivideByZeroException e) {
                Console.WriteLine("b shouldn't be zero");
                return int.MaxValue;
            }
            finally {
                Console.WriteLine("reached finally");
            }

            return result;
        }
        
        public double CgtUnDouble(double a, double x){
            if (x < 0.0 || a <= 0.0) throw new Exception("Invalid arguments");
            if (x < (a + 1.0)){
                return x;
            }

            return a;
        }

        public bool AreEqual(double a, double b) {
            return a == b;
        }
        public bool GreaterThan(double a, double b) {
            if (a > b)
                return true;
            else
                return false;
        }
        public bool LowerThan(double a, double b) {
            if (a < b)
                return true;
            else
                return false;
        }
        
        public bool AreEqual(float a, float b) {
            return a == b;
        }
        public bool GreaterThan(float a, float b) {
            if (a > b)
                return true;
            else
                return false;
        }
        public bool LowerThan(float a, float b) {
            if (a < b)
                return true;
            else
                return false;
        }
    }
}