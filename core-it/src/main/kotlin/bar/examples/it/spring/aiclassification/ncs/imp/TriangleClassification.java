package bar.examples.it.spring.aiclassification.ncs.imp;

public class TriangleClassification {

    public static int classify(int a, int b, int c) {

        if (a <= 0 || b <= 0 || c <= 0) {
            return 0;
        }

        if (a == b && b == c) {
            return 3;
        }

        int max = Math.max(a, Math.max(b, c));

        if ((max == a && max - b - c >= 0) ||
                (max == b && max - a - c >= 0) ||
                (max == c && max - a - b >= 0)) {
            return 0;
        }

        if (a == b || b == c || a == c) {
            return 2;
        } else {
            return 1;
        }
    }
}
