import ReplacementFunction from "./ReplacementFunction";


export default abstract class MethodReplacementClass{

    public abstract getReplacements() : Array<ReplacementFunction>;
}