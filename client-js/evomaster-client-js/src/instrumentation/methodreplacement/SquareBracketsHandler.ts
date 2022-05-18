import ObjectiveNaming from "../shared/ObjectiveNaming";
import HeuristicsForBooleans from "../heuristic/HeuristicsForBooleans";
import ExecutionTracer from "../staticstate/ExecutionTracer";
import {StringSpecialization} from "../shared/StringSpecialization";
import {StringSpecializationInfo} from "../shared/StringSpecializationInfo";
import Truthness from "../heuristic/Truthness";
import DistanceHelper, {EqualityAlgorithm} from "../heuristic/DistanceHelper";
import {ReplacementType} from "./ReplacementType";
import CollectionsDistanceUtils from "./CollectionsDistanceUtils";


export default class SquareBracketsHandler{


    /**
     * handle tt for squareBrackets, such as x["foo"]
     * @param fileName
     * @param line
     * @param branchId
     * @param object
     * @param property
     */
    public static squareBracketInMemberExpression(fileName: string, line: number, branchId: number, object: Object, property: any): any {


        HeuristicsForBooleans.clearLastEvaluation();

        const idTemplate = ObjectiveNaming.methodReplacementObjectiveNameTemplate(fileName, line, branchId);

        const objType = typeof object

        if (object === null || object === undefined ||
            // handle only object type
            objType != "object" ||
            // handle taint analysis for null, undefined, number and string
            (property && ((typeof  property) != "number") && ((typeof property) != "string"))){
            /*
                note that null or undefined is allowed, x = {null:"foo", undefined:"bar"}

                for other types of the property,
                    do not compute distance for them yet, eg, object,
                    might support later
             */
            return object[property as keyof object];
        }

        const keys = Object.keys(object);
        if (ExecutionTracer.isTaintInput(property)){
            keys.forEach(s=>{
                ExecutionTracer.addStringSpecialization(
                    property, new StringSpecializationInfo(StringSpecialization.CONSTANT, s)
                )
            })
        }

        const exist = property in object;

        let h: Truthness;
        if (exist){
            h = new Truthness(1, DistanceHelper.H_NOT_NULL);
        } else{
            /*
                we might set limit here for squareBrackets, ie, check all properties in the obj

                we allow the abstract equality comparison to compute the distance
             */
            let d = CollectionsDistanceUtils.getHeuristicToIncludes(keys, property, EqualityAlgorithm.AbstractEquality);
            h = new Truthness(d, 1);
        }

        ExecutionTracer.executedReplacedMethod(idTemplate, ReplacementType.BOOLEAN, h)

        HeuristicsForBooleans.clearLastEvaluation();

        return object[property as keyof object];
    }
}

