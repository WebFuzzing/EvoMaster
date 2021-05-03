import MethodReplacementClass from "../MethodReplacementClass";
import ReplacementFunction from "../ReplacementFunction";
import Truthness from "../../heuristic/Truthness";
import ExecutionTracer from "../../staticstate/ExecutionTracer";
import DistanceHelper from "../../heuristic/DistanceHelper";
import {ReplacementType} from "../ReplacementType";
import TruthnessUtils from "../../heuristic/TruthnessUtils";
import HeuristicsForBooleans from "../../heuristic/HeuristicsForBooleans";


export default class StringClassReplacement extends MethodReplacementClass{

     getReplacements(): Array<ReplacementFunction> {
        return [
            new ReplacementFunction("".startsWith, StringClassReplacement.startsWith),
            new ReplacementFunction("".endsWith, StringClassReplacement.endsWith),
            new ReplacementFunction("".includes, StringClassReplacement.includes),
            new ReplacementFunction("".indexOf, StringClassReplacement.indexOf),
            new ReplacementFunction("".lastIndexOf, StringClassReplacement.lastIndexOf)
        ];
    }


    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/startsWith

    static startsWith = (idTemplate: string, caller: string, searchString: any, position? : any) : boolean => {

        if(caller===null || caller===undefined || typeof searchString !== "string" || (position && typeof position !== "number")){
            //can't compute distance, so just fallback on standard behavior
            return caller.startsWith(searchString, position);
        }

        const prefix = searchString as string;
        const result = caller.startsWith(prefix, position);

        if (idTemplate == null) {
            return result;
        }

        const pl = prefix.length;
        const toffset = position ?? 0;

        /*
            The penalty when there is a mismatch of lengths/offset
            should be at least pl, as should be always worse than
            when doing "equals" comparisons.
            Furthermore, need to add extra penalty in case string is
            shorter than prefix
         */
        let penalty = pl;
        if (caller.length < pl) {
            penalty += (pl - caller.length);
        }

        let t : Truthness;

        if (toffset < 0) {
            const dist = (-toffset + penalty) * DistanceHelper.MAX_CHAR_DISTANCE;
            t = new Truthness(1 / (1 + dist), 1);
        } else if (toffset > caller.length - pl) {
            const dist = (toffset + penalty) * DistanceHelper.MAX_CHAR_DISTANCE;
            t = new Truthness(1 / (1 + dist), 1);
        } else {
            const len = Math.min(prefix.length, caller.length);
            const sub = caller.substring(toffset, Math.min(toffset + len, caller.length));
            t = TruthnessUtils.getEqualityTruthnessString(sub, prefix)
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        HeuristicsForBooleans.updateLastEvaluation(t);
        return result;
    }

    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/endsWith

    static endsWith = (idTemplate: string, caller: string, searchString: any, length? : any) : boolean => {

        if(caller===null || caller===undefined || typeof searchString !== "string" || (length && typeof length !== "number")){
            //can't compute distance, so just fallback on standard behavior
            return caller.endsWith(searchString, length);
        }

        const n = length ? length : caller.length;
        const startingPoint = n - searchString.length;
        if(n < 0){
            return caller.endsWith(searchString, length);
        }

        return StringClassReplacement.startsWith(idTemplate, caller, searchString, startingPoint);
    }


    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/includes

    static includes = (idTemplate: string, caller: string, searchString: any, position? : any) : boolean => {

        if(caller===null || caller===undefined || typeof searchString !== "string" || (position && typeof position !== "number")){
            //can't compute distance, so just fallback on standard behavior
            return caller.includes(searchString, position);
        }

        const result = caller.includes(searchString, position);

        if (idTemplate == null) {
            return result;
        }

        const n = position ? position : 0;
        const source = caller.substring(n);

        let t: Truthness;

        if (result) {
            t = new Truthness(1, HeuristicsForBooleans.FLAG_NO_EXCEPTION);
        } else if (source.length <= searchString.length) {
            t = TruthnessUtils.getEqualityTruthnessString(source, searchString)
        } else {
            let best = Number.MAX_VALUE;

            for (let i = 0; i < (source.length - searchString.length) + 1; i++) {
                const sub = source.substring(i, i + searchString.length);
                const h = DistanceHelper.getLeftAlignmentDistance(sub, searchString);
                if (h < best) {
                    best = h;
                }
            }
            t = new Truthness(1.0 / (1.0 + best), 1);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);
        HeuristicsForBooleans.updateLastEvaluation(t);
        return result;
    }


    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/indexOf

    static indexOf = (idTemplate: string, caller: string, searchString: any, fromIndex ? : any) : Number => {

         let position = fromIndex
         if(fromIndex >= caller.length){
             position = 0; // damn you JS!!! what an inconsistent behavior compared to "includes"...
         }

         StringClassReplacement.includes(idTemplate, caller, searchString, position);
         return caller.indexOf(searchString, fromIndex);
    }


    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/lastIndexOf

    static lastIndexOf = (idTemplate: string, caller: string, searchString: any, fromIndex ? : any) : Number => {

        let position = fromIndex
        if(fromIndex >= caller.length){
            position = 0;
        }

        StringClassReplacement.includes(idTemplate, caller, searchString, position);
        return caller.lastIndexOf(searchString, fromIndex);
    }


    //TODO Input Tracking

    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/search

    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/match

    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/matchAll

    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/test

    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/exec
}