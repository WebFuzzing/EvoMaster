

export default class ReplacementFunction{

    public readonly target: Function;
    public readonly replacement: Function;

    constructor(_target: Function, _replacement: Function) {

        // if(typeof _target !== "function"){
        //     throw Error("Target is not a function")
        // }
        // if(typeof _replacement !== "function"){
        //     throw Error("Replacement is not a function")
        // }

        this.target = _target;
        this.replacement = _replacement;
    }


}