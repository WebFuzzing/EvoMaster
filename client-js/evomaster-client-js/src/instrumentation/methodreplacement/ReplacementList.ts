import ReplacementFunction from "./ReplacementFunction";


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

    }
}

ReplacementList.init();