import ReplacementFunction from "./ReplacementFunction";
import StringClassReplacement from "./classes/StringClassReplacement";
import ArrayClassReplacement from "./classes/ArrayClassReplacement";


/*
    For list of global objects, see:

    https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects
 */

export default class ReplacementList{

    /**
     * Key -> target function for which a replacement exists
     *
     * Constraint: key === value.target
     */
    private static readonly replacements = new Map<Function,ReplacementFunction>();


    public static getReplacement(target: Function) : ReplacementFunction{
        return ReplacementList.replacements.get(target);
    }

    public static init() {

        new StringClassReplacement().getReplacements().forEach( f => {
            ReplacementList.replacements.set(f.target, f);
        });

        new ArrayClassReplacement().getReplacements().forEach(f => {
            ReplacementList.replacements.set(f.target, f);
        })
    }
}

ReplacementList.init();