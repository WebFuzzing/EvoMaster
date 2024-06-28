package org.evomaster.client.java.distance.heuristics;

import java.util.Objects;

import static java.lang.String.format;

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

    public static final Double C = DistanceHelper.H_NOT_NULL;
    public static final Truthness TRUE = new Truthness(1d, C);
    public static final Truthness FALSE_BETTER = new Truthness(C + C / 2, 1d);
    public static final Truthness FALSE = new Truthness(C, 1d);

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

    @Override
    public String toString() {
        if(equals(FALSE)) {
            return "false";
        } else if(equals(TRUE)) {
            return "true";
        } else {
            return format("%f (Truthness: (%f, %f))", ofTrue, ofTrue, ofFalse);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Truthness truthness = (Truthness) o;
        return Double.compare(ofTrue, truthness.ofTrue) == 0 && Double.compare(ofFalse, truthness.ofFalse) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ofTrue, ofFalse);
    }
}
