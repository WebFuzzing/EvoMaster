import MethodReplacementClass from "../MethodReplacementClass";
import ReplacementFunction from "../ReplacementFunction";
import Truthness from "../../heuristic/Truthness";
import ExecutionTracer from "../../staticstate/ExecutionTracer";
import DistanceHelper from "../../heuristic/DistanceHelper";


export default class StringClassReplacement extends MethodReplacementClass{

    getReplacements(): Array<ReplacementFunction> {
        return [
            //new ReplacementFunction("".startsWith, StringClassReplacement.startsWith)
        ];
    }


    // static startsWith = (caller: string, searchString: any, position? : any) : boolean => {
    //
    //     if(!caller || typeof searchString !== "string" || (position && typeof position !== "number")){
    //         //can't compute distance, so just fallback on standard behavior
    //         return caller.startsWith(searchString, position);
    //     }
    //
    //     const prefix = searchString as string;
    //     const result = caller.startsWith(prefix, position);
    //
    //     if (idTemplate == null) {
    //         return result;
    //     }
    //
    //     const pl = prefix.length;
    //     const toffset = position ?? 0;
    //
    //     /*
    //         The penalty when there is a mismatch of lengths/offset
    //         should be at least pl, as should be always worse than
    //         when doing "equals" comparisons.
    //         Furthermore, need to add extra penalty in case string is
    //         shorter than prefix
    //      */
    //     let penalty = pl;
    //     if (caller.length < pl) {
    //         penalty += (pl - caller.length);
    //     }
    //
    //     let t : Truthness;
    //
    //     if (toffset < 0) {
    //         const dist = (-toffset + penalty) * DistanceHelper.MAX_CHAR_DISTANCE;
    //         t = new Truthness(1 / (1 + dist), 1);
    //     } else if (toffset > caller.length - pl) {
    //         const dist = (toffset + penalty) * DistanceHelper.MAX_CHAR_DISTANCE;
    //         t = new Truthness(1 / (1 + dist), 1);
    //     } else {
    //         const len = Math.min(prefix.length, caller.length);
    //         const sub = caller.substring(toffset, Math.min(toffset + len, caller.length));
    //         return equals(sub, prefix, idTemplate);
    //     }
    //
    //     ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
    //     return result;
    //
    //     return false;
    // }
}