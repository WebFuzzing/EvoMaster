import h from "../../../../src/instrumentation/heuristic/HeuristicsForBooleans";
import CollectionsDistanceUtils from "../../../../src/instrumentation/methodreplacement/CollectionsDistanceUtils";
import DistanceHelper from "../../../../dist/instrumentation/heuristic/DistanceHelper";

beforeEach(() => {
    h.clearLastEvaluation();
});


test("test empty", ()=>{

    const x : number[] = [];
    const h = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);

    expect(h).toBe(DistanceHelper.H_REACHED_BUT_EMPTY)
})

test("test single not found", ()=>{

    const x : number[] = [7];
    const h = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);

    expect(h).toBeLessThan(1);
    expect(h).toBeGreaterThan(DistanceHelper.H_REACHED_BUT_EMPTY);
})


test("test increment", ()=>{
    let x : number[] = [7]
    const h7 = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);

    expect(h7).toBeLessThan(1);
    expect(h7).toBeGreaterThan(DistanceHelper.H_REACHED_BUT_EMPTY);

    x.push(10000);
    const h10k = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);
    expect(Math.abs(h10k-h7)).toBeLessThan(0.001);

    x.push(40);
    const h40 = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);
    expect(h40).toBeGreaterThan(h7);
    expect(h40).toBeLessThan(1);

    x.push(42);
    const h42 = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);
    expect(h42).toBeGreaterThan(h40);
    expect(1.0-h42).toBeLessThan(0.000001);

})

test("test only null", ()=>{
    const x : number[] = [null]
    const h = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);
    expect(Math.abs(h-DistanceHelper.H_NOT_EMPTY)).toBeLessThan(0.0001);

})

test("test with null found", ()=>{
    const x : number[] = [null, 5, 2, null, 42, null]
    const h = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);
    expect(1-h).toBeLessThan(0.0001);

})

test("test with null not found", ()=>{
    const x : number[] = [null, 5, 2, null, null]
    const h = CollectionsDistanceUtils.getHeuristicToIncludes(x, 42);
    expect(h).toBeLessThan(1);
    expect(h).toBeGreaterThan(DistanceHelper.H_NOT_EMPTY);
})

test("test with null not found", ()=>{
    const x : number[] = [null]
    const h = CollectionsDistanceUtils.getHeuristicToIncludes(x, null);
    expect(1-h).toBeLessThan(0.0001);
})

test("test not find null", ()=>{
    const x : number[] = [42]
    const h = CollectionsDistanceUtils.getHeuristicToIncludes(x, null);
    expect(h).toBeLessThan(1);
    expect(h >= DistanceHelper.H_NOT_NULL).toBeTruthy();
})