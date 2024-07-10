import assert from "assert";

/**
 * 2 values: one for true, and one for false.
 * The values are in [0,1].
 * One of them is necessarily equal to 1 (which
 * represents the actual result of the expression),but not both, ie
 * an expression evaluates to either true or false.
 * The non-1 value represents how close the other option
 * would had been from being taken
 *
 *
 * NOTE: had to change it compared to JVM, as here we need to handle the
 * case of exceptions, in which a predicate can be neither true nor false
 */

export default class Truthness {

    private readonly ofTrue: number;
    private readonly ofFalse: number;

    constructor(ofTrue: number, ofFalse: number) {
        if (ofTrue < 0 || ofTrue > 1) {
            throw new Error("Invalid value for ofTrue: " + ofTrue);
        }
        if (ofFalse < 0 || ofFalse > 1) {
            throw new Error("Invalid value for ofFalse: " + ofFalse);
        }
        //NOTE: no longer the case
        // if (ofTrue != 1 && ofFalse != 1) {
        //     throw new Error("At least one value should be equal to 1");
        // }
        if (ofTrue == 1 && ofFalse == 1) {
            throw new Error("Values cannot be both equal to 1");
        }
        this.ofTrue = ofTrue;
        this.ofFalse = ofFalse;
    }

    public invert(): Truthness {
        return new Truthness(this.ofFalse, this.ofTrue);
    }

    public rescaleFromMin(min: number) : Truthness {

        return new Truthness(
            this.ofTrue === 1 ? 1 : (min + (1-min)*this.ofTrue),
            this.ofFalse === 1 ? 1 : (min + (1-min)*this.ofFalse)
        );
    }

    /**
     * scales to a positive double value to the [0,1] range
     *
     * @param v a non-negative double value
     * @return
     */
    public static normalizeValue(v: number): number {
        if (v < 0) {
            throw new Error("Negative value: " + v);
        }

        //normalization function from old ICST/STVR paper
        const normalized = v / (v + 1);

        assert(normalized >= 0 && normalized <= 1);

        return normalized;
    }

    /**
     * @return a value in [0,1], where 1 means the expression evaluated to true
     */
    public getOfTrue(): number {
        return this.ofTrue;
    }

    public isTrue(): boolean {
        return this.ofTrue == 1;
    }

    /**
     * @return a value in [0,1], where 1 means the expression evaluated to false
     */
    public getOfFalse(): number {
        return this.ofFalse;
    }

    public isFalse(): boolean {
        return this.ofFalse == 1;
    }
}
