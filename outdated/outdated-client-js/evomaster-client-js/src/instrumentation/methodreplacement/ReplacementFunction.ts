

export default class ReplacementFunction{

    public readonly target: Function;
    public readonly replacement: Function;

    constructor(_target: Function, _replacement: Function) {

        this.target = _target;
        this.replacement = _replacement;
    }


}