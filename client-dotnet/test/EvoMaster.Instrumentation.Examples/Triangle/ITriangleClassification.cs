namespace EvoMaster.Instrumentation.Examples.Triangle {
    public interface ITriangleClassification {
        Classification Classify(int a, int b, int c);
    }

    public enum Classification {
        NOT_A_TRIANGLE,
        ISOSCELES,
        SCALENE,
        EQUILATERAL
    }
}