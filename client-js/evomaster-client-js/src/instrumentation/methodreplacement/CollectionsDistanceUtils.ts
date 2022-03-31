import DistanceHelper from "../heuristic/DistanceHelper";
import assert from "assert";


export default class CollectionsDistanceUtils {


    /**
     * Compute distance of object from each one of the elements in the collection.
     * But look only up to limit elements.
     * A negative values means look at all elements
     */
    public static getHeuristicToIncludes(c: Array<any>, o: any, limit: number = -1): number {
        // check c is null?

        const result = c.includes(o);
        if (result){
            return 1;
        } else if (c && c.length == 0){
            return DistanceHelper.H_REACHED_BUT_EMPTY;
        } else if (o == null || o == undefined){
            return DistanceHelper.H_NOT_EMPTY;
        } else {

            const base = DistanceHelper.H_NOT_EMPTY;
            let max = base;

            let counter = 0;

            for (let value of c){
                if (counter == limit)
                    break;
                counter++;

                if (value == null)
                    continue;

                let d = DistanceHelper.getDistance(o, value);
                if (d == Number.MAX_VALUE)
                    continue;
                let h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, d)

                if (h > max)
                    max = h;
            }
            assert(max < 1);
            return max;
        }

    }

}
