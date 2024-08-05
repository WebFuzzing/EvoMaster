using System;

namespace EvoMaster.Instrumentation.Examples.Triangle {
    public class TriangleClassificationImpl : ITriangleClassification {
        public Classification Classify(int a, int b, int c) {
            if (a <= 0 || b <= 0 || c <= 0) {
                return Classification.NOT_A_TRIANGLE;
            }

            if (a == b && b == c) {
                return Classification.EQUILATERAL;
            }

            var max = Math.Max(a, Math.Max(b, c));

            if ((max == a && max - b - c >= 0) ||
                (max == b && max - a - c >= 0) ||
                (max == c && max - a - b >= 0)) {
                return Classification.NOT_A_TRIANGLE;
            }

            if (a == b || b == c || a == c) {
                return Classification.ISOSCELES;
            }

            return Classification.SCALENE;
        }
    }
}