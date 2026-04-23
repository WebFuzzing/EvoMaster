import MethodReplacementClass from "../MethodReplacementClass";
import ReplacementFunction from "../ReplacementFunction";
import {StringSpecializationInfo} from "../../shared/StringSpecializationInfo";
import {StringSpecialization} from "../../shared/StringSpecialization";
import ExecutionTracer from "../../staticstate/ExecutionTracer";
import Truthness from "../../heuristic/Truthness";
import DistanceHelper, {EqualityAlgorithm} from "../../heuristic/DistanceHelper";
import CollectionsDistanceUtils from "../CollectionsDistanceUtils";
import {ReplacementType} from "../ReplacementType";


export default class ArrayClassReplacement extends MethodReplacementClass{


    getReplacements(): Array<ReplacementFunction> {
        return [
            new ReplacementFunction(Array.prototype.includes, ArrayClassReplacement.includes)
        ];
    }

    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/includes
    static includes = (idTemplate: string, caller:Array<any>, searchElement: any, fromIndex?: any) : boolean => {

        if (caller == null  || (fromIndex && typeof fromIndex  != "number")){
            return caller.includes(searchElement, fromIndex)
        }

        if (ArrayClassReplacement.isNumberOrString(searchElement) && ExecutionTracer.isTaintInput(searchElement)){
            // TODO shall we add all or fromIndex?
            for (let e of caller){
                if (ArrayClassReplacement.isNumberOrString(e)){
                    ExecutionTracer.addStringSpecialization(searchElement,
                        new StringSpecializationInfo(StringSpecialization.CONSTANT, e))
                }
            }
        }

        const result = caller.includes(searchElement, fromIndex)

        if (idTemplate == null)
            return result;

        const candidates = caller.slice(fromIndex)

        let t : Truthness;
        if (result){
            t = new Truthness(1, DistanceHelper.H_NOT_NULL);
        } else {
            // array.includes employs sameValueZero algorithm
            let d = CollectionsDistanceUtils.getHeuristicToIncludes(candidates, searchElement, EqualityAlgorithm.SameValueZero);
            t = new Truthness(d, 1);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, t);

        return result;
    }

    public static isNumberOrString(e : any) :boolean{
        const st = typeof e
        return st == "number" || st == "string"
    }
}