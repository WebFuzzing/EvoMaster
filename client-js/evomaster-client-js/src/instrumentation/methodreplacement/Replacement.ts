import ReplacementList from "./ReplacementList";


export default class Replacement{


    public static replaceCall(caller: any, targetFunction: Function, ...inputs: any) : any{

        const r = ReplacementList.getReplacement(targetFunction);

        if(r){
            return r.replacement.call(caller, ...inputs);
        } else {
            return targetFunction.call(caller, ...inputs);
        }
    }

}
