using System;
using EvoMaster.Instrumentation.Examples.Triangle;

namespace EvoMaster.Instrumentation.Tests {
    public class Program {
        public static void Main(string[] args) {
            TriangleClassificationImpl tc = new TriangleClassificationImpl();
            Console.WriteLine(tc.Classify(-9,2,1));
        }
    }
}