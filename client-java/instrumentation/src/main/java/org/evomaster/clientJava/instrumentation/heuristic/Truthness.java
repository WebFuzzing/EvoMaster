package org.evomaster.clientJava.instrumentation.heuristic;

/**
 * 2 values: one for true, and one for false.
 * The values are in [0,1].
 * One of them is necessarily equal to 1 (which
 * represents the actual result of the expression),but not both, ie
 * an expression evaluates to either true or false.
 * The non-1 value represents how close the other option
 * would had been from being taken
 */
public class Truthness {

    private final double ofTrue;
    private final double ofFalse;

    public Truthness(double ofTrue, double ofFalse) {
        if (ofTrue < 0 || ofTrue > 1) {
            throw new IllegalArgumentException("Invalid value for ofTrue: " + ofTrue);
        }
        if (ofFalse < 0 || ofFalse > 1) {
            throw new IllegalArgumentException("Invalid value for ofFalse: " + ofFalse);
        }
        if (ofTrue != 1 && ofFalse != 1) {
            throw new IllegalArgumentException("At least one value should be equal to 1");
        }
        if (ofTrue == 1 && ofFalse == 1) {
            throw new IllegalArgumentException("Values cannot be both equal to 1");
        }
        this.ofTrue = ofTrue;
        this.ofFalse = ofFalse;
    }

    public Truthness invert() {
        return new Truthness(ofFalse, ofTrue);
    }

    public static double normalizeValue(double v) {
        if (v < 0) {
            throw new IllegalArgumentException("Negative value: " + v);
        }

        //normalization function from old ICST/STVR paper
        double normalized = v / (v + 1d);

        assert normalized >= 0 && normalized <= 1;

        return normalized;
    }

    /**
     * @return a value in [0,1], where 1 means the expression evaluated to true
     */
    public double getOfTrue() {
        return ofTrue;
    }

    public boolean isTrue(){
        return ofTrue == 1d;
    }

    /**
     * @return a value in [0,1], where 1 means the expression evaluated to false
     */
    public double getOfFalse() {
        return ofFalse;
    }

    public boolean isFalse(){
        return ofFalse == 1d;
    }
}
