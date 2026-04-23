export class TaintInputName {

    /*
        WARNING:
        the naming here has to be kept in sync in ALL implementations of this class,
        including Java, JS and C#
     */

    private static readonly PREFIX = "_EM_";

    private static readonly POSTFIX = "_XYZ_";

    //Pattern.compile("\\Q"+PREFIX+"\\E\\d+\\Q"+POSTFIX+"\\E");
    private static regex = TaintInputName.PREFIX + "\\d+" + TaintInputName.POSTFIX

    private static partialMatch = new RegExp(TaintInputName.regex, "i");

    private static fullMatch = new RegExp("^" + TaintInputName.regex + "$", "i");

    /**
     * Check if a given string value is a tainted value
     */
    static isTaintInput(value: string): boolean {
        if (!value) {
            return false;
        }

        return TaintInputName.fullMatch.test(value);
    }


    static includesTaintInput(value: string): boolean {
        if (!value) {
            return false;
        }

        return TaintInputName.partialMatch.test(value);
    }


    /**
     * Create a tainted value, with the input id being part of it
     */
    static getTaintName(id: number): string {
        if (id < 0) {
            throw Error("Negative id");
        }
        /*
            Note: this is quite simple, we simply add a unique prefix
            and postfix, in lowercase.
            But we would not be able to check if the part of the id was
            modified.
         */
        return TaintInputName.PREFIX + id + TaintInputName.POSTFIX;
    }
}
