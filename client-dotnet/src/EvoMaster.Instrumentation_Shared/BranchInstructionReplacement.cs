using System;

namespace EvoMaster.Instrumentation_Shared {
    public class BranchInstructionReplacement {
        public static int Ceq(int val1, int val2) {
            Console.WriteLine($"ceq: {val1} & {val2}");//todo
            return val1 == val2 ? 1 : 0;
        }

        public static int Cgt(int val1, int val2) {
            Console.WriteLine($"cgt: {val1} & {val2}");//todo
            return val1 > val2 ? 1 : 0;
        }

        // public static int Compare(int val1, int val2) {
        //     Console.WriteLine($"compare: {val1} & {val2}");
        //     return val1 == val2 ? 1 : 0;
        // }
    }
}